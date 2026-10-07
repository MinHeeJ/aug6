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

export type Improvement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  certificationStatus: string;
  specialLectureStartDate?: string;
  specialLectureEndDate?: string;
  mockExamQuestionPeriod?: string;
  attachmentIds?: string[];
};
type ManagementItem = {
  managementItemCode: string;
  managementItemName: string;
  evaluationYear: string;
  requiredYn: string;
  dataType: string;
  teacherEditableYn: string;
};
type SearchResult = {
  achievements: Improvement[];
  page: number;
  pageSize: number;
  totalElements: number;
  managementItems: ManagementItem[];
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
};
const blank: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
};
const path = "/api/business/employment-rate-improvements";

export function EmploymentRateImprovementsPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const allowed = !!user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const writer = !!user?.roles.some((role) => ["R01", "R09"].includes(role));
  const [rows, setRows] = useState<Improvement[]>([]);
  const [items, setItems] = useState<ManagementItem[]>([]);
  const [selected, setSelected] = useState<Improvement | null>(null);
  const [form, setForm] = useState<Form>(blank);
  const [filter, setFilter] = useState("");
  const [appliedFilter, setAppliedFilter] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState("");
  const [tab, setTab] = useState("detail");
  const locked =
    !!selected &&
    (!["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.certificationStatus,
    ) ||
      (!user?.roles.includes("R09") &&
        selected.teacherUserId !== user?.userId));
  const editable = writer && !locked;
  const inputClass =
    "mt-1 w-full rounded-md border border-ld bg-white p-2 text-sm text-dark";
  const buttonClass =
    "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";

  function failure(caught: unknown) {
    setError(
      caught instanceof Error ? caught.message : "실적을 처리하지 못했습니다.",
    );
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      const raw: unknown = caught.apiError?.fields;
      setFields(
        Array.isArray(raw)
          ? Object.fromEntries(raw.map((field) => [field.field, field.message]))
          : raw && typeof raw === "object"
            ? (raw as Record<string, string>)
            : {},
      );
    }
  }

  async function load() {
    if (!allowed) return;
    setLoading(true);
    setError("");
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (appliedFilter) query.set("managementNo", appliedFilter);
      const response = await apiRequest<SearchResult>(`${path}?${query}`);
      setRows(response.data?.achievements ?? []);
      setItems(response.data?.managementItems ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [page, pageSize, appliedFilter, allowed]);

  async function detail(id: number) {
    setBusy(true);
    setError("");
    setSuccess("");
    setFields({});
    try {
      const response = await apiRequest<Improvement>(`${path}/${id}`);
      if (!response.data) throw new Error("상세 응답이 없습니다.");
      select(response.data);
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  function select(row: Improvement) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      specialLectureStartDate: row.specialLectureStartDate ?? "",
      specialLectureEndDate: row.specialLectureEndDate ?? "",
      mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
    });
  }

  async function save() {
    const validation: Record<string, string> = {};
    if (!form.managementItemCode)
      validation.managementItemCode = "관리항목을 선택하세요.";
    if (!form.achievementDate)
      validation.achievementDate = "업적발생일을 입력하세요.";
    if (
      form.specialLectureStartDate &&
      form.specialLectureEndDate &&
      form.specialLectureEndDate < form.specialLectureStartDate
    ) {
      validation.specialLectureEndDate = "종료일은 시작일 이후여야 합니다.";
    }
    setFields(validation);
    if (
      Object.keys(validation).length ||
      !editable ||
      !window.confirm("실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const response = await apiRequest<{
        achievement: Improvement;
        occurredDateWarning: boolean;
        warningMessage?: string;
      }>(selected ? `${path}/${selected.achievementId}` : path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          ...form,
          specialLectureStartDate: form.specialLectureStartDate || null,
          specialLectureEndDate: form.specialLectureEndDate || null,
          attachmentIds: selected?.attachmentIds ?? [],
        }),
      });
      if (response.data) select(response.data.achievement);
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  if (!allowed || denied) {
    return (
      <section data-testid="employment-improvements-page">
        <PermissionState
          title="취업률 제고 실적 권한이 없습니다"
          message="역할과 데이터 범위를 확인하세요."
        />
      </section>
    );
  }
  const options = items.filter(
    (item) =>
      item.teacherEditableYn === "Y" &&
      (!selected || item.evaluationYear === selected.evaluationYear),
  );
  const unique = options.filter(
    (item, index) =>
      options.findIndex(
        (candidate) => candidate.managementItemCode === item.managementItemCode,
      ) === index,
  );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-improvements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
      </header>
      {error && <ErrorState title="처리 오류" message={error} />}
      {success && <SuccessState title="처리 결과" message={success} />}
      <div className="flex flex-wrap items-end gap-3 rounded-md bg-white p-6">
        <label>
          관리번호
          <input
            className={inputClass}
            data-testid="improvement-filter"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          />
        </label>
        <button
          className={buttonClass}
          data-testid="improvement-search"
          onClick={() => {
            setPage(0);
            setAppliedFilter(filter.trim());
            if (filter.trim() === appliedFilter) void load();
          }}
        >
          조회
        </button>
        <label>
          표시 건수
          <select
            className={inputClass}
            data-testid="improvement-page-size"
            value={pageSize}
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setPage(0);
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
      {loading ? (
        <LoadingState title="조회 중" message="실적을 조회하고 있습니다." />
      ) : rows.length === 0 ? (
        <EmptyState
          title="조회 결과가 없습니다"
          message="검색조건을 변경하거나 새 실적을 입력하세요."
        />
      ) : (
        <div className="overflow-x-auto rounded-md bg-white p-4">
          <table className="w-full text-left text-sm">
            <thead>
              <tr>
                <th>관리번호</th>
                <th>교원</th>
                <th>평가년도</th>
                <th>관리항목</th>
                <th>발생일</th>
                <th>상태</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`improvement-row-${row.achievementId}`}
                >
                  <td>{row.managementNo}</td>
                  <td>{row.teacherName}</td>
                  <td>{row.evaluationYear}</td>
                  <td>{row.managementItemCode}</td>
                  <td>{row.achievementDate}</td>
                  <td>{row.certificationStatus}</td>
                  <td>
                    <button
                      data-testid={`improvement-detail-${row.achievementId}`}
                      disabled={busy}
                      onClick={() => void detail(row.achievementId)}
                    >
                      상세 보기
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="flex items-center gap-3">
        <span>
          총 {total}건 · {page + 1}페이지
        </span>
        <button
          data-testid="improvement-prev"
          disabled={page === 0 || loading}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <button
          data-testid="improvement-next"
          disabled={(page + 1) * pageSize >= total || loading}
          onClick={() => setPage(page + 1)}
        >
          다음
        </button>
        {writer && (
          <button
            data-testid="improvement-new"
            disabled={busy}
            onClick={() => {
              setSelected(null);
              setForm(blank);
              setFields({});
              setSuccess("");
              setError("");
            }}
          >
            신규 입력
          </button>
        )}
      </div>
      <div className="rounded-md bg-white p-6">
        <div className="mb-4 flex gap-4" role="tablist">
          <button
            role="tab"
            aria-selected={tab === "detail"}
            data-testid="improvement-detail-tab"
            onClick={() => setTab("detail")}
          >
            실적 상세
          </button>
          <button
            role="tab"
            aria-selected={tab === "attachments"}
            data-testid="improvement-attachments-tab"
            onClick={() => setTab("attachments")}
          >
            첨부
          </button>
        </div>
        {locked && (
          <p role="status">
            확정 또는 제출 상태·소유권 제한으로 수정할 수 없습니다.
          </p>
        )}
        {!writer && <p>조회 전용입니다. 등록·수정은 교원 본인만 가능합니다.</p>}
        {selected && (
          <p>
            관리번호: {selected.managementNo} · 평가년도:{" "}
            {selected.evaluationYear}
          </p>
        )}
        {tab === "attachments" ? (
          <div role="tabpanel" data-testid="improvement-attachment-panel">
            <p>신규 첨부 업로드 계약이 없어 첨부 추가를 지원하지 않습니다.</p>
            {(selected?.attachmentIds ?? []).map((id) => (
              <p key={id}>{id}</p>
            ))}
          </div>
        ) : (
          <div
            role="tabpanel"
            data-testid="improvement-detail-panel"
            className="grid gap-4 md:grid-cols-2"
          >
            <label>
              관리항목 *
              <select
                className={inputClass}
                data-testid="improvement-management-item"
                value={form.managementItemCode}
                disabled={!editable || busy}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
              >
                <option value="">선택하세요</option>
                {unique.map((item) => (
                  <option
                    key={item.managementItemCode}
                    value={item.managementItemCode}
                  >
                    {item.managementItemName} ({item.dataType}
                    {item.requiredYn === "Y" ? ", 필수" : ""})
                  </option>
                ))}
              </select>
              {fields.managementItemCode && (
                <span role="alert">{fields.managementItemCode}</span>
              )}
            </label>
            {(
              [
                ["achievementDate", "업적발생일 *"],
                ["specialLectureStartDate", "특강 시작일"],
                ["specialLectureEndDate", "특강 종료일"],
              ] as const
            ).map(([key, label]) => (
              <label key={key}>
                {label}
                <input
                  type="date"
                  className={inputClass}
                  data-testid={`improvement-${key}`}
                  value={form[key]}
                  disabled={!editable || busy}
                  onChange={(event) =>
                    setForm({ ...form, [key]: event.target.value })
                  }
                />
                {fields[key] && <span role="alert">{fields[key]}</span>}
              </label>
            ))}
            <label>
              모의시험 출제기간
              <textarea
                className={inputClass}
                data-testid="improvement-mock-period"
                value={form.mockExamQuestionPeriod}
                disabled={!editable || busy}
                onChange={(event) =>
                  setForm({
                    ...form,
                    mockExamQuestionPeriod: event.target.value,
                  })
                }
              />
            </label>
            {writer && (
              <button
                className={buttonClass}
                data-testid="improvement-save"
                disabled={!editable || busy}
                onClick={() => void save()}
              >
                {busy ? "처리 중" : "저장"}
              </button>
            )}
          </div>
        )}
        <p className="mt-4 text-sm text-muted">
          삭제·상태전이·엑셀 다운로드는 공개 API 계약 승인 후 제공됩니다.
        </p>
      </div>
    </section>
  );
}
