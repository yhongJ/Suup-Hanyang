package com.suuphanyang.building;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;

    public record RoomItem(Long id, String name) {}
    public record BuildingResponse(Long id, String code, String name, List<RoomItem> rooms) {}

    /** 건물·강의실 목록 */
    @GetMapping("/api/buildings")
    @Transactional(readOnly = true)
    public List<BuildingResponse> buildings() {
        Map<Long, List<RoomItem>> roomsByBuilding = roomRepository.findAllWithBuilding().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBuilding().getId(),
                        Collectors.mapping(r -> new RoomItem(r.getId(), r.getName()), Collectors.toList())));

        return buildingRepository.findAllByOrderByNameAsc().stream()
                .map(b -> new BuildingResponse(b.getId(), b.getCode(), b.getName(),
                        roomsByBuilding.getOrDefault(b.getId(), List.of())))
                .toList();
    }
}
