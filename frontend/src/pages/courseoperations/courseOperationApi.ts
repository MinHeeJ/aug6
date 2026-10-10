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

export type CourseOperationInput = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  evaluationYear?: string;
  attachmentIds: string[];
  achievementStatus?: string;
};

export type CourseOperationList = {
  achievements: CourseOperation[];
  page: number;
  pageSize: number;
  totalElements: number;
  managementItems: { managementItemCode: string; managementItemName: string }[];
};

export type CourseOperationSave = {
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string;
};

export const courseOperationApi = {
  list(params: URLSearchParams) {
    return apiRequest<CourseOperationList>(
      `/api/business/course-operations?${params}`,
    );
  },
  detail(achievementId: number) {
    return apiRequest<CourseOperation>(
      `/api/business/course-operations/${achievementId}`,
    );
  },
  create(body: CourseOperationInput) {
    return apiRequest<CourseOperationSave>("/api/business/course-operations", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
  update(achievementId: number, body: CourseOperationInput) {
    return apiRequest<CourseOperationSave>(
      `/api/business/course-operations/${achievementId}`,
      {
        method: "PUT",
        body: JSON.stringify(body),
      },
    );
  },
};
