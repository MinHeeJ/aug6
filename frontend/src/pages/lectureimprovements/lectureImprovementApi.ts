import { apiRequest } from "../../api/apiClient";

export type LectureImprovementRequest = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  attachmentIds: string[];
};
export type LectureImprovementRow = LectureImprovementRequest & {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  teacherUserId: number;
  evaluationYear: string;
  achievementStatus: string;
};
export type ManagementItem = {
  managementItemCode: string;
  managementItemName: string;
  requiredYn: string;
  dataType: string;
  teacherEditableYn: string;
  evaluationYear: string;
};
export type SearchResponse = {
  achievements: LectureImprovementRow[];
  totalElements: number;
  page: number;
  pageSize: number;
  managementItems: ManagementItem[];
};
export type SaveResult = {
  achievementId: number;
  achievement: LectureImprovementRow;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
export const lectureImprovementApi = {
  list(query: URLSearchParams) {
    return apiRequest<SearchResponse>(
      `/api/business/lecture-improvements?${query}`,
    );
  },
  get(id: number) {
    return apiRequest<LectureImprovementRow>(
      `/api/business/lecture-improvements/${id}`,
    );
  },
  save(body: LectureImprovementRequest, id?: number) {
    return apiRequest<SaveResult>(
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
