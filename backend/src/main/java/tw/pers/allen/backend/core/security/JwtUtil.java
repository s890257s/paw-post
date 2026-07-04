package tw.pers.allen.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    // 密鑰。根據長度不同，JJWT 會自動決定最安全的 HMAC-SHA 實作：
    // - 32 ~ 47 bytes (256+ bits) : HS256
    // - 48 ~ 63 bytes (384+ bits) : HS384
    // - 64 bytes 以上 (512+ bits) : HS512
    // 註：此密鑰長度為 63 bytes，因此底層實際使用的是 HS384 演算法
    private final String JWT_SECRET = "this_is_a_super_long_secret_key_and_do_not_share_it_with_anyone";

    // 使用 HMAC-SHA 演算法簽章
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    // Token 有效期設定為 24 小時
    private final long EXPIRATION = 60 * 60 * 24 * 1000;

    /**
     * 產生 JWT
     */
    public String generateToken(Integer memberId, String role) {
        return Jwts.builder() // 使用 builder 模式設定 token
                .subject(String.valueOf(memberId)) // 設定 Subject，通常放 memberId
                .claim("role", role) // 存入角色
                .issuedAt(new Date()) // 設定發行時間
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION)) // 設定到期時間
                .signWith(SECRET_KEY) // 使用私鑰簽名
                .compact(); // 產生 token
    }

    /**
     * 解析並驗證 JWT，若驗證失敗則拋出異常
     */
    public Claims getClaims(String token) {
        return Jwts.parser() // 使用 parser() 取得解析器
                .verifyWith(SECRET_KEY) // 設定解密用密鑰
                .build() // 建立解析器
                .parseSignedClaims(token) // 解析 token
                .getPayload(); // 取得解析後結果
    }

    /**
     * 解析 token 並取得 memberId
     */
    public Integer getMemberIdFromToken(String token) {
        return Integer.valueOf(getClaims(token).getSubject());
    }

    /**
     * 解析 token 並取得 role
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 驗證 Token 是否合法
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token); // 若 token 有任何異常，則由 jjwt 套件直接拋出錯誤。
            return true; // 能走到回傳表示驗證通過，token 合法
        } catch (Exception e) {
            return false;
        }
    }

}
