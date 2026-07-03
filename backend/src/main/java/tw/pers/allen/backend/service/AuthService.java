package tw.pers.allen.backend.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.model.dto.LoginRequestDto;
import tw.pers.allen.backend.model.dto.LoginResponseDto;
import tw.pers.allen.backend.model.entity.Member;
import tw.pers.allen.backend.repository.MemberRepository;
import tw.pers.allen.backend.security.JwtUtil;
import tw.pers.allen.backend.core.exception.UnauthorizedException;

// 處理會員登入與身分驗證邏輯
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    // 驗證登入憑證並回傳 JWT Token
    public LoginResponseDto login(LoginRequestDto request) {
        Member member = memberRepository.findByUsername(request.getUsername());

        if (member == null) {
            throw new UnauthorizedException("帳號或密碼錯誤。");
        }

        // 使用 BCrypt 驗證密碼
        if (!BCrypt.checkpw(request.getPassword(), member.getPassword())) {
            throw new UnauthorizedException("帳號或密碼錯誤。");
        }

        String token = jwtUtil.generateToken(member.getId(), member.getRole());
        boolean isAdmin = "ADMIN".equals(member.getRole());
        return new LoginResponseDto(token, member.getUsername(), isAdmin);
    }
}
