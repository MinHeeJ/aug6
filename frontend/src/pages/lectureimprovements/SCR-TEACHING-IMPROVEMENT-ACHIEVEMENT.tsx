import { useEffect, useRef, useState, type FormEvent } from "react";
import { RefreshCw, Save, Search } from "lucide-react";
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
  userId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  performanceContent: string;
  academicYear: string | number;
  semester: string | number;
  attachmentRef?: string | null;
  achievementStatus: string;
};
type ListResult = { achievements: LectureImprovement[]; totalElements: number };
type SaveResult = {
  achievement: LectureImprovement;
  occurredDateWarning?: boolean;
  warningMessage?: string | null;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceContent: string;
  academicYear: string;
  semester: string;
  attachmentRef: string;
};
const blank: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceContent: "",
  academicYear: "",
  semester: "",
  attachmentRef: "",
};
const labels: Record<keyof Form, string> = {
  managementItemCode: "관리항목 코드",
  achievementDate: "업적발생일",
  performanceContent: "실적내용",
  academicYear: "학년도",
  semester: "학기",
  attachmentRef: "첨부 참조",
};
function fieldLabel(key: keyof Form) {
  return `${labels[key]}${key !== "attachmentRef" ? " *" : ""}`;
}
const editableStatuses = new Set([
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
]);
const statusLabels: Record<string, string> = {
  DRAFT: "작성중",
  SUBMITTED: "제출",
  DEPARTMENT_CONFIRMED: "학과장확인",
  DEPARTMENT_REJECTED: "학과장미승인",
  CERTIFIED: "인증",
  CERTIFICATION_REJECTED: "인증반려",
  EVALUATION_CONFIRMED: "평가확정",
  DELETED: "삭제",
};
const endpoint = "/api/business/lecture-improvements" as const;
const button =
  "inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";
const input =
  "w-full rounded-md border border-ld bg-transparent px-3 py-2 text-sm";

export function LectureImprovementsPage() {
  const { user } = useAuth();
  const canRead = !!user?.roles.some((role) =>
    ["R01", "R02", "R04"].includes(role),
  );
  const canCreate = !!user?.roles.includes("R01");
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState({
    page: 0,
    pageSize: 20,
    managementItemCode: "",
    academicYear: "",
    semester: "",
  });
  const [filters, setFilters] = useState({
    managementItemCode: "",
    academicYear: "",
    semester: "",
  });
  const [form, setForm] = useState<Form>(blank);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [detailFailed, setDetailFailed] = useState(false);
  const listVersion = useRef(0);
  const detailVersion = useRef(0);
  const writable =
    canCreate &&
    !denied &&
    !detailFailed &&
    !detailLoading &&
    !saving &&
    (!selected ||
      (selected.userId === user?.userId &&
        editableStatuses.has(selected.achievementStatus)));

  function handleError(caught: unknown) {
    setSuccess(null);
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setDenied(true);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의개선 실적을 처리하지 못했습니다.",
    );
  }
  async function load() {
    const version = ++listVersion.current;
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        page: String(query.page),
        pageSize: String(query.pageSize),
      });
      for (const key of [
        "managementItemCode",
        "academicYear",
        "semester",
      ] as const) {
        if (query[key].trim()) params.set(key, query[key].trim());
      }
      const response = await apiRequest<ListResult>(`${endpoint}?${params}`);
      if (version !== listVersion.current) return;
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      if (version !== listVersion.current) return;
      setRows([]);
      setTotal(0);
      handleError(caught);
    } finally {
      if (version === listVersion.current) setLoading(false);
    }
  }
  useEffect(() => {
    if (canRead) void load();
    else setLoading(false);
    return () => {
      listVersion.current++;
    };
  }, [query, canRead]);
  useEffect(
    () => () => {
      detailVersion.current++;
    },
    [],
  );

  function applyDetail(row: LectureImprovement) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      performanceContent: row.performanceContent,
      academicYear: String(row.academicYear),
      semester: String(row.semester),
      attachmentRef: row.attachmentRef ?? "",
    });
  }
  async function readDetail(id: number) {
    const version = ++detailVersion.current;
    setDetailLoading(true);
    setDetailFailed(false);
    setSelected(null);
    setForm(blank);
    setError(null);
    setSuccess(null);
    setFieldErrors({});
    try {
      const response = await apiRequest<LectureImprovement>(
        `${endpoint}/${id}`,
      );
      if (version !== detailVersion.current) return;
      if (!response.data) throw new Error("상세 실적을 찾을 수 없습니다.");
      applyDetail(response.data);
    } catch (caught) {
      if (version !== detailVersion.current) return;
      setDetailFailed(true);
      handleError(caught);
    } finally {
      if (version === detailVersion.current) setDetailLoading(false);
    }
  }
  function newRecord() {
    detailVersion.current++;
    setDetailLoading(false);
    setDetailFailed(false);
    setSelected(null);
    setForm(blank);
    setFieldErrors({});
    setError(null);
    setSuccess(null);
  }
  async function save(event: FormEvent) {
    event.preventDefault();
    if (!writable) return;
    setSuccess(null);
    setError(null);
    const errors: Record<string, string> = {};
    const requiredFields = [
      "managementItemCode",
      "achievementDate",
      "performanceContent",
      "academicYear",
      "semester",
    ] as const;
    for (const key of requiredFields) {
      if (!form[key].trim()) errors[key] = `${labels[key]}을(를) 입력하세요.`;
    }
    if (
      form.academicYear &&
      (!/^\d{4}$/.test(form.academicYear) || Number(form.academicYear) < 2000)
    ) {
      errors.academicYear = "학년도는 2000 이상의 네 자리 연도여야 합니다.";
    }
    if (form.semester && !["1", "2"].includes(form.semester))
      errors.semester = "학기는 1 또는 2여야 합니다.";
    setFieldErrors(errors);
    if (Object.keys(errors).length) {
      setError("입력값을 확인하세요.");
      return;
    }
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    setSaving(true);
    try {
      const response = await apiRequest<SaveResult>(
        selected ? `${endpoint}/${selected.achievementId}` : endpoint,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode.trim(),
            achievementDate: form.achievementDate,
            performanceContent: form.performanceContent.trim(),
            academicYear: Number(form.academicYear),
            semester: Number(form.semester),
            attachmentRef: form.attachmentRef.trim() || null,
          }),
        },
      );
      if (response.data?.achievement) applyDetail(response.data.achievement);
      else {
        setSelected(null);
        setForm(blank);
      }
      await load();
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "업적발생일이 평가기간 밖입니다. 경고와 함께 저장되었습니다.")
          : "강의개선 실적이 저장되었습니다.",
      );
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  }

  if (!canRead || denied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-IMPROVEMENTS"
        data-testid="lecture-improvements-page"
      >
        <PermissionState
          title="강의개선 실적 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-IMPROVEMENTS"
      data-testid="lecture-improvements-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의개선 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          실적내용과 학년도·학기를 조회하고 저장합니다. 인증상태는 서버에서
          관리합니다.
        </p>
      </div>
      {error && <ErrorState title="강의개선 실적 오류" message={error} />}
      {success && <SuccessState message={success} />}
      <form
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        onSubmit={(event) => {
          event.preventDefault();
          setQuery({ ...query, ...filters, page: 0 });
        }}
      >
        <h2 className="mb-4 text-lg font-semibold text-dark">조회 조건</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <label className="text-sm">
            관리항목 검색
            <input
              className={input}
              data-testid="lecture-improvements-management-item-filter"
              value={filters.managementItemCode}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  managementItemCode: event.target.value,
                })
              }
            />
          </label>
          <label className="text-sm">
            학년도 검색
            <input
              className={input}
              data-testid="lecture-improvements-academic-year-filter"
              type="number"
              value={filters.academicYear}
              onChange={(event) =>
                setFilters({ ...filters, academicYear: event.target.value })
              }
            />
          </label>
          <label className="text-sm">
            학기 검색
            <select
              className={input}
              data-testid="lecture-improvements-semester-filter"
              value={filters.semester}
              onChange={(event) =>
                setFilters({ ...filters, semester: event.target.value })
              }
            >
              <option value="">전체</option>
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </label>
        </div>
        <div className="mt-4 flex gap-3">
          <button
            className={button}
            data-testid="lecture-improvements-search-button"
            disabled={loading || saving}
            type="submit"
          >
            <Search size={16} />
            조회
          </button>
          <button
            className={button}
            data-testid="lecture-improvements-refresh-button"
            disabled={loading || saving}
            type="button"
            onClick={() => void load()}
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </form>
      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        aria-label="실적 목록"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            실적 목록 ({total}건)
          </h2>
          <label>
            페이지 크기
            <select
              className={input}
              data-testid="lecture-improvements-page-size"
              disabled={saving}
              value={query.pageSize}
              onChange={(event) =>
                setQuery({
                  ...query,
                  page: 0,
                  pageSize: Number(event.target.value),
                })
              }
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
          <LoadingState message="강의개선 실적 목록을 불러오고 있습니다." />
        ) : !rows.length ? (
          <EmptyState />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-lightgray">
                <tr>
                  {[
                    "관리번호",
                    "성명",
                    "관리항목",
                    "업적발생일",
                    "학년도",
                    "학기",
                    "인증상태",
                    "상세",
                  ].map((label) => (
                    <th className="p-3" key={label}>
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`lecture-improvements-row-${row.achievementId}`}
                    className="border-b border-ld"
                  >
                    <td className="p-3">{row.managementNo}</td>
                    <td className="p-3">{row.teacherName}</td>
                    <td className="p-3">{row.managementItemCode}</td>
                    <td className="p-3">{row.achievementDate}</td>
                    <td className="p-3">{row.academicYear}</td>
                    <td className="p-3">{row.semester}</td>
                    <td className="p-3">
                      {statusLabels[row.achievementStatus] ??
                        row.achievementStatus}
                    </td>
                    <td className="p-3">
                      <button
                        type="button"
                        className={button}
                        data-testid={`lecture-improvements-detail-${row.achievementId}`}
                        disabled={saving}
                        aria-label={`${row.managementNo} 상세`}
                        onClick={() => void readDetail(row.achievementId)}
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
        <nav
          className="mt-4 flex items-center justify-end gap-3"
          aria-label="목록 페이지"
        >
          <button
            className={button}
            data-testid="lecture-improvements-previous-button"
            type="button"
            disabled={loading || saving || query.page === 0}
            onClick={() => setQuery({ ...query, page: query.page - 1 })}
          >
            이전
          </button>
          <span>{query.page + 1} 페이지</span>
          <button
            className={button}
            data-testid="lecture-improvements-next-button"
            type="button"
            disabled={
              loading || saving || (query.page + 1) * query.pageSize >= total
            }
            onClick={() => setQuery({ ...query, page: query.page + 1 })}
          >
            다음
          </button>
        </nav>
      </section>
      <form
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        onSubmit={(event) => void save(event)}
        noValidate
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            {selected ? "실적 상세·수정" : "실적 등록"}
          </h2>
          <button
            className={button}
            data-testid="lecture-improvements-create-button"
            type="button"
            disabled={!canCreate || saving}
            onClick={newRecord}
          >
            신규 등록
          </button>
        </div>
        {detailLoading && (
          <LoadingState message="선택한 실적의 상세를 불러오고 있습니다." />
        )}
        {selected && (
          <p className="mb-4 text-sm">
            인증상태:{" "}
            <span>
              {statusLabels[selected.achievementStatus] ??
                selected.achievementStatus}
            </span>{" "}
            (서버 관리)
          </p>
        )}
        {!writable && !detailLoading && !saving && (
          <p className="mb-4 text-sm text-warning">
            조회 전용입니다. R01 역할의 본인 실적 중 작성중·반려 상태만 수정할
            수 있습니다.
          </p>
        )}
        <fieldset disabled={!writable} className="grid gap-4 md:grid-cols-2">
          {(Object.keys(labels) as (keyof Form)[]).map((key) => (
            <label
              className={
                key === "performanceContent"
                  ? "text-sm md:col-span-2"
                  : "text-sm"
              }
              key={key}
              htmlFor={`lecture-${key}`}
            >
              {labels[key]}
              {key !== "attachmentRef" && " *"}
              {key === "performanceContent" ? (
                <textarea
                  id={`lecture-${key}`}
                  data-testid="lecture-improvements-performance-content-input"
                  aria-label={fieldLabel(key)}
                  className={input}
                  rows={5}
                  value={form[key]}
                  aria-invalid={!!fieldErrors[key]}
                  aria-describedby={
                    fieldErrors[key] ? `lecture-error-${key}` : undefined
                  }
                  onChange={(event) =>
                    setForm({ ...form, [key]: event.target.value })
                  }
                />
              ) : key === "semester" ? (
                <select
                  id={`lecture-${key}`}
                  data-testid="lecture-improvements-semester-input"
                  aria-label={fieldLabel(key)}
                  className={input}
                  value={form[key]}
                  aria-invalid={!!fieldErrors[key]}
                  aria-describedby={
                    fieldErrors[key] ? `lecture-error-${key}` : undefined
                  }
                  onChange={(event) =>
                    setForm({ ...form, [key]: event.target.value })
                  }
                >
                  <option value="">선택</option>
                  <option value="1">1학기</option>
                  <option value="2">2학기</option>
                </select>
              ) : (
                <input
                  id={`lecture-${key}`}
                  data-testid={`lecture-improvements-${key.replace(
                    /[A-Z]/g,
                    (letter) => `-${letter.toLowerCase()}`,
                  )}-input`}
                  aria-label={fieldLabel(key)}
                  className={input}
                  type={
                    key === "achievementDate"
                      ? "date"
                      : key === "academicYear"
                        ? "number"
                        : "text"
                  }
                  value={form[key]}
                  aria-invalid={!!fieldErrors[key]}
                  aria-describedby={
                    fieldErrors[key] ? `lecture-error-${key}` : undefined
                  }
                  onChange={(event) =>
                    setForm({ ...form, [key]: event.target.value })
                  }
                />
              )}
              {fieldErrors[key] && (
                <span
                  id={`lecture-error-${key}`}
                  className="mt-1 block text-error"
                  role="alert"
                >
                  {fieldErrors[key]}
                </span>
              )}
            </label>
          ))}
        </fieldset>
        <p className="mt-3 text-sm text-muted">
          첨부 참조는 기존 첨부의 참조값입니다. 파일 업로드 및 상태 변경은 이
          화면에서 제공하지 않습니다.
        </p>
        <button
          type="submit"
          className={`${button} mt-4`}
          data-testid="lecture-improvements-save-button"
          disabled={!writable}
        >
          <Save size={16} />
          {saving ? "저장 중…" : "저장"}
        </button>
      </form>
    </section>
  );
}
