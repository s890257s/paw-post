package tw.pers.allen.backend.security;

import tw.pers.allen.backend.core.exception.UnauthorizedException;

public class LoggedInMemberHolder {

    private static final ThreadLocal<LoggedInMember> memberHolder = new ThreadLocal<>();

    public static void setLoggedInMember(Integer memberId, String role) {
        memberHolder.set(new LoggedInMember(memberId, role));
    }

    public static Integer getMemberId() {
        LoggedInMember member = memberHolder.get();
        return member != null ? member.getId() : null;
    }

    public static Integer requireMemberId() {
        LoggedInMember member = memberHolder.get();
        if (member == null || member.getId() == null) {
            throw new UnauthorizedException("請先登入");
        }
        return member.getId();
    }

    public static boolean isAdmin() {
        LoggedInMember member = memberHolder.get();
        return member != null && "ADMIN".equals(member.getRole());
    }

    public static void clear() {
        memberHolder.remove();
    }
}
