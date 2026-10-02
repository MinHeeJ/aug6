import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type LectureAchievement = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  occurredDate: string;
  achievementDetail: string;
  certificationStatus: string;
  attachmentRef?: string | null;
};

type SearchResponse = {
  achievements: LectureAchievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: LectureAchievement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};

type Filters = {
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  certificationStatus: string;
};

type Form = {
  achievementId?: number;
  managementItemCode: string;
  occurredDate: string;
  achievementDetail: string;
  attachmentRef: string;
};

const initialFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  certificationStatus: "",
};

const initialForm: Form = {
  managementItemCode: "",
  occurredDate: "",
  achievementDetail: "",
  attachmentRef: "",
};

export function LectureAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [form, setForm] = useState<Form>(initialForm);
  const [rows, setRows] = useState<LectureAchievement[]>([]);
  const [selected, setSelected] = useState<LectureAchievement | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const confirmedSelected =
    selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        listPath(filters, page, pageSize),
      );
      const data = response.data;
      setRows(data?.achievements ?? []);
      setTotal(data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const save = async () => {
    if (!window.confirm("강의실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const achievementDetail = form.achievementDetail.trim()
        ? JSON.parse(form.achievementDetail)
        : {};
      const response = await apiRequest<SaveResponse>(
        "/api/business/lecture-achievements",
        {
          method: "POST",
          body: JSON.stringify({
            achievementId: form.achievementId,
            managementItemCode: form.managementItemCode.trim(),
            occurredDate: form.occurredDate,
            achievementDetail,
            attachmentRef: form.attachmentRef.trim() || undefined,
          }),
        },
      );
      const saved = response.data?.achievement;
      if (saved) setSelected(saved);
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      if (caught instanceof SyntaxError) {
        setFieldErrors({
          achievementDetail: "상세 입력값은 올바른 JSON 형식이어야 합니다.",
        });
        setError("상세 입력값을 확인하세요.");
      } else {
        handleError(caught);
      }
    } finally {
      setSaving(false);
    }
  };

  const selectRow = (row: LectureAchievement) => {
    setSelected(row);
    setForm({
      achievementId: row.achievementId,
      managementItemCode: row.managementItemCode,
      occurredDate: row.occurredDate,
      achievementDetail: row.achievementDetail || "{}",
      attachmentRef: row.attachmentRef ?? "",
    });
    setSuccess(null);
    setFieldErrors({});
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-ACHIEVEMENT-MGMT"
        data-testid="lecture-achievement-page"
      >
        <PermissionState
          title="강의실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-ACHIEVEMENT-MGMT"
      data-testid="lecture-achievement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              관리항목 설정에 따른 상세 입력값과 인증상태를 확인하고 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="lecture-achievement-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="lecture-achievement-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="관리번호">
            <input
              data-testid="lecture-achievement-management-no-input"
              value={filters.managementNo}
              onChange={(event) =>
                setFilters({ ...filters, managementNo: event.target.value })
              }
            />
          </Field>
          <Field label="성명">
            <input
              data-testid="lecture-achievement-teacher-name-input"
              value={filters.teacherName}
              onChange={(event) =>
                setFilters({ ...filters, teacherName: event.target.value })
              }
            />
          </Field>
          <Field label="관리항목">
            <input
              data-testid="lecture-achievement-management-item-filter-input"
              value={filters.managementItemCode}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  managementItemCode: event.target.value,
                })
              }
            />
          </Field>
          <Field label="인증상태">
            <input
              data-testid="lecture-achievement-status-filter-input"
              value={filters.certificationStatus}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  certificationStatus: event.target.value,
                })
              }
            />
          </Field>
          <button
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            data-testid="lecture-achievement-search-button"
            onClick={() => {
              setPage(0);
              void load();
            }}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">강의실적 목록</h2>
          <div className="flex items-center gap-2">
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                data-testid="lecture-achievement-page-size-select"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as 20 | 50 | 100);
                  setPage(0);
                }}
              >
                {[20, 50, 100].map((value) => (
                  <option key={value} value={value}>
                    {value}건
                  </option>
                ))}
              </select>
            </label>
            <button
              className="inline-flex items-center gap-2 rounded-md border border-ld px-3 py-2 text-sm font-semibold text-link"
              data-testid="lecture-achievement-excel-download-button"
              onClick={() => downloadRows(rows)}
              type="button"
            >
              <Download size={16} /> Excel 다운로드
            </button>
          </div>
        </div>
        {loading ? <LoadingState title="강의실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강의실적이 없습니다"
            message="검색조건을 변경하거나 상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">인증상태</th>
                  <th className="px-3 py-2">첨부여부</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    className={
                      selected?.achievementId === row.achievementId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    data-testid="lecture-achievement-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.occurredDate}</td>
                    <td className="px-3 py-2">{row.certificationStatus}</td>
                    <td className="px-3 py-2">
                      {row.attachmentRef ? "있음" : "없음"}
                    </td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="lecture-achievement-detail-button"
                        onClick={() => selectRow(row)}
                        type="button"
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {total}건 / {page + 1}페이지
        </p>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="lecture-achievement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">강의실적 상세</h2>
        <p className="mt-2 text-sm text-muted">
          관리항목과 업적발생일은 필수입니다. 평가대상 기간 밖 날짜는 경고와
          함께 저장됩니다.
        </p>
        {confirmedSelected ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="lecture-confirmed-lock-message"
          >
            평가확정 실적은 수정하거나 첨부를 변경할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
          >
            <input
              data-testid="lecture-achievement-management-item-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </Field>
          <Field label="업적발생일" required error={fieldErrors.occurredDate}>
            <input
              data-testid="lecture-achievement-occurred-date-input"
              type="date"
              value={form.occurredDate}
              onChange={(event) =>
                setForm({ ...form, occurredDate: event.target.value })
              }
            />
          </Field>
          <Field label="첨부 참조" error={fieldErrors.attachmentRef}>
            <input
              data-testid="lecture-achievement-attachment-ref-input"
              value={form.attachmentRef}
              onChange={(event) =>
                setForm({ ...form, attachmentRef: event.target.value })
              }
            />
          </Field>
          <Field label="인증상태">
            <input disabled value={selected?.certificationStatus ?? "작성중"} />
          </Field>
          <div className="md:col-span-2">
            <Field
              label="FR-018 동적 상세 입력(JSON)"
              error={fieldErrors.achievementDetail}
            >
              <textarea
                className="min-h-28"
                data-testid="lecture-achievement-detail-input"
                value={form.achievementDetail}
                onChange={(event) =>
                  setForm({ ...form, achievementDetail: event.target.value })
                }
              />
            </Field>
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
            data-testid="lecture-achievement-save-button"
            disabled={saving || confirmedSelected}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}

function listPath(filters: Filters, page: number, pageSize: number) {
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  if (filters.managementNo.trim())
    query.set("managementNo", filters.managementNo.trim());
  if (filters.teacherName.trim())
    query.set("teacherName", filters.teacherName.trim());
  if (filters.managementItemCode.trim())
    query.set("managementItemCode", filters.managementItemCode.trim());
  if (filters.certificationStatus.trim())
    query.set("certificationStatus", filters.certificationStatus.trim());
  return `/api/business/lecture-achievements?${query.toString()}` as `/api/${string}`;
}

function downloadRows(rows: LectureAchievement[]) {
  const header = [
    "관리번호",
    "성명",
    "관리항목",
    "업적발생일",
    "인증상태",
    "첨부여부",
  ];
  const values = rows.map((row) => [
    row.managementNo,
    row.teacherName,
    row.managementItemCode,
    row.occurredDate,
    row.certificationStatus,
    row.attachmentRef ? "있음" : "없음",
  ]);
  const csv = [header, ...values]
    .map((line) =>
      line.map((value) => `"${value.replaceAll('"', '""')}"`).join(","),
    )
    .join("\n");
  const url = URL.createObjectURL(
    new Blob([csv], { type: "text/csv;charset=utf-8" }),
  );
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = "lecture-achievements.csv";
  anchor.click();
  URL.revokeObjectURL(url);
}

function Field({
  label,
  required = false,
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span className="mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border [&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm [&_textarea]:w-full [&_textarea]:rounded-md [&_textarea]:border [&_textarea]:border-ld [&_textarea]:px-3 [&_textarea]:py-2 [&_textarea]:text-sm">
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
