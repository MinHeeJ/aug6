import { apiRequest, type ApiResponse } from "./apiClient";

export type TeachingEvaluationAchievement = {
  achievementId: number;
  facultyUserId: number;
  evaluationYear: string;
  academicYear: string;
  semester: string;
  courseCode: string;
  courseName: string;
  evaluationScore: number;
  evaluationStatus: string;
  managementItemSettingId?: number | null;
  dynamicFields: Record<string, string>;
  attachmentRefs: string[];
  createdAt: string;
  updatedAt: string;
};

export type TeachingEvaluationAchievementSearch = {
  page?: number;
  size?: 20 | 50 | 100;
  evaluationYear?: string;
  academicYear?: string;
  semester?: string;
  courseKeyword?: string;
  evaluationStatus?: string;
};

export type TeachingEvaluationAchievementSearchResponse = {
  teachingEvaluationAchievements: TeachingEvaluationAchievement[];
  page: number;
  size: number;
  totalElements: number;
};

export type SaveTeachingEvaluationAchievement = {
  evaluationYear: string;
  academicYear: string;
  semester: string;
  courseCode: string;
  courseName: string;
  evaluationScore: number;
  managementItemSettingId?: number;
  dynamicFields: Record<string, string>;
  attachmentRefs: string[];
  changeReason: string;
};

const teachingEvaluationPath =
  "/api/business/teaching-evaluation-achievements" as const;

function searchPath(params: TeachingEvaluationAchievementSearch) {
  const query = new URLSearchParams();
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));
  if (params.evaluationYear?.trim())
    query.set("evaluationYear", params.evaluationYear.trim());
  if (params.academicYear?.trim())
    query.set("academicYear", params.academicYear.trim());
  if (params.semester?.trim()) query.set("semester", params.semester.trim());
  if (params.courseKeyword?.trim())
    query.set("courseKeyword", params.courseKeyword.trim());
  if (params.evaluationStatus?.trim())
    query.set("evaluationStatus", params.evaluationStatus.trim());
  return `${teachingEvaluationPath}?${query.toString()}` as `/api/${string}`;
}

export const teachingEvaluationAchievementApi = {
  list(params: TeachingEvaluationAchievementSearch = {}) {
    return apiRequest<TeachingEvaluationAchievementSearchResponse>(
      searchPath(params),
    );
  },
  save(payload: SaveTeachingEvaluationAchievement) {
    return apiRequest("/api/business/teaching-evaluation-achievements", {
      method: "POST",
      body: JSON.stringify(payload),
    }) as Promise<ApiResponse<TeachingEvaluationAchievement>>;
  },
};

export type TeachingAchievement = Omit<
  TeachingEvaluationAchievement,
  "evaluationScore" | "evaluationStatus"
> & {
  courseType: string;
  creditHours: number;
  achievementStatus: string;
};
export type TeachingAchievementSearch = Omit<
  TeachingEvaluationAchievementSearch,
  "evaluationStatus"
> & { achievementStatus?: string };
export type TeachingAchievementSearchResponse = {
  teachingAchievements: TeachingAchievement[];
  page: number;
  size: number;
  totalElements: number;
};
export type SaveTeachingAchievement = Omit<
  SaveTeachingEvaluationAchievement,
  "evaluationScore"
> & {
  achievementId?: number;
  courseType: string;
  creditHours: number;
};

const teachingAchievementPath = "/api/business/teaching-achievements" as const;
function teachingAchievementSearchPath(params: TeachingAchievementSearch) {
  const query = new URLSearchParams();
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));
  if (params.evaluationYear?.trim())
    query.set("evaluationYear", params.evaluationYear.trim());
  if (params.academicYear?.trim())
    query.set("academicYear", params.academicYear.trim());
  if (params.semester?.trim()) query.set("semester", params.semester.trim());
  if (params.courseKeyword?.trim())
    query.set("courseKeyword", params.courseKeyword.trim());
  if (params.achievementStatus?.trim())
    query.set("achievementStatus", params.achievementStatus.trim());
  return `${teachingAchievementPath}?${query.toString()}` as `/api/${string}`;
}
export const teachingAchievementApi = {
  list(params: TeachingAchievementSearch = {}) {
    return apiRequest<TeachingAchievementSearchResponse>(
      teachingAchievementSearchPath(params),
    );
  },
  save(payload: SaveTeachingAchievement) {
    return apiRequest("/api/business/teaching-achievements", {
      method: "POST",
      body: JSON.stringify(payload),
    }) as Promise<ApiResponse<TeachingAchievement>>;
  },
};

export type StudentGuidanceAchievement = {
  achievementId: number;
  evaluationYear: string;
  academicYear: string;
  semester: string;
  studentNo: string;
  studentName: string;
  guidanceType: string;
  guidanceDate: string;
  guidanceContent: string;
  achievementStatus: string;
};
export type SaveStudentGuidanceAchievement = Omit<
  StudentGuidanceAchievement,
  "achievementId" | "achievementStatus"
> & {
  achievementId?: number;
  dynamicFields: Record<string, string>;
  attachmentRefs: string[];
  changeReason: string;
};
export type StudentGuidanceSearchResponse = {
  studentGuidanceAchievements: StudentGuidanceAchievement[];
  page: number;
  size: number;
  totalElements: number;
};
const studentGuidancePath =
  "/api/business/student-guidance-achievements" as const;
export type StudentGuidanceExcelRowError = {
  rowNumber: number;
  columnName: string;
  errorCode: string;
  errorReason: string;
};
export type StudentGuidanceExcelUploadResult = {
  uploadId: string;
  validationStatus: "COMMITTED" | "REJECTED";
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
  errors: StudentGuidanceExcelRowError[];
};

export const studentGuidanceAchievementApi = {
  list(
    params: {
      studentKeyword?: string;
      evaluationYear?: string;
      size?: 20 | 50 | 100;
    } = {},
  ) {
    const query = new URLSearchParams({
      page: "0",
      size: String(params.size ?? 20),
    });
    if (params.studentKeyword?.trim())
      query.set("studentKeyword", params.studentKeyword.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    return apiRequest<StudentGuidanceSearchResponse>(
      `${studentGuidancePath}?${query.toString()}` as `/api/${string}`,
    );
  },
  save(payload: SaveStudentGuidanceAchievement) {
    return apiRequest<StudentGuidanceAchievement>(studentGuidancePath, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  upload(file: File, templateId = "STUDENT-GUIDANCE-V1") {
    const form = new FormData();
    form.append("file", file);
    return apiRequest<StudentGuidanceExcelUploadResult>(
      `${studentGuidancePath}/excel-uploads?${new URLSearchParams({ templateId }).toString()}` as `/api/${string}`,
      { method: "POST", body: form },
    );
  },
};

export type GraduateAchievement = {
  achievementId: number;
  evaluationYear: string;
  academicYear: string;
  semester: string;
  studentNo: string;
  studentName: string;
  degreeType: "MASTER" | "DOCTOR";
  thesisTitle: string;
  awardDate: string;
  achievementStatus: string;
  managementItemSettingId?: number | null;
  dynamicFields?: Record<string, string>;
  attachmentRefs?: string[];
};
export type SaveGraduateAchievement = Omit<
  GraduateAchievement,
  "achievementId" | "achievementStatus"
> & { achievementId?: number; changeReason: string };
export type GraduateAchievementSearchResponse = {
  graduateAchievements: GraduateAchievement[];
  page: number;
  size: number;
  totalElements: number;
};
const graduateAchievementPath = "/api/business/graduate-achievements" as const;
export const graduateAchievementApi = {
  list(
    params: {
      studentKeyword?: string;
      degreeType?: string;
      evaluationYear?: string;
      size?: 20 | 50 | 100;
    } = {},
  ) {
    const query = new URLSearchParams({
      page: "0",
      size: String(params.size ?? 20),
    });
    if (params.studentKeyword?.trim())
      query.set("studentKeyword", params.studentKeyword.trim());
    if (params.degreeType?.trim())
      query.set("degreeType", params.degreeType.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    return apiRequest<GraduateAchievementSearchResponse>(
      `${graduateAchievementPath}?${query.toString()}` as `/api/${string}`,
    );
  },
  save(payload: SaveGraduateAchievement) {
    return apiRequest<GraduateAchievement>(graduateAchievementPath, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};
