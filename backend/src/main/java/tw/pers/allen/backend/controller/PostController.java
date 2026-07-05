package tw.pers.allen.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.core.security.LoggedInMemberHolder;
import tw.pers.allen.backend.model.dto.PostResponseDto;
import tw.pers.allen.backend.service.PostService;

// 處理貼文及按讚相關的 API 請求
// 設計原則：Controller 負責從 LoggedInMemberHolder 取得「登入者身分」，
// Service 只透過參數接收身分資訊，專注處理業務邏輯，兩層責任清楚分離。
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    // 單頁筆數上限，防止一次撈取過量資料
    private static final int MAX_PAGE_SIZE = 50;

    private final PostService postService;

    // 取得貼文列表，支援分頁排序
    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 【參數防禦】這是「免登入」的重查詢 API（回應含整頁圖片的 Base64）：
        // - size 不設上限，任何人都能用 size=10000 打出一發極重的查詢
        // - page/size 為負數會讓 PageRequest.of 直接拋例外，變成 500
        // 這裡採「靜默修正」(clamp)，與多數公開 API（如 GitHub）的慣例一致
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // 依規格：貼文固定以 createdAt 降序排列（最新的在最前面）。
        // 再以 id 降序當第二排序鍵：種子資料是同一批寫入的，createdAt 完全相同，
        // 若沒有第二排序鍵，相同時間的貼文順序就不穩定
        Sort sort = Sort.by("createdAt").descending().and(Sort.by("id").descending());
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        // 未登入時 memberId 為 null，isLiked 一律為 false
        Integer currentMemberId = LoggedInMemberHolder.getMemberId();
        boolean isAdmin = LoggedInMemberHolder.isAdmin();

        Page<PostResponseDto> responses = postService.getPosts(pageable, currentMemberId, isAdmin);
        return ResponseEntity.ok(responses);
    }

    // 發布新貼文，接收圖片與描述。
    // image 設為 required = false：若用 Spring 的必填檢查，缺圖時會回框架預設的
    // 英文錯誤格式；改讓請求進到 Service 統一驗證，回應格式才會與其他錯誤一致
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String description) {

        Integer memberId = LoggedInMemberHolder.requireMemberId();

        PostResponseDto response = postService.createPost(memberId, image, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 對指定貼文按讚
    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> likePost(@PathVariable Integer id) {
        Integer memberId = LoggedInMemberHolder.requireMemberId();

        postService.likePost(memberId, id);
        return ResponseEntity.ok().build();
    }

    // 取消對指定貼文的按讚
    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlikePost(@PathVariable Integer id) {
        Integer memberId = LoggedInMemberHolder.requireMemberId();

        postService.unlikePost(memberId, id);
        return ResponseEntity.ok().build();
    }
}
