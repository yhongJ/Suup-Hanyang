package com.suuphanyang.status;

import java.time.LocalTime;

/** 수업 회차별 체크 집계 */
public record Tally(int ended, int ongoing, int cancelled, LocalTime lastCheckAt) {
    public static final Tally EMPTY = new Tally(0, 0, 0, null);
}
