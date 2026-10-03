import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
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
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  certificationStatus: string;
  hasAttachments: boolean;
};

type SearchResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: Achievement;
  achievementDateWarning: boolean;
  warningMessage?: string | null;
};

type Filters = {
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  certificationStatus: string;
};

type Form = {
  achievementId?: number;
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
  attachmentIds: string;
};

const initialFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  certificationStatus: "",
};

const initialForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentIds: "",
};

/** 취업률 제고 실적의 조회, 상세 확인, 저장을 제공하는 교육영역 화면이다. */
export function EmploymentRateImprovementsPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [form, setForm] = useState<Form>(initialForm);
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const confirmedSelected =
    selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        listPath(filters, page, pageSize),
      );
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = async (row: Achievement) => {
    try {
      setError(null);
      const response = await apiRequest<Achievement>(
        `/api/business/employment-rate-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        achievementId: detail.achievementId,
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        specialLectureStartDate: detail.specialLectureStartDate ?? "",
        specialLectureEndDate: detail.specialLectureEndDate ?? "",
        mockExamQuestionPeriod: detail.mockExamQuestionPeriod ?? "",
        attachmentIds: "",
      });
      setFieldErrors({});
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await apiRequest<SaveResponse>(
        form.achievementId
          ? `/api/business/employment-rate-improvements/${form.achievementId}`
          : "/api/business/employment-rate-improvements",
        {
          method: form.achievementId ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode.trim(),
            achievementDate: form.achievementDate,
            specialLectureStartDate: form.specialLectureStartDate || undefined,
            specialLectureEndDate: form.specialLectureEndDate || undefined,
            mockExamQuestionPeriod:
              form.mockExamQuestionPeriod.trim() || undefined,
            ...attachmentRequestField(
              form.attachmentIds,
              Boolean(form.achievementId),
            ),
          }),
        },
      );
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm({
          achievementId: saved.achievementId,
          managementItemCode: saved.managementItemCode,
          achievementDate: saved.achievementDate,
          specialLectureStartDate: saved.specialLectureStartDate ?? "",
          specialLectureEndDate: saved.specialLectureEndDate ?? "",
          mockExamQuestionPeriod: saved.mockExamQuestionPeriod ?? "",
          attachmentIds: "",
        });
      }
      setSuccess(
        response.data?.achievementDateWarning
          ? (response.data.warningMessage ??
              "평가대상 기간 밖 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "취업률 제고 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
        data-testid="employment-rate-improvements-page"
      >
        <PermissionState
          title="취업률 제고 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-rate-improvements-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 제고 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특강기간과 모의평가 출제기간을 확인하고 취업률 제고 실적을
              저장합니다.
            </p>
          </div>
          <div className="flex gap-2">
            <button
              className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              data-testid="employment-rate-improvements-new-button"
              onClick={() => {
                setSelected(null);
                setForm(initialForm);
                setFieldErrors({});
                setSuccess(null);
              }}
              type="button"
            >
              새 실적 입력
            </button>
            <button
              className={
                "inline-flex items-center justify-center gap-2 rounded-md bg-primary " +
                "px-4 py-2 text-sm font-semibold text-white"
              }
              data-testid="employment-rate-improvements-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        </div>
      </div>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-improvements-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="관리번호">
            <input
              data-testid="employment-rate-improvements-management-no-input"
              value={filters.managementNo}
              onChange={(event) =>
                setFilters({ ...filters, managementNo: event.target.value })
              }
            />
          </Field>
          <Field label="성명">
            <input
              data-testid="employment-rate-improvements-teacher-name-input"
              value={filters.teacherName}
              onChange={(event) =>
                setFilters({ ...filters, teacherName: event.target.value })
              }
            />
          </Field>
          <Field label="관리항목">
            <input
              data-testid="employment-rate-improvements-management-item-filter-input"
              value={filters.managementItemCode}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  managementItemCode: event.target.value,
                })
              }
            />
          </Field>
          <Field label="인증상태">
            <input
              data-testid="employment-rate-improvements-status-filter-input"
              value={filters.certificationStatus}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  certificationStatus: event.target.value,
                })
              }
            />
          </Field>
          <button
            className={
              "mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md " +
              "border border-primary px-4 text-sm font-semibold text-primary"
            }
            data-testid="employment-rate-improvements-search-button"
            onClick={() => {
              setPage(0);
              void load();
            }}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            취업률 제고 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-rate-improvements-page-size-select"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="취업률 제고 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 취업률 제고 실적이 없습니다"
            message="검색조건을 변경하거나 상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">인증상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    className={
                      selected?.achievementId === row.achievementId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    data-testid="employment-rate-improvements-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.certificationStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="employment-rate-improvements-detail-button"
                        onClick={() => void selectRow(row)}
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
        <p className="mt-3 text-xs text-muted">
          총 {total}건 / {page + 1}페이지
        </p>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-improvements-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          취업률 제고 실적 상세
        </h2>
        <p className="mt-2 text-sm text-muted">
          관리항목과 업적발생일은 필수입니다. 평가대상 기간 밖 발생일은 경고와
          함께 저장됩니다.
        </p>
        {confirmedSelected ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="employment-rate-improvements-lock-message"
          >
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
          >
            <input
              data-testid="employment-rate-improvements-management-item-input"
              disabled={confirmedSelected}
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </Field>
          <Field
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
          >
            <input
              data-testid="employment-rate-improvements-achievement-date-input"
              disabled={confirmedSelected}
              type="date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
          </Field>
          <Field label="특강 시작일">
            <input
              data-testid="employment-rate-improvements-lecture-start-input"
              disabled={confirmedSelected}
              type="date"
              value={form.specialLectureStartDate}
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value,
                })
              }
            />
          </Field>
          <Field label="특강 종료일" error={fieldErrors.specialLectureEndDate}>
            <input
              data-testid="employment-rate-improvements-lecture-end-input"
              disabled={confirmedSelected}
              type="date"
              value={form.specialLectureEndDate}
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
            />
          </Field>
          <Field label="모의평가 출제기간">
            <input
              data-testid="employment-rate-improvements-mock-exam-input"
              disabled={confirmedSelected}
              value={form.mockExamQuestionPeriod}
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
            />
          </Field>
          <Field label="첨부 참조 (쉼표로 구분)">
            <input
              data-testid="employment-rate-improvements-attachment-input"
              disabled={confirmedSelected}
              value={form.attachmentIds}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
            />
          </Field>
          <Field label="인증상태">
            <input disabled value={selected?.certificationStatus ?? "작성중"} />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
              "text-sm font-semibold text-white disabled:opacity-60"
            }
            data-testid="employment-rate-improvements-save-button"
            disabled={saving || confirmedSelected}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}

function listPath(filters: Filters, page: number, pageSize: number) {
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  if (filters.managementNo.trim())
    query.set("managementNo", filters.managementNo.trim());
  if (filters.teacherName.trim())
    query.set("teacherName", filters.teacherName.trim());
  if (filters.managementItemCode.trim()) {
    query.set("managementItemCode", filters.managementItemCode.trim());
  }
  if (filters.certificationStatus.trim()) {
    query.set("certificationStatus", filters.certificationStatus.trim());
  }
  return `/api/business/employment-rate-improvements?${query.toString()}` as `/api/${string}`;
}

function attachmentRequestField(attachmentIdsText: string, isUpdate: boolean) {
  const attachmentIds = attachmentIdsText
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
  if (isUpdate && attachmentIds.length === 0) {
    return {};
  }
  return { attachmentIds };
}

function Field({
  label,
  required = false,
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span
        className={
          "mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border " +
          "[&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm"
        }
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
