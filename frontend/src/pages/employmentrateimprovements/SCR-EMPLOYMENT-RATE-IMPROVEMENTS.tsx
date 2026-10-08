import { useEffect, useState } from "react";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import {
  employmentRateImprovementApi as api,
  type EmploymentRateImprovement,
  type EmploymentRateImprovementRequest,
} from "./employmentRateImprovementApi";

type Form = {
  managementItemCode: string;
  evaluationYear: string;
  achievementDate: string;
  achievementName: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
  achievementDetail: string;
  attachmentRef: string;
  attachmentIds: string;
};
const empty: Form = {
  managementItemCode: "",
  evaluationYear: "",
  achievementDate: "",
  achievementName: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  achievementDetail: "",
  attachmentRef: "",
  attachmentIds: "",
};
const inputClass = "mt-1 w-full rounded border border-ld p-2 text-sm";

export function EmploymentRateImprovementsPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const [rows, setRows] = useState<EmploymentRateImprovement[]>([]);
  const [selected, setSelected] = useState<EmploymentRateImprovement | null>(
    null,
  );
  const [form, setForm] = useState<Form>(empty);
  const [filter, setFilter] = useState("");
  const [queryCode, setQueryCode] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const canRead = !!user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const admin = !!user?.roles.includes("R09");
  const canWrite =
    admin ||
    (!!user?.roles.includes("R01") &&
      (!selected || selected.teacherUserId === user.userId));
  const locked =
    !!selected &&
    !["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );

  function handleError(caught: unknown) {
    setError(
      caught instanceof Error ? caught.message : "요청을 처리하지 못했습니다.",
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
    }
  }

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(size),
      });
      if (queryCode) query.set("managementItemCode", queryCode);
      const response = await api.list(query);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      handleError(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (canRead) void load();
  }, [page, size, queryCode, canRead]);

  function populate(row: EmploymentRateImprovement) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      evaluationYear: row.evaluationYear,
      achievementDate: row.achievementDate,
      achievementName: row.achievementName ?? "",
      specialLectureStartDate: row.specialLectureStartDate ?? "",
      specialLectureEndDate: row.specialLectureEndDate ?? "",
      mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
      achievementDetail: row.achievementDetail,
      attachmentRef: row.attachmentRef ?? "",
      attachmentIds: (JSON.parse(row.attachmentIds || "[]") as string[]).join(
        "\n",
      ),
    });
  }

  async function detail(row: EmploymentRateImprovement) {
    setBusy(true);
    setError(null);
    setSuccess(null);
    setFields({});
    try {
      const response = await api.get(row.achievementId);
      if (response.data) populate(response.data);
    } catch (caught) {
      handleError(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save() {
    const validation: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      validation.managementItemCode = "관리항목을 입력하세요.";
    if (!form.achievementDate)
      validation.achievementDate = "업적발생일을 입력하세요.";
    if (form.evaluationYear && !/^\d{4}$/.test(form.evaluationYear))
      validation.evaluationYear = "YYYY 형식으로 입력하세요.";
    if (
      form.specialLectureStartDate &&
      form.specialLectureEndDate &&
      form.specialLectureEndDate < form.specialLectureStartDate
    ) {
      validation.specialLectureEndDate =
        "특강 종료일은 시작일보다 빠를 수 없습니다.";
    }
    setFields(validation);
    if (Object.keys(validation).length || !canWrite || locked) return;
    let achievementDetail: unknown;
    try {
      achievementDetail = form.achievementDetail.trim()
        ? JSON.parse(form.achievementDetail)
        : {};
    } catch {
      setFields({
        achievementDetail: "상세 입력값은 JSON 형식으로 입력하세요.",
      });
      return;
    }
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    setBusy(true);
    setError(null);
    setSuccess(null);
    const body: EmploymentRateImprovementRequest = {
      managementItemCode: form.managementItemCode.trim(),
      achievementDate: form.achievementDate,
      ...(selected ? {} : { evaluationYear: form.evaluationYear || undefined }),
      achievementName: form.achievementName || null,
      specialLectureStartDate: form.specialLectureStartDate || null,
      specialLectureEndDate: form.specialLectureEndDate || null,
      mockExamQuestionPeriod: form.mockExamQuestionPeriod || null,
      achievementDetail,
      attachmentRef: form.attachmentRef || null,
      attachmentIds: form.attachmentIds
        .split("\n")
        .map((id) => id.trim())
        .filter(Boolean),
    };
    try {
      const response = selected
        ? await api.update(selected.achievementId, body)
        : await api.create(body);
      if (response.data) {
        populate(response.data.achievement);
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
      setBusy(false);
    }
  }

  if (!canRead || denied)
    return <PermissionState message="취업률 제고 실적 접근 권한이 없습니다." />;

  function field(
    name: keyof Form,
    label: string,
    type = "text",
    readOnly = false,
  ) {
    return (
      <label className="text-sm text-bodytext" key={name}>
        {label}
        {name === "attachmentIds" || name === "achievementDetail" ? (
          <textarea
            aria-label={label}
            data-testid={`employment-improvement-${name.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`}
            className={inputClass}
            value={form[name]}
            disabled={!canWrite || locked || busy}
            onChange={(event) =>
              setForm((current) => ({ ...current, [name]: event.target.value }))
            }
          />
        ) : (
          <input
            aria-label={label}
            data-testid={`employment-improvement-${name.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`}
            className={inputClass}
            type={type}
            value={form[name]}
            disabled={!canWrite || locked || busy}
            readOnly={readOnly}
            onChange={(event) =>
              setForm((current) => ({ ...current, [name]: event.target.value }))
            }
          />
        )}
        {fields[name] && (
          <span className="mt-1 block text-error">{fields[name]}</span>
        )}
      </label>
    );
  }

  return (
    <div
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-improvements-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">업적 입력 관리 &gt; 교육영역</p>
      </div>
      {error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      <section className="mb-6 rounded-md bg-white p-6 shadow-md">
        <h2 className="mb-4 text-lg font-semibold">실적 검색</h2>
        <div className="flex flex-wrap items-end gap-4">
          <label>
            관리항목 검색
            <input
              aria-label="관리항목 검색"
              data-testid="employment-improvement-filter"
              className={inputClass}
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
          <button
            data-testid="employment-improvement-search"
            className="rounded bg-primary px-4 py-2 text-white"
            onClick={() => {
              setPage(0);
              setQueryCode(filter.trim());
              if (queryCode === filter.trim()) void load();
            }}
          >
            검색
          </button>
          <label>
            표시 건수
            <select
              aria-label="표시 건수"
              data-testid="employment-improvement-page-size"
              className={inputClass}
              value={size}
              onChange={(event) => {
                setSize(Number(event.target.value));
                setPage(0);
              }}
            >
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
        </div>
      </section>
      <section className="mb-6 rounded-md bg-white p-6 shadow-md">
        <h2 className="mb-4 text-lg font-semibold">실적 목록 · {total}건</h2>
        {loading ? (
          <LoadingState />
        ) : rows.length === 0 ? (
          <EmptyState />
        ) : (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th className="p-3">교원</th>
                  <th className="p-3">관리항목</th>
                  <th className="p-3">평가연도</th>
                  <th className="p-3">발생일</th>
                  <th className="p-3">상태</th>
                  <th className="p-3">상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`employment-improvement-row-${row.achievementId}`}
                  >
                    <td className="p-3">{row.teacherName}</td>
                    <td className="p-3">{row.managementItemCode}</td>
                    <td className="p-3">{row.evaluationYear}</td>
                    <td className="p-3">{row.achievementDate}</td>
                    <td className="p-3">{row.achievementStatus}</td>
                    <td className="p-3">
                      <button
                        data-testid={`employment-improvement-detail-${row.achievementId}`}
                        className="text-primary"
                        disabled={busy}
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
        )}
        <div className="mt-4 flex gap-4">
          <button
            data-testid="employment-improvement-previous"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            data-testid="employment-improvement-next"
            disabled={(page + 1) * size >= total || loading}
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="employment-improvement-form-panel"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">
            {selected ? "실적 상세·수정" : "실적 등록"}
          </h2>
          {(admin || user?.roles.includes("R01")) && (
            <button
              data-testid="employment-improvement-new"
              className="text-primary"
              disabled={busy}
              onClick={() => {
                setSelected(null);
                setForm(empty);
                setFields({});
                setSuccess(null);
              }}
            >
              신규 등록
            </button>
          )}
        </div>
        {locked && (
          <p className="mb-4 text-warning">
            현재 상태에서는 수정할 수 없습니다. 평가확정 실적은 잠겨 있습니다.
          </p>
        )}
        {!canWrite && (
          <p className="mb-4 text-muted">
            조회 전용입니다. 본인 실적만 수정할 수 있습니다.
          </p>
        )}
        <div className="grid gap-4 md:grid-cols-2">
          {field("managementItemCode", "관리항목 *")}
          {field("evaluationYear", "평가연도", "text", !!selected)}
          {field("achievementDate", "업적발생일 *", "date")}
          {field("achievementName", "실적명")}
          {field("specialLectureStartDate", "특강 시작일", "date")}
          {field("specialLectureEndDate", "특강 종료일", "date")}
          {field("mockExamQuestionPeriod", "모의시험 출제기간")}
          {field("attachmentRef", "기존 첨부참조")}
          {field("achievementDetail", "관리항목 상세 (JSON)")}
          {field("attachmentIds", "기존 첨부 식별자 (줄바꿈 구분)")}
        </div>
        {canWrite && (
          <button
            data-testid="employment-improvement-save"
            className="mt-6 rounded bg-primary px-4 py-2 text-white"
            disabled={locked || busy}
            onClick={() => void save()}
          >
            {busy ? "처리 중" : "저장"}
          </button>
        )}
      </section>
    </div>
  );
}
