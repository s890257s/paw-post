package tw.pers.allen.backend.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import tw.pers.allen.backend.model.dto.LoginRequestDto;
import tw.pers.allen.backend.model.dto.LoginResponseDto;
import tw.pers.allen.backend.model.entity.Member;
import tw.pers.allen.backend.repository.MemberRepository;
import tw.pers.allen.backend.util.JwtUtil;

// 處理會員登入與身分驗證邏輯
@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    public AuthService(MemberRepository memberRepository, JwtUtil jwtUtil) {
        this.memberRepository = memberRepository;
        this.jwtUtil = jwtUtil;
    }

    // 驗證登入憑證並回傳 JWT Token
    public LoginResponseDto login(LoginRequestDto request) {
        Member member = memberRepository.findByUsername(request.getUsername());

        if (member == null) {
            throw new RuntimeException("Invalid username or password");
        }

        // 使用 BCrypt 驗證密碼
        if (!BCrypt.checkpw(request.getPassword(), member.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(member.getId());
        return new LoginResponseDto(token);
    }
}
