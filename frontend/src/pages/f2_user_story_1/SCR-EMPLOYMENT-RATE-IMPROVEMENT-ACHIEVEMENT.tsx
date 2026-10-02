import { RefreshCw, Save } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type AchievementStatus = "DRAFT" | "EVALUATION_CONFIRMED" | string;

type EmploymentRateImprovement = {
  achievementId: number;
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  attachmentIds?: string[] | null;
  status?: AchievementStatus;
  certificationStatus?: AchievementStatus;
};

type ListResponse = {
  achievements?: EmploymentRateImprovement[];
  items?: EmploymentRateImprovement[];
  page?: number;
  pageSize?: number;
  totalElements?: number;
  total?: number;
};

type DetailResponse = {
  achievement?: EmploymentRateImprovement;
  data?: EmploymentRateImprovement;
};

type SaveResponse = {
  achievement?: EmploymentRateImprovement;
  occurredDateWarning?: boolean;
  warningMessage?: string | null;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
  attachmentIds: string;
};

const initialForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentIds: "",
};

/** 취업률 제고 실적을 조회하고 역할·상태 제약에 맞춰 등록 또는 수정하는 화면이다. */
export function EmploymentRateImprovementAchievementPage() {
  const [rows, setRows] = useState<EmploymentRateImprovement[]>([]);
  const [selected, setSelected] = useState<EmploymentRateImprovement | null>(
    null,
  );
  const [form, setForm] = useState<Form>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const confirmed = statusOf(selected) === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<ListResponse>(listPath(page, pageSize));
      const data = response.data;
      setRows(data?.achievements ?? data?.items ?? []);
      setTotal(data?.totalElements ?? data?.total ?? 0);
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = async (row: EmploymentRateImprovement) => {
    try {
      setError(null);
      setFieldErrors({});
      const response = await apiRequest<DetailResponse>(
        `/api/business/employment-rate-improvements/${row.achievementId}`,
      );
      const detail = response.data?.achievement ?? response.data?.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
    }
  };

  const save = async () => {
    const clientErrors = validate(form);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;

    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload = toPayload(form);
      const path = selected
        ? `/api/business/employment-rate-improvements/${selected.achievementId}`
        : "/api/business/employment-rate-improvements";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm(toForm(saved));
      }
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "평가대상 기간 밖 발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
    } finally {
      setSaving(false);
    }
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
        data-testid="employment-rate-improvement-page"
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
      data-testid="employment-rate-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 제고 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              취업률 제고 활동을 조회하고, 작성 가능한 실적을 등록 또는
              수정합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="employment-rate-improvement-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            취업률 제고 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-rate-improvement-page-size-select"
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
            message="상세 영역에서 새 실적을 등록하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">특강기간</th>
                  <th className="px-3 py-2">상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="employment-rate-improvement-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{formatPeriod(row)}</td>
                    <td className="px-3 py-2">{statusOf(row) || "작성중"}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="employment-rate-improvement-detail-button"
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
        data-testid="employment-rate-improvement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          취업률 제고 실적 상세
        </h2>
        <p className="mt-2 text-sm text-muted">
          관리항목과 업적발생일은 필수입니다. 평가대상 기간 밖 날짜는 서버
          경고를 확인한 뒤 저장됩니다.
        </p>
        {confirmed ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="employment-rate-improvement-confirmed-lock-message"
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
              data-testid="employment-rate-improvement-management-item-input"
              disabled={confirmed}
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
              data-testid="employment-rate-improvement-achievement-date-input"
              disabled={confirmed}
              type="date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
          </Field>
          <Field
            label="특강 시작일"
            error={fieldErrors.specialLectureStartDate}
          >
            <input
              data-testid="employment-rate-improvement-special-lecture-start-input"
              disabled={confirmed}
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
              data-testid="employment-rate-improvement-special-lecture-end-input"
              disabled={confirmed}
              type="date"
              value={form.specialLectureEndDate}
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
            />
          </Field>
          <Field
            label="모의고사 출제 기간"
            error={fieldErrors.mockExamQuestionPeriod}
          >
            <input
              data-testid="employment-rate-improvement-mock-exam-period-input"
              disabled={confirmed}
              value={form.mockExamQuestionPeriod}
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
            />
          </Field>
          <Field
            label="첨부파일 ID (쉼표로 구분)"
            error={fieldErrors.attachmentIds}
          >
            <input
              data-testid="employment-rate-improvement-attachment-ids-input"
              disabled={confirmed}
              value={form.attachmentIds}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
            />
          </Field>
          <Field label="상태">
            <input disabled value={statusOf(selected) || "작성중"} />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
            data-testid="employment-rate-improvement-save-button"
            disabled={saving || confirmed}
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

function listPath(page: number, pageSize: number) {
  return `/api/business/employment-rate-improvements?page=${page}&pageSize=${pageSize}` as `/api/${string}`;
}

function statusOf(row: EmploymentRateImprovement | null) {
  return row?.status ?? row?.certificationStatus ?? "";
}

function formatPeriod(row: EmploymentRateImprovement) {
  if (!row.specialLectureStartDate && !row.specialLectureEndDate) return "-";
  return `${row.specialLectureStartDate ?? "-"} ~ ${row.specialLectureEndDate ?? "-"}`;
}

function toForm(row: EmploymentRateImprovement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    specialLectureStartDate: row.specialLectureStartDate ?? "",
    specialLectureEndDate: row.specialLectureEndDate ?? "",
    mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
    attachmentIds: row.attachmentIds?.join(", ") ?? "",
  };
}

function toPayload(form: Form) {
  const attachmentIds = form.attachmentIds
    .split(",")
    .map((attachmentId) => attachmentId.trim())
    .filter(Boolean);
  return {
    managementItemCode: form.managementItemCode.trim(),
    achievementDate: form.achievementDate,
    specialLectureStartDate: form.specialLectureStartDate || undefined,
    specialLectureEndDate: form.specialLectureEndDate || undefined,
    mockExamQuestionPeriod: form.mockExamQuestionPeriod.trim() || undefined,
    attachmentIds: attachmentIds.length > 0 ? attachmentIds : undefined,
  };
}

function validate(form: Form) {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목은 필수입니다.";
  if (!form.achievementDate)
    errors.achievementDate = "업적발생일은 필수입니다.";
  if (
    form.specialLectureStartDate &&
    form.specialLectureEndDate &&
    form.specialLectureStartDate > form.specialLectureEndDate
  ) {
    errors.specialLectureEndDate = "특강 종료일은 시작일보다 빠를 수 없습니다.";
  }
  return errors;
}

function handleApiError(
  caught: unknown,
  setPermissionDenied: (value: boolean) => void,
  setError: (value: string | null) => void,
  setFieldErrors: (value: Record<string, string>) => void,
) {
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
      <span className="mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border [&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm">
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
