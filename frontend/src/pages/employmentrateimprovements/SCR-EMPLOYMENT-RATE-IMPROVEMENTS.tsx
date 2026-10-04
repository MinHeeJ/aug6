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
  teacherLoginId: string;
  organizationCode: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  attachmentIds: string[];
};

type SearchResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: Achievement;
  occurredDateWarning: boolean;
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

/** 취업률 제고 실적의 목록, 상세 조회, 생성 및 수정 흐름을 제공한다. */
export function EmploymentRateImprovementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
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
  const confirmedSelected =
    selected?.achievementStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setPermissionDenied(false);
      setError(null);
      const response = await apiRequest<SearchResponse>(
        listPath(page, pageSize),
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
        `/api/business/employment-rate-improvements/${row.achievementId}` as `/api/${string}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
      setFieldErrors({});
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    const localErrors: Record<string, string> = {};
    if (!form.managementItemCode.trim()) {
      localErrors.managementItemCode = "관리항목은 필수입니다.";
    }
    if (!form.achievementDate) {
      localErrors.achievementDate = "업적발생일은 필수입니다.";
    }
    if (
      form.specialLectureStartDate &&
      form.specialLectureEndDate &&
      form.specialLectureEndDate < form.specialLectureStartDate
    ) {
      localErrors.specialLectureEndDate =
        "특강 종료일은 시작일보다 빠를 수 없습니다.";
    }
    if (Object.keys(localErrors).length > 0) {
      setFieldErrors(localErrors);
      setError("필수 입력값을 확인하세요.");
      return;
    }
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const path = selected
        ? `/api/business/employment-rate-improvements/${selected.achievementId}`
        : "/api/business/employment-rate-improvements";
      const response = await apiRequest<SaveResponse>(
        path as `/api/${string}`,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode.trim(),
            achievementDate: form.achievementDate,
            specialLectureStartDate: form.specialLectureStartDate || undefined,
            specialLectureEndDate: form.specialLectureEndDate || undefined,
            mockExamQuestionPeriod:
              form.mockExamQuestionPeriod.trim() || undefined,
            attachmentIds: form.attachmentIds
              .split(",")
              .map((value) => value.trim())
              .filter(Boolean),
          }),
        },
      );
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm(toForm(saved));
      }
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const resetForm = () => {
    setSelected(null);
    setForm(initialForm);
    setFieldErrors({});
    setSuccess(null);
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
      <div className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 제고 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특강기간과 모의평가 출제기간을 확인하고 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2
              text-sm font-semibold text-white"
            data-testid="employment-rate-improvements-refresh-button"
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

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-improvements-search-panel"
      >
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              취업률 제고 실적 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              소속과 역할 범위에 맞는 실적만 조회됩니다.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                data-testid="employment-rate-improvements-page-size-select"
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as 20 | 50 | 100);
                  setPage(0);
                }}
                value={pageSize}
              >
                {[20, 50, 100].map((value) => (
                  <option key={value} value={value}>
                    {value}건
                  </option>
                ))}
              </select>
            </label>
            <button
              className="inline-flex items-center gap-2 rounded-md border border-primary px-3 py-2
                text-sm font-semibold text-primary"
              data-testid="employment-rate-improvements-search-button"
              onClick={() => void load()}
              type="button"
            >
              <Search size={16} /> 조회
            </button>
          </div>
        </div>
        {loading ? <LoadingState title="취업률 제고 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 취업률 제고 실적이 없습니다"
            message="새 실적을 입력하여 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">실적번호</th>
                  <th className="px-3 py-2">교원</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">특강기간</th>
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
                    <td className="px-3 py-2">{row.achievementId}</td>
                    <td className="px-3 py-2">{row.teacherLoginId}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">
                      {row.specialLectureStartDate ?? "-"} ~{" "}
                      {row.specialLectureEndDate ?? "-"}
                    </td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
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
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              취업률 제고 실적 상세
            </h2>
            <p className="mt-1 text-sm text-muted">
              관리항목과 업적발생일은 필수입니다. 평가대상 기간 밖 날짜는 경고
              후 저장됩니다.
            </p>
          </div>
          <button
            className="rounded-md border border-ld px-3 py-2 text-sm font-semibold text-link"
            data-testid="employment-rate-improvements-new-button"
            onClick={resetForm}
            type="button"
          >
            신규 입력
          </button>
        </div>
        {confirmedSelected ? (
          <p
            className="mt-3 text-sm text-error"
            data-testid="employment-rate-improvements-confirmed-lock-message"
          >
            평가확정 실적은 수정하거나 첨부를 변경할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            error={fieldErrors.managementItemCode}
            label="관리항목"
            required
          >
            <input
              data-testid="employment-rate-improvements-management-item-input"
              disabled={confirmedSelected}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Field>
          <Field
            error={fieldErrors.achievementDate}
            label="업적발생일"
            required
          >
            <input
              data-testid="employment-rate-improvements-achievement-date-input"
              disabled={confirmedSelected}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="특강 시작일">
            <input
              data-testid="employment-rate-improvements-lecture-start-input"
              disabled={confirmedSelected}
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value,
                })
              }
              type="date"
              value={form.specialLectureStartDate}
            />
          </Field>
          <Field error={fieldErrors.specialLectureEndDate} label="특강 종료일">
            <input
              data-testid="employment-rate-improvements-lecture-end-input"
              disabled={confirmedSelected}
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
              type="date"
              value={form.specialLectureEndDate}
            />
          </Field>
          <Field label="모의평가 출제기간">
            <input
              data-testid="employment-rate-improvements-mock-exam-period-input"
              disabled={confirmedSelected}
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
              value={form.mockExamQuestionPeriod}
            />
          </Field>
          <Field label="첨부 식별자 (쉼표 구분)">
            <input
              data-testid="employment-rate-improvements-attachment-ids-input"
              disabled={confirmedSelected}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
              value={form.attachmentIds}
            />
          </Field>
          <Field label="인증상태">
            <input disabled value={selected?.achievementStatus ?? "DRAFT"} />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm
              font-semibold text-white disabled:opacity-60"
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

function listPath(page: number, pageSize: number) {
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  return `/api/business/employment-rate-improvements?${query.toString()}` as `/api/${string}`;
}

function toForm(achievement: Achievement): Form {
  return {
    managementItemCode: achievement.managementItemCode,
    achievementDate: achievement.achievementDate,
    specialLectureStartDate: achievement.specialLectureStartDate ?? "",
    specialLectureEndDate: achievement.specialLectureEndDate ?? "",
    mockExamQuestionPeriod: achievement.mockExamQuestionPeriod ?? "",
    attachmentIds: achievement.attachmentIds.join(", "),
  };
}

function Field({
  children,
  error,
  label,
  required = false,
}: {
  children: ReactNode;
  error?: string;
  label: string;
  required?: boolean;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span
        className="mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border
          [&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm"
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
