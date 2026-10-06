import { apiRequest } from "../../api/apiClient";
import { checkUserIdAvailability } from "../availability/availabilityApi";

export type SignupRequest = {
  userId: string;
  password: string;
  passwordConfirm: string;
  email: string;
};

export const signupApi = {
  signup(request: SignupRequest) {
    return apiRequest<{ userId: string; message: string }>(
      "/api/v1/auth/signup",
      {
        method: "POST",
        body: JSON.stringify(request),
      },
    );
  },
  checkUserId: checkUserIdAvailability,
};
