import { Download, Upload } from "lucide-react";
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
  correctionGuide?: string;
};
type Result = {
  uploadId: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors: UploadError[];
};
type History = {
  uploadId: string;
  originalFileName: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
  processedAt?: string;
};

/** R07 학생지도 Excel 양식 검증, 오류 확인, 전체 반영 및 본인 업로드 이력을 제공한다. */
export function StudentGuidanceExcelUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<Result | null>(null);
  const [histories, setHistories] = useState<History[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string>();
  const [permission, setPermission] = useState(false);
  const loadHistory = async () => {
    try {
      const response = await fetch(
        "/api/business/student-guidance-achievements/excel-upload-histories",
        { credentials: "include" },
      );
      const body = await response.json();
      if (!response.ok || body.success === false)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "이력 조회 실패",
        );
      setHistories(body.data ?? []);
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403)
        setPermission(true);
      else
        setMessage(caught instanceof Error ? caught.message : "이력 조회 실패");
    }
  };
  useEffect(() => {
    void loadHistory();
  }, []);
  const upload = async () => {
    if (!file) {
      setMessage("업로드할 파일을 선택하세요.");
      return;
    }
    setLoading(true);
    setMessage(undefined);
    try {
      const form = new FormData();
      form.append("file", file);
      const response = await fetch(
        "/api/business/student-guidance-achievements/excel-uploads",
        { method: "POST", credentials: "include", body: form },
      );
      const body = await response.json();
      if (!response.ok || body.success === false)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "검증 실패",
        );
      setResult(body.data);
      setMessage("검증이 완료되었습니다.");
      await loadHistory();
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403)
        setPermission(true);
      else setMessage(caught instanceof Error ? caught.message : "업로드 실패");
    } finally {
      setLoading(false);
    }
  };
  const commit = async () => {
    if (
      !result ||
      result.errorCount > 0 ||
      !window.confirm(
        "오류행 0건인 검증 결과만 전체 반영합니다. 계속하시겠습니까?",
      )
    )
      return;
    setLoading(true);
    try {
      const response = await fetch(
        `/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/commit`,
        { method: "POST", credentials: "include" },
      );
      const body = await response.json();
      if (!response.ok || body.success === false)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "반영 실패",
        );
      setMessage(`전체 ${body.data.savedCount}건을 반영했습니다.`);
      await loadHistory();
    } catch (caught) {
      setMessage(caught instanceof Error ? caught.message : "반영 실패");
    } finally {
      setLoading(false);
    }
  };
  if (permission)
    return (
      <section data-testid="student-guidance-excel-upload-page">
        <PermissionState
          title="학생지도 Excel 등록 권한이 없습니다"
          message="R07 권한이 필요합니다."
        />
      </section>
    );
  return (
    <main
      className="space-y-6"
      data-testid="student-guidance-excel-upload-page"
      data-screen-id="SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">
          업적 입력 관리 &gt; 교육영역 &gt; 학생지도 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 실적 Excel 일괄등록
        </h1>
      </header>
      {loading ? (
        <LoadingState
          title="처리 중"
          message="업로드 파일을 검증하거나 반영하고 있습니다."
        />
      ) : null}
      {message ? <SuccessState title={message} /> : null}
      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-wrap gap-3">
          <a
            className="btn-secondary"
            data-testid="student-guidance-template-download"
            href="/api/business/student-guidance-achievements/excel-template"
          >
            <Download size={16} />
            템플릿 다운로드
          </a>
          <input
            data-testid="student-guidance-file-input"
            type="file"
            accept=".csv"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <button
            className="btn-primary"
            data-testid="student-guidance-upload-button"
            onClick={() => void upload()}
            disabled={loading}
          >
            <Upload size={16} />
            업로드·검증
          </button>
        </div>
      </section>
      {result ? (
        <section
          className="rounded-md bg-white p-6 shadow-md"
          data-testid="student-guidance-validation-result"
        >
          <h2 className="text-lg font-semibold text-dark">검증결과</h2>
          <p className="mt-2">
            정상행 {result.successCount}건 | 오류행 {result.errorCount}건
          </p>
          {result.errorCount > 0 ? (
            <>
              <a
                data-testid="student-guidance-error-download"
                className="btn-secondary mt-3 inline-flex"
                href={`/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/errors/download`}
              >
                오류파일 다운로드
              </a>
              <table className="mt-4 w-full text-sm">
                <tbody>
                  {result.errors.map((error) => (
                    <tr
                      key={`${error.rowNumber}-${error.columnName}`}
                      data-testid={`student-guidance-error-row-${error.rowNumber}`}
                    >
                      <td>{error.rowNumber}</td>
                      <td>{error.columnName}</td>
                      <td>{error.errorReason}</td>
                      <td>{error.correctionGuide}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          ) : (
            <button
              className="btn-primary mt-3"
              data-testid="student-guidance-commit-button"
              onClick={() => void commit()}
              disabled={loading}
            >
              반영
            </button>
          )}
        </section>
      ) : null}
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">업로드 이력</h2>
        {histories.length === 0 ? (
          <EmptyState title="업로드 이력이 없습니다" />
        ) : (
          <table className="mt-4 w-full text-sm">
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
                  key={history.uploadId}
                  data-testid={`student-guidance-history-${history.uploadId}`}
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
      {message?.includes("실패") ? (
        <ErrorState title="처리 오류" message={message} />
      ) : null}
    </main>
  );
}
