import { useState } from "react";
import {
  ApiClientError,
  studentGuidanceUploadApi,
  type StudentGuidanceHistory,
  type StudentGuidanceUploadResult,
} from "../../api/apiClient";
import { ErrorState, SuccessState } from "../../components/States";

/** R07 upload wizard: validate first and require confirmation before an atomic commit. */
export function StudentGuidanceExcelUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<StudentGuidanceUploadResult | null>(
    null,
  );
  const [histories, setHistories] = useState<StudentGuidanceHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const upload = async () => {
    if (!file) {
      setError("업로드할 파일을 선택하세요.");
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await studentGuidanceUploadApi.upload(
        "B77-STUDENT-GUIDANCE-TEMPLATE",
        file,
      );
      setResult(response.data ?? null);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "업로드·검증에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  const loadHistories = async () => {
    try {
      const response = await studentGuidanceUploadApi.histories();
      setHistories(response.data ?? []);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "업로드 이력을 불러오지 못했습니다.",
      );
    }
  };

  const commit = async () => {
    if (!result || result.errorCount > 0) return;
    setLoading(true);
    try {
      const response = await studentGuidanceUploadApi.commit(result.uploadId);
      setResult(response.data ?? result);
      setSuccess("학생지도 실적이 전체 반영되었습니다.");
      setConfirming(false);
      await loadHistories();
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "반영에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <main
      className="page-content space-y-6"
      data-testid="student-guidance-excel-upload-page"
    >
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">
          학생지도 실적 Excel 일괄등록
        </h1>
        <p className="mt-2 text-sm text-muted">
          R07 담당자가 템플릿 검증 후 오류 없는 파일만 전체 반영합니다.
        </p>
      </section>
      {error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      <section
        className="card space-y-4"
        data-testid="student-guidance-upload-panel"
      >
        <a
          data-testid="student-guidance-template-download"
          href="/api/business/student-guidance-achievements/excel-template"
        >
          템플릿 다운로드
        </a>
        <label>
          파일 선택
          <input
            data-testid="student-guidance-file-input"
            type="file"
            accept=".csv"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
        </label>
        <button
          data-testid="student-guidance-upload-button"
          disabled={loading}
          onClick={() => void upload()}
        >
          {loading ? "검증 중..." : "업로드·검증"}
        </button>
      </section>
      {result && (
        <section
          className="card space-y-3"
          data-testid="student-guidance-validation-result"
        >
          <h2>검증 결과</h2>
          <p>
            정상행 {result.successCount}건 | 오류행 {result.errorCount}건
          </p>
          {result.errors.length > 0 && (
            <>
              <ul>
                {result.errors.map((errorRow) => (
                  <li
                    data-testid={`student-guidance-error-${errorRow.rowNumber}`}
                    key={`${errorRow.rowNumber}-${errorRow.columnName}`}
                  >
                    {errorRow.rowNumber}행 {errorRow.columnName}:{" "}
                    {errorRow.errorReason}
                  </li>
                ))}
              </ul>
              <a
                data-testid="student-guidance-error-download"
                href={`/api/admin/excel-upload-errors/download?uploadId=${encodeURIComponent(result.uploadId)}`}
              >
                오류파일 다운로드
              </a>
            </>
          )}
          {result.errorCount === 0 &&
            result.validationStatus !== "COMMITTED" && (
              <button
                data-testid="student-guidance-commit-open-button"
                disabled={loading}
                onClick={() => setConfirming(true)}
              >
                반영
              </button>
            )}
        </section>
      )}
      {confirming && (
        <section
          className="card"
          role="dialog"
          data-testid="student-guidance-commit-dialog"
        >
          <h2>일괄 반영 확인</h2>
          <p>오류행 0건인 검증 결과만 전체 반영합니다.</p>
          <button
            data-testid="student-guidance-commit-cancel-button"
            onClick={() => setConfirming(false)}
          >
            취소
          </button>
          <button
            data-testid="student-guidance-commit-confirm-button"
            onClick={() => void commit()}
          >
            반영
          </button>
        </section>
      )}
      <section className="card" data-testid="student-guidance-upload-history">
        <h2>업로드 이력</h2>
        <button
          data-testid="student-guidance-history-button"
          onClick={() => void loadHistories()}
        >
          이력 조회
        </button>
        <ul>
          {histories.map((history) => (
            <li
              data-testid={`student-guidance-history-${history.uploadId}`}
              key={history.uploadId}
            >
              {history.originalFileName} · 총 {history.totalCount}건 · 성공{" "}
              {history.successCount}건 · 실패 {history.errorCount}건
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
