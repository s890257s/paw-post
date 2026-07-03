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
import tw.pers.allen.backend.model.dto.PostResponseDto;
import tw.pers.allen.backend.model.entity.Member;
import tw.pers.allen.backend.model.entity.Post;
import tw.pers.allen.backend.model.entity.PostLike;
import tw.pers.allen.backend.security.MemberContextHolder;
import tw.pers.allen.backend.model.entity.PostLike;
import tw.pers.allen.backend.repository.MemberRepository;
import tw.pers.allen.backend.repository.PostLikeRepository;
import tw.pers.allen.backend.repository.PostRepository;

// 處理貼文建立、列表查詢與按讚邏輯
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;

    // 取得分頁貼文，並計算按讚狀態
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(Pageable pageable, Integer currentMemberId) {
        Page<Post> postPage = postRepository.findAll(pageable);

        List<PostResponseDto> responses = postPage.getContent().stream().map(post -> {
            PostResponseDto dto = new PostResponseDto();
            dto.setId(post.getId());
            dto.setMemberId(post.getMember().getId());
            dto.setUsername(post.getMember().getUsername());

            if (post.getImageData() != null) {
                // 將圖片轉為 Base64 字串，統一加上 jpeg data URI schema
                String base64Image = Base64.getEncoder().encodeToString(post.getImageData());
                dto.setImageBase64("data:image/jpeg;base64," + base64Image);
            }

            dto.setDescription(post.getDescription());
            dto.setCreatedAt(post.getCreatedAt());

            int likeCount = postLikeRepository.countByPostId(post.getId());
            dto.setLikeCount(likeCount);

            boolean isLiked = false;
            if (currentMemberId != null) {
                isLiked = postLikeRepository.existsByMemberIdAndPostId(currentMemberId, post.getId());
            }
            dto.setIsLiked(isLiked);

            boolean isHidden = Boolean.TRUE.equals(post.getIsHidden());
            dto.setIsHidden(isHidden);

            // 若文章被隱藏，且目前使用者不是管理員，則遮蔽圖片與描述
            if (isHidden && !MemberContextHolder.isAdmin()) {
                dto.setImageBase64(null);
                dto.setDescription(null);
            }

            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, postPage.getTotalElements());
    }

    // 建立新貼文，將上傳圖片轉為 byte 陣列儲存
    @Transactional
    public PostResponseDto createPost(Integer memberId, MultipartFile image, String description) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("找不到會員"));

        Post post = new Post();
        post.setMember(member);
        post.setDescription(description);

        try {
            if (image != null && !image.isEmpty()) {
                post.setImageData(image.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("讀取圖片資料失敗", e);
        }

        Post savedPost = postRepository.save(post);

        PostResponseDto dto = new PostResponseDto();
        dto.setId(savedPost.getId());
        dto.setDescription(savedPost.getDescription());
        if (savedPost.getImageData() != null) {
            String base64Image = Base64.getEncoder().encodeToString(savedPost.getImageData());
            dto.setImageBase64("data:image/jpeg;base64," + base64Image);
        }
        return dto;
    }

    // 新增按讚記錄，並防範重複按讚
    @Transactional
    public void likePost(Integer memberId, Integer postId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("找不到會員"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到貼文"));

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

    // 切換貼文的隱藏狀態 (僅限管理員)
    @Transactional
    public void togglePostHidden(Integer postId, boolean hidden) {
        if (!MemberContextHolder.isAdmin()) {
            throw new tw.pers.allen.backend.core.exception.UnauthorizedException("權限不足，僅限管理員操作。");
        }
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到貼文"));
        
        post.setIsHidden(hidden);
        postRepository.save(post);
    }
}
