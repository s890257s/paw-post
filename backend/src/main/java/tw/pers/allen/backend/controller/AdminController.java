package tw.pers.allen.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.core.security.LoggedInMemberHolder;
import tw.pers.allen.backend.service.PostService;

// 後台管理 API。
// JwtAuthFilter 只驗證「是否登入」(401)，「是否為管理員」的授權檢查在這裡進行 (403)。
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminController {

    private final PostService postService;

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
