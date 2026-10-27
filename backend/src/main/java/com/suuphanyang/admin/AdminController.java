package com.suuphanyang.admin;

import com.suuphanyang.common.ApiException;
import com.suuphanyang.common.Hashes;
import com.suuphanyang.common.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import com.suuphanyang.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 관리자 API. X-Admin-Key 헤더 필요.
 * curl -X POST localhost:8080/api/admin/schedules/import \
 *   -H "X-Admin-Key: change-me" -F "file=@sample/schedules-sample.csv"
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final ScheduleImportService importService;
    private final AppProperties props;
    private final RateLimiter rateLimiter;

    @PostMapping("/schedules/import")
    public ScheduleImportService.ImportResult importSchedules(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(required = false) String semester,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest http) throws IOException {
        // 키 대입 공격 방지
        rateLimiter.check("admin:" + http.getRemoteAddr(), props.auth().ipAdminLimit(), props.auth().rateWindow(),
                "요청이 너무 많아요. 잠시 후 다시 시도하세요.");
        String expected = props.adminKey();
        if (expected == null || expected.isBlank() || !Hashes.safeEquals(adminKey, expected)) {
            throw ApiException.forbidden("관리자 키가 맞지 않아요.");
        }
        return importService.importCsv(file.getInputStream(),
                semester != null ? semester : props.currentSemester());
    }
}
