package tw.pers.allen.backend.security;

import tw.pers.allen.backend.core.exception.UnauthorizedException;

// 使用 ThreadLocal 儲存當前請求的登入會員 ID
public class MemberContextHolder {

    private static final ThreadLocal<Integer> memberIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> roleHolder = new ThreadLocal<>();

    /** 設定當前會員 ID 與 Role */
    public static void setContext(Integer memberId, String role) {
        memberIdHolder.set(memberId);
        roleHolder.set(role);
    }

    /** 取得當前會員 ID，允許未登入，可能回傳 null */
    public static Integer getMemberId() {
        return memberIdHolder.get();
    }

    /** 取得當前會員 ID，未登入則拋出例外 */
    public static Integer requireMemberId() {
        Integer memberId = memberIdHolder.get();
        if (memberId == null) {
            throw new UnauthorizedException("請先登入");
        }
        return memberId;
    }

    /** 判斷當前使用者是否為管理員 */
    public static boolean isAdmin() {
        return "ADMIN".equals(roleHolder.get());
    }

    /** 清除記錄以避免記憶體洩漏 */
    public static void clear() {
        memberIdHolder.remove();
        roleHolder.remove();
    }
}
