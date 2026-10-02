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
  achievementContent: string;
  academicYear: number;
  semester: number;
  certificationStatus: string;
};

type SearchResponse = {
  achievements: Achievement[];
};

type SaveResponse = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: string;
};

const blankForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
};

/** Renders the teaching-improvement list, detail, and guarded save flow. */
export function TeachingImprovementAchievementPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(blankForm);
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
        `/api/business/lecture-improvements?page=0&pageSize=${pageSize}`,
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
        `/api/business/lecture-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        achievementContent: detail.achievementContent,
        academicYear: String(detail.academicYear),
        semester: String(detail.semester),
      });
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          achievementContent: form.achievementContent.trim(),
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
        }),
      });
      const saved = response.data?.achievement;
      if (saved) {
        await select(saved);
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
        : "강의개선 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
        data-testid="teaching-improvement-page"
      >
        <PermissionState
          title="강의개선 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
      data-testid="teaching-improvement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의개선 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              학년도와 학기별 강의개선 실적을 조회하고 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="teaching-improvement-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의개선 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="teaching-improvement-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강의개선 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="teaching-improvement-page-size-select"
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
        {loading ? <LoadingState title="강의개선 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 강의개선 실적이 없습니다" />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">학년도</th>
                  <th className="px-3 py-2">학기</th>
                  <th className="px-3 py-2">인증상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="teaching-improvement-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.academicYear}</td>
                    <td className="px-3 py-2">{row.semester}</td>
                    <td className="px-3 py-2">{row.certificationStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="teaching-improvement-detail-button"
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
        data-testid="teaching-improvement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">강의개선 실적 상세</h2>
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
              data-testid="teaching-improvement-management-item-input"
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
              data-testid="teaching-improvement-achievement-date-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="학년도" required error={fieldErrors.academicYear}>
            <input
              data-testid="teaching-improvement-academic-year-input"
              disabled={locked}
              min="2000"
              onChange={(event) =>
                setForm({ ...form, academicYear: event.target.value })
              }
              type="number"
              value={form.academicYear}
            />
          </Field>
          <Field label="학기" required error={fieldErrors.semester}>
            <select
              data-testid="teaching-improvement-semester-select"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, semester: event.target.value })
              }
              value={form.semester}
            >
              <option value="">선택</option>
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </Field>
          <Field
            label="실적내용"
            required
            error={fieldErrors.achievementContent}
          >
            <textarea
              data-testid="teaching-improvement-content-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, achievementContent: event.target.value })
              }
              value={form.achievementContent}
            />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={[
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2",
              "text-sm font-semibold text-white disabled:cursor-not-allowed",
              "disabled:opacity-50",
            ].join(" ")}
            data-testid="teaching-improvement-save-button"
            disabled={locked || saving}
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
    <label className="flex flex-col gap-1 text-sm text-dark">
      <span>
        {label}
        {required ? " *" : ""}
      </span>
      {children}
      {error ? <span className="text-xs text-error">{error}</span> : null}
    </label>
  );
}
