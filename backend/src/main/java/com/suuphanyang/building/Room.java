package com.suuphanyang.building;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(nullable = false, length = 30)
    private String name;

    /** 평소 잠긴 강의실은 false */
    @Column(name = "is_accessible", nullable = false)
    private boolean accessible = true;

    public Room(Building building, String name) {
        this.building = building;
        this.name = name;
    }
}
