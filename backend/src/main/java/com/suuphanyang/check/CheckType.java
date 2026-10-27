package com.suuphanyang.check;

public enum CheckType {
    /** 끝남. 종료 N분 전부터 */
    ENDED,
    /** 휴강. 시작부터 */
    CANCELLED,
    /** 수업 중 (반대 제보). 시작부터 */
    ONGOING
}
