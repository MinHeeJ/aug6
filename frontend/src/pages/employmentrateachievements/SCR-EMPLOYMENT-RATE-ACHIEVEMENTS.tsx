import { Download, FileUp, RefreshCw, Save, Send } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Achievement = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string | null;
  achievementStatus: string;
};

type ListResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type BulkJob = {
  jobId: string;
  jobStatus: string;
  totalCount: number;
  processedCount: number;
  unprocessedCount: number;
};

const listPath = (page: number, pageSize: number) =>
  `/api/business/employment-rate-achievements?page=${page}&pageSize=${pageSize}` as `/api/${string}`;

/** Employment-rate achievement UI with individual registration, upload validation, and bulk result tabs. */
export function EmploymentRateAchievementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [achievementDate, setAchievementDate] = useState("");
  const [achievementName, setAchievementName] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [bulkYear, setBulkYear] = useState("");
  const [targetUserIds, setTargetUserIds] = useState("");
  const [bulkAction, setBulkAction] = useState<"GENERATE" | "DELETE">(
    "GENERATE",
  );
  const [bulkResult, setBulkResult] = useState<BulkJob | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<ListResponse>(listPath(0, pageSize));
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      showError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const select = (row: Achievement) => {
    setSelected(row);
    setManagementItemCode(row.managementItemCode);
    setAchievementDate(row.achievementDate);
    setAchievementName(row.achievementName ?? "");
    setSuccess(null);
  };

  const save = async () => {
    if (!managementItemCode.trim() || !achievementDate) {
      setError("관리항목코드와 업적발생일은 필수입니다.");
      return;
    }
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    try {
      const path = selected
        ? (`/api/business/employment-rate-achievements/${selected.achievementId}` as `/api/${string}`)
        : "/api/business/employment-rate-achievements";
      const response = await apiRequest<Achievement>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: managementItemCode.trim(),
          achievementDate,
          achievementName: achievementName.trim() || undefined,
          attachmentIds: [],
        }),
      });
      setSelected(response.data ?? null);
      setSuccess("저장되었습니다.");
      await load();
    } catch (caught) {
      showError(caught);
    }
  };

  const upload = async () => {
    if (!file) {
      setError("업로드할 Excel 파일을 선택하세요.");
      return;
    }
    if (!window.confirm("파일을 검증하고 일괄등록하시겠습니까?")) return;
    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await fetch(
        "/api/business/employment-rate-achievements/excel-uploads",
        {
          method: "POST",
          body: formData,
          credentials: "include",
        },
      );
      if (!response.ok) throw new Error("Excel 업로드를 처리하지 못했습니다.");
      const body = (await response.json()) as {
        data?: { applied: boolean; errorCount: number };
      };
      setSuccess(
        body.data?.applied
          ? "검증된 행을 모두 반영했습니다."
          : `오류 ${body.data?.errorCount ?? 0}건으로 업무 데이터는 반영하지 않았습니다.`,
      );
      await load();
    } catch (caught) {
      showError(caught);
    }
  };

  const requestBulk = async () => {
    const selectedTargetUserIds = targetUserIds
      .split(",")
      .map((value) => Number(value.trim()))
      .filter((value) => Number.isInteger(value) && value > 0);
    if (!bulkYear.trim() || selectedTargetUserIds.length === 0) {
      setError("평가연도와 대상 사용자 ID를 입력하세요.");
      return;
    }
    if (
      !window.confirm(
        "선택한 대상 미리보기를 확인하고 일괄처리를 요청하시겠습니까?",
      )
    )
      return;
    try {
      const response = await apiRequest<BulkJob>(
        "/api/business/employment-rate-achievements/bulk-jobs",
        {
          method: "POST",
          body: JSON.stringify({
            evaluationYear: bulkYear.trim(),
            actionType: bulkAction,
            targetCondition: {
              confirmed: true,
              targetUserIds: selectedTargetUserIds,
            },
          }),
        },
      );
      setBulkResult(response.data ?? null);
      setSuccess("일괄 작업을 접수했습니다.");
    } catch (caught) {
      showError(caught);
    }
  };

  const showError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error ? caught.message : "요청을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
        data-testid="employment-rate-achievements-page"
      >
        <PermissionState
          title="취업률 실적 관리 권한이 없습니다"
          message="허용된 역할과 데이터 범위를 확인하세요."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
      data-testid="employment-rate-achievements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              개별 입력, Excel 검증·반영, 일괄 처리 결과를 관리합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="employment-rate-achievements-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievements-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
          <div className="flex items-center gap-2">
            <select
              data-testid="employment-rate-achievements-page-size-select"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
            <a
              className="inline-flex items-center gap-2 rounded-md border border-ld px-3 py-2 text-sm text-link"
              data-testid="employment-rate-achievements-download-link"
              href={`/api/business/employment-rate-achievements/download?page=0&pageSize=${pageSize}`}
            >
              <Download size={16} /> Excel 다운로드
            </a>
          </div>
        </div>
        {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 취업률 실적이 없습니다"
            message="상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">발생일</th>
                  <th className="px-3 py-2">실적명</th>
                  <th className="px-3 py-2">상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="employment-rate-achievements-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementName}</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs text-primary"
                        data-testid="employment-rate-achievements-detail-button"
                        onClick={() => select(row)}
                        type="button"
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievements-form-panel"
      >
        <h2 className="text-lg font-semibold text-dark">개별 실적 입력</h2>
        {selected?.achievementStatus === "EVALUATION_CONFIRMED" ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="employment-rate-achievements-confirmed-message"
          >
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <label>
            관리항목코드
            <input
              data-testid="employment-rate-achievements-management-item-input"
              value={managementItemCode}
              onChange={(event) => setManagementItemCode(event.target.value)}
            />
          </label>
          <label>
            업적발생일
            <input
              data-testid="employment-rate-achievements-date-input"
              type="date"
              value={achievementDate}
              onChange={(event) => setAchievementDate(event.target.value)}
            />
          </label>
          <label>
            실적명
            <input
              data-testid="employment-rate-achievements-name-input"
              value={achievementName}
              onChange={(event) => setAchievementName(event.target.value)}
            />
          </label>
        </div>
        <button
          className="mt-4 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          data-testid="employment-rate-achievements-save-button"
          disabled={selected?.achievementStatus === "EVALUATION_CONFIRMED"}
          onClick={() => void save()}
          type="button"
        >
          <Save size={16} /> 저장
        </button>
      </section>

      <section
        className="grid gap-6 lg:grid-cols-2"
        data-testid="employment-rate-achievements-batch-panel"
      >
        <div className="rounded-md border border-ld bg-white p-5">
          <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
          <p className="mt-2 text-sm text-muted">
            오류 또는 중복 행이 하나라도 있으면 전체를 반영하지 않습니다.
          </p>
          <input
            className="mt-4"
            data-testid="employment-rate-achievements-file-input"
            type="file"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <button
            className="mt-4 inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            data-testid="employment-rate-achievements-upload-button"
            onClick={() => void upload()}
            type="button"
          >
            <FileUp size={16} /> 검증·업로드
          </button>
        </div>
        <div className="rounded-md border border-ld bg-white p-5">
          <h2 className="text-lg font-semibold text-dark">일괄 생성·삭제</h2>
          <div className="mt-4 grid gap-3">
            <label>
              평가연도
              <input
                data-testid="employment-rate-achievements-bulk-year-input"
                value={bulkYear}
                onChange={(event) => setBulkYear(event.target.value)}
              />
            </label>
            <label>
              대상 사용자 ID
              <input
                data-testid="employment-rate-achievements-target-user-ids-input"
                value={targetUserIds}
                onChange={(event) => setTargetUserIds(event.target.value)}
              />
            </label>
            <label>
              처리유형
              <select
                data-testid="employment-rate-achievements-bulk-action-select"
                value={bulkAction}
                onChange={(event) =>
                  setBulkAction(event.target.value as "GENERATE" | "DELETE")
                }
              >
                <option value="GENERATE">생성</option>
                <option value="DELETE">삭제</option>
              </select>
            </label>
          </div>
          <button
            className="mt-4 inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            data-testid="employment-rate-achievements-bulk-button"
            onClick={() => void requestBulk()}
            type="button"
          >
            <Send size={16} /> 대상 확인 후 요청
          </button>
          {bulkResult ? (
            <p
              className="mt-3 text-sm"
              data-testid="employment-rate-achievements-bulk-result"
            >
              작업 {bulkResult.jobId}: 처리 {bulkResult.processedCount}건 /
              미처리 {bulkResult.unprocessedCount}건
            </p>
          ) : null}
        </div>
      </section>
    </section>
  );
}
