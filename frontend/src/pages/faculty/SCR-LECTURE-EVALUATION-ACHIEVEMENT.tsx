import { RefreshCw, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import {
  educationAchievementApi,
  type EducationAchievement,
} from "../../api/educationAchievementApi";
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

/** Renders the lecture-evaluation list and common education-achievement registration workflow. */
export function LectureEvaluationAchievementPage() {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [form, setForm] = useState<FormState>(initialForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await educationAchievementApi.list({
        achievementType: "LECTURE_EVALUATION",
        page,
        size,
      });
      setItems(response.data?.items ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const save = async () => {
    const nextErrors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      nextErrors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.occurrenceDate)
      nextErrors.occurrenceDate = "발생일을 입력하세요.";
    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);
      return;
    }
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await educationAchievementApi.save({
        achievementType: "LECTURE_EVALUATION",
        managementItemCode: form.managementItemCode.trim(),
        occurrenceDate: form.occurrenceDate,
      });
      setSuccess("저장되었습니다. 목록을 재조회했습니다.");
      setForm(initialForm);
      setPage(0);
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
        : "강의평가 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT"
        data-testid="lecture-evaluation-page"
      >
        <PermissionState
          title="강의평가 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 메뉴 접근 권한이 필요합니다."
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
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의평가 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강의평가 실적을 등록하고 현재 인증상태를 조회합니다.
            </p>
          </div>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => void load()}
            data-testid="lecture-evaluation-refresh-button"
          >
            <RefreshCw size={16} /> 재조회
          </button>
        </div>
      </div>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="강의평가 실적 처리 오류" message={error} />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">강의평가 실적 등록</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <label className="text-sm font-semibold text-dark">
            관리항목<span className="ml-1 text-error">*</span>
            <input
              data-testid="lecture-evaluation-management-item-input"
              className="form-input mt-2 w-full"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            발생일<span className="ml-1 text-error">*</span>
            <input
              data-testid="lecture-evaluation-occurrence-date-input"
              type="date"
              className="form-input mt-2 w-full"
              value={form.occurrenceDate}
              onChange={(event) =>
                setForm({ ...form, occurrenceDate: event.target.value })
              }
            />
          </label>
          <button
            type="button"
            disabled={saving}
            className="btn-primary mt-7"
            onClick={() => void save()}
            data-testid="lecture-evaluation-save-button"
          >
            <Save size={16} />
            {saving ? "저장 중" : "저장"}
          </button>
        </div>
        {Object.values(fieldErrors).map((message) => (
          <p key={message} className="mt-2 text-sm text-error">
            {message}
          </p>
        ))}
      </section>
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강의평가 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수{" "}
            <select
              data-testid="lecture-evaluation-size-select"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value) as 20 | 50 | 100);
              }}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState
            title="강의평가 실적 조회 중"
            message="목록을 불러오고 있습니다."
          />
        ) : items.length === 0 ? (
          <EmptyState
            title="조회된 강의평가 실적이 없습니다"
            message="필수 항목을 입력하여 새 실적을 등록하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리항목</th>
                  <th>발생일</th>
                  <th>인증상태</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.achievementId}
                    data-testid="lecture-evaluation-row"
                  >
                    <td>{item.managementItemCode}</td>
                    <td>{item.occurrenceDate}</td>
                    <td>{statusLabel(item.certificationStatus)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <div className="flex gap-2">
            <button
              type="button"
              className="btn-secondary"
              disabled={page === 0}
              onClick={() => setPage(Math.max(0, page - 1))}
              data-testid="lecture-evaluation-previous-button"
            >
              이전
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage(page + 1)}
              data-testid="lecture-evaluation-next-button"
            >
              다음
            </button>
          </div>
        </div>
      </section>
    </section>
  );
}

function statusLabel(status: string) {
  return (
    {
      DRAFTING: "작성중",
      SUBMITTED: "제출",
      DEPARTMENT_CONFIRMED: "학과장확인",
      DEPARTMENT_REJECTED: "학과장미승인",
      CERTIFIED: "인증",
      CERTIFICATION_RETURNED: "인증반려",
      EVALUATION_CONFIRMED: "평가확정",
    }[status] ?? status
  );
}
