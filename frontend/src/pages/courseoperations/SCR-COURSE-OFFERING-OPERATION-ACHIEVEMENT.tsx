import { useEffect, useState, type ReactNode } from "react";
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

export type CourseOperation = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentRef?: string | null;
};
type SearchResult = { achievements: CourseOperation[]; totalElements: number };
type SaveResult = {
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  attachmentRef: string;
};
const initialForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  attachmentRef: "",
};
const initialFilters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  achievementStatus: "",
};
const editable = ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"];
const buttonClass =
  "rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary";

/** Course-operation form reuses the existing shell and state components; immutable lifecycle fields stay read-only. */
export function CourseOperationsPage({ user }: { user: CurrentUser | null }) {
  const readable = !!user?.roles.some((role) =>
    ["R01", "R02", "R04"].includes(role),
  );
  const [filters, setFilters] = useState(initialFilters);
  const [query, setQuery] = useState(initialFilters);
  const [form, setForm] = useState<Form>(initialForm);
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState("");
  const [warning, setWarning] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const canEdit =
    !!user?.roles.includes("R01") &&
    (!selected ||
      (selected.teacherUserId === user.userId &&
        editable.includes(selected.achievementStatus)));

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
    }
    setError(
      caught instanceof Error ? caught.message : "실적을 처리하지 못했습니다.",
    );
  };

  const load = async () => {
    if (!readable) return;
    setLoading(true);
    setError("");
    setDenied(false);
    try {
      const params = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      Object.entries(query).forEach(([key, value]) => {
        if (value.trim()) params.set(key, value.trim());
      });
      const response = await apiRequest<SearchResult>(
        `/api/business/course-operations?${params}`,
      );
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      setTotal(0);
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [query, page, pageSize, readable]);

  const select = async (id: number) => {
    setBusy(true);
    setError("");
    setFields({});
    setSuccess("");
    setWarning("");
    // Clear the old selection while fetching so an error cannot leave another row editable.
    setSelected(null);
    setForm(initialForm);
    try {
      const response = await apiRequest<CourseOperation>(
        `/api/business/course-operations/${id}`,
      );
      if (!response.data) throw new Error("실적 상세를 찾을 수 없습니다.");
      const row = response.data;
      setSelected(row);
      setForm({
        managementItemCode: row.managementItemCode,
        achievementDate: row.achievementDate,
        performanceDetails: row.performanceDetails,
        attachmentRef: row.attachmentRef ?? "",
      });
    } catch (caught) {
      handleError(caught);
    } finally {
      setBusy(false);
    }
  };

  const save = async () => {
    if (!canEdit || busy) return;
    const required = [
      "managementItemCode",
      "achievementDate",
      "performanceDetails",
    ] as const;
    const errors = Object.fromEntries(
      required
        .filter((key) => !form[key].trim())
        .map((key) => [key, "필수 입력 항목입니다."]),
    );
    setFields(errors);
    if (
      Object.keys(errors).length ||
      !window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError("");
    setSuccess("");
    setWarning("");
    try {
      const path = selected
        ? `/api/business/course-operations/${selected.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<SaveResult>(path as `/api/${string}`, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          performanceDetails: form.performanceDetails.trim(),
          attachmentRef: form.attachmentRef.trim() || null,
        }),
      });
      if (!response.data) throw new Error("저장 결과를 확인할 수 없습니다.");
      setSelected(response.data.achievement);
      setSuccess("실적이 저장되었습니다.");
      setWarning(
        response.data.occurredDateWarning
          ? (response.data.warningMessage ?? "평가대상 기간 밖입니다.")
          : "",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OPERATIONS"
      data-testid="course-operations-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          관리항목별 실적내역과 상태를 확인하고 저장합니다.
        </p>
      </div>
      {!readable || denied ? (
        <PermissionState
          title="권한이 없습니다"
          message={error || "R01, R02, R04 권한이 필요합니다."}
        />
      ) : (
        <>
          {error ? <ErrorState title="실적 처리 오류" message={error} /> : null}
          {success ? (
            <SuccessState title="처리 완료" message={success} />
          ) : null}
          {warning ? (
            <p className="text-sm text-warning" role="status">
              {warning}
            </p>
          ) : null}
          <section
            className="rounded-md border border-ld bg-white p-5"
            data-testid="course-search-panel"
          >
            <h2 className="mb-4 font-semibold">검색조건</h2>
            <div className="grid gap-4 md:grid-cols-4">
              {(
                [
                  ["managementNo", "관리번호"],
                  ["teacherName", "성명"],
                  ["managementItemCode", "관리항목"],
                  ["achievementStatus", "인증상태"],
                ] as const
              ).map(([key, label]) => (
                <Field key={key} label={label}>
                  <input
                    data-testid={`course-filter-${key.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`}
                    value={filters[key]}
                    onChange={(event) =>
                      setFilters({ ...filters, [key]: event.target.value })
                    }
                  />
                </Field>
              ))}
            </div>
            <button
              type="button"
              className={`${buttonClass} mt-4`}
              data-testid="course-search-button"
              onClick={() => {
                setPage(0);
                setQuery({ ...filters });
              }}
            >
              조회
            </button>
          </section>
          <section
            className="rounded-md border border-ld bg-white p-5"
            data-testid="course-list-panel"
          >
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 className="font-semibold">실적 목록 ({total}건)</h2>
              <Field label="표시 건수">
                <select
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
              </Field>
            </div>
            {loading ? (
              <LoadingState />
            ) : rows.length === 0 ? (
              <EmptyState />
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead>
                    <tr>
                      {[
                        "관리번호",
                        "성명",
                        "관리항목",
                        "발생일",
                        "인증상태",
                        "상세",
                      ].map((label) => (
                        <th key={label} className="px-3 py-2">
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
                        <td className="px-3 py-2">{row.managementNo}</td>
                        <td className="px-3 py-2">{row.teacherName}</td>
                        <td className="px-3 py-2">{row.managementItemCode}</td>
                        <td className="px-3 py-2">{row.achievementDate}</td>
                        <td className="px-3 py-2">{row.achievementStatus}</td>
                        <td className="px-3 py-2">
                          <button
                            type="button"
                            data-testid={`course-detail-${row.achievementId}`}
                            disabled={busy}
                            onClick={() => void select(row.achievementId)}
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
              <button
                type="button"
                data-testid="course-previous-page"
                disabled={page === 0 || loading}
                onClick={() => setPage(page - 1)}
              >
                이전
              </button>
              <span>{page + 1} 페이지</span>
              <button
                type="button"
                data-testid="course-next-page"
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
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="font-semibold">
                {selected ? "실적 상세 / 수정" : "새 실적 등록"}
              </h2>
              {user?.roles.includes("R01") ? (
                <button
                  type="button"
                  className={buttonClass}
                  data-testid="course-new-button"
                  disabled={busy}
                  onClick={() => {
                    setSelected(null);
                    setForm(initialForm);
                    setFields({});
                    setError("");
                  }}
                >
                  신규
                </button>
              ) : null}
            </div>
            {selected ? (
              <p className="mb-4 text-sm">
                관리번호: {selected.managementNo} / 평가연도:{" "}
                {selected.evaluationYear}/ 상태: {selected.achievementStatus}
              </p>
            ) : null}
            {!canEdit ? (
              <p role="status">
                읽기 전용입니다. 본인의 작성중·반려 실적만 수정할 수 있습니다.
              </p>
            ) : null}
            <div className="grid gap-4 md:grid-cols-2">
              {(
                [
                  ["managementItemCode", "관리항목 *"],
                  ["achievementDate", "업적발생일 *"],
                  ["performanceDetails", "실적내역 *"],
                  ["attachmentRef", "첨부 참조"],
                ] as const
              ).map(([key, label]) => (
                <Field key={key} label={label} error={fields[key]}>
                  {key === "performanceDetails" ? (
                    <textarea
                      data-testid="course-performance-details"
                      value={form[key]}
                      rows={5}
                      disabled={!canEdit || busy}
                      onChange={(event) =>
                        setForm({ ...form, [key]: event.target.value })
                      }
                    />
                  ) : (
                    <input
                      data-testid={`course-${key.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`}
                      type={key === "achievementDate" ? "date" : "text"}
                      value={form[key]}
                      disabled={!canEdit || busy}
                      maxLength={
                        key === "attachmentRef"
                          ? 300
                          : key === "managementItemCode"
                            ? 50
                            : undefined
                      }
                      onChange={(event) =>
                        setForm({ ...form, [key]: event.target.value })
                      }
                    />
                  )}
                </Field>
              ))}
            </div>
            <button
              type="button"
              className={`${buttonClass} mt-4`}
              data-testid="course-save-button"
              disabled={!canEdit || busy}
              onClick={() => void save()}
            >
              {busy ? "처리 중…" : "저장"}
            </button>
          </section>
        </>
      )}
    </section>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="flex flex-col gap-2 text-sm">
      <span>{label}</span>
      {children}
      {error ? (
        <span className="text-error" role="alert">
          {error}
        </span>
      ) : null}
    </label>
  );
}
