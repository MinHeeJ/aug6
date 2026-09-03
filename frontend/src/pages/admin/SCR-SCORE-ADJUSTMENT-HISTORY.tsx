import type React from "react";
import { Download, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  scoreAdjustmentHistoryApi,
  type ApiErrorField,
  type PageSize,
  type ScoreAdjustmentHistoryDetail,
  type ScoreAdjustmentHistoryRow,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type ScoreAdjustmentFilters = {
  targetUserId: string;
  evaluationYear: string;
  areaCode: string;
  adjustmentTarget: string;
};

const initialFilters: ScoreAdjustmentFilters = {
  targetUserId: "",
  evaluationYear: "2026",
  areaCode: "",
  adjustmentTarget: "",
};

export function ScoreAdjustmentHistoryPage() {
  const [filters, setFilters] =
    useState<ScoreAdjustmentFilters>(initialFilters);
  const [rows, setRows] = useState<ScoreAdjustmentHistoryRow[]>([]);
  const [selectedAdjustmentHistId, setSelectedAdjustmentHistId] = useState<
    string | null
  >(null);
  const [detail, setDetail] = useState<ScoreAdjustmentHistoryDetail | null>(
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
      rows.find((row) => row.adjustmentHistId === selectedAdjustmentHistId) ??
      rows[0] ??
      null,
    [rows, selectedAdjustmentHistId],
  );

  const conditionSummary = useMemo(
    () =>
      `대상자 ${filters.targetUserId || "전체"} / 평가연도 ${filters.evaluationYear || "전체"} / 평가영역 ${filters.areaCode || "전체"} / 조정대상 ${adjustmentTargetLabel(filters.adjustmentTarget)}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await scoreAdjustmentHistoryApi.listScoreAdjustmentHistories({
          evaluationYear: filters.evaluationYear,
          areaCode: filters.areaCode,
          targetUserId: parseOptionalNumber(filters.targetUserId),
          adjustmentTarget: filters.adjustmentTarget,
          page,
          size: pageSize,
        });
      const nextRows = response.data?.results ?? [];
      setRows(nextRows);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedAdjustmentHistId(
        (current) => current ?? nextRows[0]?.adjustmentHistId ?? null,
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
    if (!selectedRow?.adjustmentHistId) {
      setDetail(null);
      return;
    }
    void loadDetail(selectedRow.adjustmentHistId);
  }, [selectedRow?.adjustmentHistId]);

  const search = async () => {
    setPage(0);
    setSelectedAdjustmentHistId(null);
    setDetail(null);
    await load();
  };

  const loadDetail = async (adjustmentHistId: string) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response =
        await scoreAdjustmentHistoryApi.getScoreAdjustmentHistoryDetail(
          adjustmentHistId,
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
        await scoreAdjustmentHistoryApi.downloadScoreAdjustmentHistoriesExcel({
          evaluationYear: filters.evaluationYear,
          areaCode: filters.areaCode,
          targetUserId: parseOptionalNumber(filters.targetUserId),
          adjustmentTarget: filters.adjustmentTarget,
        });
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
        : "점수 조정 이력 조회를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-SCORE-ADJUSTMENT-HISTORY"
        data-testid="score-adjustment-history-page"
      >
        <PermissionState
          title="점수 조정 이력 권한이 없습니다"
          message="R04, R08, R09 권한이 필요합니다. R01 교원에게는 표시되지 않습니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-SCORE-ADJUSTMENT-HISTORY"
      data-testid="score-adjustment-history-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">파일·데이터 관리 / 데이터 이력 관리</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">점수 조정 이력</h1>
        <p className="mt-2 text-sm text-muted">
          수동 조정된 점수와 평가백분율의 전후값, 사유, 조정자, 승인자를
          조회합니다.
        </p>
      </div>

      {error ? (
        <ErrorState title="점수 조정 이력 조회 오류" message={error} />
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
              data-testid="score-adjustment-target-user-input"
              aria-label="대상자"
            />
          </Field>
          <Field label="평가연도" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="score-adjustment-year-input"
              aria-label="평가연도"
            />
          </Field>
          <Field label="평가영역" error={fieldErrors.areaCode}>
            <input
              className="form-input"
              value={filters.areaCode}
              onChange={update("areaCode")}
              data-testid="score-adjustment-area-input"
              aria-label="평가영역"
            />
          </Field>
          <Field label="조정대상" error={fieldErrors.adjustmentTarget}>
            <select
              className="form-select"
              value={filters.adjustmentTarget}
              onChange={updateSelect("adjustmentTarget")}
              data-testid="score-adjustment-target-select"
              aria-label="조정대상"
            >
              <option value="">전체</option>
              <option value="SCORE">점수</option>
              <option value="PERCENTAGE">평가백분율</option>
            </select>
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="score-adjustment-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void download()}
            data-testid="score-adjustment-excel-button"
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
              점수·평가백분율 조정 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              선택 행의 상세에서 비고 전문과 승인 경위를 확인합니다.
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
              data-testid="score-adjustment-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="점수 조정 이력을 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 점수 조정 이력이 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="score-adjustment-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">대상자</th>
                <th className="px-4 py-3">평가영역</th>
                <th className="px-4 py-3">관리항목</th>
                <th className="px-4 py-3">조정대상</th>
                <th className="px-4 py-3">전값</th>
                <th className="px-4 py-3">후값</th>
                <th className="px-4 py-3">사유</th>
                <th className="px-4 py-3">조정자</th>
                <th className="px-4 py-3">승인자</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.adjustmentHistId}
                  data-testid="score-adjustment-row"
                >
                  <td className="px-4 py-3">
                    <input
                      type="radio"
                      name="score-adjustment"
                      checked={
                        (selectedAdjustmentHistId ??
                          rows[0]?.adjustmentHistId) === row.adjustmentHistId
                      }
                      onChange={() =>
                        setSelectedAdjustmentHistId(row.adjustmentHistId)
                      }
                      data-testid="score-adjustment-radio"
                    />
                  </td>
                  <td className="px-4 py-3 font-medium text-dark">
                    {row.targetUserName}
                  </td>
                  <td className="px-4 py-3">{row.areaCode}</td>
                  <td className="px-4 py-3">{row.managementItemCode}</td>
                  <td className="px-4 py-3">
                    {adjustmentTargetLabel(row.adjustmentTarget)}
                  </td>
                  <td className="px-4 py-3">{formatValue(row.beforeValue)}</td>
                  <td className="px-4 py-3 font-semibold text-primary">
                    {formatValue(row.afterValue)}
                  </td>
                  <td className="px-4 py-3">{row.adjustmentReason}</td>
                  <td className="px-4 py-3">{row.adjustedByName}</td>
                  <td className="px-4 py-3">{row.approvedByName}</td>
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
            data-testid="score-adjustment-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="score-adjustment-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="score-adjustment-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">선택 조정 상세</h2>
        <p className="mt-2 text-sm text-muted">
          선택 이력: {selectedRow?.adjustmentHistId ?? "미선택"}
        </p>
        {detailLoading ? (
          <LoadingState title="조정 상세를 불러오는 중입니다" />
        ) : null}
        {!detailLoading && !selectedRow ? (
          <EmptyState title="상세를 볼 조정 이력을 선택하세요" />
        ) : null}
        {!detailLoading && !detail ? <ScoreAdjustmentDetailGuide /> : null}
        {!detailLoading && detail ? (
          <div className="mt-4 space-y-4">
            <div className="grid gap-3 md:grid-cols-4">
              <BasisCard
                title="조정대상"
                value={adjustmentTargetLabel(detail.adjustmentTarget)}
              />
              <BasisCard title="전값" value={formatValue(detail.beforeValue)} />
              <BasisCard title="후값" value={formatValue(detail.afterValue)} />
              <BasisCard title="사유" value={detail.adjustmentReason} />
              <BasisCard title="조정자" value={detail.adjustedByName} />
              <BasisCard title="승인자" value={detail.approvedByName} />
              <BasisCard title="조정일시" value={detail.adjustedAt} />
              <BasisCard title="승인일시" value={detail.approvedAt} />
            </div>
            <article
              className="rounded-md border border-ld bg-lightgray p-4"
              data-testid="score-adjustment-remark-card"
            >
              <h3 className="text-sm font-semibold text-dark">비고 전문</h3>
              <p className="mt-2 text-sm text-muted">
                {detail.adjustmentRemark}
              </p>
            </article>
            <article
              className="rounded-md border border-ld bg-lightgray p-4"
              data-testid="score-adjustment-approval-card"
            >
              <h3 className="text-sm font-semibold text-dark">승인 경위</h3>
              <p className="mt-2 text-sm text-muted">{detail.approvalTrace}</p>
            </article>
            <p className="text-sm text-muted">{detail.readOnlyNotice}</p>
          </div>
        ) : null}
      </section>
    </section>
  );

  function update(key: keyof ScoreAdjustmentFilters) {
    return (event: React.ChangeEvent<HTMLInputElement>) =>
      setFilters((current) => ({ ...current, [key]: event.target.value }));
  }

  function updateSelect(key: keyof ScoreAdjustmentFilters) {
    return (event: React.ChangeEvent<HTMLSelectElement>) =>
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
      data-testid="score-adjustment-basis-card"
    >
      <h3 className="text-sm font-semibold text-dark">{title}</h3>
      <p className="mt-2 text-sm text-muted">{value}</p>
    </article>
  );
}

function ScoreAdjustmentDetailGuide() {
  const detailFields = [
    "전값",
    "후값",
    "사유",
    "비고 전문",
    "조정자",
    "승인자",
    "승인 경위",
  ];
  return (
    <article
      className="mt-4 rounded-md border border-ld bg-lightgray p-4"
      data-testid="score-adjustment-detail-guide"
    >
      <h3 className="text-sm font-semibold text-dark">조정 상세</h3>
      <p className="mt-2 text-sm text-muted">
        선택 행의 상세에서 아래 조회 전용 항목이 표시됩니다.
      </p>
      <ul className="mt-3 grid gap-2 text-sm text-muted md:grid-cols-4">
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

function formatValue(value: number) {
  return Number(value).toFixed(2);
}

function adjustmentTargetLabel(value: string) {
  if (value === "SCORE") return "점수";
  if (value === "PERCENTAGE") return "평가백분율";
  return "전체";
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
