import type React from "react";
import { CalendarClock, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  periodPermissionApi,
  type ApiErrorField,
  type PeriodPermission,
  type PeriodState,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  businessPeriodId: string;
  functionPermissionId: string;
  effectiveStartAt: string;
  effectiveEndAt: string;
  changeReason: string;
};

const initialForm: FormState = {
  businessPeriodId: "BP-2026-A",
  functionPermissionId: "",
  effectiveStartAt: "",
  effectiveEndAt: "",
  changeReason: "",
};

const stateLabels: Record<PeriodState, string> = {
  BEFORE: "기간 전(비활성)",
  ACTIVE: "기간 중(활성)",
  AFTER: "기간 후(비활성)",
};

export function PeriodPermissionManagementPage() {
  const [businessPeriodId, setBusinessPeriodId] = useState("BP-2026-A");
  const [links, setLinks] = useState<PeriodPermission[]>([]);
  const [selected, setSelected] = useState<PeriodPermission | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedLabel = useMemo(() => {
    if (!selected) return "선택된 기간별 권한 없음";
    return `${selected.businessPeriodId} / ${selected.functionPermissionId} / ${selected.functionType}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await periodPermissionApi.listPeriodPermissions({
        businessPeriodId,
        page,
        size,
      });
      setLinks(response.data?.links ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, businessPeriodId });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page]);

  const selectRow = (link: PeriodPermission) => {
    setSelected(link);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      businessPeriodId: link.businessPeriodId,
      functionPermissionId: String(link.functionPermissionId),
      effectiveStartAt: toLocalInputValue(link.effectiveStartAt),
      effectiveEndAt: link.effectiveEndAt
        ? toLocalInputValue(link.effectiveEndAt)
        : "",
      changeReason: "",
    });
  };

  const resetForm = () => {
    if (selected) {
      selectRow(selected);
      return;
    }
    setForm({ ...initialForm, businessPeriodId });
  };

  const save = async () => {
    if (
      !form.functionPermissionId ||
      Number.isNaN(Number(form.functionPermissionId))
    ) {
      setFieldErrors({
        functionPermissionId: "기능 권한 ID를 숫자로 입력하세요.",
      });
      return;
    }
    const confirmed = window.confirm(
      `${form.businessPeriodId} 업무기간과 기능 권한 ${form.functionPermissionId} 연결을 저장하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await periodPermissionApi.savePeriodPermissions({
        businessPeriodId: form.businessPeriodId,
        functionPermissionId: Number(form.functionPermissionId),
        effectiveStartAt: form.effectiveStartAt,
        effectiveEndAt: form.effectiveEndAt || null,
        changeReason: form.changeReason,
      });
      setSuccessMessage("저장되었습니다");
      if (response.data) {
        setSelected(response.data);
      }
      await load();
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
        : "기간별 권한 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PERIOD-PERMISSION-MGMT"
        data-testid="period-permission-page"
      >
        <PermissionState
          title="기간별 권한 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PERIOD-PERMISSION-MGMT"
      data-testid="period-permission-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 역할·권한 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              기간별 권한 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              기존 업무기간 ID와 기능 권한을 연결하고 처리 시점 기준 기간
              전·중·후 활성 상태를 확인합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            data-testid="period-permission-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        기간별 권한 관리 권한이 없습니다 조회된 기간별 권한이 없습니다
        저장되었습니다 기간 상태 처리 시점 기준
      </div>
      {error ? (
        <ErrorState title="기간별 권한 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="period-permission-search-panel"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
          <label className="text-sm font-medium text-dark">
            업무기간 ID
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="period-permission-business-period-id-input"
              value={businessPeriodId}
              onChange={(event) => setBusinessPeriodId(event.target.value)}
            />
          </label>
          <button
            className="mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="period-permission-search-button"
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
          data-testid="period-permission-list-panel"
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">
              기간별 권한 목록
            </h2>
            <span className="text-sm text-muted">총 {totalElements}건</span>
          </div>
          {loading ? <LoadingState title="기간별 권한 조회 중" /> : null}
          {!loading && links.length === 0 ? (
            <EmptyState
              title="조회된 기간별 권한이 없습니다"
              message="업무기간 ID 조건을 확인한 뒤 조회하세요."
            />
          ) : null}
          {!loading && links.length > 0 ? (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">업무기간</th>
                    <th className="px-3 py-2">화면/기능</th>
                    <th className="px-3 py-2">기간</th>
                    <th className="px-3 py-2">기간 상태</th>
                    <th className="px-3 py-2">처리 시점 기준</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {links.map((link) => (
                    <tr
                      className="cursor-pointer hover:bg-lightprimary/40"
                      data-testid={`period-permission-row-${link.periodPermissionLinkId}`}
                      key={link.periodPermissionLinkId}
                      onClick={() => selectRow(link)}
                    >
                      <td className="px-3 py-2 font-medium text-dark">
                        {link.businessPeriodId}
                      </td>
                      <td className="px-3 py-2">
                        {link.screenName ?? link.screenId} / {link.functionType}
                      </td>
                      <td className="px-3 py-2 text-muted">
                        {link.effectiveStartAt} ~{" "}
                        {link.effectiveEndAt ?? "종료일 없음"}
                      </td>
                      <td className="px-3 py-2">
                        <span
                          className={`rounded-full px-2 py-1 text-xs font-semibold ${link.effectiveAllowed ? "bg-lightsuccess text-success" : "bg-lighterror text-error"}`}
                        >
                          {stateLabels[link.periodState]}
                        </span>
                      </td>
                      <td className="px-3 py-2">
                        {link.effectiveAllowed
                          ? "변경성 기능 허용"
                          : "조회만 허용"}
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
              data-testid="period-permission-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="period-permission-next-page-button"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((value) => value + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <aside
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-4"
          data-testid="period-permission-editor-panel"
        >
          <div className="flex items-center gap-2">
            <CalendarClock className="text-primary" size={18} />
            <h2 className="text-lg font-semibold text-dark">기간 연결 저장</h2>
          </div>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          <div className="mt-4 space-y-4">
            <Field label="업무기간 ID" error={fieldErrors.businessPeriodId}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="period-permission-form-business-period-id-input"
                value={form.businessPeriodId}
                onChange={(event) =>
                  setForm({ ...form, businessPeriodId: event.target.value })
                }
              />
            </Field>
            <Field
              label="기능 권한 ID"
              error={fieldErrors.functionPermissionId}
            >
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="period-permission-form-function-permission-id-input"
                value={form.functionPermissionId}
                onChange={(event) =>
                  setForm({ ...form, functionPermissionId: event.target.value })
                }
              />
            </Field>
            <Field label="시작일시" error={fieldErrors.effectiveStartAt}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="period-permission-start-at-input"
                type="datetime-local"
                value={form.effectiveStartAt}
                onChange={(event) =>
                  setForm({ ...form, effectiveStartAt: event.target.value })
                }
              />
            </Field>
            <Field label="종료일시" error={fieldErrors.effectiveEndAt}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="period-permission-end-at-input"
                type="datetime-local"
                value={form.effectiveEndAt}
                onChange={(event) =>
                  setForm({ ...form, effectiveEndAt: event.target.value })
                }
              />
            </Field>
            <Field label="변경 사유" error={fieldErrors.changeReason}>
              <textarea
                className="min-h-[84px] w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="period-permission-change-reason-textarea"
                value={form.changeReason}
                onChange={(event) =>
                  setForm({ ...form, changeReason: event.target.value })
                }
              />
            </Field>
          </div>
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              data-testid="period-permission-save-button"
              disabled={saving}
              onClick={() => void save()}
              type="button"
            >
              {saving ? "저장 중" : "저장"}
            </button>
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm"
              data-testid="period-permission-cancel-button"
              onClick={resetForm}
              type="button"
            >
              취소
            </button>
          </div>
        </aside>
      </div>
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
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <div className="mt-1">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function toLocalInputValue(value: string) {
  return value.length >= 16 ? value.slice(0, 16) : value;
}
