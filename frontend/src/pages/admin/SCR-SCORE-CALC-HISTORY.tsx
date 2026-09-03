import type React from "react";
import { Download, ExternalLink, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  scoreCalculationHistoryApi,
  type ApiErrorField,
  type PageSize,
  type ScoreCalculationHistoryDetail,
  type ScoreCalculationHistoryRow,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type ScoreCalculationFilters = {
  targetUserId: string;
  evaluationYear: string;
  areaCode: string;
};

const initialFilters: ScoreCalculationFilters = {
  targetUserId: "",
  evaluationYear: "2026",
  areaCode: "",
};

export function ScoreCalculationHistoryPage() {
  const [filters, setFilters] =
    useState<ScoreCalculationFilters>(initialFilters);
  const [rows, setRows] = useState<ScoreCalculationHistoryRow[]>([]);
  const [selectedCalcHistId, setSelectedCalcHistId] = useState<string | null>(
    null,
  );
  const [detail, setDetail] = useState<ScoreCalculationHistoryDetail | null>(
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
      rows.find((row) => row.calcHistId === selectedCalcHistId) ??
      rows[0] ??
      null,
    [rows, selectedCalcHistId],
  );

  const conditionSummary = useMemo(
    () =>
      `대상자 ${filters.targetUserId || "전체/본인"} / 평가연도 ${filters.evaluationYear || "전체"} / 평가영역 ${filters.areaCode || "전체"}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await scoreCalculationHistoryApi.listScoreCalculationHistories({
          evaluationYear: filters.evaluationYear,
          areaCode: filters.areaCode,
          targetUserId: parseOptionalNumber(filters.targetUserId),
          page,
          size: pageSize,
        });
      const nextRows = response.data?.results ?? [];
      setRows(nextRows);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedCalcHistId(
        (current) => current ?? nextRows[0]?.calcHistId ?? null,
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
    if (!selectedRow?.calcHistId) {
      setDetail(null);
      return;
    }
    void loadDetail(selectedRow.calcHistId);
  }, [selectedRow?.calcHistId]);

  const search = async () => {
    setPage(0);
    setSelectedCalcHistId(null);
    setDetail(null);
    await load();
  };

  const loadDetail = async (calcHistId: string) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response =
        await scoreCalculationHistoryApi.getScoreCalculationHistoryDetail(
          calcHistId,
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
        await scoreCalculationHistoryApi.downloadScoreCalculationHistoriesExcel(
          {
            evaluationYear: filters.evaluationYear,
            areaCode: filters.areaCode,
            targetUserId: parseOptionalNumber(filters.targetUserId),
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
        : "점수 산출 이력 조회를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-SCORE-CALC-HISTORY"
        data-testid="score-calculation-history-page"
      >
        <PermissionState
          title="점수 산출 이력 권한이 없습니다"
          message="R04, R08, R09 또는 본인 범위 R01 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-SCORE-CALC-HISTORY"
      data-testid="score-calculation-history-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">파일·데이터 관리 / 데이터 이력 관리</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">점수 산출 이력</h1>
        <p className="mt-2 text-sm text-muted">
          개인별 점수의 원천 실적부터 산출점수까지 단계별 산출근거를 조회합니다.
        </p>
      </div>

      {error ? (
        <ErrorState title="점수 산출 이력 조회 오류" message={error} />
      ) : null}
      {successMessage ? (
        <SuccessState title="Excel 다운로드" message={successMessage} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <p className="mt-2 text-sm text-muted">검색 조건: {conditionSummary}</p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="대상자" error={fieldErrors.targetUserId}>
            <input
              className="form-input"
              inputMode="numeric"
              value={filters.targetUserId}
              onChange={update("targetUserId")}
              data-testid="score-calculation-target-user-input"
              aria-label="대상자"
            />
          </Field>
          <Field label="평가연도" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="score-calculation-year-input"
              aria-label="평가연도"
            />
          </Field>
          <Field label="평가영역" error={fieldErrors.areaCode}>
            <input
              className="form-input"
              value={filters.areaCode}
              onChange={update("areaCode")}
              data-testid="score-calculation-area-input"
              aria-label="평가영역"
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="score-calculation-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void download()}
            data-testid="score-calculation-excel-button"
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
              대상자별 평가점수 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              항목별 산출점수를 선택하면 단계별 산출근거를 조회합니다.
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
              data-testid="score-calculation-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="점수 산출 이력을 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 점수 산출 이력이 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="score-calculation-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">대상자</th>
                <th className="px-4 py-3">평가영역</th>
                <th className="px-4 py-3">관리항목</th>
                <th className="px-4 py-3">산출점수</th>
                <th className="px-4 py-3">산식버전</th>
                <th className="px-4 py-3">세대</th>
                <th className="px-4 py-3">상한</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr key={row.calcHistId} data-testid="score-calculation-row">
                  <td className="px-4 py-3">
                    <input
                      type="radio"
                      name="score-calculation"
                      checked={
                        (selectedCalcHistId ?? rows[0]?.calcHistId) ===
                        row.calcHistId
                      }
                      onChange={() => setSelectedCalcHistId(row.calcHistId)}
                      data-testid="score-calculation-radio"
                    />
                  </td>
                  <td className="px-4 py-3 font-medium text-dark">
                    {row.targetUserName}
                  </td>
                  <td className="px-4 py-3">{row.areaCode}</td>
                  <td className="px-4 py-3">{row.managementItemCode}</td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      className="font-semibold text-primary underline"
                      onClick={() => setSelectedCalcHistId(row.calcHistId)}
                      data-testid="score-calculation-score-button"
                    >
                      {formatScore(row.calculatedScore)}
                    </button>
                  </td>
                  <td className="px-4 py-3">{row.formulaVersionId}</td>
                  <td className="px-4 py-3">{row.generationNo}</td>
                  <td className="px-4 py-3">{yesNoLabel(row.capAppliedYn)}</td>
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
            data-testid="score-calculation-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="score-calculation-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="score-calculation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">선택 점수 산출근거</h2>
        <p className="mt-2 text-sm text-muted">
          선택 이력: {selectedRow?.calcHistId ?? "미선택"}
        </p>
        {detailLoading ? (
          <LoadingState title="산출근거 상세를 불러오는 중입니다" />
        ) : null}
        {!detailLoading && !selectedRow ? (
          <EmptyState title="산출근거를 볼 점수를 선택하세요" />
        ) : null}
        {!detailLoading && !detail ? <ScoreCalculationDetailGuide /> : null}
        {!detailLoading && detail ? (
          <div className="mt-4 space-y-4">
            <div className="grid gap-3 md:grid-cols-4">
              <BasisCard
                title="원천 실적"
                value={detail.sourceAchievementTitle}
              />
              <BasisCard title="관리항목" value={detail.managementItemCode} />
              <BasisCard
                title="기준점수"
                value={formatScore(detail.baseScore)}
              />
              <BasisCard
                title="참여구분"
                value={participationTypeLabel(detail.participationType)}
              />
              <BasisCard
                title="배분율"
                value={String(detail.distributionRate)}
              />
              <BasisCard title="산식" value={detail.formulaVersionId} />
              <BasisCard
                title="산출점수"
                value={formatScore(detail.calculatedScore)}
              />
              <BasisCard title="세대" value={`${detail.generationNo}`} />
            </div>
            <article
              className="rounded-md border border-ld bg-lightgray p-4"
              data-testid="score-calculation-steps-card"
            >
              <h3 className="text-sm font-semibold text-dark">
                단계별 산출근거
              </h3>
              <pre className="mt-2 whitespace-pre-wrap break-all text-xs text-muted">
                {detail.calculationStepsJson}
              </pre>
            </article>
            <a
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              href={detail.sourceAchievementLink}
              data-testid="score-calculation-source-link"
            >
              <ExternalLink size={16} />
              원천 업적 상세로 이동
            </a>
            <p className="text-sm text-muted">{detail.readOnlyNotice}</p>
          </div>
        ) : null}
      </section>
    </section>
  );

  function update(key: keyof ScoreCalculationFilters) {
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
      data-testid="score-calculation-basis-card"
    >
      <h3 className="text-sm font-semibold text-dark">{title}</h3>
      <p className="mt-2 text-sm text-muted">{value}</p>
    </article>
  );
}

function ScoreCalculationDetailGuide() {
  const basisFields = [
    "원천 실적",
    "관리항목",
    "기준점수",
    "참여구분",
    "배분율",
    "산식",
    "산출점수",
    "세대",
  ];
  return (
    <article
      className="mt-4 rounded-md border border-ld bg-lightgray p-4"
      data-testid="score-calculation-detail-guide"
    >
      <h3 className="text-sm font-semibold text-dark">단계별 산출근거</h3>
      <p className="mt-2 text-sm text-muted">
        항목별 산출점수를 선택하면 아래 항목과 원천 업적 상세로 이동 링크가
        표시됩니다.
      </p>
      <ul className="mt-3 grid gap-2 text-sm text-muted md:grid-cols-4">
        {basisFields.map((field) => (
          <li key={field}>• {field}</li>
        ))}
      </ul>
      <button
        type="button"
        className="mt-4 inline-flex h-10 cursor-not-allowed items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
        disabled
        data-testid="score-calculation-source-link-disabled"
      >
        <ExternalLink size={16} />
        원천 업적 상세로 이동
      </button>
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

function yesNoLabel(value: "Y" | "N" | string) {
  return value === "Y" ? "상한 적용" : "상한 미적용";
}

function participationTypeLabel(value: string) {
  if (value === "SOLE") return "단독";
  if (value === "CO_AUTHOR") return "공동저자";
  if (value === "OTHER") return "기타";
  return value;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
