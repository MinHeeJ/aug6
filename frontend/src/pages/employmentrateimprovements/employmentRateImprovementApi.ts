import { apiRequest } from "../../api/apiClient";

export type EmploymentRateImprovement = {
  achievementId: number;
  teacherUserId: number;
  organizationCode: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  specialLectureStartDate: string | null;
  specialLectureEndDate: string | null;
  mockExamQuestionPeriod: string | null;
  attachmentRef: string | null;
};

export type EmploymentRateImprovementInput = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string | null;
  specialLectureEndDate: string | null;
  mockExamQuestionPeriod: string | null;
  attachmentRef: string | null;
};

export type ImprovementList = {
  achievements: EmploymentRateImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
  managementItems: string[];
};

export const employmentRateImprovementApi = {
  list(
    page: number,
    pageSize: number,
    managementItemCode = "",
    achievementStatus = "",
  ) {
    const query = new URLSearchParams({
      page: String(page),
      pageSize: String(pageSize),
    });
    if (managementItemCode) query.set("managementItemCode", managementItemCode);
    if (achievementStatus) query.set("achievementStatus", achievementStatus);
    return apiRequest<ImprovementList>(
      `/api/business/employment-rate-improvements?${query}`,
    );
  },
  get(id: number) {
    return apiRequest<EmploymentRateImprovement>(
      `/api/business/employment-rate-improvements/${id}`,
    );
  },
  save(id: number | undefined, input: EmploymentRateImprovementInput) {
    const path =
      id === undefined
        ? "/api/business/employment-rate-improvements"
        : (`/api/business/employment-rate-improvements/${id}` as const);
    return apiRequest<{
      achievement: EmploymentRateImprovement;
      occurredDateWarning: boolean;
      warningMessage?: string;
    }>(path, {
      method: id === undefined ? "POST" : "PUT",
      body: JSON.stringify(input),
    });
  },
};
