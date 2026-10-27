// 화면 렌더링, 이벤트 처리. 데이터는 window.Api로만 조회
(function () {
  const CFG = window.SUUPHANYANG_CONFIG;
  const RULE = CFG.RULE;
  const Api = window.Api;
  const DAYS = ['월', '화', '수', '목', '금', '토', '일'];
  const DAY_ENUM = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  const DAY_START = 9 * 60;   // 표시 시작 09:00
  const DAY_END = 24 * 60;    // 표시 끝 24:00

  const state = {
    tab: 'find', day: 2, now: 850, live: false,
    building: 'all',                       // 빈 강의실 탭 건물 필터
    tBuilding: null, tRoom: null, tDay: null, // 시간표 탭 선택값
    buildings: [], user: null, pendingEmail: null,
    myVotes: {},                           // `${scheduleId}|${date}` -> 체크 종류
  };

  /* ---------- 유틸 ---------- */
  const $ = (id) => document.getElementById(id);
  const fmt = (m) => `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`;
  const toMin = (s) => { const [h, m] = s.split(':').map(Number); return h * 60 + m; };
  // 23:59는 24:00으로 표시
  const showTime = (s) => (s === '23:59' ? '24:00' : s);
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  // 이번 주 해당 요일 날짜 (YYYY-MM-DD)
  function dateOf(dayIdx) {
    const d = new Date();
    const mondayOffset = (d.getDay() + 6) % 7;
    d.setDate(d.getDate() - mondayOffset + dayIdx);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  const curDate = () => dateOf(state.day);
  // 실시간이면 서버 시각 사용, 슬라이더 이동 시에만 시각 지정
  const whenQuery = () => (state.live ? {} : { date: curDate(), time: fmt(state.now) });

  function syncNow() {
    const d = new Date(), m = d.getHours() * 60 + d.getMinutes();
    state.day = (d.getDay() + 6) % 7; // 월=0 … 일=6
    if (m >= DAY_START && m < DAY_END) { state.now = m; state.live = true; }
    else { state.now = 850; state.live = false; } // 새벽엔 데모 시각(14:10)
  }

  function toast(msg) {
    const t = $('toast'); t.textContent = msg; t.classList.add('show');
    clearTimeout(toast._t); toast._t = setTimeout(() => t.classList.remove('show'), 1800);
  }

  // 체크 종류 표시명
  const CHECK_LABEL = { ENDED: '끝남', CANCELLED: '휴강', ONGOING: '수업 중' };

  /**
   * 체크 가능 여부. CheckService.validateWindow와 동일 규칙
   */
  function canCheck(cur, type) {
    if (!state.user) return { ok: false, why: '학교 이메일 인증 후 체크할 수 있어요.' };
    if (!CFG.USE_MOCK && !state.live) return { ok: false, why: '체크는 실제 시각에서만 할 수 있어요. 위의 "지금" 버튼을 눌러 주세요.' };
    const start = toMin(cur.start), end = toMin(cur.end);
    const opens = type === 'ENDED' ? Math.max(start, end - RULE.checkOpensBefore) : start;
    if (state.now < opens) return { ok: false, why: `${CHECK_LABEL[type]} 체크는 ${fmt(opens)}부터 할 수 있어요.` };
    if (state.now >= end) return { ok: false, why: '이미 종료 시각이 지난 수업이에요.' };
    return { ok: true };
  }

  async function viewFind() {
    const list = await Api.getStatuses({
      ...whenQuery(),
      buildingId: state.building === 'all' ? null : state.building,
    });
    const until = (x) => (x.nextStart ? toMin(x.nextStart) : DAY_END);
    const free = list.filter((x) => x.state === 'FREE').sort((a, b) => until(b) - until(a));
    // 끝남·휴강 판정된 강의실
    const ended = list.filter((x) => x.state === 'LIKELY_ENDED' || x.state === 'LIKELY_CANCELLED');

    const card = (x) => {
      const cls = x.state === 'FREE' ? 'free' : 'ended';
      const sub = x.state === 'LIKELY_CANCELLED' ? `${esc(x.buildingName)} · 휴강 ${x.current.cancelled}명 체크`
        : x.state === 'LIKELY_ENDED' ? `${esc(x.buildingName)} · 끝남 ${x.current.ended}명 체크`
        : `${esc(x.buildingName)}`;
      return `<button class="room ${cls}" data-room="${esc(x.roomId)}" data-bid="${esc(x.buildingId)}">
        <span class="bar"></span>
        <span><span class="room-name">${esc(x.buildingName)} ${esc(x.roomName)}</span><br><span class="room-sub">${sub}</span></span>
        <span class="until"><b>${x.nextStart ?? '마감'}</b><span>${x.nextStart ? '까지 비어 있음' : '오늘 수업 없음'}</span></span>
      </button>`;
    };

    return `
      <div class="chips" role="group" aria-label="건물">
        <button class="chip" data-bld="all" aria-pressed="${state.building === 'all'}">전체</button>
        ${state.buildings.map((b) => `<button class="chip" data-bld="${esc(b.id)}" aria-pressed="${state.building === String(b.id)}">${esc(b.name)}</button>`).join('')}
      </div>

      <h2>끝났거나 휴강인 강의실 ${ended.length}</h2>
      <p class="hint">시간표상 수업 중이지만, ${RULE.minEnded}명 이상이 끝났다고(최근 ${RULE.freshMinutes}분 안) 또는 휴강이라고 체크했어요.</p>
      ${ended.length ? ended.map(card).join('') : '<div class="empty">아직 끝났거나 휴강이라는 체크가 없어요.</div>'}

      <h2>지금 비어 있는 강의실 ${free.length}</h2>
      <p class="hint">오래 비어 있는 순서예요.</p>
      ${free.length ? free.map(card).join('') : '<div class="empty">이 건물엔 지금 빈 강의실이 없어요. 다른 건물을 골라 보세요.</div>'}
    `;
  }

  /* ---------- 화면: 강의실 시간표 ---------- */
  function checkPanel(cur) {
    const key = `${cur.scheduleId}|${curDate()}`;
    const mine = state.myVotes[key];
    const locked = !!mine; // 체크 후 잠금
    const last = cur.lastCheckAt ? `마지막 체크 ${Math.max(0, state.now - toMin(cur.lastCheckAt))}분 전` : '아직 체크 없음';
    const button = (type, text, cls) => {
      const can = canCheck(cur, type);
      return `<button class="btn ${cls}" data-check="${type}" data-sid="${esc(cur.scheduleId)}" ${can.ok && !locked ? '' : 'disabled'} aria-pressed="${mine === type}">${text}</button>`;
    };
    // 비활성 사유 (중복 제거)
    const reasons = [...new Set(['ENDED', 'CANCELLED', 'ONGOING']
      .map((t) => canCheck(cur, t)).filter((c) => !c.ok).map((c) => c.why))];
    return `
      <div class="tally">
        <div class="e"><b>${cur.ended}</b>명 끝남</div>
        <div class="e"><b>${cur.cancelled ?? 0}</b>명 휴강</div>
        <div><b>${cur.ongoing}</b>명 수업 중</div>
        <div style="align-self:end">${last}</div>
      </div>
      <div class="actions">
        ${button('ENDED', '수업 끝났어요', 'primary')}
        ${button('CANCELLED', '휴강이에요', 'primary')}
        ${button('ONGOING', '아직 수업 중이에요', '')}
      </div>
      ${locked
        ? `<p class="note">이미 "${CHECK_LABEL[mine]}" 체크를 했어요. 한 시간대에 한 번만 체크할 수 있어요.</p>`
        : reasons.map((r) => `<p class="note">${r}</p>`).join('')}
    `;
  }

  async function viewTable() {
    const bs = state.buildings;
    if (!bs.length) return '<div class="empty">등록된 건물이 없어요. 시간표 데이터를 먼저 넣어 주세요.</div>';
    let b = bs.find((x) => String(x.id) === state.tBuilding) || bs[0];
    state.tBuilding = String(b.id);
    if (!b.rooms.some((r) => String(r.id) === state.tRoom)) state.tRoom = b.rooms.length ? String(b.rooms[0].id) : null;
    if (!state.tRoom) return '<div class="empty">이 건물에 등록된 강의실이 없어요.</div>';
    const room = b.rooms.find((r) => String(r.id) === state.tRoom);
    const day = state.tDay ?? state.day;
    const isToday = day === state.day;

    const [statuses, list] = await Promise.all([
      Api.getStatuses({ ...whenQuery(), buildingId: b.id }),
      Api.getSchedules(room.id, DAY_ENUM[day]),
    ]);
    const s = statuses.find((x) => String(x.roomId) === String(room.id));

    let statusHtml;
    if (s && s.current) {
      statusHtml = `<div class="status">
        <div class="lbl">지금 이 강의실</div>
        <div class="big">${s.state === 'LIKELY_CANCELLED' ? '휴강일 가능성이 높아요' : s.state === 'LIKELY_ENDED' ? '끝났을 가능성이 높아요' : '수업 중'} · ${esc(s.current.courseName)} (~${showTime(s.current.end)})</div>
        ${checkPanel(s.current)}
      </div>`;
    } else {
      statusHtml = `<div class="status"><div class="lbl">지금 이 강의실</div>
        <div class="big" style="margin:0;color:var(--free)">비어 있음 · ${s && s.nextStart ? s.nextStart + '까지' : '오늘 수업 없음'}</div></div>`;
    }

    const PX = 52 / 60, top = (m) => (m - DAY_START) * PX;
    const hours = Array.from({ length: (DAY_END - DAY_START) / 60 + 1 }, (_, i) => DAY_START + i * 60);
    const blocks = list.map((c) => {
      const endedFlag = isToday && s && s.current && String(s.current.scheduleId) === String(c.id) && (s.state === 'LIKELY_ENDED' || s.state === 'LIKELY_CANCELLED');
      const st = toMin(c.start), en = toMin(c.end);
      return `<button class="block ${endedFlag ? 'is-ended' : ''}" style="top:${top(st)}px;height:${(en - st) * PX - 3}px"
        data-info="${esc(`${c.courseName} · ${DAYS[day]} ${c.start}–${showTime(c.end)}`)}">
        <b>${esc(c.courseName)}</b><span>${c.start}–${showTime(c.end)}</span></button>`;
    }).join('');
    const nowLine = isToday ? `<div class="nowline" style="top:${top(state.now)}px"></div>` : '';

    return `
      <div class="pickers">
        <select id="tBld" aria-label="건물">${bs.map((x) => `<option value="${esc(x.id)}" ${x.id === b.id ? 'selected' : ''}>${esc(x.name)}</option>`).join('')}</select>
        <select id="tRoom" aria-label="강의실">${b.rooms.map((r) => `<option value="${esc(r.id)}" ${r.id === room.id ? 'selected' : ''}>${esc(r.name)}</option>`).join('')}</select>
      </div>
      <h2>${esc(b.name)} ${esc(room.name)}</h2>
      ${statusHtml}
      <div class="days" role="group" aria-label="요일">${DAYS.map((d, i) => `<button data-tday="${i}" aria-pressed="${i === day}">${d}</button>`).join('')}</div>
      ${list.length ? '' : `<p class="hint">${DAYS[day]}요일엔 이 강의실에 수업이 없어요.</p>`}
      <div class="grid-scroll"><div class="grid" style="height:${top(DAY_END) + 8}px">
        <div class="hours">${hours.map((h) => `<div style="top:${top(h)}px">${h / 60}</div>`).join('')}</div>
        <div class="lane">${blocks}${nowLine}</div>
      </div></div>
    `;
  }

  /* ---------- 화면: 내 정보 ---------- */
  async function viewMe() {
    if (state.user) {
      return `<h2>학교 인증</h2>
        <div class="verified"><b>인증 완료</b><span>${esc(state.user.email)}</span></div>
        <p class="hint">수업 종료 ${RULE.checkOpensBefore}분 전부터 "수업 끝났어요"를 체크할 수 있어요.</p>
        <button class="btn" id="unverify">인증 해제</button>`;
    }
    return `<h2>학교 인증</h2>
      <p class="hint">한양대 이메일로 인증하면 수업 종료 체크를 할 수 있어요. 포털 비밀번호는 받지 않아요.</p>
      <div class="field"><label for="em">학교 이메일</label>
        <input id="em" type="email" placeholder="아이디@hanyang.ac.kr" value="${esc(state.pendingEmail || '')}" autocomplete="email"></div>
      ${state.pendingEmail ? `<div class="field"><label for="code">인증 코드</label>
        <input id="code" inputmode="numeric" maxlength="6" placeholder="6자리 숫자"></div>
        ${CFG.USE_MOCK ? '<p class="note">데모에서는 000000을 입력하세요.</p>' : '<p class="note">메일로 받은 6자리 코드를 입력하세요. 10분 동안 유효해요.</p>'}
        <button class="btn primary" id="confirm" style="width:100%;margin-top:8px">인증하기</button>`
        : '<button class="btn primary" id="send" style="width:100%">인증 코드 보내기</button>'}
      <p class="err" id="err"></p>`;
  }

  /* ---------- 렌더 ---------- */
  function renderClock() {
    $('timeBig').textContent = `${DAYS[state.day]} ${fmt(state.now)}`;
    $('daySel').value = state.day;
    $('timeRange').value = state.now;
    const src = CFG.USE_MOCK ? '샘플 데이터' : '서버 데이터';
    $('demoTag').textContent = state.live
      ? `실제 시각 기준 · ${src}`
      : `데모 시각으로 보여줘요 · 슬라이더로 시간을 바꿔 보세요 · ${src}`;
  }

  let renderSeq = 0;
  async function render() {
    renderClock();
    document.querySelectorAll('nav.tabs button').forEach((b) => {
      if (b.dataset.tab === state.tab) b.setAttribute('aria-current', 'page');
      else b.removeAttribute('aria-current');
    });
    if (Api.setDemoClock) Api.setDemoClock(curDate(), state.now);
    const seq = ++renderSeq;
    let html;
    try {
      html = state.tab === 'find' ? await viewFind() : state.tab === 'table' ? await viewTable() : await viewMe();
    } catch (e) {
      html = `<div class="empty">데이터를 불러오지 못했어요. ${esc(e.message)}<br>서버가 켜져 있는지, js/config.js의 API_BASE가 맞는지 확인하세요.</div>`;
    }
    if (seq !== renderSeq) return; // 이전 렌더 결과는 무시
    $('view').innerHTML = html;
  }

  let debounce;
  const renderSoon = () => { clearTimeout(debounce); debounce = setTimeout(render, CFG.USE_MOCK ? 0 : 150); };

  /* ---------- 이벤트 ---------- */
  $('daySel').innerHTML = DAYS.map((d, i) => `<option value="${i}">${d}요일</option>`).join('');
  $('daySel').addEventListener('change', (e) => { state.day = +e.target.value; state.live = false; state.tDay = null; render(); });
  $('timeRange').addEventListener('input', (e) => { state.now = +e.target.value; state.live = false; renderClock(); renderSoon(); });
  $('nowBtn').addEventListener('click', () => { syncNow(); state.tDay = null; render(); });
  document.querySelectorAll('nav.tabs button').forEach((b) =>
    b.addEventListener('click', () => { state.tab = b.dataset.tab; render(); window.scrollTo(0, 0); }));

  $('view').addEventListener('click', async (e) => {
    const t = e.target.closest('button'); if (!t) return;
    try {
      if (t.dataset.bld) { state.building = t.dataset.bld; render(); }
      else if (t.dataset.room) {
        state.tBuilding = t.dataset.bid; state.tRoom = t.dataset.room; state.tDay = null; state.tab = 'table';
        render(); window.scrollTo(0, 0);
      }
      else if (t.dataset.tday) { state.tDay = +t.dataset.tday; render(); }
      else if (t.dataset.check) {
        const raw = t.dataset.sid;
        const sid = /^\d+$/.test(raw) ? Number(raw) : raw;
        await Api.postCheck(sid, t.dataset.check);
        state.myVotes[`${raw}|${curDate()}`] = t.dataset.check;
        toast({ ENDED: '끝났다고 체크했어요', CANCELLED: '휴강이라고 체크했어요', ONGOING: '수업 중이라고 체크했어요' }[t.dataset.check]);
        render();
      }
      else if (t.dataset.info) { toast(t.dataset.info); }
      else if (t.id === 'send') {
        const em = $('em').value.trim();
        await Api.sendCode(em);
        state.pendingEmail = em; toast('인증 코드를 보냈어요'); render();
      }
      else if (t.id === 'confirm') {
        const r = await Api.verify(state.pendingEmail, $('code').value.trim());
        state.user = { email: r.email }; state.pendingEmail = null; toast('인증했어요'); render();
      }
      else if (t.id === 'unverify') { await Api.logout(); state.user = null; render(); }
    } catch (err) {
      const box = $('err');
      if (box) box.textContent = err.message; else toast(err.message);
    }
  });
  $('view').addEventListener('change', (e) => {
    if (e.target.id === 'tBld') { state.tBuilding = e.target.value; state.tRoom = null; render(); }
    if (e.target.id === 'tRoom') { state.tRoom = e.target.value; render(); }
  });

  /* ---------- 시작 ---------- */
  (async function init() {
    syncNow();
    try {
      [state.buildings, state.user] = await Promise.all([Api.getBuildings(), Api.me()]);
      state.buildings = state.buildings.map((b) => ({ ...b, id: String(b.id), rooms: b.rooms.map((r) => ({ ...r, id: String(r.id) })) }));
    } catch (e) { /* render()에서 오류 표시 */ }
    render();
    setInterval(() => { if (state.live) { syncNow(); render(); } }, 60000);
  })();
})();
