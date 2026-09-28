import { Plus, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  educationAchievementApi,
  type ApiErrorField,
  type DegreeCompletionStudentDetailPayload,
  type EducationAchievement,
  type EducationAchievementType,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Props = {
  achievementType: EducationAchievementType;
  screenId: string;
  title: string;
  testPrefix: string;
  degreeCompletion?: boolean;
};
type FormState = {
  managementItemCode: string;
  occurrenceDate: string;
  details: DegreeCompletionStudentDetailPayload[];
};
const initialForm: FormState = {
  managementItemCode: "",
  occurrenceDate: "",
  details: [],
};

/** Reuses the existing education-achievement API for lecture and degree-completion input screens. */
export function EducationAchievementPage({
  achievementType,
  screenId,
  title,
  testPrefix,
  degreeCompletion = false,
}: Props) {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [form, setForm] = useState<FormState>(initialForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [creating, setCreating] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await educationAchievementApi.listEducationAchievements({
        achievementType,
        page,
        size: pageSize,
      });
      setItems(response.data?.items ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [achievementType, page, pageSize]);
  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        (caught.apiError?.fields ?? []).reduce<Record<string, string>>(
          (all, item: ApiErrorField) => ({
            ...all,
            [item.field]: item.message,
          }),
          {},
        ),
      );
    } else
      setError(
        caught instanceof Error
          ? caught.message
          : `${title}을 처리하지 못했습니다.`,
      );
  };
  const save = async () => {
    const errors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      errors.managementItemCode = "관리항목 코드를 입력하세요.";
    if (!form.occurrenceDate) errors.occurrenceDate = "발생일을 입력하세요.";
    if (
      degreeCompletion &&
      form.details.some(
        (detail) =>
          !detail.studentName.trim() ||
          !detail.thesisTitle.trim() ||
          !detail.degreeAwardedOn,
      )
    )
      errors.degreeCompletionStudentDetails =
        "학생명, 논문 제목, 학위수여일을 모두 입력하세요.";
    if (degreeCompletion && form.details.length === 0)
      errors.degreeCompletionStudentDetails =
        "석사 또는 박사 배출 학생 정보를 추가하세요.";
    setFieldErrors(errors);
    if (
      Object.keys(errors).length > 0 ||
      !window.confirm(`${title}을 저장하시겠습니까?`)
    )
      return;
    try {
      setSaving(true);
      setError(null);
      await educationAchievementApi.saveEducationAchievement({
        achievementType,
        managementItemCode: form.managementItemCode.trim(),
        occurrenceDate: form.occurrenceDate,
        ...(degreeCompletion
          ? { degreeCompletionStudentDetails: form.details }
          : {}),
      });
      setSuccess(`${title}을 저장하고 목록을 재조회했습니다.`);
      setForm(initialForm);
      setCreating(false);
      setPage(0);
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };
  const addDegree = () =>
    setForm({
      ...form,
      details: [
        ...form.details,
        {
          degreeType: "MASTER",
          studentName: "",
          thesisTitle: "",
          degreeAwardedOn: "",
        },
      ],
    });
  const beginCreate = () => {
    setCreating(true);
    setSuccess(null);
    setForm(
      degreeCompletion
        ? {
            ...initialForm,
            details: [
              {
                degreeType: "MASTER",
                studentName: "",
                thesisTitle: "",
                degreeAwardedOn: "",
              },
            ],
          }
        : initialForm,
    );
  };
  const confirmedLocked =
    degreeCompletion &&
    items.some((item) => item.evaluationConfirmedYn === "Y");
  const updateDegree = (
    index: number,
    key: keyof DegreeCompletionStudentDetailPayload,
    value: string,
  ) =>
    setForm({
      ...form,
      details: form.details.map((detail, current) =>
        current === index ? { ...detail, [key]: value } : detail,
      ),
    });
  if (permissionDenied)
    return (
      <section
        data-screen-id={screenId}
        data-testid={`${testPrefix}-permission-state`}
      >
        <PermissionState
          title={`${title} 조회 권한이 없습니다.`}
          message={error ?? "이 기능에 접근할 수 있는 권한이 없습니다."}
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id={screenId}
      data-testid={`${testPrefix}-page`}
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void load()}
              data-testid={`${testPrefix}-search-button`}
            >
              <Search size={16} /> 조회
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={beginCreate}
              data-testid={`${testPrefix}-create-button`}
            >
              <Plus size={16} /> 등록
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void load()}
              data-testid={`${testPrefix}-refresh-button`}
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        </div>
      </header>
      {confirmedLocked ? (
        <div
          className="rounded-md bg-lightsecondary p-3 text-sm text-muted"
          data-testid={`${testPrefix}-confirmed-lock-notice`}
        >
          평가확정된 실적은 수정하거나 저장할 수 없습니다.
        </div>
      ) : null}
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? (
        <ErrorState title={`${title} 처리 오류`} message={error} />
      ) : null}
      <section className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-8">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">{title} 목록</h2>
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as PageSize);
                  setPage(0);
                }}
                data-testid={`${testPrefix}-size-select`}
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
              message={`${title} 목록을 불러오고 있습니다.`}
            />
          ) : items.length === 0 ? (
            <EmptyState
              title={`조회된 ${title}이 없습니다`}
              message="등록한 뒤 다시 조회하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">관리항목</th>
                    <th className="px-3 py-2">발생일</th>
                    <th className="px-3 py-2">상태</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {items.map((item) => (
                    <tr
                      key={item.achievementId}
                      data-testid={`${testPrefix}-achievement-row`}
                    >
                      <td className="px-3 py-2">
                        {item.managementItemCode ?? item.achievementTitle}
                      </td>
                      <td className="px-3 py-2">{item.occurrenceDate}</td>
                      <td className="px-3 py-2">{item.achievementStatus}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="mt-4 text-sm text-muted">총 {total}건</p>
        </div>
        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4">
          <h2 className="text-lg font-semibold text-dark">상세 입력</h2>
          {creating ? (
            <>
              <label className="mt-4 block text-sm font-semibold text-dark">
                관리항목 코드<span className="text-error">*</span>
                <input
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.managementItemCode}
                  onChange={(event) =>
                    setForm({ ...form, managementItemCode: event.target.value })
                  }
                  data-testid={`${testPrefix}-management-item-input`}
                />
              </label>
              <label className="mt-4 block text-sm font-semibold text-dark">
                발생일<span className="text-error">*</span>
                <input
                  type="date"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.occurrenceDate}
                  onChange={(event) =>
                    setForm({ ...form, occurrenceDate: event.target.value })
                  }
                  data-testid={`${testPrefix}-occurrence-date-input`}
                />
              </label>
              {degreeCompletion ? (
                <div className="mt-4">
                  <button
                    type="button"
                    className="rounded-md border border-primary px-3 py-2 text-sm text-primary"
                    onClick={addDegree}
                    data-testid={`${testPrefix}-add-student-button`}
                  >
                    학생 추가
                  </button>
                  {form.details.map((detail, index) => (
                    <div
                      key={index}
                      className="mt-3 space-y-2 border-t pt-3"
                      data-testid={`${testPrefix}-student-detail-row`}
                    >
                      <select
                        value={detail.degreeType}
                        onChange={(event) =>
                          updateDegree(index, "degreeType", event.target.value)
                        }
                        data-testid={`${testPrefix}-degree-type-select`}
                      >
                        <option value="MASTER">석사</option>
                        <option value="DOCTOR">박사</option>
                      </select>
                      <input
                        placeholder="학생명"
                        value={detail.studentName}
                        onChange={(event) =>
                          updateDegree(index, "studentName", event.target.value)
                        }
                        data-testid={`${testPrefix}-student-name-input`}
                      />
                      <input
                        placeholder="논문 제목"
                        value={detail.thesisTitle}
                        onChange={(event) =>
                          updateDegree(index, "thesisTitle", event.target.value)
                        }
                        data-testid={`${testPrefix}-thesis-title-input`}
                      />
                      <input
                        type="date"
                        value={detail.degreeAwardedOn}
                        onChange={(event) =>
                          updateDegree(
                            index,
                            "degreeAwardedOn",
                            event.target.value,
                          )
                        }
                        data-testid={`${testPrefix}-awarded-on-input`}
                      />
                    </div>
                  ))}
                </div>
              ) : null}
              {Object.values(fieldErrors).map((message) => (
                <p key={message} className="mt-1 text-xs text-error">
                  {message}
                </p>
              ))}
              <button
                type="button"
                className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                disabled={saving}
                onClick={() => void save()}
                data-testid={`${testPrefix}-save-button`}
              >
                <Save size={16} />
                {saving ? "저장 중" : "저장"}
              </button>
            </>
          ) : confirmedLocked ? (
            <>
              <p className="mt-3 text-sm text-muted">
                평가확정된 실적은 수정할 수 없습니다.
              </p>
              <button
                type="button"
                className="mt-5 inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                disabled
                data-testid={`${testPrefix}-save-button`}
              >
                저장
              </button>
            </>
          ) : (
            <p className="mt-3 text-sm text-muted">
              등록 버튼을 눌러 입력하세요.
            </p>
          )}
        </aside>
      </section>
    </section>
  );
}
