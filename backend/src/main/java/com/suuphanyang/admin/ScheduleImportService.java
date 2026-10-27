package com.suuphanyang.admin;

import com.suuphanyang.building.Building;
import com.suuphanyang.building.BuildingRepository;
import com.suuphanyang.building.Room;
import com.suuphanyang.building.RoomRepository;
import com.suuphanyang.schedule.CourseSchedule;
import com.suuphanyang.schedule.CourseScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 시간표 CSV를 DB에 저장함.
 *
 * 헤더: building_code,building_name,room,day,start,end,course,professor
 * 예:   ENG1,제1공학관,301호,MON,09:00,10:15,자료구조,
 * day: MON~SUN 또는 월~일. 따옴표 값 미지원.
 * 같은 강의실·학기·요일·시작시각이 있으면 건너뜀.
 */
@Service
@RequiredArgsConstructor
public class ScheduleImportService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final CourseScheduleRepository scheduleRepository;

    public record ImportResult(int buildingsCreated, int roomsCreated, int schedulesCreated,
                               int skipped, List<String> errors) {}

    private static final Map<String, DayOfWeek> DAYS = Map.ofEntries(
            Map.entry("MON", DayOfWeek.MONDAY), Map.entry("월", DayOfWeek.MONDAY),
            Map.entry("TUE", DayOfWeek.TUESDAY), Map.entry("화", DayOfWeek.TUESDAY),
            Map.entry("WED", DayOfWeek.WEDNESDAY), Map.entry("수", DayOfWeek.WEDNESDAY),
            Map.entry("THU", DayOfWeek.THURSDAY), Map.entry("목", DayOfWeek.THURSDAY),
            Map.entry("FRI", DayOfWeek.FRIDAY), Map.entry("금", DayOfWeek.FRIDAY),
            Map.entry("SAT", DayOfWeek.SATURDAY), Map.entry("토", DayOfWeek.SATURDAY),
            Map.entry("SUN", DayOfWeek.SUNDAY), Map.entry("일", DayOfWeek.SUNDAY));

    /** 24:00은 23:59로 저장함 (LocalTime 제약) */
    private static LocalTime parseTime(String raw) {
        String t = raw.trim();
        if (t.equals("24:00")) return LocalTime.of(23, 59);
        return LocalTime.parse(t);
    }

    @Transactional
    public ImportResult importCsv(InputStream in, String semester) throws IOException {
        int buildingsCreated = 0, roomsCreated = 0, schedulesCreated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) continue; // 헤더
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;

                String[] c = line.split(",", -1);
                if (c.length < 7) {
                    errors.add(lineNo + "행: 칸 수가 부족해요.");
                    continue;
                }
                try {
                    String bCode = c[0].trim(), bName = c[1].trim(), roomName = c[2].trim();
                    DayOfWeek day = DAYS.get(c[3].trim().toUpperCase());
                    if (day == null) throw new IllegalArgumentException("요일을 알 수 없어요: " + c[3]);
                    LocalTime start = parseTime(c[4]);
                    LocalTime end = parseTime(c[5]);
                    if (!end.isAfter(start)) throw new IllegalArgumentException("종료가 시작보다 빨라요.");
                    String course = c[6].trim();
                    String professor = c.length > 7 && !c[7].isBlank() ? c[7].trim() : null;

                    Building building = buildingRepository.findByCode(bCode).orElse(null);
                    if (building == null) {
                        building = buildingRepository.save(new Building(bCode, bName));
                        buildingsCreated++;
                    }
                    Room room = roomRepository.findByBuildingIdAndName(building.getId(), roomName).orElse(null);
                    if (room == null) {
                        room = roomRepository.save(new Room(building, roomName));
                        roomsCreated++;
                    }
                    if (scheduleRepository.existsByRoomIdAndSemesterAndDayOfWeekAndStartTime(room.getId(), semester, day, start)) {
                        skipped++;
                        continue;
                    }
                    scheduleRepository.save(new CourseSchedule(room, semester, day, start, end, course, professor));
                    schedulesCreated++;
                } catch (RuntimeException e) {
                    errors.add(lineNo + "행: " + e.getMessage());
                }
            }
        }
        return new ImportResult(buildingsCreated, roomsCreated, schedulesCreated, skipped, errors);
    }
}
