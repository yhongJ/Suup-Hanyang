-- 기본 스키마

CREATE TABLE building (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    CONSTRAINT uk_building_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE room (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT       NOT NULL,
    name        VARCHAR(30)  NOT NULL,
    is_accessible BOOLEAN    NOT NULL DEFAULT TRUE,  -- 잠긴 강의실은 FALSE (accessible은 예약어)
    CONSTRAINT uk_room UNIQUE (building_id, name),
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 수업 시간. 주 2회 수업은 2행
CREATE TABLE course_schedule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT       NOT NULL,
    semester    VARCHAR(10)  NOT NULL,              -- 예: 2026-2
    day_of_week VARCHAR(10)  NOT NULL,              -- MONDAY ~ SUNDAY
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    professor   VARCHAR(50)  NULL,
    CONSTRAINT fk_schedule_room FOREIGN KEY (room_id) REFERENCES room (id),
    INDEX idx_schedule_lookup (semester, day_of_week, room_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE app_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(100) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 이메일 인증 코드 (해시 저장)
CREATE TABLE email_verification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(100) NOT NULL,
    code_hash   VARCHAR(64)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    INDEX idx_verification_email (email, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 로그인 토큰 (해시 저장)
CREATE TABLE auth_token (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL,
    user_id     BIGINT       NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 수업 체크
CREATE TABLE room_check (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    schedule_id BIGINT       NOT NULL,
    class_date  DATE         NOT NULL,               -- 회차 = schedule + 날짜
    check_type  VARCHAR(10)  NOT NULL,               -- ENDED | CANCELLED | ONGOING
    checked_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_check_once UNIQUE (user_id, schedule_id, class_date),
    CONSTRAINT fk_check_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_check_schedule FOREIGN KEY (schedule_id) REFERENCES course_schedule (id),
    INDEX idx_check_session (schedule_id, class_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
