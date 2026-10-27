package com.suuphanyang.status;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomStatusController {

    private final RoomStatusService statusService;
    private final Clock clock;

    /**
     * 강의실별 상태. date/time 생략 시 현재 기준.
     * 예: /api/rooms/status?date=2026-09-23&time=14:10&buildingId=1
     */
    @GetMapping("/api/rooms/status")
    public List<RoomStatusResponse> statuses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime time,
            @RequestParam(required = false) Long buildingId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return statusService.statuses(
                date != null ? date : now.toLocalDate(),
                time != null ? time : now.toLocalTime(), // 초 단위 유지 (방금 체크 누락 방지)
                buildingId);
    }
}
