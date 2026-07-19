package tw.pers.allen.backend.core.security;

import tw.pers.allen.backend.core.exception.ForbiddenException;
import tw.pers.allen.backend.core.exception.UnauthorizedException;

// 以 ThreadLocal 保存「當前請求」的登入者身分。
//
// 【教學點】運作原理：Servlet 容器用一條執行緒處理一個 HTTP 請求，
// ThreadLocal 讓每條執行緒擁有自己獨立的變數副本，
// 因此同一請求中的任何地方都能取得登入者資訊，且不會與其他請求互相干擾。
//
// 【陷阱】生命週期：JwtAuthFilter 在請求開始時 set、在 finally 中 clear。
// 執行緒會被容器以 Thread Pool 重複使用，若忘記 clear，
// 下一個請求可能讀到上一位使用者的身分。
public class LoggedInMemberHolder {

    private static final ThreadLocal<LoggedInMember> memberHolder = new ThreadLocal<>();

    public static void setLoggedInMember(Integer memberId, String role) {
        memberHolder.set(new LoggedInMember(memberId, role));
    }

    // 取得登入者 id；未登入時回傳 null——適用於「登入與否皆可」的 API
    public static Integer getMemberId() {
        LoggedInMember member = memberHolder.get();
        return member != null ? member.getId() : null;
    }

    // 取得登入者 id；未登入時直接丟出 401——適用於「必須登入」的 API
    public static Integer requireMemberId() {
        LoggedInMember member = memberHolder.get();
        if (member == null || member.getId() == null) {
            throw new UnauthorizedException("請先登入");
        }
        return member.getId();
    }

    public static boolean isAdmin() {
        LoggedInMember member = memberHolder.get();
        return member != null && Role.ADMIN.name().equals(member.getRole());
    }

    // 已登入但不是管理員 -> 403 Forbidden。不是 401，因為身分是明確的，只是權限不足。
    // 訊息不重複寫狀態碼——狀態碼由 HTTP status 表達，寫兩份將來會不一致
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("權限不足，僅限管理員操作。");
        }
    }

    public static void clear() {
        memberHolder.remove();
    }
}
