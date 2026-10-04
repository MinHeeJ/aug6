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
  attachmentIds?: string | null;
  achievementStatus: string;
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

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentIds: "",
};

/** Implements the approved BASIC-83 취업률 제고 search/list/detail/save screen. */
export function EmploymentRateImprovementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        `/api/business/employment-rate-improvements?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
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

  const selectRow = (row: Achievement) => {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      specialLectureStartDate: row.specialLectureStartDate ?? "",
      specialLectureEndDate: row.specialLectureEndDate ?? "",
      mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
      attachmentIds: row.attachmentIds ?? "",
    });
    setFieldErrors({});
    setSuccess(null);
  };

  const save = async () => {
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        specialLectureStartDate: form.specialLectureStartDate || undefined,
        specialLectureEndDate: form.specialLectureEndDate || undefined,
        mockExamQuestionPeriod: form.mockExamQuestionPeriod.trim() || undefined,
        attachmentIds: form.attachmentIds.trim()
          ? form.attachmentIds
              .split(",")
              .map((id) => id.trim())
              .filter(Boolean)
          : [],
      };
      const path = selected
        ? `/api/business/employment-rate-improvements/${selected.achievementId}`
        : "/api/business/employment-rate-improvements";
      const response = await apiRequest<SaveResponse>(
        path as `/api/${string}`,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify(payload),
        },
      );
      const saved = response.data?.achievement;
      if (saved) selectRow(saved);
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ?? "기간 경고와 함께 저장되었습니다.")
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
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
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

  const confirmed = selected?.achievementStatus === "EVALUATION_CONFIRMED";
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-rate-improvements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 제고 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특강 기간과 모의고사 출제 기간을 관리합니다.
            </p>
          </div>
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
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-improvements-list"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            취업률 제고 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-rate-improvements-page-size-select"
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
          <EmptyState title="조회된 실적이 없습니다" />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">발생일</th>
                  <th className="px-3 py-2">상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="employment-rate-improvements-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="employment-rate-improvements-detail-button"
                        onClick={() => selectRow(row)}
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
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-improvements-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          취업률 제고 실적 상세
        </h2>
        {confirmed ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="employment-rate-improvements-confirmed-lock-message"
          >
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Input
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
          >
            <input
              data-testid="employment-rate-improvements-management-item-input"
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Input>
          <Input
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
          >
            <input
              data-testid="employment-rate-improvements-achievement-date-input"
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Input>
          <Input label="특강 시작일">
            <input
              data-testid="employment-rate-improvements-special-lecture-start-input"
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value,
                })
              }
              type="date"
              value={form.specialLectureStartDate}
            />
          </Input>
          <Input label="특강 종료일" error={fieldErrors.specialLectureEndDate}>
            <input
              data-testid="employment-rate-improvements-special-lecture-end-input"
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
              type="date"
              value={form.specialLectureEndDate}
            />
          </Input>
          <Input label="모의고사 출제 기간">
            <input
              data-testid="employment-rate-improvements-mock-exam-input"
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
              value={form.mockExamQuestionPeriod}
            />
          </Input>
          <Input label="첨부 식별자">
            <input
              data-testid="employment-rate-improvements-attachment-ids-input"
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
              value={form.attachmentIds}
            />
          </Input>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
              "text-sm font-semibold text-white disabled:opacity-60"
            }
            data-testid="employment-rate-improvements-save-button"
            disabled={saving || confirmed}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} />
            {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}

function Input({
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
    <label className="grid gap-1 text-sm text-dark">
      {label}
      {required ? " *" : ""}
      {children}
      {error ? <span className="text-xs text-error">{error}</span> : null}
    </label>
  );
}
