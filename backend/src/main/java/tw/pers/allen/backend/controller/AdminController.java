package tw.pers.allen.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.core.security.LoggedInMemberHolder;
import tw.pers.allen.backend.model.dto.AdminPostSummaryDto;
import tw.pers.allen.backend.service.PostService;

// 後台管理 API。
// JwtAuthFilter 只驗證「是否登入」、擋 401；「是否為管理員」的授權檢查在這裡進行、回 403。
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminController {

    // 單頁筆數上限。與 PostController 相同的參數防禦，理由見該處說明
    private static final int MAX_PAGE_SIZE = 50;

    private final PostService postService;

    // 後台貼文列表：回傳「不含圖片」的摘要資料，供管理表格使用
    @GetMapping
    public ResponseEntity<Page<AdminPostSummaryDto>> getAdminPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LoggedInMemberHolder.requireAdmin();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // 與前台列表相同：createdAt 降序，id 作為第二排序鍵確保順序穩定
        Sort sort = Sort.by("createdAt").descending().and(Sort.by("id").descending());
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        return ResponseEntity.ok(postService.getAdminPosts(pageable));
    }

    // 隱藏貼文
    @PutMapping("/{id}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable Integer id) {
        LoggedInMemberHolder.requireAdmin();
        postService.togglePostHidden(id, true);
        return ResponseEntity.ok().build();
    }

    // 解除隱藏貼文
    @PutMapping("/{id}/unhide")
    public ResponseEntity<Void> unhidePost(@PathVariable Integer id) {
        LoggedInMemberHolder.requireAdmin();
        postService.togglePostHidden(id, false);
        return ResponseEntity.ok().build();
    }

}
