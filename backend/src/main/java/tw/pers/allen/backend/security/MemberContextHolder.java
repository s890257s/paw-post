package tw.pers.allen.backend.security;

import tw.pers.allen.backend.core.exception.UnauthorizedException;

// 使用 ThreadLocal 儲存當前請求的登入會員 ID
public class MemberContextHolder {

    private static final ThreadLocal<Integer> contextHolder = new ThreadLocal<>();

    /** 設定當前會員 ID */
    public static void setMemberId(Integer memberId) {
        contextHolder.set(memberId);
    }

    /** 取得當前會員 ID，允許未登入，可能回傳 null */
    public static Integer getMemberId() {
        return contextHolder.get();
    }

    /** 取得當前會員 ID，未登入則拋出例外 */
    public static Integer requireMemberId() {
        Integer memberId = contextHolder.get();
        if (memberId == null) {
            throw new UnauthorizedException("請先登入");
        }
        return memberId;
    }

    /** 清除記錄以避免記憶體洩漏 */
    public static void clear() {
        contextHolder.remove();
    }
}
