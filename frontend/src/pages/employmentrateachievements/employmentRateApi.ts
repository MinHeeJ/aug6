import { ApiClientError, type ApiResponse } from "../../api/apiClient";

export const employmentRateBase = "/api/business/employment-rate-achievements";

// Feature transport normalizes the new object fields without changing legacy API consumers.
export async function employmentRateRequest<T>(
  suffix: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  if (!(init.body instanceof FormData) && init.body)
    headers.set("Content-Type", "application/json");
  const response = await fetch(`${employmentRateBase}${suffix}`, {
    ...init,
    headers,
    credentials: "include",
  });
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || !body.success) {
    const fields = body.error?.fields;
    const normalized = Array.isArray(fields)
      ? fields
      : Object.entries(fields ?? {}).map(([field, message]) => ({
          field,
          message: String(message),
        }));
    throw new ApiClientError(
      response.status,
      body.error?.message ?? "요청에 실패했습니다.",
      {
        code: body.error?.code ?? "ERROR",
        message: body.error?.message ?? "요청에 실패했습니다.",
        fields: normalized,
      },
    );
  }
  return body.data as T;
}

export async function employmentRateDownload(suffix: string, filename: string) {
  const response = await fetch(`${employmentRateBase}${suffix}`, {
    credentials: "include",
  });
  if (!response.ok) {
    const body = await response.json();
    throw new ApiClientError(
      response.status,
      body.error?.message ?? "파일을 내려받지 못했습니다.",
    );
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}
