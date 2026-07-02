package tw.pers.allen.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.model.dto.LoginRequestDto;
import tw.pers.allen.backend.model.dto.LoginResponseDto;
import tw.pers.allen.backend.service.AuthService;

// 處理登入相關的 API 請求
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 驗證登入憑證並發放 Token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
