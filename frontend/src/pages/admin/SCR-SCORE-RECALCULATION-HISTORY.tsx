import type React from "react";
import { Download, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  scoreRecalculationHistoryApi,
  type ApiErrorField,
  type PageSize,
  type ScoreRecalculationHistoryDetail,
  type ScoreRecalculationHistoryRow,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type ScoreRecalculationFilters = {
  targetUserId: string;
  evaluationYear: string;
  executedFrom: string;
  executedTo: string;
};

const initialFilters: ScoreRecalculationFilters = {
  targetUserId: "",
  evaluationYear: "2026",
  executedFrom: "",
  executedTo: "",
};

export function ScoreRecalculationHistoryPage() {
  const [filters, setFilters] =
    useState<ScoreRecalculationFilters>(initialFilters);
  const [rows, setRows] = useState<ScoreRecalculationHistoryRow[]>([]);
  const [selectedRecalcHistId, setSelectedRecalcHistId] = useState<
    string | null
  >(null);
  const [detail, setDetail] = useState<ScoreRecalculationHistoryDetail | null>(
    null,
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedRow = useMemo(
    () =>
      rows.find((row) => row.recalcHistId === selectedRecalcHistId) ??
      rows[0] ??
      null,
    [rows, selectedRecalcHistId],
  );

  const conditionSummary = useMemo(
    () =>
      `대상자 ${filters.targetUserId || "전체"} / 평가연도 ${filters.evaluationYear || "전체"} / 작업기간 ${filters.executedFrom || "전체"}~${filters.executedTo || "전체"}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await scoreRecalculationHistoryApi.listScoreRecalculationHistories({
          evaluationYear: filters.evaluationYear,
          targetUserId: parseOptionalNumber(filters.targetUserId),
          executedFrom: filters.executedFrom,
          executedTo: filters.executedTo,
          page,
          size: pageSize,
        });
      const nextRows = response.data?.results ?? [];
      setRows(nextRows);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedRecalcHistId(
        (current) => current ?? nextRows[0]?.recalcHistId ?? null,
      );
      setFieldErrors({});
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  useEffect(() => {
    if (!selectedRow?.recalcHistId) {
      setDetail(null);
      return;
    }
    void loadDetail(selectedRow.recalcHistId);
  }, [selectedRow?.recalcHistId]);

  const search = async () => {
    setPage(0);
    setSelectedRecalcHistId(null);
    setDetail(null);
    await load();
  };

  const loadDetail = async (recalcHistId: string) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response =
        await scoreRecalculationHistoryApi.getScoreRecalculationHistoryDetail(
          recalcHistId,
        );
      setDetail(response.data ?? null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setDetailLoading(false);
    }
  };

  const download = async () => {
    try {
      setSuccessMessage(null);
      const response =
        await scoreRecalculationHistoryApi.downloadScoreRecalculationHistoriesExcel(
          {
            evaluationYear: filters.evaluationYear,
            targetUserId: parseOptionalNumber(filters.targetUserId),
            executedFrom: filters.executedFrom,
            executedTo: filters.executedTo,
          },
        );
      const data = response.data;
      setSuccessMessage(
        data
          ? `${data.fileName} 다운로드 요청이 기록되었습니다. 대상 ${data.rowCount}건.`
          : "Excel 다운로드 요청이 기록되었습니다.",
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "재계산 이력 조회를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-SCORE-RECALCULATION-HISTORY"
        data-testid="score-recalculation-history-page"
      >
        <PermissionState
          title="재계산 이력 권한이 없습니다"
          message="R04, R08, R09 권한이 필요합니다. R01 교원에게는 표시되지 않습니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-SCORE-RECALCULATION-HISTORY"
      data-testid="score-recalculation-history-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">파일·데이터 관리 / 데이터 이력 관리</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">재계산 이력</h1>
        <p className="mt-2 text-sm text-muted">
          재계산 작업별 기준, 산식버전, 대상범위, 변경건수와 전후 총점을
          조회합니다.
        </p>
      </div>

      {error ? (
        <ErrorState title="재계산 이력 조회 오류" message={error} />
      ) : null}
      {successMessage ? (
        <SuccessState title="Excel 다운로드" message={successMessage} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <p className="mt-2 text-sm text-muted">검색 조건: {conditionSummary}</p>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <Field label="대상자" error={fieldErrors.targetUserId}>
            <input
              className="form-input"
              inputMode="numeric"
              value={filters.targetUserId}
              onChange={update("targetUserId")}
              data-testid="score-recalculation-target-user-input"
              aria-label="대상자"
            />
          </Field>
          <Field label="평가연도" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="score-recalculation-year-input"
              aria-label="평가연도"
            />
          </Field>
          <Field label="작업 시작일" error={fieldErrors.executedFrom}>
            <input
              className="form-input"
              type="date"
              value={filters.executedFrom}
              onChange={update("executedFrom")}
              data-testid="score-recalculation-from-input"
              aria-label="작업 시작일"
            />
          </Field>
          <Field label="작업 종료일" error={fieldErrors.executedTo}>
            <input
              className="form-input"
              type="date"
              value={filters.executedTo}
              onChange={update("executedTo")}
              data-testid="score-recalculation-to-input"
              aria-label="작업 종료일"
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="score-recalculation-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void download()}
            data-testid="score-recalculation-excel-button"
          >
            <Download size={16} />
            Excel
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              재계산 작업 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              선택 작업의 상세에서 대상자별 주요 변경내역과 사용기준 상세를
              확인합니다.
            </p>
          </div>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="form-select ml-2"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as PageSize)
              }
              data-testid="score-recalculation-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="재계산 이력을 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 재계산 이력이 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="score-recalculation-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">작업ID</th>
                <th className="px-4 py-3">실행일시</th>
                <th className="px-4 py-3">대상자</th>
                <th className="px-4 py-3">산식버전</th>
                <th className="px-4 py-3">대상범위</th>
                <th className="px-4 py-3">변경건수</th>
                <th className="px-4 py-3">전총점</th>
                <th className="px-4 py-3">후총점</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.recalcHistId}
                  data-testid="score-recalculation-row"
                >
                  <td className="px-4 py-3">
                    <input
                      type="radio"
                      name="score-recalculation"
                      checked={
                        (selectedRecalcHistId ?? rows[0]?.recalcHistId) ===
                        row.recalcHistId
                      }
                      onChange={() => setSelectedRecalcHistId(row.recalcHistId)}
                      data-testid="score-recalculation-radio"
                    />
                  </td>
                  <td className="px-4 py-3 font-medium text-dark">
                    {row.jobId}
                  </td>
                  <td className="px-4 py-3">{row.executedAt}</td>
                  <td className="px-4 py-3">{row.targetUserName}</td>
                  <td className="px-4 py-3">{row.formulaVersionId}</td>
                  <td className="px-4 py-3">
                    {targetScopeLabel(row.targetScope)}
                  </td>
                  <td className="px-4 py-3">{row.changedCount}</td>
                  <td className="px-4 py-3">
                    {formatScore(row.beforeTotalScore)}
                  </td>
                  <td className="px-4 py-3 font-semibold text-primary">
                    {formatScore(row.afterTotalScore)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
            data-testid="score-recalculation-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="score-recalculation-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="score-recalculation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">작업 상세</h2>
        <p className="mt-2 text-sm text-muted">
          선택 작업: {selectedRow?.jobId ?? "미선택"}
        </p>
        {detailLoading ? (
          <LoadingState title="재계산 상세를 불러오는 중입니다" />
        ) : null}
        {!detailLoading && !selectedRow ? (
          <EmptyState title="상세를 볼 재계산 작업을 선택하세요" />
        ) : null}
        {!detailLoading && !detail ? <ScoreRecalculationDetailGuide /> : null}
        {!detailLoading && detail ? (
          <div className="mt-4 space-y-4">
            <div className="grid gap-3 md:grid-cols-4">
              <BasisCard title="작업ID" value={detail.jobId} />
              <BasisCard title="산식버전" value={detail.formulaVersionId} />
              <BasisCard
                title="대상범위"
                value={targetScopeLabel(detail.targetScope)}
              />
              <BasisCard title="변경건수" value={`${detail.changedCount}건`} />
              <BasisCard
                title="전총점"
                value={formatScore(detail.beforeTotalScore)}
              />
              <BasisCard
                title="후총점"
                value={formatScore(detail.afterTotalScore)}
              />
              <BasisCard title="실행일시" value={detail.executedAt} />
              <BasisCard title="평가연도" value={detail.evaluationYear} />
            </div>
            <article
              className="rounded-md border border-ld bg-lightgray p-4"
              data-testid="score-recalculation-criteria-card"
            >
              <h3 className="text-sm font-semibold text-dark">사용기준 상세</h3>
              <p className="mt-2 text-sm text-muted">{detail.criteriaDetail}</p>
            </article>
            <article
              className="rounded-md border border-ld bg-lightgray p-4"
              data-testid="score-recalculation-target-changes-card"
            >
              <h3 className="text-sm font-semibold text-dark">
                대상자별 주요 변경내역
              </h3>
              <pre className="mt-2 whitespace-pre-wrap text-sm text-muted">
                {detail.targetChangeSummaryJson}
              </pre>
            </article>
            <p className="text-sm text-muted">{detail.readOnlyNotice}</p>
          </div>
        ) : null}
      </section>
    </section>
  );

  function update(key: keyof ScoreRecalculationFilters) {
    return (event: React.ChangeEvent<HTMLInputElement>) =>
      setFilters((current) => ({ ...current, [key]: event.target.value }));
  }
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="text-sm font-medium text-dark">
      {label}
      <div className="mt-1">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function BasisCard({ title, value }: { title: string; value: string }) {
  return (
    <article
      className="rounded-md border border-ld bg-lightgray p-4"
      data-testid="score-recalculation-basis-card"
    >
      <h3 className="text-sm font-semibold text-dark">{title}</h3>
      <p className="mt-2 text-sm text-muted">{value}</p>
    </article>
  );
}

function ScoreRecalculationDetailGuide() {
  const detailFields = [
    "작업ID",
    "실행일시",
    "산식버전",
    "대상범위",
    "변경건수",
    "전총점",
    "후총점",
    "대상자별 주요 변경내역",
    "사용기준 상세",
  ];
  return (
    <article
      className="mt-4 rounded-md border border-ld bg-lightgray p-4"
      data-testid="score-recalculation-detail-guide"
    >
      <h3 className="text-sm font-semibold text-dark">작업 상세</h3>
      <p className="mt-2 text-sm text-muted">
        선택 작업의 상세에서 아래 조회 전용 항목이 표시됩니다. 재계산 실행
        기능은 제공하지 않습니다.
      </p>
      <ul className="mt-3 grid gap-2 text-sm text-muted md:grid-cols-3">
        {detailFields.map((field) => (
          <li key={field}>• {field}</li>
        ))}
      </ul>
    </article>
  );
}

function parseOptionalNumber(value: string) {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function formatScore(value: number) {
  return Number(value).toFixed(2);
}

function targetScopeLabel(value: string) {
  if (value === "FORMULA_VERSION_CHANGE") return "산식버전 변경";
  if (value === "TARGET_SCOPE") return "대상 범위";
  if (value === "NO_CHANGE") return "변경 없음";
  return value || "전체";
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
