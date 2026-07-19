package tw.pers.allen.backend.core.exception;

// 代表「未登入或憑證無效」的例外，對應 HTTP 401 Unauthorized。
// 與 403 的 ForbiddenException 語意不同——那是已登入但權限不足。
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
