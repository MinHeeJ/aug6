import { Download, Plus, Save, Upload } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "../../app/AuthProvider";
import {
  ApiClientError,
  educationAchievementApi,
  type EducationAchievement,
  type PageSize,
  type StudentGuidanceDetailPayload,
} from "../../api/apiClient";
import {
  excelApi,
  type ExcelTemplateRow,
  type ExcelUploadResult,
} from "../admin/ExcelOperationsPages";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type GuidanceForm = {
  managementItemCode: string;
  occurrenceDate: string;
  details: StudentGuidanceDetailPayload[];
};

const emptyForm: GuidanceForm = {
  managementItemCode: "",
  occurrenceDate: "",
  details: [],
};

/** Renders individual student-guidance registration and the R07-controlled Excel bulk workflow. */
export function StudentGuidanceAchievementPage() {
  const auth = useAuth();
  const canUseBulkUpload = Boolean(
    auth.user?.roles.includes("R07") || auth.user?.roles.includes("R09"),
  );
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [form, setForm] = useState<GuidanceForm>(emptyForm);
  const [creating, setCreating] = useState(false);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [templates, setTemplates] = useState<ExcelTemplateRow[]>([]);
  const [templateId, setTemplateId] = useState("");
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState<ExcelUploadResult | null>(
    null,
  );

  const handleError = (caught: unknown, fallback: string) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(caught instanceof Error ? caught.message : fallback);
  };

  const loadAchievements = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await educationAchievementApi.listEducationAchievements({
        achievementType: "STUDENT_GUIDANCE",
        size: pageSize,
      });
      setItems(response.data?.items ?? []);
    } catch (caught) {
      handleError(caught, "학생지도 실적을 조회하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const loadTemplates = async () => {
    try {
      const response = await excelApi.listTemplates({
        businessType: "STUDENT_GUIDANCE_ACHIEVEMENT",
      });
      const rows = response.data?.templates ?? [];
      setTemplates(rows);
      setTemplateId((current) => current || rows[0]?.templateId || "");
    } catch (caught) {
      handleError(caught, "학생지도 Excel 양식을 조회하지 못했습니다.");
    }
  };

  useEffect(() => {
    void loadAchievements();
  }, [pageSize]);

  useEffect(() => {
    if (canUseBulkUpload) void loadTemplates();
  }, [canUseBulkUpload]);

  const addGuidance = () => {
    setForm((current) => ({
      ...current,
      details: [
        ...current.details,
        {
          studentName: "",
          guidanceStartDate: "",
          guidanceEndDate: "",
          studentCount: 1,
        },
      ],
    }));
  };

  const updateGuidance = (
    index: number,
    key: keyof StudentGuidanceDetailPayload,
    value: string | number,
  ) => {
    setForm((current) => ({
      ...current,
      details: current.details.map((detail, detailIndex) =>
        detailIndex === index ? { ...detail, [key]: value } : detail,
      ),
    }));
  };

  const saveIndividual = async () => {
    if (
      !form.managementItemCode.trim() ||
      !form.occurrenceDate ||
      form.details.length === 0
    ) {
      setError("관리항목, 발생일, 지도학생 정보를 모두 입력하세요.");
      return;
    }
    if (!window.confirm("학생지도 실적을 저장하시겠습니까?")) return;
    try {
      setError(null);
      await educationAchievementApi.saveEducationAchievement({
        achievementType: "STUDENT_GUIDANCE",
        managementItemCode: form.managementItemCode.trim(),
        occurrenceDate: form.occurrenceDate,
        studentGuidanceDetails: form.details,
      });
      setSuccess("학생지도 실적을 저장하고 목록을 재조회했습니다.");
      setForm(emptyForm);
      setCreating(false);
      await loadAchievements();
    } catch (caught) {
      handleError(caught, "학생지도 실적을 저장하지 못했습니다.");
    }
  };

  const downloadTemplate = async () => {
    if (!templateId) {
      setError("사용 가능한 학생지도 업로드 양식이 없습니다.");
      return;
    }
    try {
      const response = await excelApi.downloadTemplate(templateId);
      if (!response.ok)
        throw new ApiClientError(
          response.status,
          "양식 다운로드 권한이 없습니다.",
        );
      const blob = await response.blob();
      const fileName =
        templates.find((template) => template.templateId === templateId)
          ?.originalFileName ?? "학생지도_업로드양식.csv";
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch (caught) {
      handleError(caught, "양식을 다운로드하지 못했습니다.");
    }
  };

  const upload = async () => {
    if (!uploadFile || !templateId) {
      setError("양식과 업로드 파일을 선택하세요.");
      return;
    }
    if (!window.confirm("학생지도 파일을 업로드하고 검증하시겠습니까?")) return;
    try {
      setUploading(true);
      setError(null);
      const response = await excelApi.createUpload(
        "STUDENT_GUIDANCE_ACHIEVEMENT",
        templateId,
        uploadFile,
      );
      setUploadResult(response.data ?? null);
      setSuccess(
        "파일 검증이 완료되었습니다. 오류 행이 있으면 전체 반영할 수 없습니다.",
      );
    } catch (caught) {
      handleError(caught, "학생지도 파일을 검증하지 못했습니다.");
    } finally {
      setUploading(false);
    }
  };

  const commit = async () => {
    if (!uploadResult?.uploadId || uploadResult.errorCount > 0) return;
    if (!window.confirm("검증된 학생지도 데이터를 전체 반영하시겠습니까?"))
      return;
    try {
      setUploading(true);
      const response = await excelApi.commitUpload(uploadResult.uploadId);
      setSuccess(
        `학생지도 ${response.data?.savedCount ?? 0}건을 전체 반영했습니다.`,
      );
      await loadAchievements();
    } catch (caught) {
      handleError(caught, "학생지도 데이터를 반영하지 못했습니다.");
    } finally {
      setUploading(false);
    }
  };

  const downloadErrors = async () => {
    if (!uploadResult?.uploadId) return;
    try {
      const response = await excelApi.downloadErrors(uploadResult.uploadId);
      if (!response.ok)
        throw new ApiClientError(
          response.status,
          "오류 파일 다운로드 권한이 없습니다.",
        );
      const link = document.createElement("a");
      link.href = URL.createObjectURL(await response.blob());
      link.download = `학생지도_업로드오류_${uploadResult.uploadId}.csv`;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch (caught) {
      handleError(caught, "오류 파일을 다운로드하지 못했습니다.");
    }
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"
        data-testid="student-guidance-permission-state"
      >
        <PermissionState
          title="학생지도 실적 권한이 없습니다."
          message={error ?? "권한을 확인하세요."}
        />
      </section>
    );
  }

  return (
    <main
      className="space-y-6"
      data-screen-id="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"
      data-testid="student-guidance-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 실적 관리
        </h1>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              setCreating(true);
              setSuccess(null);
            }}
            data-testid="student-guidance-create-button"
          >
            <Plus size={16} />
            개별 등록
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => void loadAchievements()}
            data-testid="student-guidance-search-button"
          >
            조회
          </button>
        </div>
      </header>
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="학생지도 처리 오류" message={error} /> : null}
      <section className="grid grid-cols-12 gap-6">
        <section className="col-span-12 p-6 lg:col-span-7">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold">학생지도 실적 목록</h2>
            <select
              className="form-input w-28"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as PageSize)
              }
              data-testid="student-guidance-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </div>
          {loading ? (
            <LoadingState
              title="조회 중"
              message="학생지도 실적을 불러오고 있습니다."
            />
          ) : items.length === 0 ? (
            <EmptyState
              title="등록된 학생지도 실적이 없습니다"
              message="개별 등록 또는 Excel 일괄등록을 이용하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    <th>지도학생</th>
                    <th>지도기간</th>
                    <th>학생수</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  {items.flatMap((item) =>
                    (item.studentGuidanceDetails ?? []).map((detail) => (
                      <tr
                        key={`${item.achievementId}-${detail.studentGuidanceDetailId}`}
                        data-testid="student-guidance-row"
                      >
                        <td>{detail.studentName}</td>
                        <td>
                          {detail.guidanceStartDate} ~ {detail.guidanceEndDate}
                        </td>
                        <td>{detail.studentCount}</td>
                        <td>{item.achievementStatus}</td>
                      </tr>
                    )),
                  )}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <aside className="col-span-12 p-6 lg:col-span-5">
          <h2 className="text-lg font-semibold">개별 등록</h2>
          {creating ? (
            <div className="space-y-3">
              <input
                className="form-input w-full"
                placeholder="관리항목 코드"
                value={form.managementItemCode}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
                data-testid="student-guidance-management-item-input"
              />
              <input
                type="date"
                className="form-input w-full"
                value={form.occurrenceDate}
                onChange={(event) =>
                  setForm({ ...form, occurrenceDate: event.target.value })
                }
                data-testid="student-guidance-occurrence-date-input"
              />
              <button
                type="button"
                className="btn-secondary"
                onClick={addGuidance}
                data-testid="student-guidance-add-detail-button"
              >
                지도학생 추가
              </button>
              {form.details.map((detail, index) => (
                <div
                  key={index}
                  className="grid gap-2 border-t pt-3"
                  data-testid="student-guidance-detail-row"
                >
                  <input
                    className="form-input"
                    placeholder="지도학생"
                    value={detail.studentName}
                    onChange={(event) =>
                      updateGuidance(index, "studentName", event.target.value)
                    }
                    data-testid="student-guidance-student-name-input"
                  />
                  <input
                    type="date"
                    className="form-input"
                    value={detail.guidanceStartDate}
                    onChange={(event) =>
                      updateGuidance(
                        index,
                        "guidanceStartDate",
                        event.target.value,
                      )
                    }
                    data-testid="student-guidance-start-date-input"
                  />
                  <input
                    type="date"
                    className="form-input"
                    value={detail.guidanceEndDate}
                    onChange={(event) =>
                      updateGuidance(
                        index,
                        "guidanceEndDate",
                        event.target.value,
                      )
                    }
                    data-testid="student-guidance-end-date-input"
                  />
                  <input
                    type="number"
                    min="1"
                    className="form-input"
                    value={detail.studentCount}
                    onChange={(event) =>
                      updateGuidance(
                        index,
                        "studentCount",
                        Number(event.target.value),
                      )
                    }
                    data-testid="student-guidance-count-input"
                  />
                </div>
              ))}
              <button
                type="button"
                className="btn-primary w-full"
                onClick={() => void saveIndividual()}
                data-testid="student-guidance-save-button"
              >
                <Save size={16} />
                저장
              </button>
            </div>
          ) : (
            <p className="mt-3 text-sm text-muted">
              개별 등록 버튼을 눌러 지도학생 정보를 입력하세요.
            </p>
          )}
        </aside>
      </section>
      {canUseBulkUpload ? (
        <section className="p-6" data-testid="student-guidance-excel-panel">
          <h2 className="text-lg font-semibold">Excel 일괄등록</h2>
          <p className="mt-2 text-sm text-muted">
            템플릿 다운로드 → 파일 업로드 → 검증 → 검증결과 확인 → 반영 순으로
            처리합니다.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <select
              className="form-input"
              value={templateId}
              onChange={(event) => setTemplateId(event.target.value)}
              data-testid="student-guidance-template-select"
            >
              <option value="">양식 선택</option>
              {templates.map((template) => (
                <option key={template.templateId} value={template.templateId}>
                  {template.templateVersion} / {template.effectiveDate}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => void downloadTemplate()}
              data-testid="student-guidance-template-download-button"
            >
              <Download size={16} />
              템플릿 다운로드
            </button>
            <input
              type="file"
              accept=".csv,.xls,.xlsx"
              className="form-input"
              onChange={(event) =>
                setUploadFile(event.target.files?.[0] ?? null)
              }
              data-testid="student-guidance-upload-file-input"
            />
            <button
              type="button"
              className="btn-primary"
              disabled={uploading}
              onClick={() => void upload()}
              data-testid="student-guidance-upload-button"
            >
              <Upload size={16} />
              {uploading ? "검증 중" : "파일 업로드·검증"}
            </button>
          </div>
          {uploadResult ? (
            <div
              className="mt-5"
              data-testid="student-guidance-validation-result"
            >
              <div className="grid gap-2 text-sm sm:grid-cols-4">
                <b>전체 {uploadResult.totalCount}건</b>
                <b>정상 {uploadResult.successCount}건</b>
                <b>오류 {uploadResult.errorCount}건</b>
                <b>저장 {uploadResult.savedCount}건</b>
              </div>
              {uploadResult.errors.length > 0 ? (
                <>
                  <p className="mt-3 text-sm text-error">
                    오류 행이 있어 전체 반영할 수 없습니다.
                  </p>
                  <button
                    type="button"
                    className="btn-secondary mt-3"
                    onClick={() => void downloadErrors()}
                    data-testid="student-guidance-error-download-button"
                  >
                    오류파일 다운로드
                  </button>
                  <table className="mt-3">
                    <tbody>
                      {uploadResult.errors.map((row) => (
                        <tr
                          key={row.errorId}
                          data-testid="student-guidance-upload-error-row"
                        >
                          <td>{row.rowNumber}</td>
                          <td>{row.columnName}</td>
                          <td>{row.errorReason}</td>
                          <td>{row.correctionGuide}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              ) : (
                <button
                  type="button"
                  className="btn-primary mt-4"
                  disabled={uploading}
                  onClick={() => void commit()}
                  data-testid="student-guidance-commit-button"
                >
                  전체 반영
                </button>
              )}
            </div>
          ) : null}
        </section>
      ) : null}
    </main>
  );
}
