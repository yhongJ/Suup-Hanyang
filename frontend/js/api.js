// 서버 API 호출
// MockApi와 응답 형식 동일해야 함
(function () {
  const cfg = window.SUUPHANYANG_CONFIG;
  const TOKEN_KEY = 'suuphanyang-token';

  function getToken() { try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; } }
  function setToken(t) {
    try { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY); } catch (e) {}
  }

  async function req(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers.Authorization = 'Bearer ' + token;
    const res = await fetch(cfg.API_BASE + path, {
      method, headers, body: body ? JSON.stringify(body) : undefined,
    });
    const data = res.status === 204 ? null : await res.json().catch(() => null);
    if (!res.ok) throw new Error((data && data.message) || `요청이 실패했어요 (${res.status})`);
    return data;
  }

  const RealApi = {
    getBuildings: () => req('GET', '/api/buildings'),
    getStatuses: ({ date, time, buildingId }) => {
      // date/time 생략 시 서버 시각 기준
      const q = new URLSearchParams();
      if (date) q.set('date', date);
      if (time) q.set('time', time);
      if (buildingId) q.set('buildingId', buildingId);
      return req('GET', '/api/rooms/status?' + q);
    },
    getSchedules: (roomId, dayEnum) => req('GET', `/api/rooms/${roomId}/schedules?day=${dayEnum}`),
    postCheck: (scheduleId, type) => req('POST', '/api/checks', { scheduleId, type }),
    sendCode: (email) => req('POST', '/api/auth/email/send', { email }),
    verify: async (email, code) => {
      const r = await req('POST', '/api/auth/email/verify', { email, code });
      setToken(r.token);
      return r;
    },
    me: async () => {
      if (!getToken()) return null;
      try { return await req('GET', '/api/me'); } catch (e) { setToken(null); return null; }
    },
    logout: async () => {
      try { await req('POST', '/api/auth/logout'); } catch (e) {}
      setToken(null);
    },
  };

  window.Api = cfg.USE_MOCK ? window.MockApi : RealApi;
})();
