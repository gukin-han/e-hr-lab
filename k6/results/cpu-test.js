import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 500,          // 동시 접속자 500명 (CPU 테스트 시 컴퓨터 보호를 위해 조금 줄임)
  duration: '15s',   // 15초 테스트
};

export default function () {
  const res = http.get('http://localhost:9090/thread/cpu');
  // const res = http.get('http://localhost:9090/thread/io');
  check(res, { 'status is 200': (r) => r.status === 200 });
}