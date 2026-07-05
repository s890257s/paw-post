package tw.pers.allen.backend.service;

import java.io.IOException;
import java.util.Base64;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
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

        // Page.map 會逐筆轉換內容，並自動保留分頁資訊（總筆數、頁碼等）
        return postPage.map(post -> toDto(post, currentMemberId, isAdmin));
    }

    // 【教學點：這個方法裡藏了「三個」N+1 查詢陷阱，是刻意保留的效能反面教材】
    // 每轉換「一篇」貼文，就會多發出下列查詢：
    //   (1) post.getMember()               —— LAZY 關聯，載入發文者
    //   (2) countByPostId                  —— 查詢總按讚數
    //   (3) existsByMemberIdAndPostId     —— 登入時查詢是否按過讚
    // 一頁 10 篇貼文 = 最多 30 次額外查詢！
    // 解法方向（留作進階練習）：@EntityGraph 或 join fetch 預載關聯、
    // 或先收集本頁所有貼文 id，用 IN 一次批次查回，再於記憶體中組裝。
    private PostResponseDto toDto(Post post, Integer currentMemberId, boolean isAdmin) {
        PostResponseDto dto = new PostResponseDto();

        // 1. 映射基本關聯資料：貼文 ID 與發文者資訊 (觸發上述 N+1 之 (1))
        dto.setId(post.getId());
        dto.setMemberId(post.getMember().getId());
        dto.setUsername(post.getMember().getUsername());

        // 2. 映射內文與建立時間
        dto.setDescription(post.getDescription());
        dto.setCreatedAt(post.getCreatedAt());

        // 3-1. 向資料庫查詢該篇貼文的「總按讚數」(觸發上述 N+1 之 (2))
        int likeCount = postLikeRepository.countByPostId(post.getId());
        dto.setLikeCount(likeCount);

        // 3-2. 如果有使用者登入，向資料庫查詢他「是否已經按過讚」(觸發上述 N+1 之 (3))
        boolean isLiked = false;
        if (currentMemberId != null) {
            isLiked = postLikeRepository.existsByMemberIdAndPostId(currentMemberId, post.getId());
        }
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

    // 新增按讚記錄，並防範重複按讚。
    //
    // 【為什麼這個方法「刻意」不加 @Transactional？】
    // 下方 catch 了 UNIQUE 約束的違反例外並當成功處理。
    // 若整個方法包在同一個交易裡，repository 內層拋出例外的當下，
    // 交易就已被 Spring 標記為 rollback-only——即使我們 catch 住例外，
    // 方法結束提交交易時仍會失敗 (UnexpectedRollbackException)。
    // 此方法的各個資料庫操作彼此不需要原子性，
    // 讓它們各自跑在 repository 自己的交易裡，catch 例外才能真正生效。
    public void likePost(Integer memberId, Integer postId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("找不到會員"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("找不到貼文"));

        // 被管理員禁用的貼文不開放按讚（前端雖然隱藏了按鈕，但 API 仍可能被直接呼叫）
        if (Boolean.TRUE.equals(post.getIsHidden())) {
            throw new ForbiddenException("此文章已被禁用");
        }

        // 若已按讚，不做任何事情（冪等：目標狀態已達成就視為成功）
        if (postLikeRepository.existsByMemberIdAndPostId(memberId, postId)) {
            return;
        }

        // 新增按讚記錄
        PostLike postLike = new PostLike();
        postLike.setMember(member);
        postLike.setPost(post);

        try {
            postLikeRepository.save(postLike);
        } catch (DataIntegrityViolationException e) {
            // 【教學點：check-then-act 不是原子操作】
            // 上面的 exists 檢查與這裡的 insert 之間，可能有另一個相同的請求
            // 同時通過了檢查，此時第二筆 insert 會撞上資料庫的
            // UNIQUE(member_id, post_id) 約束並拋出這個例外。
            // 應用層的 exists 檢查只是「快速路徑」，資料庫約束才是防重複的真正防線。
            // 目標狀態（已按讚）已經達成，因此當作成功、靜默返回。
        }
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
