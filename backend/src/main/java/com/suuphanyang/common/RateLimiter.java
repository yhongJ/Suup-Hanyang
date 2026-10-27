package com.suuphanyang.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 고정 윈도 요청 제한기 (메모리).
 * 서버 다중화 시 Redis 기반으로 교체 필요.
 */
@Component
public class RateLimiter {

    private record Window(long startMillis, int count) {}

    private static final int CLEANUP_THRESHOLD = 50_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** window 동안 limit회 허용. 허용 시 true */
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long w = window.toMillis();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= w);
        }
        Window updated = windows.compute(key, (k, cur) ->
                (cur == null || now - cur.startMillis() >= w)
                        ? new Window(now, 1)
                        : new Window(cur.startMillis(), cur.count() + 1));
        return updated.count() <= limit;
    }

    /** 초과 시 429 */
    public void check(String key, int limit, Duration window, String message) {
        if (!tryAcquire(key, limit, window)) {
            throw ApiException.tooMany(message);
        }
    }
}
