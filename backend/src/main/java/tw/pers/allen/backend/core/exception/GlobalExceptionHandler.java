package tw.pers.allen.backend.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

// 捕捉全域例外並統一回傳格式
//
// 【401 與 403 的差異，面試常考】
// 401 Unauthorized：「你是誰我不知道」—— 未登入、Token 無效或過期。
// 403 Forbidden   ：「你是誰我知道，但你沒有權限」—— 已登入但角色不符。
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 未登入或憑證無效 -> 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedException(UnauthorizedException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    // 已登入但權限不足 -> 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenException(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    // 查無資源 -> 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    // 請求內容不合法 -> 400
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequestException(BadRequestException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // 上傳檔案超過大小上限 -> 400
    // (Spring 預設上限為 1MB；前端會先壓縮圖片，正常流程不會觸發，
    //  但直接呼叫 API 上傳大檔時，沒有這個 handler 就會變成格式不一致的 500)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "圖片檔案過大");
    }

    // 路徑或參數型別錯誤 -> 400 (例如 /api/posts/abc/likes 的 abc 轉不成數字)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "參數格式不正確");
    }

    // 兜底：接住所有未被上面處理的例外，維持統一的錯誤格式
    //
    // 【重要觀念】內部錯誤細節（stack trace、例外訊息）只進伺服器 log，
    // 絕不回傳給前端——那可能洩漏資料庫結構、套件版本等資訊給攻擊者。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        // Spring MVC 框架自身的例外（404 查無路徑、405 方法不支援等）實作了 ErrorResponse，
        // 各自帶有正確的狀態碼，不能一律蓋成 500——沿用它的狀態碼即可
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return buildResponse(status, status.getReasonPhrase(), ex.getMessage());
        }

        log.error("未預期的錯誤", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "系統發生錯誤，請稍後再試");
    }

    // 統一組裝錯誤回應：{ "error": "...", "message": "..." }
    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String error, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
