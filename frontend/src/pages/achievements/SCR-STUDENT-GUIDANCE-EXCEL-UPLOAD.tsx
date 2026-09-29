import { Download, Upload } from "lucide-react";
import { useState } from "react";
import {
  ApiClientError,
  apiRequest,
  type ApiResponse,
} from "../../api/apiClient";
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
type UploadResult = {
  uploadId: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors: UploadError[];
};
type UploadHistory = {
  uploadId: string;
  originalFileName: string;
  uploaderUserId: number;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
  processedAt: string;
};
async function upload(file: File) {
  const body = new FormData();
  body.append("file", file);
  const response = await fetch(
    "/api/business/student-guidance-achievements/excel-uploads",
    { method: "POST", body, credentials: "include" },
  );
  const payload = (await response.json()) as ApiResponse<UploadResult>;
  if (!response.ok || !payload.success)
    throw new ApiClientError(
      response.status,
      payload.error?.message ?? "업로드 검증에 실패했습니다.",
      payload.error,
    );
  return payload.data!;
}
export function StudentGuidanceExcelUploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [histories, setHistories] = useState<UploadHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const loadHistory = async () => {
    const response = await apiRequest<{ histories: UploadHistory[] }>(
      "/api/business/student-guidance-achievements/excel-upload-histories",
    );
    setHistories(response.data?.histories ?? []);
  };
  const validate = async () => {
    if (!file) {
      setError("업로드할 Excel 파일을 선택하세요.");
      return;
    }
    try {
      setLoading(true);
      setError(null);
      setDenied(false);
      setSuccess(null);
      setResult(await upload(file));
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403)
        setDenied(true);
      setError(
        caught instanceof Error
          ? caught.message
          : "업로드 검증에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  const commit = async () => {
    if (!result) return;
    try {
      setLoading(true);
      await apiRequest<{ savedCount: number }>(
        `/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/commit` as `/api/${string}`,
        { method: "POST" },
      );
      setSuccess("학생지도 실적을 전체 반영했습니다.");
      setConfirming(false);
      await loadHistory();
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "일괄 반영에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  if (denied)
    return (
      <section
        data-screen-id="SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD"
        data-testid="student-guidance-upload-page"
      >
        <PermissionState
          title="학생지도 Excel 등록 권한이 없습니다"
          message="R07 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD"
      data-testid="student-guidance-upload-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">
          업적 입력 관리 / 교육영역 / 학생지도 실적 관리 / Excel 일괄등록
        </p>
        <h1 className="mt-2 text-xl font-semibold">
          학생지도 실적 Excel 일괄등록
        </h1>
      </div>
      {error ? <ErrorState title="Excel 처리 오류" message={error} /> : null}
      {success ? <SuccessState title="반영 완료" message={success} /> : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <a
          className="btn-secondary"
          href="/api/business/student-guidance-achievements/excel-template"
          data-testid="student-guidance-template-download"
        >
          <Download size={16} />
          템플릿 다운로드
        </a>
        <div className="mt-4 flex flex-wrap gap-3">
          <input
            type="file"
            accept=".csv,.xls,.xlsx"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            data-testid="student-guidance-file-input"
          />
          <button
            className="btn-primary"
            type="button"
            disabled={loading}
            onClick={() => void validate()}
            data-testid="student-guidance-validate-button"
          >
            <Upload size={16} />
            {loading ? "검증 중" : "업로드·검증"}
          </button>
        </div>
      </section>
      {loading ? (
        <LoadingState title="학생지도 Excel을 처리하고 있습니다" />
      ) : null}
      {result ? (
        <section
          className="rounded-md border border-ld bg-white p-6"
          data-testid="student-guidance-validation-result"
        >
          <h2 className="text-lg font-semibold">검증결과</h2>
          <p className="mt-2">
            정상행 {result.successCount}건 | 오류행 {result.errorCount}건
          </p>
          {result.errorCount > 0 ? (
            <>
              <table className="mt-4">
                <thead>
                  <tr>
                    <th>행번호</th>
                    <th>컬럼</th>
                    <th>사유</th>
                  </tr>
                </thead>
                <tbody>
                  {result.errors.map((item) => (
                    <tr
                      key={`${item.rowNumber}-${item.columnName}`}
                      data-testid="student-guidance-error-row"
                    >
                      <td>{item.rowNumber}</td>
                      <td>{item.columnName}</td>
                      <td>{item.errorReason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <a
                className="btn-secondary mt-4"
                href={`/api/business/student-guidance-achievements/excel-uploads/${encodeURIComponent(result.uploadId)}/errors/download`}
                data-testid="student-guidance-error-download"
              >
                오류파일 다운로드
              </a>
            </>
          ) : (
            <button
              className="btn-primary mt-4"
              type="button"
              onClick={() => setConfirming(true)}
              data-testid="student-guidance-commit-button"
            >
              반영
            </button>
          )}
        </section>
      ) : null}
      {confirming ? (
        <section
          className="rounded-md border border-ld bg-white p-6"
          role="dialog"
          data-testid="student-guidance-commit-dialog"
        >
          <h2 className="text-lg font-semibold">일괄 반영 확인</h2>
          <p className="mt-2">오류행 0건인 검증 결과만 전체 반영합니다.</p>
          <button
            className="btn-secondary mt-4"
            type="button"
            onClick={() => setConfirming(false)}
            data-testid="student-guidance-commit-cancel"
          >
            취소
          </button>
          <button
            className="btn-primary ml-2 mt-4"
            type="button"
            onClick={() => void commit()}
            data-testid="student-guidance-commit-confirm"
          >
            반영
          </button>
        </section>
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex justify-between">
          <h2 className="text-lg font-semibold">업로드 이력</h2>
          <button
            className="btn-secondary"
            type="button"
            onClick={() => void loadHistory()}
            data-testid="student-guidance-history-load"
          >
            조회
          </button>
        </div>
        {histories.length === 0 ? (
          <EmptyState title="조회된 업로드 이력이 없습니다" />
        ) : (
          <table className="mt-4">
            <thead>
              <tr>
                <th>파일명</th>
                <th>업로더</th>
                <th>일시</th>
                <th>총/성공/실패</th>
              </tr>
            </thead>
            <tbody>
              {histories.map((item) => (
                <tr
                  key={item.uploadId}
                  data-testid="student-guidance-history-row"
                >
                  <td>{item.originalFileName}</td>
                  <td>{item.uploaderUserId}</td>
                  <td>{item.processedAt}</td>
                  <td>
                    {item.totalCount}/{item.successCount}/{item.errorCount}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </section>
  );
}
