import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    concurrent_transfers: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '10s',
    },
  },
};

const BASE_URL = 'http://localhost:8081/api/v1';

export default function () {
  const payload = JSON.stringify({
    senderAccountId: '1000000001',
    recipientAccountNumber: '970422001999',
    amount: 50000,
    transferType: 'INTERNAL',
    description: 'k6 Race Condition Test'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Idempotency-Key': `k6-race-${Math.random()}`
    },
  };

  const res = http.post(`${BASE_URL}/transfers/internal`, payload, params);

  check(res, {
    'handled gracefully (no 500 unhandled exceptions)': (r) => r.status === 200 || r.status === 202 || r.status === 409 || r.status === 400,
  });
}
