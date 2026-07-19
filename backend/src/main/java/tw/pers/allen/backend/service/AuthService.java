package tw.pers.allen.backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.model.dto.LoginRequestDto;
import tw.pers.allen.backend.model.dto.LoginResponseDto;
import tw.pers.allen.backend.model.entity.Member;
import tw.pers.allen.backend.repository.MemberRepository;
import tw.pers.allen.backend.core.exception.UnauthorizedException;
import tw.pers.allen.backend.core.security.JwtUtil;
import tw.pers.allen.backend.core.security.Role;

// 處理會員登入與身分驗證邏輯
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    // BCrypt 密碼驗證器，由 spring-security-crypto 提供，
    // 與資料庫中既有的 $2a$ 開頭 hash 完全相容
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 驗證登入憑證並回傳 JWT Token
    public LoginResponseDto login(LoginRequestDto request) {
        Member member = memberRepository.findByUsername(request.getUsername());

        if (member == null) {
            throw new UnauthorizedException("帳號或密碼錯誤。");
        }

        // 使用 BCrypt 驗證密碼
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new UnauthorizedException("帳號或密碼錯誤。");
        }

        String token = jwtUtil.generateToken(member.getId(), member.getRole());
        boolean isAdmin = Role.ADMIN.name().equals(member.getRole());
        return new LoginResponseDto(token, member.getUsername(), isAdmin);
    }
}
