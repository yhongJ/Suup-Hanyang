package com.suuphanyang.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {
    private AuthDtos() {}

    public record SendCodeRequest(
            @NotBlank(message = "이메일을 입력하세요.")
            @Email(message = "이메일 형식이 아니에요.")
            String email) {}

    public record VerifyRequest(
            @NotBlank(message = "이메일을 입력하세요.") String email,
            @Pattern(regexp = "\\d{6}", message = "코드는 6자리 숫자예요.") String code) {}

    public record LoginResponse(String token, String email) {}

    public record MeResponse(Long id, String email) {}
}
