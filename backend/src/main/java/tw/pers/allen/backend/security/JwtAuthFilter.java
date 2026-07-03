package tw.pers.allen.backend.security;

import java.io.IOException;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 自訂過濾器，用於攔截 HTTP 請求並驗證 JWT Token
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 取得並驗證 JWT Token
            String jwt = getJwtFromRequest(request);
            boolean hasValidToken = Strings.isNotBlank(jwt) && jwtUtil.validateToken(jwt);

            if (hasValidToken) {
                // 記錄登入者身分與角色
                Integer memberId = jwtUtil.getMemberIdFromToken(jwt);
                String role = jwtUtil.getRoleFromToken(jwt);
                MemberContextHolder.setContext(memberId, role);

                // 放行請求
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 阻擋未授權請求
            if (requiresAuth(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 3. 公開 API 直接放行
            filterChain.doFilter(request, response);

        } finally {
            // 4. 清理 Context 避免記憶體洩漏與資料污染
            MemberContextHolder.clear();
        }
    }

    // 從 HTTP Header 中擷取 Bearer Token
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (Strings.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 判斷該請求路徑是否需要進行登入驗證
    private boolean requiresAuth(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 永遠放行 CORS 的 Preflight 請求 (OPTIONS)
        if (method.equalsIgnoreCase("OPTIONS")) {
            return false;
        }

        // 登入 API 不需驗證
        if (path.startsWith("/api/login")) {
            return false;
        }

        // 取得貼文列表 API 不強制驗證
        if (path.equals("/api/posts") && method.equalsIgnoreCase("GET")) {
            return false;
        }

        // 發布貼文與按讚相關 API 必須驗證身分
        if (path.startsWith("/api/posts")) {
            return true;
        }

        // 後台管理 API 必須驗證身分
        if (path.startsWith("/api/admin")) {
            return true;
        }

        return false;
    }
}
