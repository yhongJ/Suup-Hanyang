package com.suuphanyang.auth;

import com.suuphanyang.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Bearer 토큰 확인 후 userId를 request attribute에 넣음.
 * 컨트롤러에서 @RequestAttribute(AuthInterceptor.USER_ID)로 사용.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID = "userId";
    public static final String TOKEN = "authToken";

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) return true;

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw ApiException.unauthorized("학교 이메일 인증이 필요해요.");
        }
        String token = header.substring(7).trim();
        Long userId = authService.resolveUserId(token)
                .orElseThrow(() -> ApiException.unauthorized("로그인이 만료됐어요. 다시 인증하세요."));
        request.setAttribute(USER_ID, userId);
        request.setAttribute(TOKEN, token);
        return true;
    }
}
