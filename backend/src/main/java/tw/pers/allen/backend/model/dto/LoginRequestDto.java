package tw.pers.allen.backend.model.dto;

import lombok.Data;

// 接收登入請求的資料封裝
@Data
public class LoginRequestDto {
    private String username;
    private String password;
}
