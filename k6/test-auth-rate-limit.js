import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '5s',
};

const BASE_URL = 'http://localhost:8081/api/v1';

export default function () {
  const payload = JSON.stringify({
    username: 'invalid_user',
    password: 'wrong_password_123'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(`${BASE_URL}/auth/login`, payload, params);

  check(res, {
    'rate limit or authentication failure handled': (r) => r.status === 401 || r.status === 429 || r.status === 400,
  });

  sleep(0.1);
}
