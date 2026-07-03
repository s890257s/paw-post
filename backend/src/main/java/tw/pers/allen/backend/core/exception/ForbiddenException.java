package tw.pers.allen.backend.core.exception;

// 代表「已登入，但沒有權限執行此操作」的例外，對應 HTTP 403 Forbidden。
// 與 UnauthorizedException (401，未登入或憑證無效) 的語意不同，請勿混用。
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
