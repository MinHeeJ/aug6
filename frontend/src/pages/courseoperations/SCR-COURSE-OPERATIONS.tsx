import { useEffect, useState } from "react";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type CourseOperation = {
  achievementId: number;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds: string[];
};
type Page = { achievements: CourseOperation[]; totalElements: number };
type Saved = {
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  evaluationYear: string;
};
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  evaluationYear: "",
};
const inputStyle = "mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm";
const buttonStyle =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";

export function CourseOperationsPage({ user }: { user: CurrentUser | null }) {
  const admitted =
    user?.roles.some((role) => ["R01", "R02", "R04", "R09"].includes(role)) ??
    false;
  const writer =
    user?.roles.some((role) => ["R01", "R09"].includes(role)) ?? false;
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [appliedFilter, setAppliedFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const owned =
    !selected ||
    selected.teacherUserId === user?.userId ||
    user?.roles.includes("R09");
  const editableStatus =
    !selected ||
    ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const readOnly =
    !writer || !owned || !editableStatus || detailLoading || saving;

  const failed = (caught: unknown) => {
    setError(
      caught instanceof Error ? caught.message : "실적을 처리하지 못했습니다.",
    );
    if (caught instanceof ApiClientError) {
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    }
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    const query = new URLSearchParams({
      page: String(page),
      pageSize: String(pageSize),
    });
    if (appliedFilter.trim())
      query.set("managementItemCode", appliedFilter.trim());
    try {
      const response = await apiRequest<Page>(
        `/api/business/course-operations?${query}`,
      );
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
      setDenied(false);
    } catch (caught) {
      setRows([]);
      setTotal(0);
      setDenied(caught instanceof ApiClientError && caught.status === 403);
      failed(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (admitted) void load();
  }, [page, pageSize, appliedFilter, admitted]);

  const select = async (row: CourseOperation) => {
    setDetailLoading(true);
    setSelected(null);
    setForm(emptyForm);
    setSuccess(null);
    setError(null);
    setFields({});
    try {
      const response = await apiRequest<CourseOperation>(
        `/api/business/course-operations/${row.achievementId}`,
      );
      if (response.data) {
        setSelected(response.data);
        setForm(response.data);
      }
    } catch (caught) {
      failed(caught);
    } finally {
      setDetailLoading(false);
    }
  };

  const save = async () => {
    if (readOnly) return;
    const errors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      errors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.achievementDate)
      errors.achievementDate = "업적발생일을 입력하세요.";
    if (!form.performanceDetails.trim())
      errors.performanceDetails = "실적내역을 입력하세요.";
    if (form.evaluationYear && !/^\d{4}$/.test(form.evaluationYear))
      errors.evaluationYear = "YYYY 형식으로 입력하세요.";
    setFields(errors);
    if (Object.keys(errors).length) return;
    if (!window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const path: `/api/${string}` = selected
        ? `/api/business/course-operations/${selected.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<Saved>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          performanceDetails: form.performanceDetails.trim(),
          attachmentIds: selected?.attachmentIds ?? [],
          ...(selected || !form.evaluationYear
            ? {}
            : { evaluationYear: form.evaluationYear }),
        }),
      });
      if (response.data) {
        setSelected(response.data.achievement);
        setForm(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "발생일 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      failed(caught);
    } finally {
      setSaving(false);
    }
  };

  if (!admitted || denied) {
    return (
      <section
        data-screen-id="SCR-COURSE-OPERATIONS"
        data-testid="course-operations-page"
      >
        <PermissionState message="강좌 개설·운영 실적 조회 권한이 없습니다." />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OPERATIONS"
      data-testid="course-operations-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
      </div>
      {error ? <ErrorState message={error} /> : null}
      {success ? <SuccessState message={success} /> : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-search-panel"
      >
        <div className="flex flex-wrap items-end gap-3">
          <label>
            관리항목 검색
            <input
              className={inputStyle}
              data-testid="course-filter-input"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
          <button
            className={buttonStyle}
            data-testid="course-search-button"
            onClick={() => {
              setPage(0);
              setAppliedFilter(filter);
              if (filter === appliedFilter) void load();
            }}
          >
            조회
          </button>
          <label>
            표시 건수
            <select
              className={inputStyle}
              data-testid="course-page-size"
              value={pageSize}
              onChange={(event) => {
                setPage(0);
                setPageSize(Number(event.target.value));
              }}
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-list-panel"
      >
        {loading ? (
          <LoadingState />
        ) : rows.length === 0 ? (
          <EmptyState />
        ) : (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>교원</th>
                  <th>평가연도</th>
                  <th>관리항목</th>
                  <th>발생일</th>
                  <th>실적내역</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`course-row-${row.achievementId}`}
                  >
                    <td>{row.teacherName}</td>
                    <td>{row.evaluationYear}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td className="max-w-xs break-words">
                      {row.performanceDetails}
                    </td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        className="text-primary"
                        data-testid={`course-detail-${row.achievementId}`}
                        disabled={saving || detailLoading}
                        onClick={() => void select(row)}
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="mt-4 flex items-center gap-3">
          <span>
            총 {total}건 / {page + 1}페이지
          </span>
          <button
            data-testid="course-previous-button"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <button
            data-testid="course-next-button"
            disabled={(page + 1) * pageSize >= total || loading}
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-detail-panel"
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">
            {selected ? "실적 상세" : "새 실적 등록"}
          </h2>
          {writer ? (
            <button
              data-testid="course-new-button"
              disabled={saving || detailLoading}
              onClick={() => {
                setSelected(null);
                setForm(emptyForm);
                setFields({});
                setSuccess(null);
              }}
            >
              신규
            </button>
          ) : null}
        </div>
        {detailLoading ? <LoadingState title="상세 불러오는 중" /> : null}
        {!editableStatus ? (
          <p data-testid="course-lock-message">
            제출·확인·인증·평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        {!owned ? <p>타인의 실적은 읽기 전용입니다.</p> : null}
        {!writer ? <p>조회 전용 권한입니다.</p> : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          {(
            [
              "managementItemCode",
              "evaluationYear",
              "achievementDate",
              "performanceDetails",
            ] as const
          ).map((key) => (
            <label
              key={key}
              className={key === "performanceDetails" ? "md:col-span-2" : ""}
            >
              {
                {
                  managementItemCode: "관리항목 *",
                  evaluationYear: "평가연도",
                  achievementDate: "업적발생일 *",
                  performanceDetails: "실적내역 *",
                }[key]
              }
              {key === "performanceDetails" ? (
                <textarea
                  className={inputStyle}
                  data-testid="course-performance-details"
                  value={form[key]}
                  disabled={readOnly}
                  rows={5}
                  onChange={(event) =>
                    setForm({ ...form, [key]: event.target.value })
                  }
                />
              ) : (
                <input
                  className={inputStyle}
                  data-testid={`course-${key}`}
                  type={key === "achievementDate" ? "date" : "text"}
                  value={form[key]}
                  disabled={
                    readOnly || (key === "evaluationYear" && selected !== null)
                  }
                  onChange={(event) =>
                    setForm({ ...form, [key]: event.target.value })
                  }
                />
              )}
              {fields[key] ? (
                <span className="text-sm text-error" role="alert">
                  {fields[key]}
                </span>
              ) : null}
            </label>
          ))}
        </div>
        <p className="mt-3 text-sm text-muted">
          평가연도 미입력 시 서버의 활성 입력기간을 사용합니다. 업적발생일과
          평가연도는 독립 관리합니다.
        </p>
        <p className="mt-3 text-sm">
          첨부 참조: {selected?.attachmentIds.join(", ") || "없음"}
        </p>
        {writer ? (
          <button
            className={`${buttonStyle} mt-4`}
            data-testid="course-save-button"
            disabled={readOnly}
            onClick={() => void save()}
          >
            {saving ? "저장 중" : "저장"}
          </button>
        ) : null}
      </section>
    </section>
  );
}
