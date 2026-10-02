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

type Achievement = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  attachmentRef?: string | null;
  certificationStatus: string;
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

type Form = Omit<
  Achievement,
  "achievementId" | "managementNo" | "teacherName" | "certificationStatus"
>;

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentRef: "",
};

/** Renders the T004 list/detail form for the 취업률 제고 source rows. */
export function EmploymentRateImprovementAchievementPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const locked = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiRequest<SearchResponse>(
        `/api/business/employment-rate-improvements?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
      setPermissionDenied(false);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const select = async (row: Achievement) => {
    try {
      const response = await apiRequest<Achievement>(
        `/api/business/employment-rate-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        specialLectureStartDate: detail.specialLectureStartDate ?? "",
        specialLectureEndDate: detail.specialLectureEndDate ?? "",
        mockExamQuestionPeriod: detail.mockExamQuestionPeriod ?? "",
        attachmentRef: detail.attachmentRef ?? "",
      });
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
      const path = selected
        ? `/api/business/employment-rate-improvements/${selected.achievementId}`
        : "/api/business/employment-rate-improvements";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          specialLectureStartDate: form.specialLectureStartDate || undefined,
          specialLectureEndDate: form.specialLectureEndDate || undefined,
          mockExamQuestionPeriod:
            form.mockExamQuestionPeriod.trim() || undefined,
          attachmentIds: form.attachmentRef.trim()
            ? [form.attachmentRef.trim()]
            : undefined,
        }),
      });
      const saved = response.data?.achievement;
      if (saved) await select(saved);
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
        data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
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
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
      data-testid="employment-rate-improvement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 제고 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특강 및 모의평가 출제 실적을 조회하고 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="employment-rate-improvement-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-improvement-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            취업률 제고 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-rate-improvement-page-size-select"
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
              value={pageSize}
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
          <EmptyState title="조회된 취업률 제고 실적이 없습니다" />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">인증상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="employment-rate-improvement-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.certificationStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="employment-rate-improvement-detail-button"
                        onClick={() => void select(row)}
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
        data-testid="employment-rate-improvement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          취업률 제고 실적 상세
        </h2>
        {locked ? (
          <p className="mt-2 text-sm text-error">
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
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Field>
          <Field
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
          >
            <input
              data-testid="employment-rate-improvement-achievement-date-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="특강 시작일">
            <input
              data-testid="employment-rate-improvement-special-lecture-start-input"
              disabled={locked}
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value,
                })
              }
              type="date"
              value={form.specialLectureStartDate ?? ""}
            />
          </Field>
          <Field label="특강 종료일" error={fieldErrors.specialLectureEndDate}>
            <input
              data-testid="employment-rate-improvement-special-lecture-end-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
              type="date"
              value={form.specialLectureEndDate ?? ""}
            />
          </Field>
          <Field label="모의평가 출제기간">
            <input
              data-testid="employment-rate-improvement-mock-exam-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
              value={form.mockExamQuestionPeriod ?? ""}
            />
          </Field>
          <Field label="첨부 참조">
            <input
              data-testid="employment-rate-improvement-attachment-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, attachmentRef: event.target.value })
              }
              value={form.attachmentRef ?? ""}
            />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={[
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2",
              "text-sm font-semibold text-white disabled:opacity-60",
            ].join(" ")}
            data-testid="employment-rate-improvement-save-button"
            disabled={saving || locked}
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
        className={[
          "mt-2 block [&_input]:w-full [&_input]:rounded-md",
          "[&_input]:border [&_input]:border-ld [&_input]:px-3 [&_input]:py-2",
        ].join(" ")}
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
