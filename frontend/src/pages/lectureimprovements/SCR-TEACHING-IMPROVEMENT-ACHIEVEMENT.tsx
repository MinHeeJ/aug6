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
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  achievementStatus: string;
  attachmentIds: string[];
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: string;
};
type Search = { achievements: LectureImprovement[]; totalElements: number };
type Save = {
  achievement: LectureImprovement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
const initial: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
};
const root = "/api/business/lecture-improvements";
const control =
  "w-full rounded-md border border-border bg-white px-3 py-2 text-sm";
const button =
  "rounded-md border border-border px-4 py-2 text-sm disabled:opacity-50";

export function TeachingImprovementAchievementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<Form>(initial);
  const [filter, setFilter] = useState("");
  const [appliedFilter, setAppliedFilter] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const canRead = user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const canWrite = user?.roles.some((role) => ["R01", "R09"].includes(role));
  const editable =
    !selected ||
    (["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    ) &&
      (selected.teacherUserId === user?.userId || user?.roles.includes("R09")));

  function failed(caught: unknown) {
    setError(
      caught instanceof Error
        ? caught.message
        : "강의개선 실적 처리에 실패했습니다.",
    );
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
      if (caught.apiError?.code === "CONFIRMED_DATA_LOCKED") {
        setSelected((old) =>
          old ? { ...old, achievementStatus: "EVALUATION_CONFIRMED" } : old,
        );
      }
    }
  }

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (appliedFilter) query.set("managementItemCode", appliedFilter);
      const response = await apiRequest<Search>(`${root}?${query}`);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      failed(caught);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    if (canRead) void load();
    else setLoading(false);
  }, [page, pageSize, appliedFilter, canRead]);

  function populate(row: LectureImprovement) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      achievementContent: row.achievementContent,
      academicYear: String(row.academicYear),
      semester: String(row.semester),
    });
  }

  async function detail(id: number) {
    setBusy(true);
    setError(null);
    setSuccess(null);
    setFields({});
    try {
      const response = await apiRequest<LectureImprovement>(`${root}/${id}`);
      if (response.data) populate(response.data);
    } catch (caught) {
      failed(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    const invalid: Record<string, string> = {};
    for (const [key, value] of Object.entries(form))
      if (!value.trim()) invalid[key] = "필수 항목입니다.";
    if (Number(form.academicYear) < 2000)
      invalid.academicYear = "학년도는 2000 이상이어야 합니다.";
    if (![1, 2].includes(Number(form.semester)))
      invalid.semester = "학기를 선택하세요.";
    setFields(invalid);
    if (Object.keys(invalid).length || !canWrite || !editable) return;
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await apiRequest<Save>(
        selected ? `${root}/${selected.achievementId}` : root,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            ...form,
            managementItemCode: form.managementItemCode.trim(),
            academicYear: Number(form.academicYear),
            semester: Number(form.semester),
            attachmentIds: selected?.attachmentIds ?? [],
          }),
        },
      );
      if (response.data) {
        populate(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "평가대상 기간 밖 발생일로 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      failed(caught);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
      data-testid="lecture-improvements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold">강의개선 실적 관리</h1>
        <p className="mt-2 text-sm">
          학년도와 학기별 실적을 조회하고 저장합니다.
        </p>
      </header>
      {!canRead || denied ? (
        <PermissionState
          title="강의개선 실적 권한이 없습니다"
          message="역할 및 업무 데이터 범위를 확인하세요."
        />
      ) : (
        <>
          {error && <ErrorState title="처리 오류" message={error} />}
          {success && <SuccessState title="처리 완료" message={success} />}
          <div className="flex flex-wrap items-end gap-3 rounded-md bg-white p-4">
            <label>
              관리항목 검색
              <input
                className={control}
                data-testid="lecture-filter"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
              />
            </label>
            <button
              className={button}
              data-testid="lecture-search"
              onClick={() => {
                setPage(0);
                setAppliedFilter(filter.trim());
              }}
            >
              검색
            </button>
            <button
              className={button}
              data-testid="lecture-refresh"
              onClick={() => void load()}
            >
              새로고침
            </button>
            <label>
              표시 건수
              <select
                className={control}
                data-testid="lecture-page-size"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value));
                  setPage(0);
                }}
              >
                {[20, 50, 100].map((size) => (
                  <option key={size} value={size}>
                    {size}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {loading ? (
            <LoadingState title="조회 중" message="실적을 불러오고 있습니다." />
          ) : rows.length === 0 ? (
            <EmptyState
              title="조회 결과 없음"
              message="등록된 실적이 없습니다."
            />
          ) : (
            <div className="overflow-x-auto rounded-md bg-white p-4">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr>
                    {[
                      "관리번호",
                      "평가연도",
                      "학년도",
                      "학기",
                      "발생일",
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
                      data-testid={`lecture-row-${row.achievementId}`}
                    >
                      <td className="p-2">{row.managementNo}</td>
                      <td className="p-2">{row.evaluationYear}</td>
                      <td className="p-2">{row.academicYear}</td>
                      <td className="p-2">{row.semester}</td>
                      <td className="p-2">{row.achievementDate}</td>
                      <td className="p-2">{row.achievementStatus}</td>
                      <td className="p-2">
                        <button
                          className={button}
                          data-testid={`lecture-detail-${row.achievementId}`}
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
            </div>
          )}
          <nav className="flex items-center gap-3" aria-label="실적 페이지">
            <button
              className={button}
              data-testid="lecture-previous"
              disabled={page === 0 || loading}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <span>
              총 {total}건 · {page + 1}페이지
            </span>
            <button
              className={button}
              data-testid="lecture-next"
              disabled={(page + 1) * pageSize >= total || loading}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </nav>
          <form
            className="space-y-4 rounded-md bg-white p-6"
            onSubmit={(event) => void save(event)}
            noValidate
          >
            <h2 className="text-lg font-semibold">
              {selected ? "실적 상세·수정" : "신규 실적"}
            </h2>
            {selected && (
              <p>
                관리번호 {selected.managementNo} · 평가연도{" "}
                {selected.evaluationYear} (변경 불가)
              </p>
            )}
            {!editable && (
              <p role="status">
                타인 또는 현재 상태의 실적은 수정할 수 없습니다.
              </p>
            )}
            <div className="grid gap-4 md:grid-cols-2">
              {(
                [
                  ["managementItemCode", "관리항목코드", "text"],
                  ["achievementDate", "업적발생일", "date"],
                  ["academicYear", "학년도", "number"],
                ] as const
              ).map(([key, label, type]) => (
                <label key={key}>
                  {label} *
                  <input
                    className={control}
                    data-testid={`lecture-${key}`
                      .replace(/([A-Z])/g, "-$1")
                      .toLowerCase()}
                    aria-label={label}
                    aria-invalid={Boolean(fields[key])}
                    disabled={!canWrite || !editable || busy}
                    required
                    type={type}
                    value={form[key]}
                    onChange={(event) =>
                      setForm({ ...form, [key]: event.target.value })
                    }
                  />
                  {fields[key] && (
                    <span role="alert" className="text-error">
                      {fields[key]}
                    </span>
                  )}
                </label>
              ))}
              <label>
                학기 *
                <select
                  className={control}
                  data-testid="lecture-semester"
                  aria-label="학기"
                  disabled={!canWrite || !editable || busy}
                  value={form.semester}
                  onChange={(event) =>
                    setForm({ ...form, semester: event.target.value })
                  }
                >
                  <option value="">선택</option>
                  <option value="1">1학기</option>
                  <option value="2">2학기</option>
                </select>
                {fields.semester && <span role="alert">{fields.semester}</span>}
              </label>
            </div>
            <label className="block">
              실적내용 *
              <textarea
                className={control}
                data-testid="lecture-content"
                aria-label="실적내용"
                disabled={!canWrite || !editable || busy}
                value={form.achievementContent}
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
              />
              {fields.achievementContent && (
                <span role="alert">{fields.achievementContent}</span>
              )}
            </label>
            <p className="text-sm">
              첨부파일 등록은 공통 파일 연동 후 제공됩니다. 임의 첨부 참조를
              입력하지 않습니다.
            </p>
            {selected?.attachmentIds.map((token, index) => (
              <p key={token}>첨부 {index + 1}: 등록된 참조</p>
            ))}
            <div className="flex gap-3">
              <button
                className={`${button} bg-primary text-white`}
                data-testid="lecture-save"
                type="submit"
                disabled={!canWrite || !editable || busy}
              >
                {busy ? "처리 중" : "저장"}
              </button>
              <button
                className={button}
                data-testid="lecture-new"
                type="button"
                disabled={!canWrite || busy}
                onClick={() => {
                  setSelected(null);
                  setForm(initial);
                  setFields({});
                  setSuccess(null);
                  setError(null);
                }}
              >
                신규
              </button>
            </div>
          </form>
        </>
      )}
    </section>
  );
}
