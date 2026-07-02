package tw.pers.allen.backend.core.exception;

/**
 * 未授權例外
 * 用於表示當前請求缺少有效的驗證憑證或身分驗證失敗
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * 建立帶有錯誤訊息的未授權例外
     *
     * @param message 錯誤詳細訊息
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
