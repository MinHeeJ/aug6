import { useEffect, useState } from "react";
import type { CurrentUser } from "../../api/apiClient";
import { ApiClientError } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import {
  lectureImprovementApi,
  type LectureImprovementRow,
  type ManagementItem,
} from "./lectureImprovementApi";

const empty = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
};
const inputClass =
  "mt-1 w-full rounded-md border border-ld bg-white px-3 py-2 text-sm";
const buttonClass =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";

export function LectureImprovementPage({ user }: { user: CurrentUser | null }) {
  const allowed =
    user?.roles.some((role) => ["R01", "R02", "R04", "R09"].includes(role)) ??
    false;
  const writer =
    user?.roles.some((role) => ["R01", "R09"].includes(role)) ?? false;
  const [form, setForm] = useState(empty);
  const [selected, setSelected] = useState<LectureImprovementRow | null>(null);
  const [rows, setRows] = useState<LectureImprovementRow[]>([]);
  const [items, setItems] = useState<ManagementItem[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const locked =
    selected !== null &&
    !["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const otherOwner =
    selected !== null &&
    selected.teacherUserId !== user?.userId &&
    !user?.roles.includes("R09");
  const readonly = !writer || locked || otherOwner;

  function fail(caught: unknown) {
    setError(
      caught instanceof Error
        ? caught.message
        : "강의개선 실적 처리에 실패했습니다.",
    );
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      const values = caught.apiError?.fields;
      setFields(
        Array.isArray(values)
          ? Object.fromEntries(
              values.map((value) => [value.field, value.message]),
            )
          : ((values as unknown as Record<string, string>) ?? {}),
      );
    }
  }

  async function load() {
    if (!allowed) return;
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (filter.trim()) query.set("managementNo", filter.trim());
      const response = await lectureImprovementApi.list(query);
      setRows(response.data?.achievements ?? []);
      setItems(response.data?.managementItems ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      fail(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    void load();
  }, [page, pageSize, allowed]);

  async function detail(id: number) {
    setBusy(true);
    setError(null);
    setFields({});
    setSuccess(null);
    try {
      const response = await lectureImprovementApi.get(id);
      const row = response.data;
      if (row) {
        setSelected(row);
        setForm({
          managementItemCode: row.managementItemCode,
          achievementDate: row.achievementDate,
          achievementContent: row.achievementContent,
          academicYear: String(row.academicYear),
          semester: String(row.semester),
        });
      }
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (readonly || !window.confirm("강의개선 실적을 저장하시겠습니까?"))
      return;
    setBusy(true);
    setError(null);
    setFields({});
    setSuccess(null);
    try {
      const response = await lectureImprovementApi.save(
        {
          managementItemCode: form.managementItemCode,
          achievementDate: form.achievementDate,
          achievementContent: form.achievementContent,
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
          attachmentIds: selected?.attachmentIds ?? [],
        },
        selected?.achievementId,
      );
      const saved = response.data;
      if (saved) {
        setSelected(saved.achievement);
        setSuccess(
          saved.occurredDateWarning
            ? (saved.warningMessage ?? "발생일 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }

  if (!allowed || denied)
    return (
      <section
        data-testid="lecture-improvements-page"
        data-screen-id="SCR-LECTURE-IMPROVEMENTS"
      >
        <PermissionState
          title="강의개선 실적 권한이 없습니다"
          message="역할과 데이터 범위 권한을 확인하세요."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-testid="lecture-improvements-page"
      data-screen-id="SCR-LECTURE-IMPROVEMENTS"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의개선 실적 관리
        </h1>
      </div>
      {error && <ErrorState title="처리 실패" message={error} />}
      {success && <SuccessState title="저장 완료" message={success} />}
      <section className="rounded-md bg-white p-6">
        <div className="flex flex-wrap items-end gap-3">
          <label>
            관리번호
            <input
              className={inputClass}
              data-testid="lecture-improvements-search"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
          <button
            className={buttonClass}
            data-testid="lecture-improvements-query"
            type="button"
            onClick={() => {
              if (page === 0) void load();
              else setPage(0);
            }}
          >
            조회
          </button>
          <label>
            표시 건수
            <select
              className={inputClass}
              data-testid="lecture-improvements-page-size"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setPage(0);
              }}
            >
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
          {writer && (
            <button
              className={buttonClass}
              type="button"
              data-testid="lecture-improvements-new"
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
        {loading ? (
          <LoadingState
            title="조회 중"
            message="실적 목록을 조회하고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회 결과 없음"
            message="검색 조건을 확인하거나 신규 실적을 등록하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  {[
                    "관리번호",
                    "교원",
                    "평가년도",
                    "학년도",
                    "학기",
                    "상태",
                    "상세",
                  ].map((title) => (
                    <th key={title}>{title}</th>
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
                    <td>{row.evaluationYear}</td>
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
          </div>
        )}
        <div className="mt-4 flex items-center gap-3">
          <button
            type="button"
            data-testid="lecture-improvements-previous"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <span>
            {total}건 / {page + 1} 페이지
          </span>
          <button
            type="button"
            data-testid="lecture-improvements-next"
            disabled={(page + 1) * pageSize >= total || loading}
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <form
        className="rounded-md bg-white p-6"
        onSubmit={(event) => void save(event)}
      >
        <h2 className="mb-4 text-lg font-semibold">
          {selected ? "실적 상세" : "신규 실적"}
        </h2>
        {selected && (
          <p>
            관리번호: {selected.managementNo} / 평가년도:{" "}
            {selected.evaluationYear}
          </p>
        )}
        {readonly && (
          <p role="status">
            {locked
              ? "현재 상태에서는 수정할 수 없습니다."
              : "조회 전용 실적입니다."}
          </p>
        )}
        <fieldset
          disabled={readonly || busy}
          className="grid gap-4 md:grid-cols-2"
        >
          <label>
            관리항목 *
            <select
              required
              className={inputClass}
              data-testid="lecture-improvements-management-item"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">선택하세요</option>
              {items
                .filter((item) => item.teacherEditableYn === "Y")
                .map((item, index) => (
                  <option
                    key={`${item.evaluationYear}-${item.managementItemCode}-${index}`}
                    value={item.managementItemCode}
                  >
                    {item.managementItemName} ({item.evaluationYear}) ·{" "}
                    {item.dataType} {item.requiredYn === "Y" ? "필수" : "선택"}
                  </option>
                ))}
            </select>
          </label>
          <label>
            업적발생일 *
            <input
              required
              type="date"
              className={inputClass}
              data-testid="lecture-improvements-date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
          </label>
          <label>
            학년도 *
            <input
              required
              type="number"
              min={2000}
              className={inputClass}
              data-testid="lecture-improvements-academic-year"
              value={form.academicYear}
              onChange={(event) =>
                setForm({ ...form, academicYear: event.target.value })
              }
            />
          </label>
          <label>
            학기 *
            <select
              required
              className={inputClass}
              data-testid="lecture-improvements-semester"
              value={form.semester}
              onChange={(event) =>
                setForm({ ...form, semester: event.target.value })
              }
            >
              <option value="">선택하세요</option>
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </label>
          <label className="md:col-span-2">
            실적내용 *
            <textarea
              required
              className={inputClass}
              data-testid="lecture-improvements-content"
              value={form.achievementContent}
              onChange={(event) =>
                setForm({ ...form, achievementContent: event.target.value })
              }
            />
          </label>
        </fieldset>
        {Object.entries(fields).map(([field, message]) => (
          <p className="text-error" role="alert" key={field}>
            {field}: {message}
          </p>
        ))}
        <p className="my-3 text-sm text-muted">
          신규 첨부 업로드·삭제·Excel 다운로드는 공개 API 계약이 없어 제공하지
          않습니다.
        </p>
        {selected?.attachmentIds?.map((id) => (
          <p key={id}>기존 첨부 참조: {id}</p>
        ))}
        {writer && (
          <button
            className={buttonClass}
            type="submit"
            data-testid="lecture-improvements-save"
            disabled={readonly || busy}
          >
            {busy ? "처리 중" : "저장"}
          </button>
        )}
      </form>
    </section>
  );
}
