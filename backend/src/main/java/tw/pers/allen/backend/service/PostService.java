package tw.pers.allen.backend.service;

import java.io.IOException;
import java.util.Base64;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.core.exception.BadRequestException;
import tw.pers.allen.backend.core.exception.ForbiddenException;
import tw.pers.allen.backend.core.exception.NotFoundException;
import tw.pers.allen.backend.model.dto.AdminPostSummaryDto;
import tw.pers.allen.backend.model.dto.PostResponseDto;
import tw.pers.allen.backend.model.entity.Member;
import tw.pers.allen.backend.model.entity.Post;
import tw.pers.allen.backend.model.entity.PostLike;
import tw.pers.allen.backend.repository.MemberRepository;
import tw.pers.allen.backend.repository.PostLikeRepository;
import tw.pers.allen.backend.repository.PostRepository;

// 處理貼文建立、列表查詢與按讚邏輯
// 注意：Service 層不直接讀取 LoggedInMemberHolder，
// 登入者身分一律由 Controller 取出後以「參數」傳入，讓 Service 好測試、責任單純。
@Service
@RequiredArgsConstructor
public class PostService {

    // 圖片格式未知時的預設值
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpeg";

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;

    // 取得分頁貼文，並逐筆計算按讚數與按讚狀態
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(Pageable pageable, Integer currentMemberId, boolean isAdmin) {
        Page<Post> postPage = postRepository.findAll(pageable);

        // Page.map 會逐筆轉換內容，並自動保留總筆數、頁碼等分頁資訊
        return postPage.map(post -> toDto(post, currentMemberId, isAdmin));
    }

    // 將 Post 轉換為 DTO
    //
    // 【進階】N+1 查詢:這個方法會對「每一筆」貼文各查一次
    // 發文者、按讚數、是否按過讚——一頁 10 筆就是 30 條以上的 SQL,
    // 這就是知名的 N+1 問題。教學版刻意保留這個最直觀的寫法;
    // 優化方向見 README 的進階課題,例如 @EntityGraph 預載關聯、IN 批次查詢後在記憶體彙總。
    private PostResponseDto toDto(Post post, Integer currentMemberId, boolean isAdmin) {
        PostResponseDto dto = new PostResponseDto();

        // 1. 映射基本欄位與發文者資訊。member 是 LAZY 關聯,這裡會觸發一次查詢
        dto.setId(post.getId());
        dto.setMemberId(post.getMember().getId());
        dto.setUsername(post.getMember().getUsername());

        // 2. 映射內文與建立時間
        dto.setDescription(post.getDescription());
        dto.setCreatedAt(post.getCreatedAt());

        // 3-1. 查詢該貼文的總按讚數
        dto.setLikeCount(postLikeRepository.countByPostId(post.getId()));

        // 3-2. 查詢目前登入者是否按過讚；未登入時 currentMemberId 為 null,一律 false
        boolean isLiked = currentMemberId != null
                && postLikeRepository.existsByMemberIdAndPostId(currentMemberId, post.getId());
        dto.setIsLiked(isLiked);

        // 4. 商業權限邏輯：處理文章的「隱藏」狀態
        // 使用 Boolean.TRUE.equals 是為了安全的判斷，避免 isHidden 為 null 時報錯
        boolean isHidden = Boolean.TRUE.equals(post.getIsHidden());
        dto.setIsHidden(isHidden);

        // 5. 圖片資料：先判斷隱藏狀態再決定要不要編碼——
        // 整張圖片的 Base64 編碼是很貴的操作，被隱藏的貼文根本不必做
        if (isHidden && !isAdmin) {
            // 文章被隱藏且目前使用者不是管理員：機密內容在後端直接清空，不傳給前端
            dto.setImageBase64(null);
            dto.setDescription(null);
        } else {
            dto.setImageBase64(toDataUri(post));
        }

        return dto;
    }

    // 取得後台管理用的貼文摘要列表。不含圖片,理由見 AdminPostSummaryDto 的說明
    @Transactional(readOnly = true)
    public Page<AdminPostSummaryDto> getAdminPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.map(this::toAdminSummaryDto);
    }

    // 注意：這裡同樣是逐筆查詢 getMember()、countByPostId 的寫法，
    // 與前台的 toDto 是同一個刻意保留的 N+1 教學課題，說明見該處註解。
    private AdminPostSummaryDto toAdminSummaryDto(Post post) {
        AdminPostSummaryDto dto = new AdminPostSummaryDto();
        dto.setId(post.getId());
        dto.setUsername(post.getMember().getUsername());
        dto.setDescription(post.getDescription());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setLikeCount(postLikeRepository.countByPostId(post.getId()));
        dto.setIsHidden(Boolean.TRUE.equals(post.getIsHidden()));
        return dto;
    }

    // 將圖片轉為 data URI 字串，MIME type 以資料庫記錄的實際格式為準
    private String toDataUri(Post post) {
        if (post.getImageData() == null) {
            return null;
        }
        String contentType = post.getImageContentType() != null
                ? post.getImageContentType()
                : DEFAULT_IMAGE_CONTENT_TYPE;
        String base64Image = Base64.getEncoder().encodeToString(post.getImageData());
        return "data:" + contentType + ";base64," + base64Image;
    }

    // 建立新貼文，將上傳圖片轉為 byte 陣列儲存
    @Transactional
    public PostResponseDto createPost(Integer memberId, MultipartFile image, String description) {
        // 廉價的參數驗證放最前面，全部通過了才做較貴的資料庫查詢。
        // 圖片為必填——資料表 image_data 也定義了 NOT NULL——
        // 在這裡先驗證並回 400，而不是讓資料庫丟出難以理解的 500 錯誤
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("請附上圖片");
        }

        // 【安全】不要盲目信任 client:Content-Type 由 client 宣告、可以偽造，
        // 這裡只擋掉明顯不是圖片的請求；嚴謹做法是驗證檔案開頭的檔案簽章，
        // 也就是 magic number——JPEG 開頭固定是 FF D8——留作進階課題。
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("只接受圖片檔案");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("找不到會員"));

        Post post = new Post();
        post.setMember(member);
        post.setDescription(description);

        try {
            post.setImageData(image.getBytes());
            // 圖片內容與它的 MIME type 要一起保存，讀取時才能標示正確的 data URI
            post.setImageContentType(contentType);
        } catch (IOException e) {
            throw new RuntimeException("讀取圖片資料失敗", e);
        }

        Post savedPost = postRepository.save(post);

        PostResponseDto dto = new PostResponseDto();
        dto.setId(savedPost.getId());
        dto.setDescription(savedPost.getDescription());
        dto.setImageBase64(toDataUri(savedPost));
        return dto;
    }

    // 新增按讚記錄
    //
    // 【進階】併發問題與優化方向:
    // 目前是 check-then-act 的寫法——先檢查 exists 再 save——一般情況下可以正常運作。
    // 但在「極高併發」的情境下，例如同一個使用者瞬間送出兩次按讚 API，
    // 兩個請求可能同時通過 exists 檢查，導致第二個請求在 save 時違反資料庫 UNIQUE 約束而引發 Error 500。
    // 作為基礎專案，這樣寫已足夠滿足需求。若未來要優化，可以考慮：
    // 1. 利用資料庫層級的忽略重複寫入語法，如 MySQL 的 INSERT IGNORE。
    // 2. 深入理解 Spring 的 @Transactional 傳播機制與 UnexpectedRollbackException，並以 try-catch 精準處理。
    @Transactional
    public void likePost(Integer memberId, Integer postId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("找不到會員"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("找不到貼文"));

        // 被管理員禁用的貼文不開放按讚——前端雖然隱藏了按鈕，但 API 仍可能被直接呼叫
        if (Boolean.TRUE.equals(post.getIsHidden())) {
            throw new ForbiddenException("此文章已被禁用");
        }

        // 1. 檢查是否已按過讚
        if (postLikeRepository.existsByMemberIdAndPostId(memberId, postId)) {
            return;
        }

        // 2. 新增按讚記錄
        PostLike postLike = new PostLike();
        postLike.setMember(member);
        postLike.setPost(post);
        postLikeRepository.save(postLike);
    }

    // 移除指定的按讚記錄
    @Transactional
    public void unlikePost(Integer memberId, Integer postId) {
        PostLike postLike = postLikeRepository.findByMemberIdAndPostId(memberId, postId);
        if (postLike != null) {
            postLikeRepository.delete(postLike);
        }
    }

    // 切換貼文的隱藏狀態。是否為管理員的授權檢查由 AdminController 負責
    @Transactional
    public void togglePostHidden(Integer postId, boolean hidden) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("找不到貼文"));

        post.setIsHidden(hidden);
        postRepository.save(post);
    }
}
