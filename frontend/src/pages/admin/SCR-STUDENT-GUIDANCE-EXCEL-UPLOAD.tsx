import { Download, FileSpreadsheet, Upload } from "lucide-react";
import { useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type UploadResult = {
  uploadId: string;
  validationStatus: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors: { rowNumber: number; errorReason: string }[];
};

/** R07-only wizard for the required template, validation, error review, and all-or-nothing commit flow. */
export function StudentGuidanceExcelUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [status, setStatus] = useState<
    "idle" | "loading" | "error" | "permission" | "success"
  >("idle");
  const [message, setMessage] = useState("");
  const upload = async () => {
    if (!file) {
      setStatus("error");
      setMessage("업로드할 Excel 파일을 선택하세요.");
      return;
    }
    setStatus("loading");
    try {
      const form = new FormData();
      form.append("file", file);
      const response = await fetch(
        "/api/business/student-guidance-achievements/excel-uploads",
        { method: "POST", body: form, credentials: "include" },
      );
      const body = await response.json();
      if (!response.ok || body.success === false)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "업로드에 실패했습니다.",
          body.error,
        );
      setResult(body.data);
      setStatus("success");
      setMessage("사전 검증이 완료되었습니다.");
    } catch (caught) {
      setStatus(
        caught instanceof ApiClientError && caught.status === 403
          ? "permission"
          : "error",
      );
      setMessage(
        caught instanceof Error ? caught.message : "업로드에 실패했습니다.",
      );
    }
  };
  const commit = async () => {
    if (
      !result ||
      result.errorCount > 0 ||
      !window.confirm(
        "오류행 0건인 검증 결과만 전체 반영합니다. 반영하시겠습니까?",
      )
    )
      return;
    setStatus("loading");
    try {
      const response = await apiRequest<{ savedCount: number }>(
        `/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/commit` as `/api/${string}`,
        { method: "POST" },
      );
      setStatus("success");
      setMessage(`${response.data?.savedCount ?? 0}건을 전체 반영했습니다.`);
    } catch (caught) {
      setStatus(
        caught instanceof ApiClientError && caught.status === 403
          ? "permission"
          : "error",
      );
      setMessage(
        caught instanceof Error ? caught.message : "반영에 실패했습니다.",
      );
    }
  };
  if (status === "permission")
    return (
      <section data-testid="student-guidance-upload-page">
        <PermissionState
          title="학생지도 Excel 등록 권한이 없습니다"
          message="R07 역할과 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD"
      data-testid="student-guidance-upload-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">
          업적 입력 관리 &gt; 교육영역 &gt; 학생지도 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 실적 Excel 일괄등록
        </h1>
        <p className="mt-2 text-sm text-muted">
          템플릿 다운로드, 업로드·검증, 오류 확인 후 전체 반영을 진행합니다.
        </p>
      </header>
      {status === "loading" ? (
        <LoadingState
          title="검증 또는 반영 처리 중"
          message="완료될 때까지 잠시 기다려 주세요."
        />
      ) : null}
      {status === "error" ? (
        <ErrorState title="학생지도 Excel 처리 오류" message={message} />
      ) : null}
      {status === "success" ? (
        <SuccessState title="처리 완료" message={message} />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex flex-wrap items-end gap-3">
          <a
            className="btn-secondary"
            data-testid="student-guidance-template-download"
            href="/api/admin/excel-upload-templates"
          >
            {" "}
            <Download size={16} /> 템플릿 다운로드
          </a>
          <label className="text-sm font-medium text-dark">
            파일
            <input
              data-testid="student-guidance-file-input"
              className="mt-1 block"
              type="file"
              accept=".csv,.xls,.xlsx"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            />
          </label>
          <button
            className="btn-primary"
            data-testid="student-guidance-upload-button"
            disabled={status === "loading"}
            onClick={() => void upload()}
            type="button"
          >
            <Upload size={16} /> 업로드·검증
          </button>
        </div>
      </section>
      {result ? (
        <section
          className="rounded-md border border-ld bg-white p-6"
          data-testid="student-guidance-validation-result"
        >
          <h2 className="text-lg font-semibold text-dark">검증 결과</h2>
          <p className="mt-3">
            정상행 {result.successCount}건 | 오류행 {result.errorCount}건 | 전체{" "}
            {result.totalCount}건
          </p>
          {result.errorCount > 0 ? (
            <>
              <p className="mt-3 text-error">
                오류행이 있어 전체 반영하지 않습니다.
              </p>
              <ul className="mt-2 list-disc pl-5 text-sm">
                {result.errors.map((error) => (
                  <li
                    data-testid="student-guidance-error-row"
                    key={`${error.rowNumber}-${error.errorReason}`}
                  >
                    {error.rowNumber}행: {error.errorReason}
                  </li>
                ))}
              </ul>
              <a
                className="btn-secondary mt-4 inline-flex"
                data-testid="student-guidance-error-download"
                href={`/api/admin/excel-upload-errors/download?uploadId=${encodeURIComponent(result.uploadId)}`}
              >
                <FileSpreadsheet size={16} /> 오류파일 다운로드
              </a>
            </>
          ) : (
            <button
              className="btn-primary mt-4"
              data-testid="student-guidance-commit-button"
              disabled={status === "loading"}
              onClick={() => void commit()}
              type="button"
            >
              전체 반영
            </button>
          )}
        </section>
      ) : null}
    </section>
  );
}
