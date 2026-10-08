import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type LectureImprovement = {
  achievementId: number;
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
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: string;
};
const blank: Form = {
  evaluationYear: "",
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
};
const base = "/api/business/lecture-improvements";
const editable = ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"];

export function LectureImprovementsPage() {
  const { user } = useAuth();
  const allowed = !!user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const writer = !!user?.roles.some((role) => ["R01", "R09"].includes(role));
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<Form>(blank);
  const [filter, setFilter] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permission, setPermission] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const locked =
    !!selected &&
    (!editable.includes(selected.achievementStatus) ||
      (selected.teacherUserId !== user?.userId &&
        !user?.roles.includes("R09")));

  function failure(caught: unknown) {
    setError(caught instanceof Error ? caught.message : "처리하지 못했습니다.");
    if (caught instanceof ApiClientError) {
      setPermission(caught.status === 403);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((f) => [f.field, f.message]),
        ),
      );
    }
  }

  async function load(targetPage = page) {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(targetPage),
        pageSize: String(pageSize),
      });
      if (filter.trim()) query.set("managementItemCode", filter.trim());
      const response = await apiRequest<{
        achievements: LectureImprovement[];
        totalElements: number;
      }>(`${base}?${query}`);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      failure(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (allowed) void load();
  }, [allowed, page, pageSize]);

  function choose(row: LectureImprovement) {
    setSelected(row);
    setForm({
      evaluationYear: row.evaluationYear,
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      achievementContent: row.achievementContent,
      academicYear: String(row.academicYear),
      semester: String(row.semester),
    });
    setFields({});
  }

  async function detail(id: number) {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await apiRequest<LectureImprovement>(`${base}/${id}`);
      if (response.data) choose(response.data);
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save() {
    if (!writer || locked) return;
    const missing = Object.entries(form).filter(([, value]) => !value.trim());
    if (missing.length) {
      setFields(
        Object.fromEntries(
          missing.map(([key]) => [key, "필수 입력 항목입니다."]),
        ),
      );
      return;
    }
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    setBusy(true);
    setError(null);
    setSuccess(null);
    setFields({});
    try {
      const response = await apiRequest<{
        achievement: LectureImprovement;
        occurredDateWarning: boolean;
        warningMessage?: string;
      }>(selected ? `${base}/${selected.achievementId}` : base, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          ...form,
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
          attachmentIds: selected?.attachmentIds ?? [],
        }),
      });
      if (response.data) {
        choose(response.data.achievement);
        setSuccess(response.data.warningMessage || "저장되었습니다.");
      }
      await load();
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  if (!allowed || permission)
    return <PermissionState title="강의개선 실적 관리 권한이 없습니다" />;
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-IMPROVEMENTS"
      data-testid="lecture-improvements-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">업적 입력 관리 / 교육영역</p>
        <h1 className="text-xl font-semibold text-dark">강의개선 실적 관리</h1>
      </div>
      {error && <ErrorState title="강의개선 실적 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      <section className="rounded-md border border-ld bg-white p-5">
        <label>
          관리항목 검색
          <input
            data-testid="lecture-improvement-filter"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
        </label>
        <button
          data-testid="lecture-improvement-search"
          onClick={() => {
            setPage(0);
            void load(0);
          }}
        >
          조회
        </button>
        <label>
          표시 건수
          <select
            data-testid="lecture-improvement-page-size"
            value={pageSize}
            onChange={(e) => {
              setPageSize(Number(e.target.value));
              setPage(0);
            }}
          >
            {[20, 50, 100].map((n) => (
              <option key={n} value={n}>
                {n}건
              </option>
            ))}
          </select>
        </label>
        {loading && <LoadingState title="강의개선 실적 조회 중" />}
        {!loading && !error && rows.length === 0 && (
          <EmptyState title="조회된 강의개선 실적이 없습니다" />
        )}
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr>
                {["관리항목", "발생일", "학년도", "학기", "상태", "상세"].map(
                  (title) => (
                    <th key={title}>{title}</th>
                  ),
                )}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`lecture-improvement-row-${row.achievementId}`}
                >
                  <td>{row.managementItemCode}</td>
                  <td>{row.achievementDate}</td>
                  <td>{row.academicYear}</td>
                  <td>{row.semester}</td>
                  <td>{row.achievementStatus}</td>
                  <td>
                    <button
                      data-testid={`lecture-improvement-detail-${row.achievementId}`}
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
        <p>
          총 {total}건 / {page + 1}페이지
        </p>
        <button
          data-testid="lecture-improvement-previous"
          disabled={page === 0 || loading}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <button
          data-testid="lecture-improvement-next"
          disabled={(page + 1) * pageSize >= total || loading}
          onClick={() => setPage(page + 1)}
        >
          다음
        </button>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="lecture-improvement-form"
      >
        <h2 className="text-lg font-semibold">강의개선 실적 상세</h2>
        {locked && (
          <p role="status">
            확정·제출 상태 또는 타인 실적은 수정할 수 없습니다.
          </p>
        )}
        {!writer && <p>조회 전용입니다.</p>}
        <div className="grid gap-4 md:grid-cols-2">
          {(
            [
              ["evaluationYear", "평가연도"],
              ["managementItemCode", "관리항목"],
              ["achievementDate", "업적발생일"],
              ["academicYear", "학년도"],
              ["semester", "학기"],
              ["achievementContent", "실적내용"],
            ] as [keyof Form, string][]
          ).map(([key, label]) => (
            <label key={key}>
              {label} *
              {key === "achievementContent" ? (
                <textarea
                  data-testid={`lecture-improvement-${key}`}
                  value={form[key]}
                  disabled={!writer || locked || busy}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                />
              ) : (
                <input
                  data-testid={`lecture-improvement-${key}`}
                  value={form[key]}
                  required
                  type={key === "achievementDate" ? "date" : "text"}
                  disabled={
                    !writer ||
                    locked ||
                    busy ||
                    (key === "evaluationYear" && selected !== null)
                  }
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                />
              )}
              {fields[key] && (
                <span className="text-error" role="alert">
                  {fields[key]}
                </span>
              )}
            </label>
          ))}
        </div>
        {writer && (
          <button
            data-testid="lecture-improvement-new"
            disabled={busy}
            onClick={() => {
              setSelected(null);
              setForm(blank);
              setFields({});
              setSuccess(null);
            }}
          >
            새 실적
          </button>
        )}
        {writer && (
          <button
            className="rounded-md bg-primary px-4 py-2 text-white"
            data-testid="lecture-improvement-save"
            disabled={locked || busy}
            onClick={() => void save()}
          >
            {busy ? "처리 중" : "저장"}
          </button>
        )}
      </section>
    </section>
  );
}
