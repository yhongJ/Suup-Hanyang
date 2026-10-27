package com.suuphanyang.check;

import com.suuphanyang.auth.AuthInterceptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    public record CheckRequest(
            @NotNull(message = "수업을 골라 주세요.") Long scheduleId,
            @NotNull(message = "체크 종류가 없어요.") CheckType type) {}

    /** 체크 등록. 인증 필요 */
    @PostMapping("/api/checks")
    public CheckService.CheckResponse check(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                            @Valid @RequestBody CheckRequest req) {
        return checkService.check(userId, req.scheduleId(), req.type());
    }
}
