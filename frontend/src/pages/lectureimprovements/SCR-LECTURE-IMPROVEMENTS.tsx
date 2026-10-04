import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type LectureImprovement = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  attachmentRef?: string | null;
};

type SearchResponse = {
  achievements: LectureImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: "1" | "2";
  attachmentIds: string;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "1",
  attachmentIds: "",
};

/** Renders the approved lecture-improvement list, detail lookup, and R01 write flow. */
export function LectureImprovementsPage() {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        `/api/business/lecture-improvements?page=${page}&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
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

  const selectRow = async (row: LectureImprovement) => {
    try {
      setError(null);
      const response = await apiRequest<LectureImprovement>(
        `/api/business/lecture-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementContent: form.achievementContent.trim(),
        academicYear: Number(form.academicYear),
        semester: Number(form.semester),
        attachmentIds: attachmentIds(form.attachmentIds),
      };
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<LectureImprovement>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      if (response.data) {
        setSelected(response.data);
        setForm(toForm(response.data));
      }
      setSuccess("저장되었습니다.");
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
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
        : "강의개선 실적을 처리하지 못했습니다.",
    );
  };

  const finalized = selected?.achievementStatus === "EVALUATION_CONFIRMED";

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-IMPROVEMENTS"
        data-testid="lecture-improvements-page"
      >
        <PermissionState
          title="강의개선 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
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
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의개선 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강의개선 내용과 학년도·학기를 입력하고 인증 상태를 확인합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="lecture-improvements-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의개선 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="lecture-improvements-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강의개선 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="lecture-improvements-page-size-select"
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
              value={pageSize}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="강의개선 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강의개선 실적이 없습니다"
            message="상세 영역에서 새 강의개선 실적을 저장하세요."
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
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="lecture-improvements-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="lecture-improvements-detail-button"
                        onClick={() => void selectRow(row)}
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
        data-testid="lecture-improvements-detail-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              강의개선 실적 상세
            </h2>
            <p className="mt-1 text-sm text-muted">
              필수 항목은 서버에서 다시 검증됩니다.
            </p>
          </div>
          <button
            className={
              "inline-flex items-center gap-2 rounded-md border border-primary px-3 py-2 " +
              "text-sm font-semibold text-primary"
            }
            data-testid="lecture-improvements-new-button"
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setSuccess(null);
              setFieldErrors({});
            }}
            type="button"
          >
            <Search size={16} /> 신규 입력
          </button>
        </div>
        {finalized ? (
          <p
            className="mt-3 text-sm text-error"
            data-testid="lecture-improvements-finalized-message"
          >
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            error={fieldErrors.managementItemCode}
            label="관리항목"
            required
          >
            <input
              data-testid="lecture-improvements-management-item-input"
              disabled={finalized}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Field>
          <Field
            error={fieldErrors.achievementDate}
            label="업적발생일"
            required
          >
            <input
              data-testid="lecture-improvements-date-input"
              disabled={finalized}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field error={fieldErrors.academicYear} label="학년도" required>
            <input
              data-testid="lecture-improvements-academic-year-input"
              disabled={finalized}
              min="2000"
              onChange={(event) =>
                setForm({ ...form, academicYear: event.target.value })
              }
              type="number"
              value={form.academicYear}
            />
          </Field>
          <Field error={fieldErrors.semester} label="학기" required>
            <select
              data-testid="lecture-improvements-semester-select"
              disabled={finalized}
              onChange={(event) =>
                setForm({ ...form, semester: event.target.value as "1" | "2" })
              }
              value={form.semester}
            >
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </Field>
          <Field error={fieldErrors.attachmentIds} label="첨부 참조">
            <input
              data-testid="lecture-improvements-attachments-input"
              disabled={finalized}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
              placeholder="첨부 참조값을 쉼표로 구분"
              value={form.attachmentIds}
            />
          </Field>
          <Field label="인증상태">
            <input disabled value={selected?.achievementStatus ?? "DRAFT"} />
          </Field>
          <div className="md:col-span-2">
            <Field
              error={fieldErrors.achievementContent}
              label="강의개선 내용"
              required
            >
              <textarea
                className="min-h-28"
                data-testid="lecture-improvements-content-input"
                disabled={finalized}
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
                value={form.achievementContent}
              />
            </Field>
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm " +
              "font-semibold text-white disabled:opacity-60"
            }
            data-testid="lecture-improvements-save-button"
            disabled={saving || finalized}
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

function attachmentIds(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function toForm(row: LectureImprovement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementContent: row.achievementContent,
    academicYear: String(row.academicYear),
    semester: row.semester === 2 ? "2" : "1",
    attachmentIds: row.attachmentRef ?? "",
  };
}

function Field({
  children,
  error,
  label,
  required = false,
}: {
  children: ReactNode;
  error?: string;
  label: string;
  required?: boolean;
}) {
  return (
    <label className="grid gap-1 text-sm text-dark">
      <span>
        {label}
        {required ? <span className="ml-1 text-error">*</span> : null}
      </span>
      {children}
      {error ? <span className="text-xs text-error">{error}</span> : null}
    </label>
  );
}
