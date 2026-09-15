import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 20 },  // Ramp up 20 Virtual Users
    { duration: '30s', target: 100 }, // Peak load 100 Virtual Users
    { duration: '10s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete under 500ms
    http_req_failed: ['rate<0.05'],   // Error rate must be less than 5%
  },
};

const BASE_URL = 'http://localhost:8081/api/v1';

export default function () {
  const payload = JSON.stringify({
    senderAccountId: '1000000001',
    recipientAccountNumber: '970422001999',
    amount: 100000,
    transferType: 'INTERNAL',
    description: 'k6 Load Test Transfer'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Idempotency-Key': `k6-key-${Math.random()}`
    },
  };

  const res = http.post(`${BASE_URL}/transfers/internal`, payload, params);

  check(res, {
    'status is 200 or 202': (r) => r.status === 200 || r.status === 202,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);
}
