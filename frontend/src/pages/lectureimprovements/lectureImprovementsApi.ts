import { apiRequest } from "../../api/apiClient";

export type LectureImprovement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceContent: string;
  academicYear: string;
  semester: string;
  achievementStatus: string;
  attachmentRef: string | null;
};

export type LectureImprovementInput = {
  managementItemCode: string;
  achievementDate: string;
  performanceContent: string;
  academicYear: string;
  semester: string;
  evaluationYear?: string;
  attachmentRef?: string;
  achievementStatus?: string;
};

export type LectureImprovementOption = {
  code: string;
  name: string;
  academicYear?: string;
  evaluationYear?: string;
};

export type LectureImprovementList = {
  achievements: LectureImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
  semesters: LectureImprovementOption[];
  managementItems: LectureImprovementOption[];
};

export const lectureImprovementsApi = {
  list(query: URLSearchParams) {
    return apiRequest<LectureImprovementList>(
      `/api/business/lecture-improvements?${query}`,
    );
  },
  detail(id: number) {
    return apiRequest<LectureImprovement>(
      `/api/business/lecture-improvements/${id}`,
    );
  },
  save(body: LectureImprovementInput, id?: number) {
    return apiRequest<{
      achievement: LectureImprovement;
      occurredDateWarning: boolean;
      warningMessage?: string;
    }>(
      id === undefined
        ? "/api/business/lecture-improvements"
        : `/api/business/lecture-improvements/${id}`,
      {
        method: id === undefined ? "POST" : "PUT",
        body: JSON.stringify(body),
      },
    );
  },
};
