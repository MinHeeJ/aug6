import {
  ApiClientError,
  apiRequest,
  type ApiResponse,
} from "../../api/apiClient";
import type { ExcelUploadResult } from "../admin/ExcelOperationsPages";
export type AchievementInput = {
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  attachmentIds: string[];
};
export type Achievement = AchievementInput & {
  achievementId: number;
  teacherUserId: number;
  managementNo: string;
  teacherName: string;
  certificationStatus: string;
  achievementStatus: string;
};
export type AchievementSaveResult = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage: string | null;
};
export type AchievementList = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};
export type BulkResult = {
  jobId: string;
  status: string;
  processedCount: number;
  unprocessedCount: number;
  items: {
    targetUserId: number;
    processedYn: string;
    unprocessedReason?: string;
  }[];
};
const base = "/api/business/employment-rate-achievements";
export const BUSINESS_TYPE = "EMPLOYMENT_RATE_ACHIEVEMENT";
export const employmentRateApi = {
  list(page: number, pageSize: number) {
    return apiRequest<AchievementList>(
      `${base}?${new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      })}`,
    );
  },
  detail(id: number) {
    return apiRequest<Achievement>(`${base}/${id}`);
  },
  create(input: AchievementInput) {
    return apiRequest<AchievementSaveResult>(base, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
  update(id: number, input: AchievementInput) {
    return apiRequest<AchievementSaveResult>(`${base}/${id}`, {
      method: "PUT",
      body: JSON.stringify(input),
    });
  },
  download(page: number, pageSize: number) {
    return fetch(
      `${base}/download?${new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      })}`,
      {
        credentials: "include",
      },
    );
  },
  async upload(file: File) {
    const form = new FormData();
    form.append("file", file);
    const response = await fetch(`${base}/excel-uploads`, {
      method: "POST",
      credentials: "include",
      body: form,
    });
    const body = (await response.json()) as ApiResponse<ExcelUploadResult>;
    // Validation failures still carry the persisted upload ID and row diagnostics.
    return {
      body,
      ok: response.ok && body.success !== false,
      status: response.status,
    };
  },
  createBulk(input: {
    evaluationYear: string;
    actionType: "GENERATE" | "DELETE";
    targetCondition: Record<string, unknown>;
  }) {
    return apiRequest<{
      jobId: string;
    }>(`${base}/bulk-jobs`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
  bulkResult(id: string) {
    return apiRequest<BulkResult>(
      `${base}/bulk-jobs/${encodeURIComponent(id)}`,
    );
  },
};
export async function saveXlsx(response: Response, fallback: string) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiClientError(
      response.status,
      body.error?.message ?? "파일 다운로드에 실패했습니다.",
      body.error,
    );
  }
  const type = response.headers.get("content-type") ?? "";
  if (
    !type.includes(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ) &&
    !type.includes("application/octet-stream")
  ) {
    throw new Error("서버가 XLSX 파일을 반환하지 않았습니다.");
  }
  const blob = await response.blob();
  const disposition = response.headers.get("content-disposition") ?? "";
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
  const plain = /filename="?([^";]+)"?/i.exec(disposition)?.[1];
  const name = (
    encoded ? decodeURIComponent(encoded) : (plain ?? fallback)
  ).replace(/[\\/]/g, "_");
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = name;
  document.body.appendChild(anchor);
  try {
    anchor.click();
  } finally {
    anchor.remove();
    URL.revokeObjectURL(url);
  }
}
