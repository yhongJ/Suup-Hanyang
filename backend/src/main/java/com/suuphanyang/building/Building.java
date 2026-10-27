package com.suuphanyang.building;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "building")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 건물 코드. CSV 업로드 시 같은 건물 판별 기준 */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    public Building(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
