import { Save } from "lucide-react";
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
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  achievementStatus: string;
};

type SearchResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type Form = Omit<Achievement, "achievementId" | "achievementStatus"> & {
  achievementId?: number;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
};

/** 취업률 제고 실적의 목록·상세·저장 흐름을 제공하는 BASIC-83 화면이다. */
export function EmploymentRateImprovementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
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

  const select = async (achievementId: number) => {
    try {
      const response = await apiRequest<Achievement>(
        `/api/business/employment-rate-improvements/${achievementId}`,
      );
      const detail = response.data;
      if (!detail) return;
      setForm({
        achievementId: detail.achievementId,
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        specialLectureStartDate: detail.specialLectureStartDate ?? "",
        specialLectureEndDate: detail.specialLectureEndDate ?? "",
        mockExamQuestionPeriod: detail.mockExamQuestionPeriod ?? "",
      });
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setFieldErrors({});
      setError(null);
      const path = form.achievementId
        ? `/api/business/employment-rate-improvements/${form.achievementId}`
        : "/api/business/employment-rate-improvements";
      await apiRequest(path as `/api/${string}`, {
        method: form.achievementId ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          specialLectureStartDate: form.specialLectureStartDate || undefined,
          specialLectureEndDate: form.specialLectureEndDate || undefined,
          mockExamQuestionPeriod:
            form.mockExamQuestionPeriod?.trim() || undefined,
        }),
      });
      setSuccess("저장되었습니다.");
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
          message="R01, R02 또는 R04 권한이 필요합니다."
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
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          특강 기간과 모의고사 문항 출제 실적을 관리합니다.
        </p>
      </div>
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-improvements-list"
      >
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-rate-improvements-page-size"
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
          <EmptyState
            title="조회된 실적이 없습니다"
            message="아래 상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <table className="min-w-full text-sm">
            <thead>
              <tr>
                <th>관리항목</th>
                <th>발생일</th>
                <th>특강 기간</th>
                <th>상태</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  data-testid="employment-rate-improvements-row"
                  key={row.achievementId}
                >
                  <td>{row.managementItemCode}</td>
                  <td>{row.achievementDate}</td>
                  <td>
                    {row.specialLectureStartDate ?? "-"} ~{" "}
                    {row.specialLectureEndDate ?? "-"}
                  </td>
                  <td>{row.achievementStatus}</td>
                  <td>
                    <button
                      className="text-link"
                      data-testid="employment-rate-improvements-detail-button"
                      onClick={() => void select(row.achievementId)}
                      type="button"
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-improvements-form"
      >
        <h2 className="text-lg font-semibold text-dark">
          취업률 제고 실적 상세
        </h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="관리항목" error={fieldErrors.managementItemCode}>
            <input
              data-testid="employment-rate-improvements-management-item"
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Field>
          <Field label="업적발생일" error={fieldErrors.achievementDate}>
            <input
              data-testid="employment-rate-improvements-achievement-date"
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="특강 시작일">
            <input
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
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
              type="date"
              value={form.specialLectureEndDate ?? ""}
            />
          </Field>
          <Field label="모의고사 문항 출제 기간">
            <input
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
              value={form.mockExamQuestionPeriod ?? ""}
            />
          </Field>
        </div>
        <button
          className="mt-5 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          data-testid="employment-rate-improvements-save-button"
          disabled={saving}
          onClick={() => void save()}
          type="button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <span className="mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border [&_input]:border-ld [&_input]:px-3 [&_input]:py-2">
        {children}
      </span>
      {error ? <span className="text-xs text-error">{error}</span> : null}
    </label>
  );
}
