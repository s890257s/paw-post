package tw.pers.allen.backend.service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.core.exception.BadRequestException;
import tw.pers.allen.backend.core.exception.ForbiddenException;
import tw.pers.allen.backend.core.exception.NotFoundException;
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

    // 取得分頁貼文，並計算按讚狀態
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(Pageable pageable, Integer currentMemberId, boolean isAdmin) {
        Page<Post> postPage = postRepository.findAll(pageable);

        List<PostResponseDto> responses = postPage.getContent().stream()
                .map(post -> toDto(post, currentMemberId, isAdmin))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, postPage.getTotalElements());
    }

    private PostResponseDto toDto(Post post, Integer currentMemberId, boolean isAdmin) {
        PostResponseDto dto = new PostResponseDto();

        // 1. 映射基本關聯資料：貼文 ID 與發文者資訊
        dto.setId(post.getId());
        dto.setMemberId(post.getMember().getId());
        dto.setUsername(post.getMember().getUsername());

        // 2. 處理圖片資料：若有圖片，轉換為 Base64 格式供前端直接渲染
        dto.setImageBase64(toDataUri(post));

        // 3. 映射內文與建立時間
        dto.setDescription(post.getDescription());
        dto.setCreatedAt(post.getCreatedAt());

        // 4. 按讚相關資料 (注意：此處保留了直覺的迴圈內查詢，即常見的 N+1 效能陷阱)
        // 4-1. 向資料庫查詢該篇貼文的「總按讚數」
        int likeCount = postLikeRepository.countByPostId(post.getId());
        dto.setLikeCount(likeCount);

        // 4-2. 如果有使用者登入，向資料庫查詢他「是否已經按過讚」
        boolean isLiked = false;
        if (currentMemberId != null) {
            isLiked = postLikeRepository.existsByMemberIdAndPostId(currentMemberId, post.getId());
        }
        dto.setIsLiked(isLiked);

        // 5. 商業權限邏輯：處理文章的「隱藏」狀態
        // 使用 Boolean.TRUE.equals 是為了安全的判斷，避免 isHidden 為 null 時報錯
        boolean isHidden = Boolean.TRUE.equals(post.getIsHidden());
        dto.setIsHidden(isHidden);

        // 若文章被隱藏，且目前使用者不是管理員，則在後端直接將機密內容清空，不傳給前端
        if (isHidden && !isAdmin) {
            dto.setImageBase64(null);
            dto.setDescription(null);
        }

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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("找不到會員"));

        // 圖片為必填（資料表 image_data 也定義了 NOT NULL），
        // 在這裡先驗證並回 400，而不是讓資料庫丟出難以理解的 500 錯誤
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("請附上圖片");
        }

        Post post = new Post();
        post.setMember(member);
        post.setDescription(description);

        try {
            post.setImageData(image.getBytes());
            // 圖片內容與它的格式 (MIME type) 要一起保存，讀取時才能標示正確的 data URI
            post.setImageContentType(
                    image.getContentType() != null ? image.getContentType() : DEFAULT_IMAGE_CONTENT_TYPE);
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

    // 新增按讚記錄，並防範重複按讚
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

        // 若已按讚，不做任何事情
        if (postLikeRepository.existsByMemberIdAndPostId(memberId, postId)) {
            return;
        }

        // 新增按讚記錄
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
