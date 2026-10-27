// USE_MOCK: true면 샘플 데이터 사용 (백엔드 불필요)
window.SUUPHANYANG_CONFIG = {
  USE_MOCK: false,
  // localhost면 로컬 서버, 아니면 같은 도메인의 /api
  API_BASE: ['localhost', '127.0.0.1'].includes(location.hostname) ? 'http://localhost:8080' : '',

  // 화면 표시용. 실제 판정은 서버 app.rule 기준
  RULE: {
    checkOpensBefore: 60, // 끝남 체크는 종료 N분 전부터
    minEnded: 2,          // 끝남·휴강 최소 인원
    freshMinutes: 10,     // 끝남 체크 유효 시간(분)
  },
};
