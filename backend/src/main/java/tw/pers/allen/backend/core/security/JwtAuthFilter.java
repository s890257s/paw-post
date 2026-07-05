package tw.pers.allen.backend.core.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
            boolean hasValidToken = StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt);

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

            // 2. 沒有有效 Token：公開路徑直接放行
            if (isPublic(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 其餘一律阻擋（回傳 JSON 格式，與 GlobalExceptionHandler 的 401 格式一致）
            writeUnauthorizedResponse(response);

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

    // 從 HTTP Header 中擷取 Bearer Token。
    // 字串判空用 Spring 內建的 StringUtils——不要 import 日誌框架 (log4j) 內部的
    // 工具類，那不是公開 API，只是恰好在 classpath 上的傳遞依賴，隨時可能異動。
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 判斷該請求路徑是否為「公開路徑」（不需登入即可存取）。
    //
    // 【安全設計：預設拒絕 (fail-closed)】
    // 這裡採白名單：明確列出公開路徑，「其餘一律要求登入」。
    // 若反過來寫（列出需要保護的路徑、其他放行，即 fail-open），
    // 未來新增的 API 會預設公開，忘記回來補名單就是安全漏洞。
    // Spring Security 慣例中最後一條 .anyRequest().authenticated() 正是同一個道理。
    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // CORS 的 Preflight 請求 (OPTIONS) 永遠放行
        if (method.equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        // 登入 API。用 equals 精確比對——startsWith 會連 /api/loginXyz 都一起放行
        if (path.equals("/api/login")) {
            return true;
        }

        // 取得貼文列表：不需登入（帶有效 Token 時才會計算 isLiked）
        if (path.equals("/api/posts") && method.equalsIgnoreCase("GET")) {
            return true;
        }

        // 本機開發工具（注意：若要部署上線，這兩者不可原樣公開）
        if (path.startsWith("/h2-console")) {
            return true;
        }
        if (path.startsWith("/actuator")) {
            return true;
        }

        // 預設拒絕：所有未列名的路徑（含未來新增的 API）一律要求登入
        return false;
    }
}
