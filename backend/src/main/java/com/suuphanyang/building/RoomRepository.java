package com.suuphanyang.building;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("select r from Room r join fetch r.building b order by b.name, r.name")
    List<Room> findAllWithBuilding();

    @Query("select r from Room r join fetch r.building b where b.id = :buildingId order by r.name")
    List<Room> findAllWithBuildingByBuildingId(@Param("buildingId") Long buildingId);

    Optional<Room> findByBuildingIdAndName(Long buildingId, String name);
}
