import { apiErrorFromResponse } from "../errors/messages";
import type {
  AcceptedResponse,
  CreateStrategyBody,
  SessionResponse,
  UpdateStrategyBody,
} from "./types";

const JSON_TYPE = "application/json";

type JsonRequest = {
  method: string;
  path: string;
  body?: unknown;
  token?: string | null;
};

async function requestJson<T>(input: JsonRequest): Promise<T> {
  const headers: Record<string, string> = {
    Accept: JSON_TYPE,
  };
  if (input.body !== undefined) {
    headers["Content-Type"] = JSON_TYPE;
  }
  if (input.token) {
    headers.Authorization = `Bearer ${input.token}`;
  }
  const response = await fetch(input.path, {
    method: input.method,
    headers,
    body: input.body === undefined ? undefined : JSON.stringify(input.body),
  });
  const text = await response.text();
  const parsed = text.length === 0 ? {} : JSON.parse(text);
  if (!response.ok) {
    throw apiErrorFromResponse({ status: response.status, body: parsed });
  }
  return parsed as T;
}

export function signup(body: {
  email: string;
  password: string;
  displayName: string;
}): Promise<SessionResponse> {
  return requestJson<SessionResponse>({ method: "POST", path: "/auth/signup", body });
}

export function login(body: { email: string; password: string }): Promise<SessionResponse> {
  return requestJson<SessionResponse>({ method: "POST", path: "/auth/login", body });
}

export function createStrategy(token: string, body: CreateStrategyBody): Promise<AcceptedResponse> {
  return requestJson<AcceptedResponse>({
    method: "POST",
    path: "/strategies",
    body,
    token,
  });
}

export function listStrategies(
  token: string,
  query: { page: number; size: number },
): Promise<AcceptedResponse> {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
    sort: "name",
    order: "asc",
  });
  return requestJson<AcceptedResponse>({
    method: "GET",
    path: `/strategies?${params.toString()}`,
    token,
  });
}

export function getStrategy(token: string, strategyId: string): Promise<AcceptedResponse> {
  return requestJson<AcceptedResponse>({
    method: "GET",
    path: `/strategies/${encodeURIComponent(strategyId)}`,
    token,
  });
}

export function updateStrategy(
  token: string,
  strategyId: string,
  body: UpdateStrategyBody,
): Promise<AcceptedResponse> {
  return requestJson<AcceptedResponse>({
    method: "PUT",
    path: `/strategies/${encodeURIComponent(strategyId)}`,
    body,
    token,
  });
}

export function deleteStrategy(token: string, strategyId: string): Promise<AcceptedResponse> {
  return requestJson<AcceptedResponse>({
    method: "DELETE",
    path: `/strategies/${encodeURIComponent(strategyId)}`,
    token,
  });
}
