import { apiRequest } from "../../api/apiClient";

/** Checks the entered public login ID, never an internal numeric account key. */
export async function checkUserIdAvailability(userId: string) {
  const query = new URLSearchParams({ userId });
  const response = await apiRequest<{ available: boolean }>(
    `/api/v1/auth/check-userid?${query.toString()}`,
  );
  // Missing/invalid data is a failed check, not evidence that the identifier is reserved.
  if (typeof response.data?.available !== "boolean") {
    throw new Error("아이디 확인 응답을 확인할 수 없습니다.");
  }
  return response;
}
