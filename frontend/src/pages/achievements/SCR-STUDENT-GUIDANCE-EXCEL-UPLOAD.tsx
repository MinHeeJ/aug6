import { Download, FileUp, History, Upload } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type UploadError = {
  rowNumber: number;
  columnName: string;
  errorReason: string;
};
type UploadResult = {
  uploadId: string;
  originalFileName: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors: UploadError[];
};
type HistoryRow = {
  uploadId: string;
  originalFileName: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
  processedAt?: string;
};

/** R07-only wizard for template download, CSV validation, all-or-nothing commit, and upload history. */
export function StudentGuidanceExcelUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [histories, setHistories] = useState<HistoryRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const loadHistories = async () => {
    try {
      const response = await fetch(
        "/api/business/student-guidance-achievements/excel-uploads/histories",
        { credentials: "include" },
      );
      const body = await response.json();
      if (!response.ok)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "이력 조회에 실패했습니다.",
          body.error,
        );
      setHistories(body.data ?? []);
    } catch (caught) {
      handleError(caught);
    }
  };
  useEffect(() => {
    void loadHistories();
  }, []);

  const upload = async () => {
    if (!file) {
      setError("현행 학생지도 CSV 파일을 선택하세요.");
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
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
          body.error?.message ?? "업로드 검증에 실패했습니다.",
          body.error,
        );
      setResult(body.data);
      setSuccess(
        "파일 검증이 완료되었습니다. 오류가 없을 때만 전체 반영할 수 있습니다.",
      );
      await loadHistories();
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  const commit = async () => {
    if (!result || result.errorCount > 0) return;
    setConfirming(false);
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(
        `/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/commit`,
        { method: "POST", credentials: "include" },
      );
      const body = await response.json();
      if (!response.ok || body.success === false)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "일괄 반영에 실패했습니다.",
          body.error,
        );
      setSuccess(`${body.data.savedCount}건을 전체 반영했습니다.`);
      await loadHistories();
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
    );
  };

  if (permissionDenied)
    return (
      <section data-testid="student-guidance-upload-page">
        <PermissionState
          title="학생지도 Excel 등록 권한이 없습니다"
          message="이 화면은 R07 역할만 사용할 수 있습니다."
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
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 실적 Excel 일괄등록
        </h1>
        <p className="mt-2 text-sm text-muted">
          현행 양식 다운로드, 업로드·검증, 확인 후 전체 반영을 순서대로
          수행합니다.
        </p>
      </header>
      {loading ? (
        <LoadingState
          title="학생지도 Excel 처리 중"
          message="검증 또는 전체 반영 상태를 확인하고 있습니다."
        />
      ) : null}
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="학생지도 Excel 오류" message={error} />
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="student-guidance-upload-panel"
      >
        <div className="flex flex-wrap items-center gap-3">
          <a
            className="inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            data-testid="student-guidance-template-download-link"
            href="/api/business/student-guidance-achievements/excel-uploads/template"
          >
            <Download size={16} /> 템플릿 다운로드
          </a>
          <input
            accept=".csv"
            data-testid="student-guidance-file-input"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            type="file"
          />
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="student-guidance-upload-button"
            disabled={loading}
            onClick={() => void upload()}
            type="button"
          >
            <Upload size={16} /> 업로드·검증
          </button>
        </div>
      </section>
      {result ? (
        <section
          className="rounded-md border border-ld bg-white p-5"
          data-testid="student-guidance-validation-result"
        >
          <h2 className="text-lg font-semibold text-dark">검증결과</h2>
          <p className="mt-2 text-sm text-muted">
            정상행 {result.successCount}건 / 오류행 {result.errorCount}건 / 전체{" "}
            {result.totalCount}건
          </p>
          {result.errorCount > 0 ? (
            <div className="mt-4">
              <p className="text-sm text-error">
                오류 행이 있어 전체 반영하지 않습니다.
              </p>
              <a
                className="mt-3 inline-flex items-center gap-2 rounded-md border border-error px-3 py-2 text-sm text-error"
                data-testid="student-guidance-error-download-link"
                href={`/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/errors/download`}
              >
                <Download size={16} /> 오류파일 다운로드
              </a>
              <ul className="mt-3 list-disc pl-5 text-sm">
                {result.errors.map((row) => (
                  <li key={`${row.rowNumber}-${row.columnName}`}>
                    {row.rowNumber}행 {row.columnName}: {row.errorReason}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <button
              className="mt-4 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              data-testid="student-guidance-commit-button"
              onClick={() => setConfirming(true)}
              type="button"
            >
              <FileUp size={16} /> 반영
            </button>
          )}
        </section>
      ) : null}
      {confirming ? (
        <section
          aria-modal="true"
          className="rounded-md border border-primary bg-white p-5 shadow-md"
          data-testid="student-guidance-commit-modal"
          role="dialog"
        >
          <h2 className="text-lg font-semibold text-dark">일괄 반영 확인</h2>
          <p className="mt-2 text-sm text-muted">
            오류행 0건인 검증 결과만 전체 반영합니다.
          </p>
          <div className="mt-4 flex justify-end gap-2">
            <button
              data-testid="student-guidance-commit-cancel-button"
              onClick={() => setConfirming(false)}
              type="button"
            >
              취소
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-white"
              data-testid="student-guidance-commit-confirm-button"
              onClick={() => void commit()}
              type="button"
            >
              반영
            </button>
          </div>
        </section>
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="student-guidance-history-panel"
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">업로드 이력</h2>
          <button
            className="inline-flex items-center gap-2 text-sm text-link"
            data-testid="student-guidance-history-refresh-button"
            onClick={() => void loadHistories()}
            type="button"
          >
            <History size={16} /> 조회
          </button>
        </div>
        {histories.length === 0 ? (
          <EmptyState
            title="업로드 이력이 없습니다"
            message="검증한 파일의 업로더, 일시, 파일명, 정상·오류 건수가 여기에 표시됩니다."
          />
        ) : (
          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr>
                  <th>파일명</th>
                  <th>전체</th>
                  <th>정상</th>
                  <th>오류</th>
                  <th>반영</th>
                  <th>처리일시</th>
                </tr>
              </thead>
              <tbody>
                {histories.map((row) => (
                  <tr
                    data-testid="student-guidance-history-row"
                    key={row.uploadId}
                  >
                    <td>{row.originalFileName}</td>
                    <td>{row.totalCount}</td>
                    <td>{row.successCount}</td>
                    <td>{row.errorCount}</td>
                    <td>{row.savedCount}</td>
                    <td>{row.processedAt ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}
