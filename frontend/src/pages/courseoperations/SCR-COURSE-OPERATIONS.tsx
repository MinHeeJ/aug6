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

type Row = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds: string[];
};
type Form = Pick<
  Row,
  | "managementItemCode"
  | "achievementDate"
  | "performanceDetails"
  | "attachmentIds"
>;
type SearchResult = {
  achievements: Row[];
  page: number;
  pageSize: number;
  totalElements: number;
};
type SaveResult = {
  achievement: Row;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  attachmentIds: [],
};
const editableStatuses = [
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
];
const inputStyle =
  "mt-1 w-full rounded-md border border-ld bg-white px-3 py-2 text-sm disabled:bg-lightgray";
const buttonStyle =
  "rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50";

/** Reuses the existing education screen states and shell; only the approved course payload is editable. */
export function CourseOperationsPage({ user }: { user: CurrentUser | null }) {
  const [rows, setRows] = useState<Row[]>([]);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [filter, setFilter] = useState("");
  const [appliedFilter, setAppliedFilter] = useState("");
  const [tab, setTab] = useState<"details" | "attachments">("details");
  const canRead = !!user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const canWrite = !!user?.roles.some((role) => ["R01", "R09"].includes(role));
  const owns =
    !selected ||
    selected.teacherUserId === user?.userId ||
    !!user?.roles.includes("R09");
  const locked =
    !!selected && !editableStatuses.includes(selected.achievementStatus);
  const readOnly = !canWrite || !owns || locked;

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      setError(caught.message);
    } else {
      setError("강좌 개설·운영 실적을 처리하지 못했습니다.");
    }
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (appliedFilter) query.set("managementItemCode", appliedFilter);
      const response = await apiRequest<SearchResult>(
        `/api/business/course-operations?${query}`,
      );
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (canRead) void load();
  }, [page, pageSize, appliedFilter, canRead]);

  const detail = async (id: number) => {
    setError(null);
    setSuccess(null);
    setLoading(true);
    try {
      const response = await apiRequest<Row>(
        `/api/business/course-operations/${id}`,
      );
      if (response.data) {
        setSelected(response.data);
        setForm({
          managementItemCode: response.data.managementItemCode,
          achievementDate: response.data.achievementDate,
          performanceDetails: response.data.performanceDetails,
          attachmentIds: response.data.attachmentIds ?? [],
        });
        setFields({});
      }
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  const save = async () => {
    const invalid: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      invalid.managementItemCode = "관리항목코드를 입력하세요.";
    if (!form.achievementDate)
      invalid.achievementDate = "업적발생일을 입력하세요.";
    if (!form.performanceDetails.trim())
      invalid.performanceDetails = "실적내역을 입력하세요.";
    setFields(invalid);
    if (
      readOnly ||
      Object.keys(invalid).length ||
      !window.confirm("강좌 실적을 저장하시겠습니까?")
    )
      return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const path: `/api/${string}` = selected
        ? `/api/business/course-operations/${selected.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<SaveResult>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          ...form,
          managementItemCode: form.managementItemCode.trim(),
        }),
      });
      if (response.data) {
        setSelected(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "발생일 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  if (!canRead || denied) {
    return (
      <section data-testid="course-operations-page">
        <PermissionState message="강좌 개설·운영 실적에 접근할 권한 또는 데이터 범위가 없습니다." />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-testid="course-operations-page"
      data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
      </header>
      {error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-search-panel"
      >
        <label htmlFor="course-filter">관리항목코드 검색</label>
        <input
          id="course-filter"
          data-testid="course-filter"
          className={inputStyle}
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
        />
        <button
          data-testid="course-search"
          className={buttonStyle}
          onClick={() => {
            setPage(0);
            setAppliedFilter(filter.trim());
            if (filter === appliedFilter) void load();
          }}
        >
          조회
        </button>
        <label className="ml-4" htmlFor="course-page-size">
          표시 건수
        </label>
        <select
          id="course-page-size"
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
      </section>
      {loading ? (
        <LoadingState />
      ) : rows.length === 0 ? (
        <EmptyState />
      ) : (
        <section
          className="overflow-x-auto rounded-md border border-ld bg-white p-5"
          data-testid="course-list"
        >
          <table className="w-full text-left text-sm">
            <thead>
              <tr>
                {["관리번호", "성명", "발생일", "관리항목", "상태", "상세"].map(
                  (label) => (
                    <th key={label}>{label}</th>
                  ),
                )}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`course-row-${row.achievementId}`}
                >
                  <td>{row.managementNo}</td>
                  <td>{row.teacherName}</td>
                  <td>{row.achievementDate}</td>
                  <td>{row.managementItemCode}</td>
                  <td>{row.achievementStatus}</td>
                  <td>
                    <button
                      data-testid={`course-detail-${row.achievementId}`}
                      className="text-primary"
                      onClick={() => void detail(row.achievementId)}
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
      <div className="flex items-center gap-3">
        <button
          data-testid="course-previous"
          disabled={page === 0 || loading}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <span>
          {page + 1} 페이지 / 총 {total}건
        </span>
        <button
          data-testid="course-next"
          disabled={(page + 1) * pageSize >= total || loading}
          onClick={() => setPage(page + 1)}
        >
          다음
        </button>
        {canWrite && (
          <button
            className={buttonStyle}
            data-testid="course-new"
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setFields({});
              setSuccess(null);
            }}
          >
            신규 등록
          </button>
        )}
      </div>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-form-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          {selected ? "선택 실적 상세" : "신규 실적"}
        </h2>
        {selected && (
          <p>
            관리번호: {selected.managementNo} / 평가연도:{" "}
            {selected.evaluationYear}
          </p>
        )}
        {locked && <p role="status">확정·제출 실적은 수정할 수 없습니다.</p>}
        <div className="my-4 flex gap-4" role="tablist">
          <button
            data-testid="course-details-tab"
            role="tab"
            aria-selected={tab === "details"}
            onClick={() => setTab("details")}
          >
            실적내역
          </button>
          <button
            data-testid="course-attachments-tab"
            role="tab"
            aria-selected={tab === "attachments"}
            onClick={() => setTab("attachments")}
          >
            첨부
          </button>
        </div>
        {tab === "details" ? (
          <div
            role="tabpanel"
            data-testid="course-details-panel"
            className="space-y-4"
          >
            <label className="block">
              관리항목코드 *
              <input
                data-testid="course-management-item"
                className={inputStyle}
                required
                disabled={readOnly}
                value={form.managementItemCode}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
              />
              {fields.managementItemCode && (
                <span role="alert">{fields.managementItemCode}</span>
              )}
            </label>
            <label className="block">
              업적발생일 *
              <input
                data-testid="course-date"
                className={inputStyle}
                type="date"
                required
                disabled={readOnly}
                value={form.achievementDate}
                onChange={(event) =>
                  setForm({ ...form, achievementDate: event.target.value })
                }
              />
              {fields.achievementDate && (
                <span role="alert">{fields.achievementDate}</span>
              )}
            </label>
            <label className="block">
              실적내역 *
              <textarea
                data-testid="course-performance-details"
                className={inputStyle}
                rows={5}
                required
                disabled={readOnly}
                value={form.performanceDetails}
                onChange={(event) =>
                  setForm({ ...form, performanceDetails: event.target.value })
                }
              />
              {fields.performanceDetails && (
                <span role="alert">{fields.performanceDetails}</span>
              )}
            </label>
            <p className="text-sm text-muted">
              관리항목코드는 기존 교육 관리항목 설정과 서버에서 대조합니다.
            </p>
          </div>
        ) : (
          <div role="tabpanel" data-testid="course-attachments-panel">
            <p>
              첨부 업로드 계약·검증 서비스 연결 전에는 새 첨부를 등록할 수
              없습니다.
            </p>
            {form.attachmentIds.map((token) => (
              <p key={token} data-testid={`course-attachment-${token}`}>
                {token}
              </p>
            ))}
          </div>
        )}
        <button
          data-testid="course-save"
          className={`${buttonStyle} mt-4`}
          disabled={readOnly || saving || loading}
          onClick={() => void save()}
        >
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}
