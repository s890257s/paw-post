package tw.pers.allen.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import tw.pers.allen.backend.model.dto.PostResponseDto;
import tw.pers.allen.backend.service.PostService;
import tw.pers.allen.backend.security.LoggedInMemberHolder;

// 處理貼文及按讚相關的 API 請求
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 取得貼文列表，支援分頁排序
    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Integer currentMemberId = LoggedInMemberHolder.getMemberId();

        Page<PostResponseDto> responses = postService.getPosts(pageable, currentMemberId);
        return ResponseEntity.ok(responses);
    }

    // 發布新貼文，接收圖片與描述
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestParam MultipartFile image,
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
        Integer memberId = LoggedInMemberHolder.getMemberId();
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        postService.unlikePost(memberId, id);
        return ResponseEntity.ok().build();
    }
}
