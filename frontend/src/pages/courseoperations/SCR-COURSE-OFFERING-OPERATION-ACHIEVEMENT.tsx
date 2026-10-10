import { useEffect, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import {
  courseOperationApi,
  type CourseOperation,
  type CourseOperationList,
} from "./courseOperationApi";

const emptyForm = {
  managementItemCode: "",
  achievementDate: "",
  evaluationYear: "",
  performanceDetails: "",
  attachments: "",
};
const editableStatuses = [
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
];

export function CourseOperationManagementPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes("R09");
  const canRead =
    admin || roles.some((role) => ["R01", "R02", "R04"].includes(role));
  const canWrite = admin || roles.includes("R01");
  const [data, setData] = useState<CourseOperationList | null>(null);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [filters, setFilters] = useState({
    managementNo: "",
    teacherName: "",
    managementItemCode: "",
  });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [tab, setTab] = useState<"detail" | "attachment">("detail");
  const locked =
    selected !== null && !editableStatuses.includes(selected.achievementStatus);
  const otherOwner =
    selected !== null && !admin && selected.teacherUserId !== user?.userId;
  const readOnly = !canWrite || locked || otherOwner || detailLoading || saving;

  function handleError(caught: unknown) {
    if (caught instanceof ApiClientError) {
      setError(caught.message);
      setDenied(caught.status === 403);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    } else {
      setError("실적을 처리하지 못했습니다. 다시 시도하세요.");
    }
  }

  async function load(nextPage = page) {
    setLoading(true);
    setError(null);
    setDenied(false);
    try {
      const params = new URLSearchParams({
        page: String(nextPage),
        pageSize: String(pageSize),
      });
      Object.entries(filters).forEach(([key, value]) => {
        if (value.trim()) params.set(key, value.trim());
      });
      const response = await courseOperationApi.list(params);
      setData(response.data ?? null);
    } catch (caught) {
      setData(null);
      handleError(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (canRead) void load();
    else setLoading(false);
  }, [page, pageSize, canRead]);

  function showDetail(row: CourseOperation) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      evaluationYear: row.evaluationYear,
      performanceDetails: row.performanceDetails,
      attachments: (row.attachmentIds ?? []).join("\n"),
    });
  }

  async function select(id: number) {
    setDetailLoading(true);
    setError(null);
    setSuccess(null);
    setFields({});
    try {
      const response = await courseOperationApi.detail(id);
      if (response.data) showDetail(response.data);
    } catch (caught) {
      setSelected(null);
      setForm(emptyForm);
      handleError(caught);
    } finally {
      setDetailLoading(false);
    }
  }

  function reset() {
    setSelected(null);
    setForm(emptyForm);
    setError(null);
    setSuccess(null);
    setFields({});
  }

  async function save(submit = false) {
    if (readOnly) return;
    const invalid: Record<string, string> = {};
    if (!form.managementItemCode)
      invalid.managementItemCode = "관리항목을 선택하세요.";
    if (!form.achievementDate)
      invalid.achievementDate = "업적발생일을 입력하세요.";
    if (!form.performanceDetails.trim())
      invalid.performanceDetails = "실적내역을 입력하세요.";
    if (form.evaluationYear && !/^\d{4}$/.test(form.evaluationYear))
      invalid.evaluationYear = "YYYY 형식으로 입력하세요.";
    setFields(invalid);
    if (Object.keys(invalid).length > 0) return;
    if (
      !window.confirm(
        submit
          ? "수정한 실적을 제출하시겠습니까?"
          : "강좌 운영 실적을 저장하시겠습니까?",
      )
    )
      return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const body = {
        managementItemCode: form.managementItemCode,
        achievementDate: form.achievementDate,
        performanceDetails: form.performanceDetails.trim(),
        // Evaluation year is immutable on update, even when the achievement date changes year.
        ...(!selected && form.evaluationYear
          ? { evaluationYear: form.evaluationYear }
          : {}),
        attachmentIds: form.attachments
          .split("\n")
          .map((token) => token.trim())
          .filter(Boolean),
        ...(submit ? { achievementStatus: "SUBMITTED" } : {}),
      };
      const response = selected
        ? await courseOperationApi.update(selected.achievementId, body)
        : await courseOperationApi.create(body);
      if (response.data?.achievement) {
        const detail = await courseOperationApi.detail(
          response.data.achievement.achievementId,
        );
        if (detail.data) showDetail(detail.data);
      }
      await load();
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  }

  if (!canRead || denied) {
    return (
      <section data-testid="course-operation-page">
        <PermissionState
          title="강좌 운영 실적 권한이 없습니다"
          message="권한과 대상 범위를 확인하세요."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
      data-testid="course-operation-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
      </header>
      {error && <ErrorState title="실적 처리 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      <form
        className="flex flex-wrap items-end gap-3 rounded-md border border-ld bg-white p-5"
        onSubmit={(event) => {
          event.preventDefault();
          if (page !== 0) setPage(0);
          else void load(0);
        }}
      >
        {(["managementNo", "teacherName", "managementItemCode"] as const).map(
          (key, index) => (
            <label key={key} className="text-sm">
              {["관리번호", "교원", "관리항목코드"][index]}
              <input
                className="mt-1 block border px-3 py-2"
                data-testid={`course-filter-${key.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}`}
                value={filters[key]}
                onChange={(event) =>
                  setFilters({ ...filters, [key]: event.target.value })
                }
              />
            </label>
          ),
        )}
        <button
          className="btn-primary"
          data-testid="course-search"
          disabled={loading}
        >
          조회
        </button>
        <label className="text-sm">
          표시 건수
          <select
            className="ml-2 border px-3 py-2"
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
        </label>
      </form>
      {loading ? (
        <LoadingState
          title="실적 조회 중"
          message="목록을 불러오고 있습니다."
        />
      ) : (
        <section className="rounded-md border border-ld bg-white p-5">
          <h2 className="font-semibold">
            실적 목록 · {data?.totalElements ?? 0}건
          </h2>
          {!data?.achievements.length ? (
            <EmptyState
              title="실적이 없습니다"
              message="검색조건을 확인하세요."
            />
          ) : (
            <div className="mt-3 overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    {[
                      "관리번호",
                      "교원",
                      "관리항목",
                      "발생일",
                      "상태",
                      "상세",
                    ].map((name) => (
                      <th key={name}>{name}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {data.achievements.map((row) => (
                    <tr
                      key={row.achievementId}
                      data-testid={`course-row-${row.achievementId}`}
                    >
                      <td>{row.managementNo}</td>
                      <td>{row.teacherName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.achievementDate}</td>
                      <td>{row.achievementStatus}</td>
                      <td>
                        <button
                          className="text-primary"
                          data-testid={`course-detail-${row.achievementId}`}
                          disabled={detailLoading || saving}
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
          <div className="mt-4 flex items-center gap-3 text-sm">
            <button
              data-testid="course-previous"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <span>{page + 1} 페이지</span>
            <button
              data-testid="course-next"
              disabled={(page + 1) * pageSize >= (data?.totalElements ?? 0)}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </div>
        </section>
      )}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-form-panel"
      >
        <div className="flex items-center justify-between gap-3">
          <h2 className="font-semibold">
            {selected
              ? `실적 상세 · ${selected.managementNo}`
              : "실적 신규 등록"}
          </h2>
          {canWrite && (
            <button
              data-testid="course-new"
              disabled={saving || detailLoading}
              onClick={reset}
            >
              신규 등록
            </button>
          )}
        </div>
        {detailLoading && (
          <LoadingState
            title="상세 조회 중"
            message="선택한 실적을 다시 조회합니다."
          />
        )}
        {locked && (
          <p className="mt-3 text-warning" data-testid="course-lock">
            제출·확인·인증·평가확정 실적은 수정할 수 없습니다.
          </p>
        )}
        {(!canWrite || otherOwner) && (
          <p className="mt-3 text-muted">조회 전용입니다.</p>
        )}
        <div
          className="my-4 flex gap-4"
          role="tablist"
          aria-label="실적 상세 탭"
        >
          <button
            role="tab"
            aria-selected={tab === "detail"}
            data-testid="course-detail-tab"
            onClick={() => setTab("detail")}
          >
            기본 정보
          </button>
          <button
            role="tab"
            aria-selected={tab === "attachment"}
            data-testid="course-attachment-tab"
            onClick={() => setTab("attachment")}
          >
            첨부 참조
          </button>
        </div>
        {tab === "detail" ? (
          <div
            className="grid gap-4 md:grid-cols-2"
            role="tabpanel"
            data-testid="course-detail-panel"
          >
            <label className="text-sm">
              관리항목 *
              <select
                className="mt-1 block w-full border px-3 py-2"
                data-testid="course-management-item"
                disabled={readOnly}
                value={form.managementItemCode}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
              >
                <option value="">선택하세요</option>
                {(data?.managementItems ?? []).map((item) => (
                  <option
                    key={item.managementItemCode}
                    value={item.managementItemCode}
                  >
                    {item.managementItemName}
                  </option>
                ))}
              </select>
              {fields.managementItemCode && (
                <span className="text-error">{fields.managementItemCode}</span>
              )}
            </label>
            <label className="text-sm">
              업적발생일 *
              <input
                className="mt-1 block w-full border px-3 py-2"
                type="date"
                data-testid="course-achievement-date"
                disabled={readOnly}
                value={form.achievementDate}
                onChange={(event) =>
                  setForm({ ...form, achievementDate: event.target.value })
                }
              />
              {fields.achievementDate && (
                <span className="text-error">{fields.achievementDate}</span>
              )}
            </label>
            <label className="text-sm">
              평가연도 (신규 생략 시 발생일 연도)
              <input
                className="mt-1 block w-full border px-3 py-2"
                data-testid="course-evaluation-year"
                disabled={readOnly || selected !== null}
                value={form.evaluationYear}
                onChange={(event) =>
                  setForm({ ...form, evaluationYear: event.target.value })
                }
              />
              {fields.evaluationYear && (
                <span className="text-error">{fields.evaluationYear}</span>
              )}
            </label>
            <label className="text-sm md:col-span-2">
              실적내역 *
              <textarea
                className="mt-1 block w-full border px-3 py-2"
                rows={5}
                data-testid="course-performance-details"
                disabled={readOnly}
                value={form.performanceDetails}
                onChange={(event) =>
                  setForm({ ...form, performanceDetails: event.target.value })
                }
              />
              {fields.performanceDetails && (
                <span className="text-error">{fields.performanceDetails}</span>
              )}
            </label>
          </div>
        ) : (
          <div role="tabpanel" data-testid="course-attachment-panel">
            <label className="text-sm">
              보관된 첨부 참조 (한 줄에 하나)
              <textarea
                className="mt-1 block w-full border px-3 py-2"
                data-testid="course-attachments"
                disabled={readOnly}
                value={form.attachments}
                onChange={(event) =>
                  setForm({ ...form, attachments: event.target.value })
                }
              />
              {fields.attachmentIds && (
                <span className="text-error">{fields.attachmentIds}</span>
              )}
            </label>
          </div>
        )}
        {canWrite && (
          <div className="mt-5 flex gap-3">
            <button
              className="btn-primary"
              data-testid="course-save"
              disabled={readOnly || loading}
              onClick={() => void save()}
            >
              {saving ? "저장 중" : "저장"}
            </button>
            {selected && !locked && !otherOwner && (
              <button
                data-testid="course-submit"
                disabled={readOnly}
                onClick={() => void save(true)}
              >
                제출/재제출
              </button>
            )}
          </div>
        )}
      </section>
    </section>
  );
}
