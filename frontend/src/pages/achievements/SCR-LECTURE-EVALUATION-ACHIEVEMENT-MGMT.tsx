import { Download, RotateCcw, Save, Search } from "lucide-react";
import type React from "react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type ApiErrorField,
  type LectureEvaluationAchievement,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type Filters = {
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  occurredDateFrom: string;
  occurredDateTo: string;
  certificationStatus: string;
};

type FormState = {
  managementItemCode: string;
  occurredDate: string;
  achievementDetail: string;
};

const initialFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  occurredDateFrom: "",
  occurredDateTo: "",
  certificationStatus: "",
};

const initialForm: FormState = {
  managementItemCode: "",
  occurredDate: "",
  achievementDetail: "",
};

/**
 * Search, list, detail, and creation surface for the OpenAPI-defined lecture-evaluation
 * achievement source. Attachment/status mutations remain unavailable until their API contracts
 * are added, so the page exposes them as read-only result fields rather than local-only actions.
 */
export function LectureEvaluationAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [form, setForm] = useState<FormState>(initialForm);
  const [rows, setRows] = useState<LectureEvaluationAchievement[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const selectedRow = useMemo(
    () =>
      rows.find((row) => row.achievementId === selectedId) ?? rows[0] ?? null,
    [rows, selectedId],
  );

  const load = async (requestedPage = page) => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await lectureEvaluationAchievementApi.listLectureEvaluationAchievements(
          {
            ...filters,
            page: requestedPage,
            size: pageSize,
          },
        );
      const achievements = response.data?.achievements ?? [];
      setRows(achievements);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedId(
        (current) => current ?? achievements[0]?.achievementId ?? null,
      );
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const search = async () => {
    setPage(0);
    await load(0);
  };

  const save = async () => {
    const clientErrors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      clientErrors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.occurredDate)
      clientErrors.occurredDate = "업적발생일을 입력하세요.";
    if (form.achievementDetail.trim()) {
      try {
        JSON.parse(form.achievementDetail);
      } catch {
        clientErrors.achievementDetail =
          "세부 입력값은 JSON 객체 형식이어야 합니다.";
      }
    }
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await lectureEvaluationAchievementApi.saveLectureEvaluationAchievement({
          managementItemCode: form.managementItemCode.trim(),
          occurredDate: form.occurredDate,
          achievementDetail: form.achievementDetail.trim()
            ? (JSON.parse(form.achievementDetail) as Record<string, unknown>)
            : {},
        });
      const warning = response.data?.occurredDateWarning
        ? " 평가대상 기간 밖 발생일 경고가 있어도 저장은 허용되었습니다."
        : "";
      setSuccess(`강의평가 실적을 저장했습니다.${warning}`);
      setForm(initialForm);
      setSelectedId(response.data?.achievement.achievementId ?? null);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
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
        : "강의평가 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
        data-testid="lecture-evaluation-achievement-page"
      >
        <PermissionState
          title="강의평가 실적 접근 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-achievement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의평가 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              검색 조건으로 강의평가 실적을 조회하고 새 실적을 등록합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="lecture-evaluation-refresh-button"
          >
            <RotateCcw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {success ? (
        <SuccessState
          title={success}
          message="저장 후 같은 조건으로 목록을 다시 조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="강의평가 실적 처리 오류" message={error} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색 조건</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="관리번호">
            <input
              className="form-input"
              value={filters.managementNo}
              onChange={updateFilter("managementNo")}
              data-testid="lecture-evaluation-management-no-input"
            />
          </Field>
          <Field label="성명">
            <input
              className="form-input"
              value={filters.teacherName}
              onChange={updateFilter("teacherName")}
              data-testid="lecture-evaluation-teacher-name-input"
            />
          </Field>
          <Field label="관리항목">
            <input
              className="form-input"
              value={filters.managementItemCode}
              onChange={updateFilter("managementItemCode")}
              data-testid="lecture-evaluation-management-item-filter"
            />
          </Field>
          <Field label="업적발생일(부터)">
            <input
              type="date"
              className="form-input"
              value={filters.occurredDateFrom}
              onChange={updateFilter("occurredDateFrom")}
              data-testid="lecture-evaluation-date-from-input"
            />
          </Field>
          <Field label="업적발생일(까지)">
            <input
              type="date"
              className="form-input"
              value={filters.occurredDateTo}
              onChange={updateFilter("occurredDateTo")}
              data-testid="lecture-evaluation-date-to-input"
            />
          </Field>
          <Field label="인증상태">
            <input
              className="form-input"
              value={filters.certificationStatus}
              onChange={updateFilter("certificationStatus")}
              data-testid="lecture-evaluation-status-filter"
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="lecture-evaluation-search-button"
          >
            <Search size={16} /> 조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={() => {
              setFilters(initialFilters);
              setPage(0);
            }}
            data-testid="lecture-evaluation-reset-button"
          >
            초기화
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">
            강의평가 실적 목록
          </h2>
          <div className="flex items-center gap-3">
            <label className="text-sm text-muted">
              표시 건수{" "}
              <select
                className="form-select ml-2"
                value={pageSize}
                onChange={(event) =>
                  setPageSize(Number(event.target.value) as PageSize)
                }
                data-testid="lecture-evaluation-page-size-select"
              >
                <option value={20}>20건</option>
                <option value={50}>50건</option>
                <option value={100}>100건</option>
              </select>
            </label>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
              onClick={() =>
                downloadCsv("lecture-evaluation-achievements.csv", rows, [
                  { header: "관리번호", value: (row) => row.managementNo },
                  { header: "성명", value: (row) => row.teacherName },
                  {
                    header: "관리항목",
                    value: (row) => row.managementItemCode,
                  },
                  { header: "업적발생일", value: (row) => row.occurredDate },
                  {
                    header: "인증상태",
                    value: (row) => row.certificationStatus,
                  },
                  {
                    header: "첨부여부",
                    value: (row) => (row.hasAttachment ? "Y" : "N"),
                  },
                ])
              }
              data-testid="lecture-evaluation-export-button"
            >
              <Download size={16} /> Excel 다운로드
            </button>
          </div>
        </div>
        {loading ? (
          <div className="mt-4">
            <LoadingState title="강의평가 실적을 불러오는 중입니다" />
          </div>
        ) : null}
        {!loading && rows.length === 0 ? (
          <div className="mt-4">
            <EmptyState title="조회된 강의평가 실적이 없습니다" />
          </div>
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="lecture-evaluation-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">관리번호</th>
                <th className="px-4 py-3">성명</th>
                <th className="px-4 py-3">관리항목</th>
                <th className="px-4 py-3">업적발생일</th>
                <th className="px-4 py-3">인증상태</th>
                <th className="px-4 py-3">첨부여부</th>
                <th className="px-4 py-3">상세</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid="lecture-evaluation-row"
                >
                  <td className="px-4 py-3">{row.managementNo}</td>
                  <td className="px-4 py-3">{row.teacherName}</td>
                  <td className="px-4 py-3">{row.managementItemCode}</td>
                  <td className="px-4 py-3">{row.occurredDate}</td>
                  <td className="px-4 py-3">{row.certificationStatus}</td>
                  <td className="px-4 py-3">
                    {row.hasAttachment ? "첨부" : "없음"}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      className="text-primary underline"
                      onClick={() => setSelectedId(row.achievementId)}
                      data-testid="lecture-evaluation-detail-button"
                    >
                      상세
                    </button>
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
            disabled={page === 0}
            onClick={() => setPage((value) => Math.max(0, value - 1))}
            data-testid="lecture-evaluation-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            disabled={(page + 1) * pageSize >= totalElements}
            onClick={() => setPage((value) => value + 1)}
            data-testid="lecture-evaluation-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="lecture-evaluation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          강의평가 실적 상세·등록
        </h2>
        {selectedRow ? (
          <p className="mt-2 text-sm text-muted">
            선택 실적: {selectedRow.managementNo} / 인증상태{" "}
            {selectedRow.certificationStatus} / 첨부{" "}
            {selectedRow.hasAttachment ? "있음" : "없음"}
          </p>
        ) : (
          <p className="mt-2 text-sm text-muted">
            목록에서 상세를 선택하거나 새 실적을 입력하세요.
          </p>
        )}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="관리항목 *" error={fieldErrors.managementItemCode}>
            <input
              className="form-input"
              value={form.managementItemCode}
              onChange={updateForm("managementItemCode")}
              data-testid="lecture-evaluation-management-item-input"
            />
          </Field>
          <Field label="업적발생일 *" error={fieldErrors.occurredDate}>
            <input
              type="date"
              className="form-input"
              value={form.occurredDate}
              onChange={updateForm("occurredDate")}
              data-testid="lecture-evaluation-occurred-date-input"
            />
          </Field>
          <div className="md:col-span-2">
            <Field
              label="FR-018 동적 세부 입력(JSON)"
              error={fieldErrors.achievementDetail}
            >
              <textarea
                className="form-input min-h-28"
                value={form.achievementDetail}
                onChange={updateForm("achievementDetail")}
                data-testid="lecture-evaluation-detail-input"
              />
            </Field>
          </div>
        </div>
        <p className="mt-3 text-sm text-muted">
          첨부파일과 상태 전이는 현재 승인된 저장 API 요청 필드에 포함되지 않아
          이 화면에서는 조회 결과만 표시합니다.
        </p>
        <button
          type="button"
          className="mt-4 inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          disabled={saving}
          onClick={() => void save()}
          data-testid="lecture-evaluation-save-button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );

  function updateFilter(key: keyof Filters) {
    return (event: React.ChangeEvent<HTMLInputElement>) =>
      setFilters((current) => ({ ...current, [key]: event.target.value }));
  }

  function updateForm(key: keyof FormState) {
    return (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm((current) => ({ ...current, [key]: event.target.value }));
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
    <label className="block text-sm font-medium text-dark">
      {label}
      {children}
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return Object.fromEntries(
    fields.map((field) => [field.field, field.message]),
  );
}
