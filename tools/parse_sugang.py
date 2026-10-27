#!/usr/bin/env python3
"""
수강편람 저장 페이지(html) → 시간표 CSV 변환

사용법: python3 parse_sugang.py <html 폴더> [출력 csv]
- table#gdMain만 읽음. 교강사·학번 등 개인정보는 읽지 않음
"""
import csv
import re
import sys
from collections import defaultdict
from html.parser import HTMLParser
from pathlib import Path

# 읽을 칸 (td id)
WANTED = {"suupNo2", "gwamokNm", "suupTimes", "suupRoomNms"}
TIME_RE = re.compile(r"([월화수목금토일])\((\d{2}:\d{2})-(\d{2}:\d{2})\)")


class GridParser(HTMLParser):
    """table#gdMain 행을 {td id: 텍스트}로 수집"""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.rows, self.in_grid, self.in_body = [], False, False
        self.row, self.cell, self.buf = None, None, []

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "table" and a.get("id") == "gdMain":
            self.in_grid = True
        elif self.in_grid and tag == "tbody":
            self.in_body = True
        elif self.in_body and tag == "tr":
            self.row = {}
        elif self.row is not None and tag == "td":
            self.cell, self.buf = a.get("id"), []
        elif self.cell and tag == "br":
            self.buf.append("\n")

    def handle_endtag(self, tag):
        if tag == "td" and self.cell is not None:
            if self.cell in WANTED:
                self.row[self.cell] = "".join(self.buf).strip()
            self.cell = None
        elif tag == "tr" and self.row is not None:
            if self.row:
                self.rows.append(self.row)
            self.row = None
        elif tag == "tbody":
            self.in_body = False
        elif tag == "table" and self.in_grid:
            self.in_grid = False

    def handle_data(self, data):
        if self.cell:
            self.buf.append(data)


# 건물 이름 통일
BUILDING_ALIASES = {
    "IT.BT관": "IT/BT관",
    "경제금육관": "경제금융관",               # 원본 오타
    "HIT": "한양종합기술연구원(HIT)",
    "한양종합기술연구원": "한양종합기술연구원(HIT)",
    "퓨전테크놀러지": "퓨전테크놀로지센터",
    "퓨전테크센터": "퓨전테크놀로지센터",
}


def normalize(building, room):
    """건물 이름 보정"""
    # 사범대학 + 별관 … → 사범대학별관
    if building == "사범대학" and room.startswith("별관 "):
        return "사범대학별관", room[3:].strip()
    # 역량교육 + …(HIT …) → HIT
    if building == "역량교육" and "HIT" in room:
        return "한양종합기술연구원(HIT)", re.sub(r"\s*\(.*\)$", "", room).strip()
    return BUILDING_ALIASES.get(building, building), re.sub(r"\s+", " ", room)


def split_room(full):
    """'서울 재성토목관 멀티미디어실 305호' → ('재성토목관', '멀티미디어실 305호')"""
    s = full.strip()
    if not s or "비지정" in s:
        return None
    if s.startswith("서울 "):
        s = s[3:]
    parts = s.split(" ", 1)
    if len(parts) < 2:
        return None
    return normalize(parts[0].strip(), parts[1].strip())


def read_html(path):
    """UTF-8, 실패 시 CP949로 읽음"""
    data = path.read_bytes()
    for enc in ("utf-8", "cp949"):
        try:
            return data.decode(enc)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="replace")


def to_min(t):
    h, m = t.split(":")
    return int(h) * 60 + int(m)


def fmt(m):
    return f"{m // 60:02d}:{m % 60:02d}"


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    src = Path(sys.argv[1]).expanduser()
    out = Path(sys.argv[2] if len(sys.argv) > 2 else "schedules.csv")
    if not src.exists():
        sys.exit(f"html 폴더를 찾을 수 없어요: {src}\n"
                 "사용법: python3 parse_sugang.py <html 폴더> [만들 csv 파일]")
    files = sorted(src.glob("*.html")) if src.is_dir() else [src]
    if not files:
        sys.exit(f"{src} 에 .html 파일이 없어요.")

    sessions = set()  # (건물, 강의실, 요일, 시작분, 종료분, 과목명, 수업번호) — 중복 행 제거
    stats = defaultdict(int)

    for f in files:
        p = GridParser()
        p.feed(read_html(f))
        if not p.rows:
            print(f"  경고: {f.name} 에서 강의 표를 못 찾았어요.")
        stats["files"] += 1
        for row in p.rows:
            stats["rows"] += 1
            times = [t for t in row.get("suupTimes", "").split("\n") if t.strip()]
            rooms = [r for r in row.get("suupRoomNms", "").split(",") if r.strip()]
            course = row.get("gwamokNm", "").replace(",", "·").strip()
            # 수업번호로 분반 구분
            suup_no = re.sub(r"\D", "", row.get("suupNo2", "")) or course
            parsed_any = False
            for i, t in enumerate(times):
                m = TIME_RE.search(t)
                if not m:
                    continue  # 시간미지정강좌 등
                # 시간·강의실은 순서대로 대응. 강의실 1개면 전체 공통
                room_str = rooms[i] if i < len(rooms) else (rooms[0] if len(rooms) == 1 else "")
                room = split_room(room_str)
                if not room:
                    continue  # 비지정(원격 등)
                day, st, en = m.group(1), to_min(m.group(2)), to_min(m.group(3))
                sessions.add((room[0], room[1], day, st, en, course, suup_no))
                parsed_any = True
            if not parsed_any:
                stats["skipped_rows"] += 1

    # 같은 분반의 연속 시간은 하나로 합침
    grouped = defaultdict(list)
    for b, r, d, st, en, c, no in sessions:
        grouped[(b, r, d, c, no)].append((st, en))
    merged = []
    for (b, r, d, c, no), spans in grouped.items():
        spans.sort()
        cur_s, cur_e = spans[0]
        for s, e in spans[1:]:
            if s <= cur_e:
                cur_e = max(cur_e, e)
            else:
                merged.append((b, r, d, cur_s, cur_e, c))
                cur_s, cur_e = s, e
        merged.append((b, r, d, cur_s, cur_e, c))

    order = "월화수목금토일"
    merged.sort(key=lambda x: (x[0], x[1], order.index(x[2]), x[3]))

    # 엑셀 한글 깨짐 방지 (BOM)
    with out.open("w", encoding="utf-8-sig", newline="") as fp:
        w = csv.writer(fp)
        w.writerow(["building_code", "building_name", "room", "day", "start", "end", "course", "professor"])
        for b, r, d, st, en, c in merged:
            # building_code = 건물 이름
            w.writerow([b, b, r, d, fmt(st), fmt(en), c, ""])

    buildings = {m[0] for m in merged}
    rooms = {(m[0], m[1]) for m in merged}
    print(f"파일 {stats['files']}개, 강의 행 {stats['rows']}개 읽음")
    print(f"강의실 없는 행(원격·시간미지정 등) {stats['skipped_rows']}개 제외")
    print(f"→ 건물 {len(buildings)}개, 강의실 {len(rooms)}개, 수업 {len(merged)}건을 {out} 에 저장")


if __name__ == "__main__":
    main()
