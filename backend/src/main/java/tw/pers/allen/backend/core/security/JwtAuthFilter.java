package tw.pers.allen.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.util.Strings;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 自訂過濾器，用於攔截 HTTP 請求並驗證 JWT Token
// @Order 設定為「最高優先權 + 1」：CORS Filter (最高優先權) 必須先執行，
// 否則此 Filter 直接回傳的 401 會缺少 CORS header，瀏覽器會擋下回應，
// 前端就只能看到「網路錯誤」而看不到真正的 401。
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
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
                // 記錄登入者身分與角色。
                // 【為什麼用 ThreadLocal？】Servlet 容器以「一條執行緒處理一個請求」，
                // 把身分存進 ThreadLocal，同一請求後續的 Controller / Service
                // 不需要層層傳參數也能取得登入者資訊。
                Integer memberId = jwtUtil.getMemberIdFromToken(jwt);
                String role = jwtUtil.getRoleFromToken(jwt);
                LoggedInMemberHolder.setLoggedInMember(memberId, role);

                // 放行請求
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 阻擋未授權請求（回傳 JSON 格式，與 GlobalExceptionHandler 的 401 格式一致）
            if (requiresAuth(request)) {
                writeUnauthorizedResponse(response);
                return;
            }

            // 3. 公開 API 直接放行
            filterChain.doFilter(request, response);

        } finally {
            // 4. 清理 ThreadLocal。
            // 【為什麼一定要 clear？】容器的執行緒是「重複使用」的（Thread Pool），
            // 若不清除，下一個被分配到同一條執行緒的請求會讀到上一位使用者的身分，
            // 造成嚴重的資料污染；同時也會造成記憶體洩漏。
            LoggedInMemberHolder.clear();
        }
    }

    // 回傳 401 與 JSON 錯誤訊息
    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"請先登入\"}");
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

        // 後台管理 API 必須驗證身分（是否為管理員的「授權」檢查在 AdminController）
        if (path.startsWith("/api/admin")) {
            return true;
        }

        return false;
    }
}
