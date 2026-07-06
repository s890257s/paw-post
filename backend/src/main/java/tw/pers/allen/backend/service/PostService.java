package tw.pers.allen.backend.service;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import tw.pers.allen.backend.repository.PostLikeRepository.PostLikeCount;
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

    // 取得分頁貼文，並計算按讚狀態
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(Pageable pageable, Integer currentMemberId, boolean isAdmin) {
        // 優化 1: findAll 已在 Repository 中加上 @EntityGraph 預載 member
        Page<Post> postPage = postRepository.findAll(pageable);

        if (postPage.isEmpty()) {
            return postPage.map(post -> toDto(post, currentMemberId, isAdmin, Collections.emptyMap(), Collections.emptySet()));
        }

        // 收集本頁所有貼文 ID
        List<Integer> postIds = postPage.getContent().stream()
                .map(Post::getId)
                .toList();

        // 批次查詢本頁所有貼文的按讚總數
        Map<Integer, Integer> likeCounts = postLikeRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostLikeCount::getPostId, PostLikeCount::getLikeCount));

        // 優化 3: 批次查詢當前登入者對「本頁貼文」按過讚的 ID 集合
        Set<Integer> likedPostIds = Collections.emptySet();
        if (currentMemberId != null) {
            likedPostIds = postLikeRepository.findLikedPostIdsByMemberAndPosts(currentMemberId, postIds);
        }

        final Set<Integer> finalLikedPostIds = likedPostIds;
        // Page.map 會逐筆轉換內容，並自動保留分頁資訊（總筆數、頁碼等）
        return postPage.map(post -> toDto(post, currentMemberId, isAdmin, likeCounts, finalLikedPostIds));
    }

    // 將 Post 轉換為 DTO（已解決 N+1 問題的優化版本）
    private PostResponseDto toDto(Post post, Integer currentMemberId, boolean isAdmin, 
                                  Map<Integer, Integer> likeCounts, Set<Integer> likedPostIds) {
        PostResponseDto dto = new PostResponseDto();

        // 1. 映射基本關聯資料：貼文 ID 與發文者資訊 (已由 @EntityGraph 預載)
        dto.setId(post.getId());
        dto.setMemberId(post.getMember().getId());
        dto.setUsername(post.getMember().getUsername());

        // 2. 映射內文與建立時間
        dto.setDescription(post.getDescription());
        dto.setCreatedAt(post.getCreatedAt());

        // 3-1. 從批次查詢的 Map 中取得「總按讚數」，若無紀錄則為 0
        int likeCount = likeCounts.getOrDefault(post.getId(), 0);
        dto.setLikeCount(likeCount);

        // 3-2. 從批次查詢的 Set 中判斷該使用者是否已經按過讚
        boolean isLiked = likedPostIds.contains(post.getId());
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

    // 取得後台管理用的貼文摘要列表（不含圖片，理由見 AdminPostSummaryDto 的說明）
    @Transactional(readOnly = true)
    public Page<AdminPostSummaryDto> getAdminPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.map(this::toAdminSummaryDto);
    }

    // 注意：這裡同樣有 getMember()、countByPostId 的 N+1 查詢，
    // 與上方 toDto 是同一個刻意保留的效能反面教材，解法方向見該處註解。
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
        // 圖片為必填（資料表 image_data 也定義了 NOT NULL），
        // 在這裡先驗證並回 400，而不是讓資料庫丟出難以理解的 500 錯誤
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("請附上圖片");
        }

        // 【不要盲目信任 client】Content-Type 由 client 宣告、可以偽造，
        // 這裡只擋掉明顯不是圖片的請求；嚴謹做法是驗證檔案開頭的
        // 檔案簽章 (magic number，如 JPEG 為 FF D8)，留作進階課題。
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
            // 圖片內容與它的格式 (MIME type) 要一起保存，讀取時才能標示正確的 data URI
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
    // 【教學備註：併發問題與優化方向】
    // 目前使用 check-then-act (先檢查 exists 再 save) 的寫法在一般情況下可以正常運作。
    // 但在「極高併發」的情境下（例如同一個使用者瞬間送出兩次按讚 API），
    // 兩個請求可能同時通過 exists 檢查，導致第二個請求在 save 時違反資料庫 UNIQUE 約束而引發 Error 500。
    // 作為基礎專案，這樣寫已足夠滿足需求。若未來要優化，可以考慮：
    // 1. 利用資料庫層級的 INSERT IGNORE (MySQL) 語法。
    // 2. 深入理解 Spring 的 @Transactional 傳播機制與 UnexpectedRollbackException，並以 try-catch 精準處理。
    @Transactional
    public void likePost(Integer memberId, Integer postId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("找不到會員"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("找不到貼文"));

        // 被管理員禁用的貼文不開放按讚（前端雖然隱藏了按鈕，但 API 仍可能被直接呼叫）
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

    // 切換貼文的隱藏狀態（是否為管理員的授權檢查由 AdminController 負責）
    @Transactional
    public void togglePostHidden(Integer postId, boolean hidden) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("找不到貼文"));

        post.setIsHidden(hidden);
        postRepository.save(post);
    }
}
