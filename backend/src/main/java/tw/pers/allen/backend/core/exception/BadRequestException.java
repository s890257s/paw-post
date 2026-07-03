package tw.pers.allen.backend.core.exception;

// 代表「請求內容不合法」的例外（例如缺少必要欄位），對應 HTTP 400 Bad Request。
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
