package tw.pers.allen.backend.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// 捕捉全域例外並統一回傳格式
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理 UnauthorizedException 例外
     * 統一回傳 HTTP 狀態碼 401 (Unauthorized) 以及包含錯誤詳細訊息的 JSON 回應
     *
     * @param ex 捕獲到的未授權例外
     * @return 包含錯誤訊息的 401 回應實體
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedException(UnauthorizedException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Unauthorized");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
