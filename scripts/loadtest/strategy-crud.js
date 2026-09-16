import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const OWNER = __ENV.OWNER || 'load-owner';
const TARGET_RPS = Number(__ENV.TARGET_RPS || '10');
const DURATION = __ENV.DURATION || '45s';
const PRE_VUS = Number(__ENV.PRE_VUS || String(Math.max(TARGET_RPS * 2, 10)));
const MAX_VUS = Number(__ENV.MAX_VUS || String(Math.max(TARGET_RPS * 8, 50)));

const flowErrors = new Counter('flow_errors');
const flowDuration = new Trend('flow_duration', true);

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    combined_crud: {
      executor: 'constant-arrival-rate',
      rate: TARGET_RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_VUS,
      maxVUs: MAX_VUS,
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    checks: ['rate==1'],
    flow_errors: ['count==0'],
  },
};

function createBody(name) {
  return JSON.stringify({
    ownerId: OWNER,
    name: name,
    rules: [
      {
        id: 'r1',
        conditionType: 'price_above',
        actionType: 'hold',
        instrument: 'ETH-USD',
        indicator: '',
        threshold: '3000',
        allocationPercent: '',
      },
    ],
  });
}

function updateBody(threshold) {
  return JSON.stringify({
    rules: [
      {
        id: 'r1',
        conditionType: 'price_under',
        actionType: 'hold',
        instrument: 'ETH-USD',
        indicator: '',
        threshold: String(threshold),
        allocationPercent: '',
      },
    ],
  });
}

export default function () {
  const started = Date.now();
  const name = `lt-${__VU}-${__ITER}-${Date.now()}`;

  const created = http.post(`${BASE_URL}/strategies`, createBody(name), {
    headers: { 'Content-Type': 'application/json' },
    tags: { flow_step: 'create' },
  });
  const createdOk = check(created, {
    'create status 201': (r) => r.status === 201,
    'create has strategyId': (r) => {
      try {
        return Boolean(r.json('strategyId'));
      } catch (e) {
        return false;
      }
    },
  });
  if (!createdOk) {
    flowErrors.add(1);
    fail(`create failed: status=${created.status} body=${created.body}`);
  }
  const strategyId = created.json('strategyId');

  const detail = http.get(
    `${BASE_URL}/strategies/${strategyId}?ownerId=${encodeURIComponent(OWNER)}`,
    { tags: { flow_step: 'get_one' } },
  );
  const detailOk = check(detail, {
    'get one status 200': (r) => r.status === 200,
  });
  if (!detailOk) {
    flowErrors.add(1);
    fail(`get one failed: status=${detail.status}`);
  }

  const listed = http.get(
    `${BASE_URL}/strategies?ownerId=${encodeURIComponent(OWNER)}&page=0&size=20`,
    { tags: { flow_step: 'list' } },
  );
  const listOk = check(listed, {
    'list status 200': (r) => r.status === 200,
  });
  if (!listOk) {
    flowErrors.add(1);
    fail(`list failed: status=${listed.status}`);
  }

  const updated = http.put(
    `${BASE_URL}/strategies/${strategyId}?ownerId=${encodeURIComponent(OWNER)}`,
    updateBody(2000 + (__ITER % 500)),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { flow_step: 'update' },
    },
  );
  const updateOk = check(updated, {
    'update status 200': (r) => r.status === 200,
  });
  if (!updateOk) {
    flowErrors.add(1);
    fail(`update failed: status=${updated.status}`);
  }

  flowDuration.add(Date.now() - started);
}

export function handleSummary(data) {
  const path = __ENV.SUMMARY_PATH || 'k6-summary.json';
  return {
    [path]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: false }),
  };
}

function textSummary(data, _opts) {
  const failed = data.metrics.http_req_failed?.values?.rate ?? 1;
  const checks = data.metrics.checks?.values?.rate ?? 0;
  const flows = data.metrics.flow_errors?.values?.count ?? 0;
  const http = data.metrics.http_req_duration?.values ?? {};
  const flow = data.metrics.flow_duration?.values ?? {};
  return (
    `k6 summary: http_fail=${failed} checks=${checks} flow_errors=${flows}\n` +
    `http_req_duration_ms p50=${http.med ?? 'n/a'} p90=${http['p(90)'] ?? 'n/a'} ` +
    `p95=${http['p(95)'] ?? 'n/a'} p99=${http['p(99)'] ?? 'n/a'}\n` +
    `flow_duration_ms p50=${flow.med ?? 'n/a'} p90=${flow['p(90)'] ?? 'n/a'} ` +
    `p95=${flow['p(95)'] ?? 'n/a'} p99=${flow['p(99)'] ?? 'n/a'}\n`
  );
}
