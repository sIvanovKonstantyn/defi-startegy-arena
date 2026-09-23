export class ApiError extends Error {
  readonly status: number;
  readonly reason: string;

  constructor(input: { status: number; reason: string }) {
    super(input.reason);
    this.status = input.status;
    this.reason = input.reason;
  }
}

const HTTP_PREFIX = /^HTTP\s+\d{3}\b[:\s-]*/i;
const STATUS_PREFIX = /^status\s+\d{3}\b[:\s-]*/i;
const CODE_ONLY = /^\d{3}$/;

const STATUS_MESSAGES: Record<number, string> = {
  400: "The request could not be understood. Check the form and try again.",
  401: "Your session is invalid or expired. Please sign in again.",
  403: "You are not allowed to perform this action.",
  404: "The requested resource was not found.",
  409: "This conflicts with an existing record.",
  422: "Some fields are invalid. Review and try again.",
  429: "Too many requests. Wait a moment and try again.",
  500: "Something went wrong on the server. Try again shortly.",
  502: "The service is temporarily unreachable. Try again shortly.",
  503: "The service is temporarily unavailable. Try again shortly.",
};

function stripHttpCodes(raw: string): string {
  let text = raw.trim();
  text = text.replace(HTTP_PREFIX, "").trim();
  text = text.replace(STATUS_PREFIX, "").trim();
  if (CODE_ONLY.test(text)) {
    return "";
  }
  return text;
}

function reasonFromBody(body: unknown): string {
  if (typeof body !== "object" || body === null) {
    return "";
  }
  const record = body as Record<string, unknown>;
  for (const key of ["reasonCode", "reason", "message", "error", "detail"]) {
    const value = record[key];
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
  }
  return "";
}

export function toUserErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const cleaned = stripHttpCodes(error.reason);
    if (cleaned.length > 0) {
      return cleaned;
    }
    return STATUS_MESSAGES[error.status] ?? "Request failed. Please try again.";
  }
  if (error instanceof Error) {
    const cleaned = stripHttpCodes(error.message);
    if (cleaned.length > 0) {
      return cleaned;
    }
    const statusMatch = error.message.match(/\b([45]\d{2})\b/);
    if (statusMatch) {
      const status = Number(statusMatch[1]);
      return STATUS_MESSAGES[status] ?? "Request failed. Please try again.";
    }
  }
  if (typeof error === "string") {
    const cleaned = stripHttpCodes(error);
    if (cleaned.length > 0) {
      return cleaned;
    }
    const statusMatch = error.match(/\b([45]\d{2})\b/);
    if (statusMatch) {
      const status = Number(statusMatch[1]);
      return STATUS_MESSAGES[status] ?? "Request failed. Please try again.";
    }
  }
  return "Something went wrong. Please try again.";
}

export function apiErrorFromResponse(input: { status: number; body: unknown }): ApiError {
  const fromBody = reasonFromBody(input.body);
  if (fromBody.length > 0) {
    return new ApiError({ status: input.status, reason: fromBody });
  }
  return new ApiError({
    status: input.status,
    reason: STATUS_MESSAGES[input.status] ?? "Request failed. Please try again.",
  });
}
