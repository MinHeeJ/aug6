import { apiRequest } from "./apiClient";

export type EducationAchievementType =
  | "LECTURE_EVALUATION"
  | "LECTURE_ACHIEVEMENT"
  | "STUDENT_GUIDANCE"
  | "DEGREE_COMPLETION";

export type StudentGuidanceDetail = {
  guidanceStudentName: string;
  guidanceStartDate: string;
  guidanceEndDate: string;
  studentCount: number;
};

export type DegreeCompletionDetail = {
  degreeType: "MASTER" | "DOCTOR";
  studentName: string;
  thesisTitle: string;
  degreeAwardedDate: string;
};

export type EducationAchievement = {
  achievementId: number;
  facultyUserId: number;
  achievementType: EducationAchievementType;
  managementItemCode: string;
  occurrenceDate: string;
  certificationStatus: string;
  attachmentToken?: string | null;
  degreeCompletionDetails?: DegreeCompletionDetail[];
  studentGuidanceDetails?: StudentGuidanceDetail[];
};

export type EducationAchievementSearchResponse = {
  items: EducationAchievement[];
  page: number;
  size: 20 | 50 | 100;
  totalElements: number;
};

export type EducationAchievementSavePayload = {
  achievementId?: number;
  achievementType: EducationAchievementType;
  managementItemCode: string;
  occurrenceDate: string;
  degreeCompletionDetails?: DegreeCompletionDetail[];
  studentGuidanceDetails?: StudentGuidanceDetail[];
};

/** Uses only the normal relative API path for the education-achievement portal flow. */
export const educationAchievementApi = {
  list(params: {
    achievementType: EducationAchievementType;
    page?: number;
    size?: 20 | 50 | 100;
  }) {
    const query = new URLSearchParams({
      achievementType: params.achievementType,
      page: String(params.page ?? 0),
      size: String(params.size ?? 20),
    });
    return apiRequest<EducationAchievementSearchResponse>(
      `/api/business/education-achievements?${query.toString()}` as `/api/${string}`,
    );
  },
  save(payload: EducationAchievementSavePayload) {
    return apiRequest<EducationAchievement>(
      "/api/business/education-achievements",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};
