// 샘플 API (가짜 데이터)
// RealApi와 응답 형식 동일
(function () {
  const RULE = window.SUUPHANYANG_CONFIG.RULE;
  const DAY_ENUM = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  const USER_KEY = 'suuphanyang-mock-user';

  function rng(seed) {
    let s = seed >>> 0;
    return () => (s = (s * 1664525 + 1013904223) >>> 0) / 4294967296;
  }
  function hash(str) {
    let h = 2166136261;
    for (const c of str) { h ^= c.charCodeAt(0); h = Math.imul(h, 16777619); }
    return h >>> 0;
  }
  const fmt = (m) => `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`;
  const toMin = (s) => { const [h, m] = s.split(':').map(Number); return h * 60 + m; };

  const SAMPLE_BUILDINGS = [
    { id: 'eng1', name: '제1공학관', floors: [2, 3, 4] },
    { id: 'eng2', name: '제2공학관', floors: [3, 4, 5] },
    { id: 'itbt', name: 'IT/BT관', floors: [2, 5, 6] },
    { id: 'hum', name: '인문관', floors: [1, 2, 3] },
    { id: 'biz', name: '경영관', floors: [2, 3] },
    { id: 'soc', name: '사회과학관', floors: [3, 4] },
  ];
  const COURSES = ['자료구조', '미적분학1', '일반물리학', '회로이론', '경영학원론', '철학의 이해', '선형대수', '운영체제',
    '글쓰기', '통계학개론', '마케팅관리', '사회학개론', '데이터베이스', '유기화학', '미시경제학', '영어회화', '컴퓨터구조', '심리학개론'];
  const STARTS = [540, 630, 720, 810, 900, 990, 1080];

  const buildings = SAMPLE_BUILDINGS.map((b) => {
    const r = rng(hash(b.id));
    const rooms = [];
    b.floors.forEach((f) => {
      const n = 2 + Math.floor(r() * 2);
      for (let i = 1; i <= n; i++) {
        rooms.push({ id: `${b.id}-${f}0${i}`, name: `${f}0${i}호`, buildingId: b.id });
      }
    });
    return { id: b.id, name: b.name, rooms };
  });
  const allRooms = buildings.flatMap((b) => b.rooms);
  const buildingName = (id) => buildings.find((b) => b.id === id).name;

  const classCache = {};
  function classesOf(roomId) {
    if (classCache[roomId]) return classCache[roomId];
    const r = rng(hash(roomId));
    const byDay = DAY_ENUM.map((_, d) => {
      const list = []; let cursor = 0;
      if (d >= 5) return list; // 주말 수업 없음
      for (const st of STARTS) {
        if (st < cursor || r() < 0.45) continue;
        const len = r() < 0.25 ? 165 : 75;
        if (st + len > 1260) continue;
        list.push({ id: `${roomId}@${d}@${st}`, roomId, day: d, start: st, end: st + len,
          courseName: COURSES[Math.floor(r() * COURSES.length)], professor: '교수명' });
        cursor = st + len;
      }
      return list;
    });
    return (classCache[roomId] = byDay);
  }

  // 데모 시계. app.js가 슬라이더 시각 전달
  let clock = { date: null, now: 0 };
  const myChecks = {}; // `${scheduleId}|${date}` -> {type, minute}

  function fakeOthers(cls, date) {
    const r = rng(hash(cls.id + date));
    const out = []; const n = Math.floor(r() * 5);
    const kind = r(); // 약 15% 휴강, 나머지 절반은 조기 종료
    for (let i = 0; i < n; i++) {
      if (kind < 0.15) {
        out.push({ type: r() < 0.85 ? 'CANCELLED' : 'ONGOING', minute: cls.start + Math.floor(r() * 15) });
      } else {
        const base = kind < 0.55 ? cls.end - 15 : cls.end;
        out.push({ type: r() < 0.8 ? 'ENDED' : 'ONGOING', minute: base + Math.floor(r() * 12) });
      }
    }
    return out;
  }
  function tally(cls, date, now) {
    const list = fakeOthers(cls, date).filter((c) => c.minute <= now);
    const mine = myChecks[`${cls.id}|${date}`];
    if (mine && mine.minute <= now) list.push(mine);
    const ended = list.filter((c) => c.type === 'ENDED').length;
    const ongoing = list.filter((c) => c.type === 'ONGOING').length;
    const cancelled = list.filter((c) => c.type === 'CANCELLED').length;
    const last = list.length ? Math.max(...list.map((c) => c.minute)) : null;
    return { ended, ongoing, cancelled, last };
  }
  function dayIndexOf(date) { return (new Date(date + 'T00:00:00').getDay() + 6) % 7; } // 월=0 … 일=6

  function statusOf(room, date, now) {
    const d = dayIndexOf(date);
    const list = classesOf(room.id)[d];
    const cur = list.find((c) => c.start <= now && now < c.end);
    const next = list.find((c) => c.start > now);
    let state = 'FREE', current = null;
    if (cur) {
      const t = tally(cur, date, now);
      const likelyCancelled = t.cancelled >= RULE.minEnded && t.cancelled > t.ongoing; // 휴강은 최근 조건 없음
      const likelyEnded = t.ended >= RULE.minEnded && t.ended > t.ongoing && now - t.last <= RULE.freshMinutes;
      state = likelyCancelled ? 'LIKELY_CANCELLED' : likelyEnded ? 'LIKELY_ENDED' : 'BUSY';
      current = { scheduleId: cur.id, courseName: cur.courseName, start: fmt(cur.start), end: fmt(cur.end),
        ended: t.ended, ongoing: t.ongoing, cancelled: t.cancelled, lastCheckAt: t.last === null ? null : fmt(t.last) };
    }
    return { roomId: room.id, roomName: room.name, buildingId: room.buildingId, buildingName: buildingName(room.buildingId),
      state, current, nextStart: next ? fmt(next.start) : null };
  }

  let memUser = null; // localStorage 불가 환경용
  function currentUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null') || memUser; } catch (e) { return memUser; }
  }

  window.MockApi = {
    setDemoClock(date, now) { clock = { date, now }; },

    async getBuildings() {
      return buildings.map((b) => ({ id: b.id, code: b.id, name: b.name,
        rooms: b.rooms.map((r) => ({ id: r.id, name: r.name })) }));
    },

    async getStatuses({ date, time, buildingId }) {
      date = date || clock.date;
      const now = time ? toMin(time) : clock.now;
      return allRooms.filter((r) => !buildingId || r.buildingId === buildingId).map((r) => statusOf(r, date, now));
    },

    async getSchedules(roomId, dayEnum) {
      const d = DAY_ENUM.indexOf(dayEnum);
      return classesOf(roomId)[d].map((c) => ({ id: c.id, courseName: c.courseName, professor: c.professor,
        start: fmt(c.start), end: fmt(c.end) }));
    },

    async postCheck(scheduleId, type) {
      if (!currentUser()) throw new Error('로그인이 필요해요.');
      const [roomId, d, st] = scheduleId.split('@');
      const cls = classesOf(roomId)[+d].find((c) => c.start === +st);
      const opens = type === 'ENDED' ? Math.max(cls.start, cls.end - RULE.checkOpensBefore) : cls.start;
      if (clock.now < opens) throw new Error(`체크는 ${fmt(opens)}부터 할 수 있어요.`);
      if (clock.now >= cls.end) throw new Error('이미 종료 시각이 지난 수업이에요.');
      if (myChecks[`${cls.id}|${clock.date}`]) throw new Error('이 시간대에 이 강의실은 이미 체크했어요.');
      myChecks[`${cls.id}|${clock.date}`] = { type, minute: clock.now };
      const t = tally(cls, clock.date, clock.now);
      return { ended: t.ended, ongoing: t.ongoing, cancelled: t.cancelled, lastCheckAt: fmt(t.last), myType: type };
    },

    async sendCode(email) {
      if (!/^[^@\s]+@hanyang\.ac\.kr$/i.test(email)) throw new Error('@hanyang.ac.kr 주소만 인증할 수 있어요.');
    },

    async verify(email, code) {
      if (code !== '000000') throw new Error('코드가 맞지 않아요. 메일을 다시 확인하세요.');
      memUser = { email };
      try { localStorage.setItem(USER_KEY, JSON.stringify({ email })); } catch (e) {}
      return { token: 'mock-token', email };
    },

    async me() { return currentUser(); },

    async logout() { memUser = null; try { localStorage.removeItem(USER_KEY); } catch (e) {} },
  };
})();
