package com.suuphanyang.status;

import com.suuphanyang.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 끝남·휴강 판정 규칙 테스트.
 * DB 불필요. 안 쓰는 저장소는 null.
 * 규칙값은 application.yml 기본값과 동일 (60분, 2명, 10분).
 */
class RoomStatusServiceTest {

    private final AppProperties props = new AppProperties(
            "Asia/Seoul", "2026-2", "test-key", List.of(),
            null, new AppProperties.Rule(60, 2, 10), null);

    private final RoomStatusService service = new RoomStatusService(null, null, null, props);

    private static final LocalTime NOW = LocalTime.of(14, 40);

    @Test
    @DisplayName("끝남 2명, 수업 중 0명, 5분 전 체크 → 끝났을 가능성 높음")
    void endedByTwoRecentChecks() {
        assertTrue(service.isLikelyEnded(new Tally(2, 0, 0, LocalTime.of(14, 35)), NOW));
    }

    @Test
    @DisplayName("끝남 1명뿐 → 아직 판단 안 함")
    void notEnoughChecks() {
        assertFalse(service.isLikelyEnded(new Tally(1, 0, 0, LocalTime.of(14, 35)), NOW));
    }

    @Test
    @DisplayName("끝남 2명, 수업 중 2명 → 반대 제보와 같으면 판단 안 함")
    void tiedWithOngoing() {
        assertFalse(service.isLikelyEnded(new Tally(2, 2, 0, LocalTime.of(14, 35)), NOW));
    }

    @Test
    @DisplayName("마지막 체크가 정확히 10분 전 → 아직 유효")
    void exactlyAtFreshLimit() {
        assertTrue(service.isLikelyEnded(new Tally(3, 0, 0, LocalTime.of(14, 30)), NOW));
    }

    @Test
    @DisplayName("마지막 체크가 11분 전 → 오래된 정보라 무시")
    void staleChecks() {
        assertFalse(service.isLikelyEnded(new Tally(3, 0, 0, LocalTime.of(14, 29)), NOW));
    }

    @Test
    @DisplayName("체크가 하나도 없음 → 판단 안 함")
    void noChecks() {
        assertFalse(service.isLikelyEnded(Tally.EMPTY, NOW));
    }

    @Test
    @DisplayName("휴강 2명, 수업 중 0명 → 휴강일 가능성 높음")
    void cancelledByTwo() {
        assertTrue(service.isLikelyCancelled(new Tally(0, 0, 2, LocalTime.of(13, 5))));
    }

    @Test
    @DisplayName("휴강은 오래전 체크여도 유효 (수업 시간 내내 비어 있으므로)")
    void cancelledStaysValid() {
        assertTrue(service.isLikelyCancelled(new Tally(0, 0, 2, LocalTime.of(9, 0))));
    }

    @Test
    @DisplayName("휴강 2명, 수업 중 2명 → 판단 안 함")
    void cancelledTiedWithOngoing() {
        assertFalse(service.isLikelyCancelled(new Tally(0, 2, 2, LocalTime.of(13, 5))));
    }

    @Test
    @DisplayName("휴강 1명뿐 → 판단 안 함")
    void cancelledNotEnough() {
        assertFalse(service.isLikelyCancelled(new Tally(0, 0, 1, LocalTime.of(13, 5))));
    }
}
