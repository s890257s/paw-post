package tw.pers.allen.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.pers.allen.backend.service.PostService;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminController {

    private final PostService postService;

    public AdminController(PostService postService) {
        this.postService = postService;
    }

    // 隱藏貼文
    @PutMapping("/{id}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable Integer id) {
        postService.togglePostHidden(id, true);
        return ResponseEntity.ok().build();
    }

    // 解除隱藏貼文
    @PutMapping("/{id}/unhide")
    public ResponseEntity<Void> unhidePost(@PathVariable Integer id) {
        postService.togglePostHidden(id, false);
        return ResponseEntity.ok().build();
    }
}
