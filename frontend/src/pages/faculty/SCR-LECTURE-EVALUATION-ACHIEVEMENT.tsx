import { Plus, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  educationAchievementApi,
  type ApiErrorField,
  type EducationAchievement,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  managementItemCode: string;
  occurrenceDate: string;
};

const initialForm: FormState = { managementItemCode: "", occurrenceDate: "" };

/** Provides the Phase 2 lecture-evaluation list, entry validation, confirmation, and post-save refresh flow. */
export function LectureEvaluationAchievementPage() {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [form, setForm] = useState<FormState>(initialForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [creating, setCreating] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await educationAchievementApi.listEducationAchievements({
        achievementType: "LECTURE_EVALUATION",
        page,
        size: pageSize,
      });
      setItems(response.data?.items ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const save = async () => {
    const errors = validateForm(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;

    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await educationAchievementApi.saveEducationAchievement({
        achievementType: "LECTURE_EVALUATION",
        managementItemCode: form.managementItemCode.trim(),
        occurrenceDate: form.occurrenceDate,
      });
      setSuccessMessage("강의평가 실적을 저장하고 목록을 재조회했습니다.");
      setCreating(false);
      setForm(initialForm);
      setPage(0);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
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
        data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT"
        data-testid="lecture-evaluation-permission-state"
      >
        <PermissionState
          title="강의평가 실적 조회 권한이 없습니다."
          message={error ?? "이 기능에 접근할 수 있는 권한이 없습니다."}
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT"
      data-testid="lecture-evaluation-page"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의평가 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강의평가 실적을 조회하고 등록한 뒤 저장 결과를 목록에서 다시
              확인합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void load()}
              data-testid="lecture-evaluation-search-button"
            >
              <Search size={16} /> 조회
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => {
                setCreating(true);
                setFieldErrors({});
                setSuccessMessage(null);
              }}
              data-testid="lecture-evaluation-create-button"
            >
              <Plus size={16} /> 등록
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void load()}
              data-testid="lecture-evaluation-refresh-button"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        </div>
      </header>

      {successMessage ? (
        <SuccessState title="저장 완료" message={successMessage} />
      ) : null}
      {error ? (
        <ErrorState title="강의평가 실적 처리 오류" message={error} />
      ) : null}

      <section className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-8">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-dark">
              강의평가 실적 목록
            </h2>
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as PageSize);
                  setPage(0);
                }}
                data-testid="lecture-evaluation-size-select"
              >
                <option value={20}>20건</option>
                <option value={50}>50건</option>
                <option value={100}>100건</option>
              </select>
            </label>
          </div>
          {loading ? (
            <LoadingState
              title="조회 중"
              message="강의평가 실적 목록을 불러오고 있습니다."
            />
          ) : items.length === 0 ? (
            <EmptyState
              title="조회된 강의평가 실적이 없습니다"
              message="등록하거나 검색조건을 확인한 뒤 다시 조회하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">관리항목</th>
                    <th className="px-3 py-2">발생일</th>
                    <th className="px-3 py-2">상태</th>
                    <th className="px-3 py-2">평가연도</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {items.map((item) => (
                    <tr
                      key={item.achievementId}
                      data-testid="lecture-evaluation-achievement-row"
                      className="hover:bg-lightsecondary"
                    >
                      <td className="px-3 py-2 font-semibold text-link">
                        {item.managementItemCode ?? item.achievementTitle}
                      </td>
                      <td className="px-3 py-2">{item.occurrenceDate}</td>
                      <td className="px-3 py-2">
                        {statusLabel(item.achievementStatus)}
                      </td>
                      <td className="px-3 py-2">{item.evaluationYear}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="mt-4 text-sm text-muted">총 {totalElements}건</p>
        </div>

        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4">
          <h2 className="text-lg font-semibold text-dark">상세 입력</h2>
          {creating ? (
            <>
              <label className="mt-4 block text-sm font-semibold text-dark">
                관리항목 코드<span className="ms-1 text-error">*</span>
                <input
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
                  value={form.managementItemCode}
                  onChange={(event) =>
                    setForm({ ...form, managementItemCode: event.target.value })
                  }
                  data-testid="lecture-evaluation-management-item-input"
                />
              </label>
              {fieldErrors.managementItemCode ? (
                <p
                  className="mt-1 text-xs text-error"
                  data-testid="lecture-evaluation-management-item-error"
                >
                  {fieldErrors.managementItemCode}
                </p>
              ) : null}
              <label className="mt-4 block text-sm font-semibold text-dark">
                발생일<span className="ms-1 text-error">*</span>
                <input
                  type="date"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
                  value={form.occurrenceDate}
                  onChange={(event) =>
                    setForm({ ...form, occurrenceDate: event.target.value })
                  }
                  data-testid="lecture-evaluation-occurrence-date-input"
                />
              </label>
              {fieldErrors.occurrenceDate ? (
                <p
                  className="mt-1 text-xs text-error"
                  data-testid="lecture-evaluation-occurrence-date-error"
                >
                  {fieldErrors.occurrenceDate}
                </p>
              ) : null}
              <button
                type="button"
                className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                disabled={saving}
                onClick={() => void save()}
                data-testid="lecture-evaluation-save-button"
              >
                <Save size={16} />
                {saving ? "저장 중" : "저장"}
              </button>
            </>
          ) : (
            <p className="mt-3 text-sm text-muted">
              등록 버튼을 눌러 강의평가 실적을 입력하세요.
            </p>
          )}
        </aside>
      </section>
    </section>
  );
}

function validateForm(form: FormState) {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목 코드를 입력하세요.";
  if (!form.occurrenceDate) errors.occurrenceDate = "발생일을 입력하세요.";
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>(
    (errors, field) => ({ ...errors, [field.field]: field.message }),
    {},
  );
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    DRAFTING: "작성중",
    SUBMITTED: "제출",
    DEPARTMENT_CONFIRMED: "학과장확인",
    CERTIFIED: "인증",
  };
  return labels[status] ?? status;
}
