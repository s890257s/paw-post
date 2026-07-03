package tw.pers.allen.backend.core.exception;

// 代表「查無資源」的例外，對應 HTTP 404 Not Found。
// 若直接丟 RuntimeException 會被當成伺服器錯誤回 500，狀態碼語意就錯了。
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
