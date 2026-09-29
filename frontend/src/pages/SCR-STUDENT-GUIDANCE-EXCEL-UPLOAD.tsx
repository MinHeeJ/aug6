import { useState } from "react";
import { Download, FileSpreadsheet, Save, Upload } from "lucide-react";
import { useAuth } from "../app/AuthProvider";
import { ApiClientError, apiRequest } from "../api/apiClient";
import {
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../components/States";
import type { ExcelUploadResult } from "./admin/ExcelOperationsPages";

type ScreenStatus = "idle" | "loading" | "error" | "permission" | "success";

function errorMessage(error: unknown) {
  if (error instanceof ApiClientError && error.status === 403)
    return "학생지도 Excel 일괄등록 권한이 없습니다.";
  return error instanceof Error
    ? error.message
    : "처리 중 오류가 발생했습니다.";
}

async function uploadStudentGuidanceFile(templateId: string, file: File) {
  const form = new FormData();
  form.append("file", file);
  const query = templateId.trim()
    ? `?${new URLSearchParams({ templateId: templateId.trim() }).toString()}`
    : "";
  const response = await fetch(
    `/api/business/student-guidance-achievements/excel-uploads${query}`,
    {
      method: "POST",
      body: form,
      credentials: "include",
    },
  );
  const body = await response.json();
  if (!response.ok || body.success === false) {
    throw new ApiClientError(
      response.status,
      body.error?.message ?? "업로드·검증에 실패했습니다.",
      body.error,
    );
  }
  return body.data as ExcelUploadResult;
}

/**
 * R07의 학생지도 Excel 일괄등록 화면으로, 양식 다운로드부터 검증결과 확인과 전체 반영까지
 * 공통 Excel 업로드 상태를 업무 전용 API로 연결한다.
 */
export function StudentGuidanceExcelUploadPage() {
  const auth = useAuth();
  const [templateId, setTemplateId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ExcelUploadResult | null>(null);
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [message, setMessage] = useState("");
  const [commitOpen, setCommitOpen] = useState(false);

  const downloadTemplate = () => {
    if (!templateId.trim()) {
      setStatus("error");
      setMessage("다운로드할 양식 ID를 입력하세요.");
      return;
    }
    window.location.assign(
      `/api/business/student-guidance-achievements/excel-upload-templates/${encodeURIComponent(templateId.trim())}/file`,
    );
  };

  const upload = async () => {
    if (!file) {
      setStatus("error");
      setMessage("업로드할 Excel 파일을 선택하세요.");
      return;
    }
    setStatus("loading");
    try {
      const nextResult = await uploadStudentGuidanceFile(templateId, file);
      setResult(nextResult);
      setStatus("success");
      setMessage("검증이 완료되었습니다. 오류 행을 확인한 뒤 반영하세요.");
    } catch (error) {
      const nextMessage = errorMessage(error);
      setMessage(nextMessage);
      setStatus(nextMessage.includes("권한") ? "permission" : "error");
    }
  };

  const commit = async () => {
    if (!result) return;
    setCommitOpen(false);
    setStatus("loading");
    try {
      const response = await apiRequest<{
        uploadId: string;
        savedCount: number;
      }>(
        `/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/commit` as `/api/${string}`,
        { method: "POST" },
      );
      setStatus("success");
      setMessage(
        `전체 반영이 완료되었습니다. 저장 ${response.data?.savedCount ?? 0}건`,
      );
    } catch (error) {
      const nextMessage = errorMessage(error);
      setMessage(nextMessage);
      setStatus(nextMessage.includes("권한") ? "permission" : "error");
    }
  };

  if (!auth.user?.roles.includes("R07")) {
    return (
      <PermissionState
        title="권한이 없습니다"
        message="학생지도 Excel 일괄등록은 R07 역할만 사용할 수 있습니다."
      />
    );
  }

  return (
    <main
      className="space-y-6"
      data-testid="student-guidance-excel-upload-screen"
    >
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          업적 입력 관리 &gt; 교육영역 &gt; 학생지도 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 실적 Excel 일괄등록
        </h1>
        <p className="mt-2 text-sm text-muted">
          R07 전용: 템플릿 다운로드 → 업로드 → 검증 → 확인 → 전체 반영
        </p>
      </section>
      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
          <label className="grid gap-1 text-sm font-medium text-link">
            양식 ID
            <input
              data-testid="student-guidance-template-id-input"
              className="form-input"
              value={templateId}
              onChange={(event) => setTemplateId(event.target.value)}
              placeholder="업로드 양식 ID"
            />
          </label>
          <button
            data-testid="student-guidance-template-download-button"
            className="btn-secondary self-end"
            type="button"
            onClick={downloadTemplate}
          >
            <Download size={16} /> 템플릿 다운로드
          </button>
        </div>
        <div className="mt-4 grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
          <label className="grid gap-1 text-sm font-medium text-link">
            업로드 파일 <span className="text-error">*</span>
            <input
              data-testid="student-guidance-upload-file-input"
              className="form-input"
              type="file"
              accept=".csv,.xls,.xlsx"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            />
          </label>
          <button
            data-testid="student-guidance-upload-validate-button"
            className="btn-primary self-end"
            type="button"
            onClick={() => void upload()}
          >
            <Upload size={16} /> 업로드·검증
          </button>
        </div>
      </section>
      {status === "loading" ? (
        <LoadingState
          title="처리 중"
          message="파일을 검증하거나 전체 반영하고 있습니다."
        />
      ) : null}
      {status === "permission" ? (
        <PermissionState title="권한이 없습니다" message={message} />
      ) : null}
      {status === "error" ? (
        <ErrorState title="처리 오류" message={message} />
      ) : null}
      {status === "success" ? (
        <SuccessState title="처리 완료" message={message} />
      ) : null}
      {result ? (
        <section
          className="rounded-md bg-white p-6 shadow-md"
          data-testid="student-guidance-validation-result"
        >
          <h2 className="text-lg font-semibold text-dark">검증 결과</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-4">
            <strong>정상행 {result.successCount}</strong>
            <strong>오류행 {result.errorCount}</strong>
            <strong>제외 {result.excludedCount}</strong>
            <strong>총 {result.totalCount}</strong>
          </div>
          {result.errors.length > 0 ? (
            <>
              <p className="mt-4 rounded-xl bg-lighterror p-3 text-sm text-error">
                오류 행이 있어 전체 반영하지 않습니다. 오류파일을 내려받아
                수정한 뒤 다시 업로드하세요.
              </p>
              <table className="table mt-4">
                <thead>
                  <tr>
                    <th>행번호</th>
                    <th>컬럼</th>
                    <th>오류코드</th>
                    <th>오류사유</th>
                    <th>수정안내</th>
                  </tr>
                </thead>
                <tbody>
                  {result.errors.map((error) => (
                    <tr
                      key={error.errorId}
                      data-testid="student-guidance-upload-error-row"
                    >
                      <td>{error.rowNumber}</td>
                      <td>{error.columnName}</td>
                      <td>{error.errorCode}</td>
                      <td>{error.errorReason}</td>
                      <td>{error.correctionGuide}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <a
                data-testid="student-guidance-error-download-link"
                className="btn-secondary mt-4"
                href={`/api/business/student-guidance-achievements/excel-upload-errors/download?${new URLSearchParams({ uploadId: result.uploadId }).toString()}`}
              >
                <Download size={16} /> 오류파일 다운로드
              </a>
            </>
          ) : (
            <button
              data-testid="student-guidance-commit-button"
              className="btn-primary mt-4"
              type="button"
              onClick={() => setCommitOpen(true)}
            >
              <Save size={16} /> 전체 반영
            </button>
          )}
        </section>
      ) : null}
      {commitOpen ? (
        <section
          role="dialog"
          aria-modal="true"
          aria-label="일괄 반영 확인"
          className="rounded-md bg-white p-6 shadow-md"
          data-testid="student-guidance-commit-dialog"
        >
          <div className="flex items-start gap-3">
            <FileSpreadsheet className="mt-1 text-primary" />
            <div>
              <h2 className="text-lg font-semibold text-dark">
                일괄 반영 확인
              </h2>
              <p className="mt-2 text-sm text-muted">
                오류행 0건인 검증 결과만 전체 반영합니다. 반영 후 업로드 이력이
                갱신됩니다.
              </p>
            </div>
          </div>
          <div className="mt-5 flex justify-end gap-3">
            <button
              data-testid="student-guidance-commit-cancel-button"
              className="btn-secondary"
              type="button"
              onClick={() => setCommitOpen(false)}
            >
              취소
            </button>
            <button
              data-testid="student-guidance-commit-confirm-button"
              className="btn-primary"
              type="button"
              onClick={() => void commit()}
            >
              반영
            </button>
          </div>
        </section>
      ) : null}
    </main>
  );
}
