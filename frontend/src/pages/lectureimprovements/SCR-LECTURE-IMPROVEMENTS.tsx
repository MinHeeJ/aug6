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
  attachmentIds?: string;
};

type ListResponse = {
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
  semester: "1" | "2" | "";
  attachmentIds: string;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
  attachmentIds: "",
};

/**
 * Provides the education-area lecture-improvement search, detail, create, and
 * update workflow using the approved relative API routes.
 */
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
  const locked = selected?.achievementStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<ListResponse>(
        `/api/business/lecture-improvements?page=${page}&pageSize=${pageSize}` as `/api/${string}`,
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
        `/api/business/lecture-improvements/${row.achievementId}` as `/api/${string}`,
      );
      const detail = response.data;
      if (!detail) return;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        achievementContent: detail.achievementContent,
        academicYear: String(detail.academicYear),
        semester: String(detail.semester) as "1" | "2",
        attachmentIds: attachmentInput(detail.attachmentIds),
      });
      setFieldErrors({});
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const startCreate = () => {
    setSelected(null);
    setForm(emptyForm);
    setFieldErrors({});
    setSuccess(null);
  };

  const save = async () => {
    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;

    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload = JSON.stringify({
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementContent: form.achievementContent.trim(),
        academicYear: Number(form.academicYear),
        semester: Number(form.semester),
        attachmentIds: form.attachmentIds
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
      });
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<LectureImprovement>(
        path as `/api/${string}`,
        {
          method: selected ? "PUT" : "POST",
          body: payload,
        },
      );
      if (response.data) {
        setSelected(response.data);
        setForm({
          managementItemCode: response.data.managementItemCode,
          achievementDate: response.data.achievementDate,
          achievementContent: response.data.achievementContent,
          academicYear: String(response.data.academicYear),
          semester: String(response.data.semester) as "1" | "2",
          attachmentIds: attachmentInput(response.data.attachmentIds),
        });
      }
      setSuccess("저장되었습니다. 목록을 최신 값으로 다시 조회했습니다.");
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
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의개선 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              학년도·학기와 실적내용을 확인하고 저장합니다.
            </p>
          </div>
          <div className="flex gap-2">
            <button
              className="btn-secondary"
              data-testid="lecture-improvements-new-button"
              onClick={startCreate}
              type="button"
            >
              신규 입력
            </button>
            <button
              className="btn-primary"
              data-testid="lecture-improvements-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        </div>
      </div>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의개선 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="lecture-improvements-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              강의개선 실적 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              조회 결과에서 상세를 선택해 수정할 수 있습니다.
            </p>
          </div>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 px-2 py-1"
              data-testid="lecture-improvements-page-size-select"
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
              value={pageSize}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="강의개선 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강의개선 실적이 없습니다"
            message="신규 입력으로 첫 실적을 등록하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>학년도</th>
                  <th>학기</th>
                  <th>인증상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    data-testid="lecture-improvements-row"
                    key={row.achievementId}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.academicYear}</td>
                    <td>{row.semester}학기</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        className="btn-secondary px-3 py-1 text-xs"
                        data-testid={`lecture-improvements-detail-${row.achievementId}`}
                        onClick={() => void selectRow(row)}
                        type="button"
                      >
                        <Search size={14} /> 상세
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
        <h2 className="text-lg font-semibold text-dark">강의개선 실적 상세</h2>
        <p className="mt-2 text-sm text-muted">
          별표(*) 항목은 필수입니다. 평가확정 실적은 수정할 수 없습니다.
        </p>
        {locked ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="lecture-improvements-confirmed-message"
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
              data-testid="lecture-improvements-management-item-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Field>
          <Field
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
          >
            <input
              data-testid="lecture-improvements-achievement-date-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="학년도" required error={fieldErrors.academicYear}>
            <input
              data-testid="lecture-improvements-academic-year-input"
              disabled={locked}
              min="2000"
              onChange={(event) =>
                setForm({ ...form, academicYear: event.target.value })
              }
              type="number"
              value={form.academicYear}
            />
          </Field>
          <Field label="학기" required error={fieldErrors.semester}>
            <select
              data-testid="lecture-improvements-semester-select"
              disabled={locked}
              onChange={(event) =>
                setForm({
                  ...form,
                  semester: event.target.value as "1" | "2" | "",
                })
              }
              value={form.semester}
            >
              <option value="">선택</option>
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </Field>
          <div className="md:col-span-2">
            <Field
              label="실적내용"
              required
              error={fieldErrors.achievementContent}
            >
              <textarea
                className="min-h-28"
                data-testid="lecture-improvements-content-input"
                disabled={locked}
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
                value={form.achievementContent}
              />
            </Field>
          </div>
          <div className="md:col-span-2">
            <Field
              label="첨부파일 참조 (쉼표로 구분)"
              error={fieldErrors.attachmentIds}
            >
              <input
                data-testid="lecture-improvements-attachment-input"
                disabled={locked}
                onChange={(event) =>
                  setForm({ ...form, attachmentIds: event.target.value })
                }
                value={form.attachmentIds}
              />
            </Field>
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="btn-primary"
            data-testid="lecture-improvements-save-button"
            disabled={saving || locked}
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

function attachmentInput(value?: string) {
  if (!value) return "";
  try {
    const ids = JSON.parse(value) as unknown;
    return Array.isArray(ids)
      ? ids.filter((id): id is string => typeof id === "string").join(", ")
      : "";
  } catch {
    return "";
  }
}

function validateForm(form: Form) {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim()) {
    errors.managementItemCode = "관리항목을 입력하세요.";
  }
  if (!form.achievementDate) {
    errors.achievementDate = "업적발생일을 입력하세요.";
  }
  if (!form.achievementContent.trim()) {
    errors.achievementContent = "실적내용을 입력하세요.";
  }
  if (!form.academicYear || Number(form.academicYear) < 2000) {
    errors.academicYear = "학년도를 확인하세요.";
  }
  if (form.semester !== "1" && form.semester !== "2") {
    errors.semester = "학기를 선택하세요.";
  }
  return errors;
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
      <span
        className={[
          "mt-2 block",
          "[&_input]:w-full [&_input]:px-3 [&_input]:py-2",
          "[&_select]:w-full [&_select]:px-3 [&_select]:py-2",
          "[&_textarea]:w-full [&_textarea]:px-3 [&_textarea]:py-2",
        ].join(" ")}
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
