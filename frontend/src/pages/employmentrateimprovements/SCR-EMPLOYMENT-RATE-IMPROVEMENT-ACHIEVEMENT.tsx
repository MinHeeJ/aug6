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
  type EmploymentRateImprovementInput,
  type ImprovementList,
} from "./employmentRateImprovementApi";

const initial: EmploymentRateImprovementInput = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: null,
  specialLectureEndDate: null,
  mockExamQuestionPeriod: null,
  attachmentRef: null,
};
const editable = ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"];

export function EmploymentRateImprovementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const [data, setData] = useState<ImprovementList | null>(null);
  const [selected, setSelected] = useState<EmploymentRateImprovement | null>(
    null,
  );
  const [form, setForm] = useState(initial);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [item, setItem] = useState("");
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState("");
  const [denied, setDenied] = useState(false);
  const admitted = user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const writer = user?.roles.some((role) => ["R01", "R09"].includes(role));
  const locked =
    !!selected &&
    (!editable.includes(selected.achievementStatus) ||
      (selected.teacherUserId !== user?.userId &&
        !user?.roles.includes("R09")));

  function failure(caught: unknown) {
    setError(caught instanceof Error ? caught.message : "처리하지 못했습니다.");
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      const values = caught.apiError?.fields;
      setFields(
        Array.isArray(values)
          ? Object.fromEntries(
              values.map((value) => [value.field, value.message]),
            )
          : ((values ?? {}) as Record<string, string>),
      );
    }
  }

  async function load() {
    if (!admitted) return;
    setLoading(true);
    setError("");
    try {
      setData((await api.list(page, pageSize, item)).data ?? null);
    } catch (caught) {
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [page, pageSize, admitted]);

  async function select(row: EmploymentRateImprovement) {
    setBusy(true);
    setError("");
    setSuccess("");
    setFields({});
    try {
      const response = await api.get(row.achievementId);
      if (response.data) {
        setSelected(response.data);
        const value = response.data;
        setForm({
          managementItemCode: value.managementItemCode,
          achievementDate: value.achievementDate,
          specialLectureStartDate: value.specialLectureStartDate ?? null,
          specialLectureEndDate: value.specialLectureEndDate ?? null,
          mockExamQuestionPeriod: value.mockExamQuestionPeriod ?? null,
          attachmentRef: value.attachmentRef ?? null,
        });
      }
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save() {
    setFields({});
    setSuccess("");
    const invalid: Record<string, string> = {};
    if (!form.managementItemCode)
      invalid.managementItemCode = "관리항목은 필수입니다.";
    if (!form.achievementDate)
      invalid.achievementDate = "업적발생일은 필수입니다.";
    if (
      !!form.specialLectureStartDate !== !!form.specialLectureEndDate ||
      (form.specialLectureStartDate &&
        form.specialLectureEndDate &&
        form.specialLectureEndDate < form.specialLectureStartDate)
    ) {
      invalid.specialLectureEndDate = "특강 기간을 확인하세요.";
    }
    if (Object.keys(invalid).length) {
      setFields(invalid);
      return;
    }
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    setBusy(true);
    setError("");
    try {
      const response = await api.save(selected?.achievementId, form);
      if (response.data) {
        setSelected(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "평가대상 기간 밖 발생일 경고와 함께 저장되었습니다.")
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

  if (!admitted || denied)
    return <PermissionState title="취업률 제고 실적 권한이 없습니다" />;

  return (
    <section
      className="space-y-6"
      data-testid="employment-improvement-page"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
      </header>
      {error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      <section className="rounded-md border border-ld bg-white p-5">
        <label>
          관리항목 검색
          <input
            className="ml-3 rounded border border-ld p-2"
            data-testid="improvement-filter"
            value={item}
            onChange={(event) => setItem(event.target.value)}
          />
        </label>
        <button
          className="ml-3 rounded bg-primary px-4 py-2 text-white"
          data-testid="improvement-search"
          onClick={() => {
            if (page === 0) void load();
            else setPage(0);
          }}
        >
          조회
        </button>
        <label className="ml-3">
          표시 건수
          <select
            data-testid="improvement-page-size"
            value={pageSize}
            onChange={(event) => {
              setPage(0);
              setPageSize(Number(event.target.value));
            }}
          >
            {[20, 50, 100].map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>
      </section>
      {loading ? (
        <LoadingState />
      ) : data?.achievements.length ? (
        <div className="overflow-x-auto rounded-md border border-ld bg-white p-5">
          <table className="w-full text-sm">
            <thead>
              <tr>
                <th>관리항목</th>
                <th>평가연도</th>
                <th>발생일</th>
                <th>상태</th>
                <th>상세</th>
              </tr>
            </thead>
            <tbody>
              {data.achievements.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`improvement-row-${row.achievementId}`}
                >
                  <td>{row.managementItemCode}</td>
                  <td>{row.evaluationYear}</td>
                  <td>{row.achievementDate}</td>
                  <td>{row.achievementStatus}</td>
                  <td>
                    <button
                      data-testid={`improvement-detail-${row.achievementId}`}
                      disabled={busy}
                      onClick={() => void select(row)}
                    >
                      상세 조회
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p>총 {data.totalElements}건</p>
        </div>
      ) : (
        !error && <EmptyState />
      )}
      <div className="flex gap-4">
        <button
          data-testid="improvement-prev"
          disabled={page === 0 || loading}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <span>{page + 1} 페이지</span>
        <button
          data-testid="improvement-next"
          disabled={
            loading || (page + 1) * pageSize >= (data?.totalElements ?? 0)
          }
          onClick={() => setPage(page + 1)}
        >
          다음
        </button>
      </div>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="improvement-detail-panel"
      >
        <h2 className="text-lg font-semibold">
          {selected ? "실적 상세" : "실적 등록"}
        </h2>
        {selected && (
          <p>
            평가연도 {selected.evaluationYear} · {selected.achievementStatus}
          </p>
        )}
        {locked && (
          <p role="status">현재 상태 또는 소유권에 따라 수정할 수 없습니다.</p>
        )}
        {!writer && <p>조회 전용 화면입니다.</p>}
        {!data?.managementItems.length && (
          <p>입력 가능한 관리항목 설정이 없습니다.</p>
        )}
        <fieldset
          disabled={!writer || locked || busy}
          className="mt-4 grid gap-4 md:grid-cols-2"
        >
          <label>
            관리항목 *
            <select
              className="block w-full rounded border border-ld p-2"
              data-testid="improvement-item"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">선택하세요</option>
              {data?.managementItems.map((code) => (
                <option key={code} value={code}>
                  {code}
                </option>
              ))}
              {selected &&
                !data?.managementItems.includes(form.managementItemCode) && (
                  <option value={form.managementItemCode}>
                    {form.managementItemCode}
                  </option>
                )}
            </select>
            {fields.managementItemCode && (
              <span role="alert">{fields.managementItemCode}</span>
            )}
          </label>
          <label>
            업적발생일 *
            <input
              className="block w-full rounded border border-ld p-2"
              type="date"
              data-testid="improvement-date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
            {fields.achievementDate && (
              <span role="alert">{fields.achievementDate}</span>
            )}
          </label>
          <label>
            특강 시작일
            <input
              className="block w-full rounded border border-ld p-2"
              type="date"
              data-testid="improvement-start"
              value={form.specialLectureStartDate ?? ""}
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value || null,
                })
              }
            />
          </label>
          <label>
            특강 종료일
            <input
              className="block w-full rounded border border-ld p-2"
              type="date"
              data-testid="improvement-end"
              value={form.specialLectureEndDate ?? ""}
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureEndDate: event.target.value || null,
                })
              }
            />
            {fields.specialLectureEndDate && (
              <span role="alert">{fields.specialLectureEndDate}</span>
            )}
          </label>
          <label>
            모의고사 출제기간
            <textarea
              className="block w-full rounded border border-ld p-2"
              data-testid="improvement-period"
              value={form.mockExamQuestionPeriod ?? ""}
              onChange={(event) =>
                setForm({
                  ...form,
                  mockExamQuestionPeriod: event.target.value || null,
                })
              }
            />
          </label>
          <label>
            첨부 참조
            <input
              className="block w-full rounded border border-ld p-2"
              data-testid="improvement-attachment"
              value={form.attachmentRef ?? ""}
              onChange={(event) =>
                setForm({ ...form, attachmentRef: event.target.value || null })
              }
            />
          </label>
          {writer && (
            <button
              className="rounded bg-primary px-4 py-2 text-white"
              data-testid="improvement-save"
              disabled={!data?.managementItems.length}
              onClick={() => void save()}
            >
              저장
            </button>
          )}
        </fieldset>
        {writer && (
          <button
            className="mt-4"
            data-testid="improvement-new"
            disabled={busy}
            onClick={() => {
              setSelected(null);
              setForm(initial);
              setFields({});
              setSuccess("");
            }}
          >
            신규 등록
          </button>
        )}
      </section>
    </section>
  );
}
