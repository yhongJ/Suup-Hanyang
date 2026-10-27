package com.suuphanyang.status;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record RoomStatusResponse(
        Long roomId,
        String roomName,
        Long buildingId,
        String buildingName,
        RoomState state,
        CurrentClass current,                               // 진행 중 수업. 없으면 null
        @JsonFormat(pattern = "HH:mm") LocalTime nextStart  // 다음 수업 시작. 없으면 null
) {
    public record CurrentClass(
            Long scheduleId,
            String courseName,
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end,
            int ended,
            int ongoing,
            int cancelled,
            @JsonFormat(pattern = "HH:mm") LocalTime lastCheckAt) {}
}
