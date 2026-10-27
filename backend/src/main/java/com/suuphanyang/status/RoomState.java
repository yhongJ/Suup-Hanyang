package com.suuphanyang.status;

public enum RoomState {
    /** 시간표상 수업 없음 */
    FREE,
    /** 수업 중 */
    BUSY,
    /** 수업 중이나 끝남 체크 다수 */
    LIKELY_ENDED,
    /** 수업 중이나 휴강 체크 다수 */
    LIKELY_CANCELLED
}
