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

export type LectureImprovement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  achievementStatus: string;
  attachmentIds: string[];
};
type Item = {
  managementItemId: number;
  managementItemCode: string;
  managementItemName: string;
};
type Search = {
  achievements: LectureImprovement[];
  totalElements: number;
  managementItems: Item[];
};
type Saved = {
  achievement: LectureImprovement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: string;
  attachmentIds: string[];
};
const empty: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
  attachmentIds: [],
};
const endpoint = "/api/business/lecture-improvements";

export function LectureImprovementPage({ user }: { user: CurrentUser | null }) {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<Form>(empty);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const admitted = user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const canWrite = user?.roles.some((role) => ["R01", "R09"].includes(role));
  const readonly =
    !canWrite ||
    (selected !== null &&
      (selected.achievementStatus !== "DRAFT" ||
        (!user?.roles.includes("R09") &&
          selected.teacherUserId !== user?.userId)));

  function failure(caught: unknown) {
    setError(caught instanceof Error ? caught.message : "처리하지 못했습니다.");
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
    }
  }
  async function load() {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (query) params.set("managementNo", query);
      const response = await apiRequest<Search>(`${endpoint}?${params}`);
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
    if (admitted) void load();
    else setLoading(false);
  }, [page, pageSize, query, admitted]);

  function populate(row: LectureImprovement) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      achievementContent: row.achievementContent,
      academicYear: String(row.academicYear),
      semester: String(row.semester),
      attachmentIds: row.attachmentIds ?? [],
    });
  }
  async function detail(id: number) {
    setBusy(true);
    setFields({});
    setSuccess("");
    try {
      const response = await apiRequest<LectureImprovement>(
        `${endpoint}/${id}`,
      );
      if (response.data) populate(response.data);
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }
  async function save() {
    const invalid: Record<string, string> = {};
    if (!form.managementItemCode)
      invalid.managementItemCode = "관리항목을 선택하세요.";
    if (!form.achievementDate)
      invalid.achievementDate = "업적발생일을 입력하세요.";
    if (!form.achievementContent.trim())
      invalid.achievementContent = "실적내용을 입력하세요.";
    if (!form.academicYear || Number(form.academicYear) < 2000)
      invalid.academicYear = "학년도는 2000 이상입니다.";
    if (!["1", "2"].includes(form.semester))
      invalid.semester = "학기를 선택하세요.";
    setFields(invalid);
    if (
      readonly ||
      Object.keys(invalid).length ||
      !window.confirm("강의개선 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const response = await apiRequest<Saved>(
        selected ? `${endpoint}/${selected.achievementId}` : endpoint,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            ...form,
            academicYear: Number(form.academicYear),
            semester: Number(form.semester),
          }),
        },
      );
      if (response.data) {
        populate(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "기간 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }
  const field = (name: keyof Form, value: string) =>
    setForm((previous) => ({ ...previous, [name]: value }));
  const inputClass = "form-input w-full";

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
      data-testid="lecture-improvements-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">업적 입력 관리 / 교육영역</p>
        <h1 className="text-xl font-semibold">강의개선 실적 관리</h1>
      </div>
      {!admitted || denied ? (
        <PermissionState title="강의개선 실적 관리 권한이 없습니다" />
      ) : (
        <>
          {error && <ErrorState title="강의개선 실적 오류" message={error} />}
          {success && <SuccessState title="처리 완료" message={success} />}
          <form
            className="rounded-md border border-ld bg-white p-5"
            onSubmit={(event) => {
              event.preventDefault();
              setPage(0);
              setQuery(filter.trim());
              if (query === filter.trim()) void load();
            }}
          >
            <label>
              관리번호 검색
              <input
                className={inputClass}
                data-testid="lecture-improvements-search-input"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
              />
            </label>
            <button
              className="btn-secondary mt-3"
              data-testid="lecture-improvements-search-button"
              type="submit"
            >
              조회
            </button>
            <label className="ml-4">
              표시 건수
              <select
                data-testid="lecture-improvements-page-size"
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
          </form>
          <section
            className="rounded-md border border-ld bg-white p-5 overflow-x-auto"
            aria-label="실적 목록"
          >
            {loading ? (
              <LoadingState />
            ) : rows.length === 0 ? (
              <EmptyState />
            ) : (
              <table>
                <thead>
                  <tr>
                    {[
                      "관리번호",
                      "성명",
                      "관리항목",
                      "업적발생일",
                      "학년도",
                      "학기",
                      "상태",
                      "상세",
                    ].map((label) => (
                      <th key={label}>{label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.achievementId}
                      data-testid={`lecture-improvements-row-${row.achievementId}`}
                    >
                      <td>{row.managementNo}</td>
                      <td>{row.teacherName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.achievementDate}</td>
                      <td>{row.academicYear}</td>
                      <td>{row.semester}</td>
                      <td>{row.achievementStatus}</td>
                      <td>
                        <button
                          type="button"
                          data-testid={`lecture-improvements-detail-${row.achievementId}`}
                          disabled={busy}
                          onClick={() => void detail(row.achievementId)}
                        >
                          상세
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <p>
              전체 {total}건 · {page + 1}페이지
            </p>
            <button
              type="button"
              data-testid="lecture-improvements-prev"
              disabled={page === 0 || loading}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <button
              type="button"
              data-testid="lecture-improvements-next"
              disabled={(page + 1) * pageSize >= total || loading}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </section>
          <form
            className="rounded-md border border-ld bg-white p-5 space-y-4"
            data-testid="lecture-improvements-form"
            onSubmit={(event) => {
              event.preventDefault();
              void save();
            }}
          >
            <h2 className="text-lg font-semibold">
              {selected ? "실적 상세 / 수정" : "새 실적 등록"}
            </h2>
            {selected && (
              <p>
                관리번호: {selected.managementNo} / 성명: {selected.teacherName}
                / 평가연도: {selected.evaluationYear} / 상태:{" "}
                {selected.achievementStatus}
              </p>
            )}
            {readonly && (
              <p>
                조회 전용입니다. 본인 소유 작성중 실적만 수정할 수 있습니다.
              </p>
            )}
            <fieldset
              disabled={readonly || busy}
              className="grid gap-4 md:grid-cols-2"
            >
              <label>
                관리항목 *
                <select
                  className={inputClass}
                  value={form.managementItemCode}
                  data-testid="lecture-improvements-item"
                  onChange={(event) =>
                    field("managementItemCode", event.target.value)
                  }
                >
                  <option value="">선택하세요</option>
                  {items.map((item) => (
                    <option
                      key={item.managementItemId}
                      value={item.managementItemCode}
                    >
                      {item.managementItemName ?? item.managementItemCode}
                    </option>
                  ))}
                  {selected &&
                    !items.some(
                      (item) =>
                        item.managementItemCode === selected.managementItemCode,
                    ) && (
                      <option value={selected.managementItemCode}>
                        {selected.managementItemCode}
                      </option>
                    )}
                </select>
              </label>
              <label>
                업적발생일 *
                <input
                  className={inputClass}
                  type="date"
                  data-testid="lecture-improvements-date"
                  value={form.achievementDate}
                  onChange={(event) =>
                    field("achievementDate", event.target.value)
                  }
                />
              </label>
              <label>
                학년도 *
                <input
                  className={inputClass}
                  type="number"
                  min="2000"
                  data-testid="lecture-improvements-year"
                  value={form.academicYear}
                  onChange={(event) =>
                    field("academicYear", event.target.value)
                  }
                />
              </label>
              <label>
                학기 *
                <select
                  className={inputClass}
                  data-testid="lecture-improvements-semester"
                  value={form.semester}
                  onChange={(event) => field("semester", event.target.value)}
                >
                  <option value="">선택하세요</option>
                  <option value="1">1학기</option>
                  <option value="2">2학기</option>
                </select>
              </label>
              <label className="md:col-span-2">
                실적내용 *
                <textarea
                  className={inputClass}
                  data-testid="lecture-improvements-content"
                  value={form.achievementContent}
                  onChange={(event) =>
                    field("achievementContent", event.target.value)
                  }
                />
              </label>
            </fieldset>
            {Object.entries(fields).map(([name, message]) => (
              <p role="alert" key={name}>
                {name}: {message}
              </p>
            ))}
            <p>
              첨부 참조:{" "}
              {form.attachmentIds.length
                ? form.attachmentIds.join(", ")
                : "없음"}
              (기존 첨부 참조 유지, 신규 파일 업로드는 지원하지 않습니다.)
            </p>
            <button
              className="btn-primary"
              type="submit"
              data-testid="lecture-improvements-save"
              disabled={readonly || busy}
            >
              {busy ? "처리 중" : "저장"}
            </button>
            {canWrite && (
              <button
                className="btn-secondary ml-3"
                type="button"
                data-testid="lecture-improvements-new"
                disabled={busy}
                onClick={() => {
                  setSelected(null);
                  setForm(empty);
                  setFields({});
                  setSuccess("");
                }}
              >
                새 실적
              </button>
            )}
          </form>
        </>
      )}
    </section>
  );
}
