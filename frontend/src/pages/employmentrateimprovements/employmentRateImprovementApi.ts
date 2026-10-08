import { apiRequest } from "../../api/apiClient";

export type EmploymentRateImprovement = {
  achievementId: number;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string | null;
  achievementDetail: string;
  attachmentRef?: string | null;
  attachmentIds: string;
  achievementStatus: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
};

export type EmploymentRateImprovementRequest = {
  managementItemCode: string;
  achievementDate: string;
  evaluationYear?: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  achievementName?: string | null;
  achievementDetail?: unknown;
  attachmentRef?: string | null;
  attachmentIds: string[];
};

export type EmploymentRateImprovementSearch = {
  achievements: EmploymentRateImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type EmploymentRateImprovementSave = {
  achievement: EmploymentRateImprovement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};

const base = "/api/business/employment-rate-improvements";

export const employmentRateImprovementApi = {
  list(query: URLSearchParams) {
    return apiRequest<EmploymentRateImprovementSearch>(`${base}?${query}`);
  },
  get(achievementId: number) {
    return apiRequest<EmploymentRateImprovement>(`${base}/${achievementId}`);
  },
  create(body: EmploymentRateImprovementRequest) {
    return apiRequest<EmploymentRateImprovementSave>(base, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
  update(achievementId: number, body: EmploymentRateImprovementRequest) {
    return apiRequest<EmploymentRateImprovementSave>(
      `${base}/${achievementId}`,
      {
        method: "PUT",
        body: JSON.stringify(body),
      },
    );
  },
};
