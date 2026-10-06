import { apiRequest } from "../../api/apiClient";

export type SignupRequest = {
  userId: string;
  password: string;
  passwordConfirm: string;
  email: string;
};
export type SignupResponse = { userId: string; message: string };

export const signupApi = {
  checkUserIdAvailability(userId: string) {
    const query = new URLSearchParams({ userId });
    return apiRequest<{ available: boolean }>(
      `/api/v1/auth/check-userid?${query}`,
    );
  },
  signup(request: SignupRequest) {
    return apiRequest<SignupResponse>("/api/v1/auth/signup", {
      method: "POST",
      body: JSON.stringify(request),
    });
  },
};
