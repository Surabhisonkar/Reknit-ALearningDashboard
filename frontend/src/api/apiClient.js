import { config } from "../config/env.js";

export class ApiError extends Error {
  constructor(message, status, details, body) {
    super(message);
    this.status = status;
    this.details = details;
    // Full parsed error body - lets callers read structured fields such as
    // { code: "DUPLICATE_TITLE", duplicateConceptId } without re-fetching.
    this.body = body ?? {};
  }
}

/**
 * The one place that knows the backend's base URL and error response
 * shape ({@code { error, details? } }, matching GlobalExceptionHandler).
 * Every {@code src/api/*} module builds on this rather than calling
 * {@code fetch} directly.
 */
export async function apiRequest(method, path, { accessToken, body, signal } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${config.apiBaseUrl}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal,
  });

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new ApiError(data.error ?? "Request failed.", response.status, data.details, data);
  }

  return data;
}
