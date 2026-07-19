package tw.pers.allen.backend.core.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 從 JWT 解析出的登入者身分,由 LoggedInMemberHolder 以 ThreadLocal 保存
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoggedInMember {
    private Integer id;
    private String role;
}
