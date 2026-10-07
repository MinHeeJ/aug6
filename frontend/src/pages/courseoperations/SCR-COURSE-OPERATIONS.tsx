import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import type { CurrentUser } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Item = {
  managementItemCode: string;
  managementItemName: string;
  evaluationYear: string;
  requiredYn: string;
  dataType: string;
  teacherEditableYn: string;
};
export type CourseOperation = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  teacherUserId: number;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  performanceDetails: string;
  attachmentIds: string[];
};
type ListResult = {
  achievements: CourseOperation[];
  totalElements: number;
  managementItems: Item[];
};
type SaveResult = {
  achievementId: number;
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
};
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
};
const path = "/api/business/course-operations";

export function CourseOperationsPage({ user }: { user: CurrentUser | null }) {
  const allowed = !!user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const writer = !!user?.roles.some((role) => ["R01", "R09"].includes(role));
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [confirming, setConfirming] = useState(false);
  const editableStatus =
    !selected ||
    ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const owns =
    !selected ||
    selected.teacherUserId === user?.userId ||
    user?.roles.includes("R09");
  const editable = writer && editableStatus && owns;

  function failure(caught: unknown) {
    setError(caught instanceof Error ? caught.message : "요청에 실패했습니다.");
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403 || caught.status === 401);
      const details = caught.apiError?.fields as unknown;
      if (details && !Array.isArray(details) && typeof details === "object") {
        setFields(details as Record<string, string>);
      }
    }
  }

  async function load() {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({
        page: String(page),
        pageSize: String(size),
      });
      if (query) params.set("managementNo", query);
      const response = await apiRequest<ListResult>(`${path}?${params}`);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
      setItems(response.data?.managementItems ?? []);
    } catch (caught) {
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (allowed) void load();
  }, [allowed, page, size, query]);

  async function detail(row: CourseOperation) {
    setLoading(true);
    setSuccess("");
    setFields({});
    try {
      const response = await apiRequest<CourseOperation>(
        `${path}/${row.achievementId}`,
      );
      if (response.data) {
        setSelected(response.data);
        setForm({
          managementItemCode: response.data.managementItemCode,
          achievementDate: response.data.achievementDate,
          performanceDetails: response.data.performanceDetails,
        });
      }
    } catch (caught) {
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  function validate() {
    const errors: Record<string, string> = {};
    if (!form.managementItemCode)
      errors.managementItemCode = "관리항목을 선택하세요.";
    if (!form.achievementDate)
      errors.achievementDate = "업적발생일을 입력하세요.";
    if (!form.performanceDetails.trim())
      errors.performanceDetails = "실적내역을 입력하세요.";
    setFields(errors);
    if (!Object.keys(errors).length) setConfirming(true);
  }

  async function save() {
    setConfirming(false);
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const response = await apiRequest<SaveResult>(
        selected ? `${path}/${selected.achievementId}` : path,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            ...form,
            attachmentIds: selected?.attachmentIds ?? [],
          }),
        },
      );
      if (response.data) {
        setSelected(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? `저장했습니다. ${response.data.warningMessage}`
            : "저장했습니다.",
        );
      }
      await load();
    } catch (caught) {
      failure(caught);
    } finally {
      setSaving(false);
    }
  }

  if (!allowed || denied)
    return <PermissionState title="강좌 운영 실적 접근 권한이 없습니다" />;
  return (
    <section
      data-screen-id="SCR-COURSE-OPERATIONS"
      data-testid="course-operations-screen"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          교육영역 실적을 조회하고 실적내역을 입력합니다.
        </p>
      </div>
      {error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      {loading && <LoadingState />}
      <form
        className="mb-6 flex flex-wrap items-end gap-4 rounded-md border border-ld p-5"
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
          setQuery(filter.trim());
        }}
      >
        <label>
          관리번호 검색
          <input
            data-testid="course-search"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          />
        </label>
        <label>
          표시 건수
          <select
            data-testid="course-page-size"
            value={size}
            onChange={(event) => {
              setSize(Number(event.target.value));
              setPage(0);
            }}
          >
            {[20, 50, 100].map((value) => (
              <option key={value} value={value}>
                {value}건
              </option>
            ))}
          </select>
        </label>
        <button type="submit" data-testid="course-search-button">
          조회
        </button>
        {writer && (
          <button
            type="button"
            data-testid="course-new-button"
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setFields({});
              setSuccess("");
            }}
          >
            신규 입력
          </button>
        )}
      </form>
      <section className="mb-6 rounded-md border border-ld p-5">
        <h2 className="text-lg font-semibold">실적 목록</h2>
        {!loading && !rows.length && !error && (
          <EmptyState message="조회된 강좌 운영 실적이 없습니다." />
        )}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr>
                {[
                  "관리번호",
                  "교원",
                  "평가년도",
                  "관리항목",
                  "업적발생일",
                  "상태",
                  "상세",
                ].map((label) => (
                  <th key={label} className="p-2">
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`course-row-${row.achievementId}`}
                >
                  <td className="p-2">{row.managementNo}</td>
                  <td>{row.teacherName}</td>
                  <td>{row.evaluationYear}</td>
                  <td>{row.managementItemCode}</td>
                  <td>{row.achievementDate}</td>
                  <td>{row.achievementStatus}</td>
                  <td>
                    <button
                      data-testid={`course-detail-${row.achievementId}`}
                      type="button"
                      onClick={() => void detail(row)}
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-3 text-sm text-muted">
          총 {total}건 / {page + 1}페이지
        </p>
        <button
          data-testid="course-prev"
          disabled={page === 0}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <button
          data-testid="course-next"
          disabled={(page + 1) * size >= total}
          onClick={() => setPage(page + 1)}
        >
          다음
        </button>
      </section>
      <section
        className="rounded-md border border-ld p-5"
        data-testid="course-detail-panel"
      >
        <h2 className="text-lg font-semibold">실적 상세</h2>
        {selected && (
          <p>
            {selected.managementNo} / {selected.evaluationYear} /{" "}
            {selected.achievementStatus}
          </p>
        )}
        {!editableStatus && (
          <p role="status">제출·확정 상태에서는 수정할 수 없습니다.</p>
        )}
        {!writer && <p>조회 전용 화면입니다.</p>}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label>
            관리항목 *
            <select
              data-testid="course-management-item"
              value={form.managementItemCode}
              disabled={!editable}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">선택하세요</option>
              {items.map((item, index) => (
                <option
                  key={`${item.managementItemCode}-${item.evaluationYear}-${index}`}
                  value={item.managementItemCode}
                  disabled={item.teacherEditableYn !== "Y"}
                >
                  {item.managementItemName} ({item.evaluationYear}) ·{" "}
                  {item.dataType}
                </option>
              ))}
            </select>
            <span className="text-error">{fields.managementItemCode}</span>
          </label>
          <label>
            업적발생일 *
            <input
              data-testid="course-date"
              type="date"
              value={form.achievementDate}
              disabled={!editable}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
            <span className="text-error">{fields.achievementDate}</span>
          </label>
          <label className="md:col-span-2">
            실적내역 *
            <textarea
              className="min-h-28 w-full"
              data-testid="course-performance-details"
              value={form.performanceDetails}
              disabled={!editable}
              onChange={(event) =>
                setForm({ ...form, performanceDetails: event.target.value })
              }
            />
            <span className="text-error">{fields.performanceDetails}</span>
          </label>
        </div>
        <p className="mt-3 text-sm text-muted">
          신규 파일 첨부는 업로드 계약 승인 전 사용할 수 없습니다. 기존 참조는
          유지됩니다. 삭제·상태변경·Excel 내려받기는 현재 공개 API 계약에
          없습니다.
        </p>
        {selected?.attachmentIds.map((id) => <p key={id}>첨부 참조: {id}</p>)}
        {writer && (
          <button
            className="mt-4 rounded-md bg-primary px-4 py-2 text-white disabled:opacity-60"
            data-testid="course-save"
            disabled={!editable || saving || loading}
            onClick={validate}
          >
            {saving ? "저장 중" : "저장"}
          </button>
        )}
      </section>
      {confirming && (
        <section
          role="dialog"
          aria-label="저장 확인"
          data-testid="course-save-confirm"
          className="rounded-md p-5"
        >
          <p>실적을 저장하시겠습니까?</p>
          <button data-testid="course-confirm" onClick={() => void save()}>
            확인
          </button>
          <button
            data-testid="course-cancel"
            onClick={() => setConfirming(false)}
          >
            취소
          </button>
        </section>
      )}
    </section>
  );
}
