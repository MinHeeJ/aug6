import { useState } from "react";
import {
  ApiClientError,
  studentGuidanceUploadApi,
  type StudentGuidanceUploadResult,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

/** R07-only student-guidance template, validation, confirmation, and atomic commit workflow. */
export function StudentGuidanceExcelUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<StudentGuidanceUploadResult | null>(
    null,
  );
  const [histories, setHistories] = useState<
    Array<{
      uploadId: string;
      originalFileName: string;
      totalCount: number;
      successCount: number;
      errorCount: number;
      savedCount: number;
    }>
  >([]);
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
      const response = await studentGuidanceUploadApi.upload(file);
      setResult(response.data ?? null);
      setStatus("success");
      setMessage("검증이 완료되었습니다. 결과를 확인한 뒤 반영하세요.");
    } catch (error) {
      setStatus(
        error instanceof ApiClientError && error.status === 403
          ? "permission"
          : "error",
      );
      setMessage(
        error instanceof Error ? error.message : "검증 중 오류가 발생했습니다.",
      );
    }
  };
  const commit = async () => {
    if (!result || result.errorCount > 0) return;
    if (!window.confirm("오류행 0건인 검증 결과를 전체 반영하시겠습니까?"))
      return;
    setStatus("loading");
    try {
      const response = await studentGuidanceUploadApi.commit(result.uploadId);
      setStatus("success");
      setMessage(`${response.data?.savedCount ?? 0}건을 전체 반영했습니다.`);
      await loadHistories();
    } catch (error) {
      setStatus("error");
      setMessage(
        error instanceof Error ? error.message : "반영 중 오류가 발생했습니다.",
      );
    }
  };
  const loadHistories = async () => {
    try {
      const response = await studentGuidanceUploadApi.histories();
      setHistories(response.data ?? []);
    } catch (error) {
      setStatus(
        error instanceof ApiClientError && error.status === 403
          ? "permission"
          : "error",
      );
      setMessage(
        error instanceof Error ? error.message : "이력을 불러오지 못했습니다.",
      );
    }
  };
  const download = async (request: () => Promise<Blob>, fileName: string) => {
    try {
      const blob = await request();
      if (typeof URL.createObjectURL === "function") {
        const link = document.createElement("a");
        const objectUrl = URL.createObjectURL(blob);
        link.href = objectUrl;
        link.download = fileName;
        link.click();
        URL.revokeObjectURL(objectUrl);
      }
    } catch (error) {
      setStatus(
        error instanceof ApiClientError && error.status === 403
          ? "permission"
          : "error",
      );
      setMessage(
        error instanceof Error ? error.message : "파일을 내려받지 못했습니다.",
      );
    }
  };

  return (
    <main
      className="space-y-6"
      data-testid="student-guidance-excel-upload-screen"
    >
      <section className="rounded-md bg-white p-6 shadow-md">
        <h1 className="text-xl font-semibold text-dark">
          학생지도 실적 Excel 일괄등록
        </h1>
        <p className="mt-2 text-sm text-muted">
          템플릿 다운로드 → 파일 업로드 → 검증 → 검증결과 확인 → 반영 순서로
          처리합니다.
        </p>
        <div className="mt-5 flex flex-wrap items-center gap-3">
          <button
            data-testid="student-guidance-template-download-button"
            className="btn-secondary"
            onClick={() =>
              void download(
                studentGuidanceUploadApi.template,
                "학생지도_일괄등록_v1.csv",
              )
            }
          >
            템플릿 다운로드
          </button>
          <input
            data-testid="student-guidance-upload-file-input"
            aria-label="학생지도 Excel 파일"
            className="form-input"
            type="file"
            accept=".csv"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <button
            data-testid="student-guidance-upload-validate-button"
            className="btn-primary"
            onClick={() => void upload()}
          >
            업로드·검증
          </button>
          <button
            data-testid="student-guidance-history-load-button"
            className="btn-secondary"
            onClick={() => void loadHistories()}
          >
            업로드 이력 조회
          </button>
        </div>
      </section>
      {status === "loading" && (
        <LoadingState
          title="처리 중"
          message="학생지도 Excel을 검증하거나 반영하고 있습니다."
        />
      )}
      {status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message="R07 역할만 학생지도 Excel 일괄등록을 처리할 수 있습니다."
        />
      )}
      {status === "error" && <ErrorState title="처리 오류" message={message} />}
      {status === "success" && (
        <SuccessState title="처리 완료" message={message} />
      )}
      {result && (
        <section
          className="rounded-md bg-white p-6 shadow-md"
          data-testid="student-guidance-validation-result"
        >
          <h2 className="text-lg font-semibold text-dark">검증결과</h2>
          <p className="mt-3 text-sm">
            정상행 {result.successCount}건 | 오류행 {result.errorCount}건
          </p>
          {result.errorCount > 0 ? (
            <>
              <p className="mt-3 text-sm text-error">
                오류행이 있어 전체 반영하지 않습니다. 오류파일을 내려받아
                수정하세요.
              </p>
              <button
                data-testid="student-guidance-error-download-button"
                className="btn-secondary mt-3"
                onClick={() =>
                  void download(
                    () => studentGuidanceUploadApi.errorFile(result.uploadId),
                    `학생지도_업로드오류_${result.uploadId}.csv`,
                  )
                }
              >
                오류파일 다운로드
              </button>
              <ul className="mt-3 space-y-1 text-sm">
                {result.errors.map((error) => (
                  <li
                    data-testid="student-guidance-error-row"
                    key={`${error.rowNumber}-${error.columnName}`}
                  >
                    {error.rowNumber}행 {error.columnName}: {error.errorReason}
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <button
              data-testid="student-guidance-commit-button"
              className="btn-primary mt-3"
              onClick={() => void commit()}
            >
              반영
            </button>
          )}
        </section>
      )}
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">업로드 이력</h2>
        {histories.length === 0 ? (
          <EmptyState
            title="이력 없음"
            message="조회된 학생지도 업로드 이력이 없습니다."
          />
        ) : (
          <table className="table mt-3">
            <thead>
              <tr>
                <th>파일명</th>
                <th>총 건수</th>
                <th>정상</th>
                <th>오류</th>
                <th>반영</th>
              </tr>
            </thead>
            <tbody>
              {histories.map((history) => (
                <tr
                  data-testid="student-guidance-history-row"
                  key={history.uploadId}
                >
                  <td>{history.originalFileName}</td>
                  <td>{history.totalCount}</td>
                  <td>{history.successCount}</td>
                  <td>{history.errorCount}</td>
                  <td>{history.savedCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}
