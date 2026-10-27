package com.suuphanyang.check;

import com.suuphanyang.schedule.CourseSchedule;
import com.suuphanyang.user.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 수업 회차(schedule + classDate)별 사용자 체크 */
@Entity
@Table(name = "room_check")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private CourseSchedule schedule;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 10)
    private CheckType checkType;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    public RoomCheck(AppUser user, CourseSchedule schedule, LocalDate classDate, CheckType checkType, LocalDateTime checkedAt) {
        this.user = user;
        this.schedule = schedule;
        this.classDate = classDate;
        this.checkType = checkType;
        this.checkedAt = checkedAt;
    }
}
