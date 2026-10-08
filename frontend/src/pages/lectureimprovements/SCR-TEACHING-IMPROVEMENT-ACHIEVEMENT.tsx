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
  organizationCode: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  academicYear: string;
  semester: string | null;
  achievementContent: string | null;
  attachmentIds: string[] | null;
};
type InputOption = { code: string; name: string; teacherEditablePart: string };
type PageData = {
  achievements: LectureImprovement[];
  totalElements: number;
  academicYears: string[];
  semesters: string[];
  managementItems: InputOption[];
};
type FormData = {
  managementItemCode: string;
  achievementDate: string;
  academicYear: string;
  semester: string;
  achievementContent: string;
  attachmentRef: string;
};
const blank: FormData = {
  managementItemCode: "",
  achievementDate: "",
  academicYear: "",
  semester: "",
  achievementContent: "",
  attachmentRef: "",
};
const basePath = "/api/business/lecture-improvements";
const editableStates = [
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
];

export function LectureImprovementPage({ user }: { user: CurrentUser | null }) {
  const allowed = !!user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const writer = !!user?.roles.some((role) => ["R01", "R09"].includes(role));
  const [data, setData] = useState<PageData | null>(null);
  const [form, setForm] = useState<FormData>(blank);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [filter, setFilter] = useState({
    academicYear: "",
    semester: "",
    managementItemCode: "",
  });
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const locked =
    !!selected &&
    (!editableStates.includes(selected.achievementStatus) ||
      (selected.teacherUserId !== user?.userId &&
        !user?.roles.includes("R09")));
  const enabled = writer && !locked && !busy;
  const available =
    !!data?.semesters.length &&
    !!data?.academicYears.length &&
    !!data?.managementItems.length;
  const complete =
    !!form.managementItemCode &&
    !!form.achievementDate &&
    !!form.academicYear &&
    !!form.semester;

  function failure(caught: unknown) {
    setSuccess(null);
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setError(caught.message);
      // Shared legacy client returns list fields; the feature also understands the approved object fields.
      const value = caught.apiError?.fields as unknown;
      setFields(
        Array.isArray(value)
          ? Object.fromEntries(
              value.map((field: { field: string; message: string }) => [
                field.field,
                field.message,
              ]),
            )
          : value && typeof value === "object"
            ? (value as Record<string, string>)
            : {},
      );
    } else {
      setError("강의개선 실적을 처리하지 못했습니다.");
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
      Object.entries(filter).forEach(([key, value]) => {
        if (value) query.set(key, value);
      });
      const response = await apiRequest<PageData>(`${basePath}?${query}`);
      setData(response.data ?? null);
    } catch (caught) {
      setData(null);
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (allowed) void load();
  }, [page, pageSize, allowed]);

  async function detail(id: number) {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await apiRequest<LectureImprovement>(
        `${basePath}/${id}`,
      );
      if (response.data) {
        const row = response.data;
        setSelected(row);
        setForm({
          managementItemCode: row.managementItemCode,
          achievementDate: row.achievementDate,
          academicYear: row.academicYear ?? "",
          semester: row.semester ?? "",
          achievementContent: row.achievementContent ?? "",
          attachmentRef: row.attachmentIds?.join(", ") ?? "",
        });
        setFields({});
      }
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save() {
    if (
      !enabled ||
      !complete ||
      !available ||
      !window.confirm("강의개선 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError(null);
    setFields({});
    try {
      const path = selected
        ? (`${basePath}/${selected.achievementId}` as const)
        : basePath;
      const response = await apiRequest<{
        achievement: LectureImprovement;
        occurredDateWarning: boolean;
        warningMessage: string | null;
      }>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode,
          achievementDate: form.achievementDate,
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
          achievementContent: form.achievementContent,
          attachmentIds: form.attachmentRef
            .split(",")
            .map((id) => id.trim())
            .filter(Boolean),
        }),
      });
      if (response.data) setSelected(response.data.achievement);
      await load();
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  if (!allowed || denied)
    return <PermissionState title="강의개선 실적 관리 권한이 없습니다" />;

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의개선 실적 관리
        </h1>
      </div>
      {error && <ErrorState title="강의개선 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="grid gap-4 md:grid-cols-4">
          <label>
            학년도 검색
            <select
              value={filter.academicYear}
              onChange={(e) =>
                setFilter({ ...filter, academicYear: e.target.value })
              }
            >
              <option value="">전체</option>
              {data?.academicYears.map((year) => (
                <option key={year}>{year}</option>
              ))}
            </select>
          </label>
          <label>
            학기 검색
            <select
              value={filter.semester}
              onChange={(e) =>
                setFilter({ ...filter, semester: e.target.value })
              }
            >
              <option value="">전체</option>
              {data?.semesters.map((semester) => (
                <option key={semester}>{semester}</option>
              ))}
            </select>
          </label>
          <label>
            관리항목 검색
            <select
              value={filter.managementItemCode}
              onChange={(e) =>
                setFilter({ ...filter, managementItemCode: e.target.value })
              }
            >
              <option value="">전체</option>
              {data?.managementItems.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            disabled={loading}
            onClick={() => {
              setPage(0);
              void load(0);
            }}
          >
            조회
          </button>
        </div>
      </section>
      {loading ? (
        <LoadingState />
      ) : !data?.achievements.length ? (
        <EmptyState />
      ) : (
        <section className="rounded-md border border-ld bg-white p-5 overflow-x-auto">
          <table>
            <thead>
              <tr>
                <th>관리번호</th>
                <th>평가연도</th>
                <th>학년도</th>
                <th>학기</th>
                <th>상태</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {data.achievements.map((row) => (
                <tr key={row.achievementId}>
                  <td>{row.managementNo}</td>
                  <td>{row.evaluationYear}</td>
                  <td>{row.academicYear}</td>
                  <td>{row.semester ?? "미설정"}</td>
                  <td>{row.achievementStatus}</td>
                  <td>
                    <button
                      type="button"
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
        </section>
      )}
      <div className="flex flex-wrap items-center gap-4">
        <span>총 {data?.totalElements ?? 0}건</span>
        <label>
          표시 건수
          <select
            value={pageSize}
            onChange={(e) => {
              setPage(0);
              setPageSize(Number(e.target.value));
            }}
          >
            {[20, 50, 100].map((size) => (
              <option key={size}>{size}</option>
            ))}
          </select>
        </label>
        <button
          type="button"
          disabled={page === 0 || loading}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <span>{page + 1} 페이지</span>
        <button
          type="button"
          disabled={
            loading || (page + 1) * pageSize >= (data?.totalElements ?? 0)
          }
          onClick={() => setPage(page + 1)}
        >
          다음
        </button>
      </div>
      <section className="rounded-md border border-ld bg-white p-5">
        <h2 className="text-lg font-semibold">
          {selected ? "실적 상세·수정" : "신규 실적"}
        </h2>
        {selected && (
          <p>
            평가연도: {selected.evaluationYear} / 소속:{" "}
            {selected.organizationCode} (변경 불가)
          </p>
        )}
        {locked && (
          <p role="status">
            확정 또는 현재 상태·소유권에 따라 수정할 수 없습니다.
          </p>
        )}
        {!writer && <p>조회 전용입니다.</p>}
        {!available && (
          <p role="status">
            선택 가능한 학년도·학기·교원 입력 관리항목 설정이 없어 저장할 수
            없습니다.
          </p>
        )}
        <fieldset
          disabled={!enabled}
          className="mt-4 grid gap-4 md:grid-cols-2"
        >
          <label>
            관리항목 *
            <select
              value={form.managementItemCode}
              onChange={(e) =>
                setForm({ ...form, managementItemCode: e.target.value })
              }
            >
              <option value="">선택</option>
              {data?.managementItems.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.name}
                </option>
              ))}
              {selected &&
                !data?.managementItems.some(
                  (item) => item.code === form.managementItemCode,
                ) && (
                  <option value={form.managementItemCode}>
                    {form.managementItemCode}
                  </option>
                )}
            </select>
          </label>
          <label>
            업적발생일 *
            <input
              type="date"
              value={form.achievementDate}
              onChange={(e) =>
                setForm({ ...form, achievementDate: e.target.value })
              }
            />
          </label>
          <label>
            학년도 *
            <select
              value={form.academicYear}
              onChange={(e) =>
                setForm({ ...form, academicYear: e.target.value })
              }
            >
              <option value="">선택</option>
              {data?.academicYears.map((year) => (
                <option key={year}>{year}</option>
              ))}
            </select>
          </label>
          <label>
            학기 *
            <select
              value={form.semester}
              onChange={(e) => setForm({ ...form, semester: e.target.value })}
            >
              <option value="">선택</option>
              {data?.semesters.map((semester) => (
                <option key={semester}>{semester}</option>
              ))}
              {selected &&
                form.semester &&
                !data?.semesters.includes(form.semester) && (
                  <option value={form.semester}>{form.semester}</option>
                )}
            </select>
          </label>
          <label>
            실적내용
            <textarea
              value={form.achievementContent}
              onChange={(e) =>
                setForm({ ...form, achievementContent: e.target.value })
              }
            />
          </label>
          <label>
            첨부참조
            <input
              value={form.attachmentRef}
              onChange={(e) =>
                setForm({ ...form, attachmentRef: e.target.value })
              }
            />
          </label>
        </fieldset>
        {Object.entries(fields).map(([field, message]) => (
          <p key={field} role="alert">
            {field}: {message}
          </p>
        ))}
        <div className="mt-4 flex gap-4">
          {writer && (
            <button
              type="button"
              disabled={!enabled || !complete || !available}
              className="rounded-md bg-primary px-4 py-2 text-white"
              onClick={() => void save()}
            >
              저장
            </button>
          )}
          {writer && (
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                setSelected(null);
                setForm(blank);
                setFields({});
                setSuccess(null);
              }}
            >
              신규 입력
            </button>
          )}
        </div>
      </section>
    </section>
  );
}
