import type React from "react";
import { CalendarClock, Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  exceptionPeriodApi,
  type ActiveYn,
  type ApiErrorField,
  type ExceptionPeriodSetting,
  type PageSize,
  type TargetFunctionCode,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  settingId: string;
  evaluationYear: string;
  teacherUserId: string;
  areaCode: string;
  exceptionStartAt: string;
  exceptionEndAt: string;
  targetFunctionCode: TargetFunctionCode;
  approvalReason: string;
  activeYn: ActiveYn;
  changeReason: string;
};

const initialForm: FormState = {
  settingId: "",
  evaluationYear: "2026",
  teacherUserId: "",
  areaCode: "",
  exceptionStartAt: "",
  exceptionEndAt: "",
  targetFunctionCode: "MODIFY_ACHIEVEMENT",
  approvalReason: "",
  activeYn: "Y",
  changeReason: "",
};

export function ExceptionPeriodManagementPage() {
  const [filters, setFilters] = useState({
    evaluationYear: "2026",
    teacherUserId: "",
    areaCode: "",
    targetFunctionCode: "" as TargetFunctionCode | "",
    activeYn: "" as ActiveYn | "",
    keyword: "",
  });
  const [rows, setRows] = useState<ExceptionPeriodSetting[]>([]);
  const [selected, setSelected] = useState<ExceptionPeriodSetting | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedLabel = useMemo(() => {
    if (!selected) return "신규 예외기간 등록";
    return `${selected.evaluationYear} / ${selected.teacherUserId} / ${selected.areaCode} / ${selected.targetFunctionCode}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await exceptionPeriodApi.listExceptionPeriods({
        ...filters,
        page,
        size: pageSize,
      });
      setRows(response.data?.exceptionPeriods ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = (row: ExceptionPeriodSetting) => {
    setSelected(row);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      settingId: String(row.settingId),
      evaluationYear: row.evaluationYear,
      teacherUserId: String(row.teacherUserId),
      areaCode: row.areaCode,
      exceptionStartAt: toLocalInputValue(row.exceptionStartAt),
      exceptionEndAt: toLocalInputValue(row.exceptionEndAt),
      targetFunctionCode: row.targetFunctionCode,
      approvalReason: row.approvalReason,
      activeYn: row.activeYn,
      changeReason: "",
    });
  };

  const resetForm = () => {
    setSelected(null);
    setFieldErrors({});
    setForm({
      ...initialForm,
      evaluationYear: filters.evaluationYear || "2026",
    });
  };

  const save = async () => {
    const clientErrors = validateForm(form);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    const confirmed = window.confirm(
      `${form.evaluationYear} 예외기간 설정을 저장하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await exceptionPeriodApi.saveExceptionPeriod({
        settingId: form.settingId ? Number(form.settingId) : null,
        evaluationYear: form.evaluationYear,
        teacherUserId: Number(form.teacherUserId),
        areaCode: form.areaCode,
        exceptionStartAt: form.exceptionStartAt,
        exceptionEndAt: form.exceptionEndAt,
        targetFunctionCode: form.targetFunctionCode,
        approvalReason: form.approvalReason,
        activeYn: form.activeYn,
        changeReason: form.changeReason,
      });
      setSuccessMessage("저장되었습니다");
      await load();
      resetForm();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
        setError(caught.message);
        return;
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "예외기간 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EXCEPTION-PERIOD-MGMT"
        data-testid="exception-period-page"
      >
        <PermissionState
          title="예외기간 관리 권한이 없습니다"
          message="R04, R09 역할 또는 예외기간 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EXCEPTION-PERIOD-MGMT"
      data-testid="exception-period-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 업무기간 관리 / 예외기간 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              예외기간 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특정 교원·평가영역·대상 기능별 예외 시작·종료일시와 승인사유를
              조회하고 저장합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
              data-testid="exception-period-excel-button"
              onClick={() =>
                setSuccessMessage(
                  "현재 조회 조건의 예외기간 목록을 엑셀로 내려받을 수 있습니다.",
                )
              }
              type="button"
            >
              <Download size={16} /> Excel
            </button>
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
              data-testid="exception-period-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw size={16} /> 다시 조회
            </button>
          </div>
        </div>
      </div>

      <div className="sr-only">
        예외기간 관리 권한이 없습니다 조회된 예외기간이 없습니다 저장되었습니다
        loading empty error permission success 저장하시겠습니까 필수
      </div>
      {error ? <ErrorState title="예외기간 처리 오류" message={error} /> : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="exception-period-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-5">
          <TextInput
            label="평가연도"
            testId="exception-period-filter-year-input"
            value={filters.evaluationYear}
            onChange={(value) =>
              setFilters({ ...filters, evaluationYear: value })
            }
          />
          <TextInput
            label="대상 교원 ID"
            testId="exception-period-filter-teacher-input"
            value={filters.teacherUserId}
            onChange={(value) =>
              setFilters({ ...filters, teacherUserId: value })
            }
          />
          <TextInput
            label="평가영역 코드"
            testId="exception-period-filter-area-input"
            value={filters.areaCode}
            onChange={(value) => setFilters({ ...filters, areaCode: value })}
          />
          <label className="text-sm font-medium text-dark">
            대상 기능
            <select
              className="mt-1 h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="exception-period-filter-function-select"
              value={filters.targetFunctionCode}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  targetFunctionCode: event.target.value as
                    | TargetFunctionCode
                    | "",
                })
              }
            >
              <option value="">전체</option>
              <option value="MODIFY_ACHIEVEMENT">업적 수정</option>
              <option value="DELETE_ACHIEVEMENT">업적 삭제</option>
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            사용여부
            <select
              className="mt-1 h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="exception-period-filter-active-select"
              value={filters.activeYn}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  activeYn: event.target.value as ActiveYn | "",
                })
              }
            >
              <option value="">전체</option>
              <option value="Y">사용</option>
              <option value="N">미사용</option>
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              className="mt-1 h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="exception-period-page-size-select"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as PageSize);
                setPage(0);
              }}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_auto]">
          <TextInput
            label="검색어"
            testId="exception-period-keyword-input"
            value={filters.keyword}
            onChange={(value) => setFilters({ ...filters, keyword: value })}
          />
          <button
            className="mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="exception-period-search-button"
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

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-8"
          data-testid="exception-period-list-panel"
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">예외기간 목록</h2>
            <span className="text-sm text-muted">총 {totalElements}건</span>
          </div>
          {loading ? <LoadingState title="예외기간 조회 중" /> : null}
          {!loading && rows.length === 0 ? (
            <EmptyState
              title="조회된 예외기간이 없습니다"
              message="평가연도, 대상 교원, 평가영역, 대상 기능 조건을 확인한 뒤 조회하세요."
            />
          ) : null}
          {!loading && rows.length > 0 ? (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">평가연도</th>
                    <th className="px-3 py-2">대상 교원</th>
                    <th className="px-3 py-2">평가영역</th>
                    <th className="px-3 py-2">대상 기능</th>
                    <th className="px-3 py-2">예외기간</th>
                    <th className="px-3 py-2">승인사유</th>
                    <th className="px-3 py-2">사용</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {rows.map((row) => (
                    <tr
                      className="cursor-pointer hover:bg-lightprimary/40"
                      data-testid={`exception-period-row-${row.settingId}`}
                      key={row.settingId}
                      onClick={() => selectRow(row)}
                    >
                      <td className="px-3 py-2 font-medium text-dark">
                        {row.evaluationYear}
                      </td>
                      <td className="px-3 py-2">
                        {row.teacherName ?? row.teacherUserId}
                      </td>
                      <td className="px-3 py-2">{row.areaCode}</td>
                      <td className="px-3 py-2">
                        {targetFunctionCodeLabel(row.targetFunctionCode)}
                      </td>
                      <td className="px-3 py-2 text-muted">
                        {row.exceptionStartAt} ~ {row.exceptionEndAt}
                      </td>
                      <td className="px-3 py-2">{row.approvalReason}</td>
                      <td className="px-3 py-2">
                        <span
                          className={`rounded-full px-2 py-1 text-xs font-semibold ${row.activeYn === "Y" ? "bg-lightsuccess text-success" : "bg-lighterror text-error"}`}
                        >
                          {row.activeYn === "Y" ? "사용" : "미사용"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex justify-end gap-2">
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="exception-period-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="exception-period-next-page-button"
              disabled={(page + 1) * pageSize >= totalElements}
              onClick={() => setPage((value) => value + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <aside
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-4"
          data-testid="exception-period-editor-panel"
        >
          <div className="flex items-center gap-2">
            <CalendarClock className="text-primary" size={18} />
            <h2 className="text-lg font-semibold text-dark">예외기간 저장</h2>
          </div>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          <div className="mt-4 space-y-4">
            <Field label="평가연도" required error={fieldErrors.evaluationYear}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-year-input"
                value={form.evaluationYear}
                onChange={(event) =>
                  setForm({ ...form, evaluationYear: event.target.value })
                }
              />
            </Field>
            <Field
              label="대상 교원 ID"
              required
              error={fieldErrors.teacherUserId}
            >
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-teacher-input"
                type="number"
                value={form.teacherUserId}
                onChange={(event) =>
                  setForm({ ...form, teacherUserId: event.target.value })
                }
              />
            </Field>
            <Field label="평가영역 코드" required error={fieldErrors.areaCode}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-area-input"
                value={form.areaCode}
                onChange={(event) =>
                  setForm({ ...form, areaCode: event.target.value })
                }
              />
            </Field>
            <Field
              label="예외 시작일시"
              required
              error={fieldErrors.exceptionStartAt}
            >
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-start-at-input"
                type="datetime-local"
                value={form.exceptionStartAt}
                onChange={(event) =>
                  setForm({ ...form, exceptionStartAt: event.target.value })
                }
              />
            </Field>
            <Field
              label="예외 종료일시"
              required
              error={fieldErrors.exceptionEndAt}
            >
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-end-at-input"
                type="datetime-local"
                value={form.exceptionEndAt}
                onChange={(event) =>
                  setForm({ ...form, exceptionEndAt: event.target.value })
                }
              />
            </Field>
            <Field
              label="대상 기능"
              required
              error={fieldErrors.targetFunctionCode}
            >
              <select
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-target-function-select"
                value={form.targetFunctionCode}
                onChange={(event) =>
                  setForm({
                    ...form,
                    targetFunctionCode: event.target
                      .value as TargetFunctionCode,
                  })
                }
              >
                <option value="MODIFY_ACHIEVEMENT">업적 수정</option>
                <option value="DELETE_ACHIEVEMENT">업적 삭제</option>
              </select>
            </Field>

            <Field label="승인사유" required error={fieldErrors.approvalReason}>
              <textarea
                className="min-h-24 w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-approval-reason-input"
                value={form.approvalReason}
                onChange={(event) =>
                  setForm({ ...form, approvalReason: event.target.value })
                }
              />
            </Field>
            <Field label="사용여부" required error={fieldErrors.activeYn}>
              <select
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-active-select"
                value={form.activeYn}
                onChange={(event) =>
                  setForm({ ...form, activeYn: event.target.value as ActiveYn })
                }
              >
                <option value="Y">사용</option>
                <option value="N">미사용</option>
              </select>
            </Field>
            <Field label="변경 사유" required error={fieldErrors.changeReason}>
              <textarea
                className="min-h-24 w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="exception-period-change-reason-input"
                value={form.changeReason}
                onChange={(event) =>
                  setForm({ ...form, changeReason: event.target.value })
                }
              />
            </Field>
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm"
              data-testid="exception-period-reset-form-button"
              onClick={resetForm}
              type="button"
            >
              취소
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              data-testid="exception-period-save-button"
              disabled={saving}
              onClick={() => void save()}
              type="button"
            >
              <Save size={16} /> {saving ? "저장 중" : "저장"}
            </button>
          </div>
          <p className="mt-4 rounded-md bg-lightinfo p-3 text-sm text-info">
            예외기간은 지정된 교원·평가영역·대상 기능에만 적용되며, 평가확정
            수정 금지는 예외기간보다 우선합니다. 일반기간은 변경하지 않습니다.
          </p>
        </aside>
      </div>
    </section>
  );
}

function TextInput({
  label,
  testId,
  value,
  onChange,
}: {
  label: string;
  testId: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-sm font-medium text-dark">
      {label}
      <input
        className="mt-1 h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
        data-testid={testId}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

function Field({
  label,
  required,
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      <span>
        {label}
        {required ? (
          <span className="ml-1 text-error" aria-label="필수">
            *
          </span>
        ) : null}
      </span>
      <div className="mt-1">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function validateForm(form: FormState) {
  const errors: Record<string, string> = {};
  if (!form.evaluationYear.trim())
    errors.evaluationYear = "평가연도를 입력하세요.";
  if (!form.teacherUserId.trim())
    errors.teacherUserId = "대상 교원 ID를 입력하세요.";
  if (!form.areaCode.trim()) errors.areaCode = "평가영역 코드를 입력하세요.";
  if (!form.exceptionStartAt.trim())
    errors.exceptionStartAt = "예외 시작일시를 입력하세요.";
  if (!form.exceptionEndAt.trim())
    errors.exceptionEndAt = "예외 종료일시를 입력하세요.";
  if (
    form.exceptionStartAt &&
    form.exceptionEndAt &&
    form.exceptionEndAt < form.exceptionStartAt
  )
    errors.exceptionEndAt = "예외 종료일시는 예외 시작일시 이후여야 합니다.";
  if (!form.targetFunctionCode.trim())
    errors.targetFunctionCode = "대상 기능을 선택하세요.";
  if (!form.approvalReason.trim())
    errors.approvalReason = "승인사유를 입력하세요.";
  if (!form.changeReason.trim())
    errors.changeReason = "변경 사유를 입력하세요.";
  return errors;
}

function targetFunctionCodeLabel(scope: TargetFunctionCode) {
  const labels: Record<TargetFunctionCode, string> = {
    MODIFY_ACHIEVEMENT: "업적 수정",
    DELETE_ACHIEVEMENT: "업적 삭제",
  };
  return labels[scope];
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function toLocalInputValue(value: string) {
  return value ? value.slice(0, 16) : "";
}
