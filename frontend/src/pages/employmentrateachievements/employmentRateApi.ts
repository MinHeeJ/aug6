import {
  apiRequest,
  ApiClientError,
  type ApiResponse,
} from "../../api/apiClient";

export const employmentRatePath = "/api/business/employment-rate-achievements";
export type EmploymentAchievement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  evaluationYear: string;
  title: string;
  attachmentRef?: string;
  achievementStatus: string;
};
export type EmploymentSearch = {
  achievements: EmploymentAchievement[];
  managementItems: { code: string; name: string; evaluationYear: string }[];
  totalElements: number;
  page: number;
  pageSize: number;
};
export type EmploymentInput = {
  managementItemCode: string;
  achievementDate: string;
  title: string;
  attachmentRef: string | null;
  evaluationYear?: string;
  achievementStatus?: string;
};
export type EmploymentUpload = {
  uploadId: string;
  validationStatus: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
  originalFileName?: string;
};
export type EmploymentJob = {
  jobId: string;
  totalCount: number;
  processedCount: number;
  unprocessedCount: number;
  items: {
    targetUserId: number;
    processedYn: string;
    unprocessedReason?: string;
  }[];
};

export const employmentRateApi = {
  list(query: URLSearchParams) {
    return apiRequest<EmploymentSearch>(`${employmentRatePath}?${query}`);
  },
  detail(id: number) {
    return apiRequest<EmploymentAchievement>(`${employmentRatePath}/${id}`);
  },
  save(body: EmploymentInput, id?: number) {
    return apiRequest<{
      achievement: EmploymentAchievement;
      occurredDateWarning: boolean;
      warningMessage: string;
    }>(id === undefined ? employmentRatePath : `${employmentRatePath}/${id}`, {
      method: id === undefined ? "POST" : "PUT",
      body: JSON.stringify(body),
    });
  },
  job(id: string) {
    return apiRequest<EmploymentJob>(
      `${employmentRatePath}/bulk-jobs/${encodeURIComponent(id)}`,
    );
  },
  histories() {
    return apiRequest<EmploymentUpload[]>(
      `${employmentRatePath}/excel-uploads/histories`,
    );
  },
  commit(id: string) {
    return apiRequest<EmploymentUpload>(
      `${employmentRatePath}/excel-uploads/${encodeURIComponent(id)}/commit`,
      {
        method: "POST",
      },
    );
  },
  async upload(file: File) {
    const form = new FormData();
    form.append("file", file);
    const response = await fetch(`${employmentRatePath}/excel-uploads`, {
      method: "POST",
      credentials: "include",
      body: form,
    });
    const body = (await response.json()) as ApiResponse<EmploymentUpload>;
    if (!response.ok) {
      if (response.status === 400 && typeof body.meta.uploadId === "string") {
        return {
          success: false,
          data: body.meta as unknown as EmploymentUpload,
          meta: body.meta,
        };
      }
      throw new ApiClientError(
        response.status,
        body.error?.message ?? "업로드 실패",
        body.error,
      );
    }
    return body;
  },
  async download(suffix: string, fileName: string) {
    const response = await fetch(`${employmentRatePath}${suffix}`, {
      credentials: "include",
    });
    if (!response.ok) {
      const body = (await response.json()) as ApiResponse<never>;
      throw new ApiClientError(
        response.status,
        body.error?.message ?? "다운로드 실패",
        body.error,
      );
    }
    const url = URL.createObjectURL(await response.blob());
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  },
};
