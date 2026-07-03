package tw.pers.allen.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.core.exception.ForbiddenException;
import tw.pers.allen.backend.security.LoggedInMemberHolder;
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
        requireAdmin();
        postService.togglePostHidden(id, true);
        return ResponseEntity.ok().build();
    }

    // 解除隱藏貼文
    @PutMapping("/{id}/unhide")
    public ResponseEntity<Void> unhidePost(@PathVariable Integer id) {
        requireAdmin();
        postService.togglePostHidden(id, false);
        return ResponseEntity.ok().build();
    }

    // 已登入但不是管理員 -> 403 Forbidden（不是 401，因為身分是明確的，只是權限不足）
    private void requireAdmin() {
        if (!LoggedInMemberHolder.isAdmin()) {
            throw new ForbiddenException("權限不足，僅限管理員操作。");
        }
    }
}
