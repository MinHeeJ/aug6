import { apiRequest } from "../../api/apiClient";

export type CourseOperation = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  organizationCode: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds: string[];
};

export type CourseOperationRequest = Pick<
  CourseOperation,
  | "managementItemCode"
  | "achievementDate"
  | "performanceDetails"
  | "attachmentIds"
>;

export type CourseOperationPage = {
  achievements: CourseOperation[];
  page: number;
  pageSize: number;
  totalElements: number;
  managementItems: {
    code: string;
    name: string;
    evaluationYear: string;
    teacherEditablePart: string;
  }[];
};

export type CourseOperationSaveResult = {
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string;
};

export const courseOperationsApi = {
  list(query: URLSearchParams) {
    return apiRequest<CourseOperationPage>(
      `/api/business/course-operations?${query.toString()}`,
    );
  },
  detail(id: number) {
    return apiRequest<CourseOperation>(`/api/business/course-operations/${id}`);
  },
  save(id: number | undefined, body: CourseOperationRequest) {
    return apiRequest<CourseOperationSaveResult>(
      id === undefined
        ? "/api/business/course-operations"
        : `/api/business/course-operations/${id}`,
      { method: id === undefined ? "POST" : "PUT", body: JSON.stringify(body) },
    );
  },
};
