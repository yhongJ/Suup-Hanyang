package com.suuphanyang.schedule;

import com.suuphanyang.building.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** 수업 1회분 (과목·요일·시간·강의실) */
@Entity
@Table(name = "course_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 10)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "course_name", nullable = false, length = 100)
    private String courseName;

    @Column(length = 50)
    private String professor;

    public CourseSchedule(Room room, String semester, DayOfWeek dayOfWeek, LocalTime startTime,
                          LocalTime endTime, String courseName, String professor) {
        this.room = room;
        this.semester = semester;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courseName = courseName;
        this.professor = professor;
    }

    /** 수업 시간 안이면 true (시작 포함, 종료 미포함) */
    public boolean isRunningAt(LocalTime time) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }
}
