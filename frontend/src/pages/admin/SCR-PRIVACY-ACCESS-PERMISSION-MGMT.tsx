import { RefreshCw, Search, ShieldCheck } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  privacyPermissionApi,
  type ApiErrorField,
  type PrivacyAccessPermission,
  type PrivacyAccessType,
  type YesNo,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  roleCode: string;
  fieldKey: string;
  rawViewAllowedYn: YesNo;
  maskedViewAllowedYn: YesNo;
  exportAllowedYn: YesNo;
  accountViewAllowedYn: YesNo;
  changeReason: string;
};

type EvaluateState = {
  roleCode: string;
  fieldKey: string;
  accessType: PrivacyAccessType;
  processPurpose: string;
};

const roleCodes = [
  "R01",
  "R02",
  "R03",
  "R04",
  "R05",
  "R06",
  "R07",
  "R08",
  "R09",
];
const yesNoValues: YesNo[] = ["Y", "N"];
const accessTypes: PrivacyAccessType[] = [
  "RAW_VIEW",
  "MASKED_VIEW",
  "EXPORT",
  "ACCOUNT_VIEW",
];
const pageSizes = [20, 50, 100];
const initialForm: FormState = {
  roleCode: "R09",
  fieldKey: "",
  rawViewAllowedYn: "N",
  maskedViewAllowedYn: "N",
  exportAllowedYn: "N",
  accountViewAllowedYn: "N",
  changeReason: "",
};
const initialEvaluate: EvaluateState = {
  roleCode: "R01",
  fieldKey: "",
  accessType: "RAW_VIEW",
  processPurpose: "",
};

export function PrivacyPermissionManagementPage() {
  const [roleCode, setRoleCode] = useState("R09");
  const [fieldKey, setFieldKey] = useState("");
  const [permissions, setPermissions] = useState<PrivacyAccessPermission[]>([]);
  const [selected, setSelected] = useState<PrivacyAccessPermission | null>(
    null,
  );
  const [form, setForm] = useState<FormState>(initialForm);
  const [evaluateForm, setEvaluateForm] =
    useState<EvaluateState>(initialEvaluate);
  const [evaluateResult, setEvaluateResult] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [evaluating, setEvaluating] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedLabel = useMemo(
    () =>
      selected
        ? `${selected.roleCode} / ${selected.fieldKey}`
        : "선택된 개인정보 권한 없음",
    [selected],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await privacyPermissionApi.listPrivacyAccessPermissions({
        roleCode,
        fieldKey,
        page,
        size,
      });
      setPermissions(response.data?.permissions ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, roleCode });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (permission: PrivacyAccessPermission) => {
    setSelected(permission);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      roleCode: permission.roleCode,
      fieldKey: permission.fieldKey,
      rawViewAllowedYn: permission.rawViewAllowedYn,
      maskedViewAllowedYn: permission.maskedViewAllowedYn,
      exportAllowedYn: permission.exportAllowedYn,
      accountViewAllowedYn: permission.accountViewAllowedYn,
      changeReason: "",
    });
    setEvaluateForm((current) => ({
      ...current,
      roleCode: permission.roleCode,
      fieldKey: permission.fieldKey,
    }));
  };

  const save = async () => {
    const confirmed = window.confirm(
      `${form.roleCode} / ${form.fieldKey} 개인정보 조회·출력 권한을 저장하시겠습니까? 사용자 역할 부여·회수는 수행하지 않습니다.`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await privacyPermissionApi.savePrivacyAccessPermissions([form]);
      setSuccessMessage("저장되었습니다");
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const evaluate = async () => {
    try {
      setEvaluating(true);
      setError(null);
      setFieldErrors({});
      const response =
        await privacyPermissionApi.evaluatePrivacyAccessPermission(
          evaluateForm,
        );
      const data = response.data;
      setEvaluateResult(
        data
          ? `${data.allowed ? "허용" : "차단"}: ${data.reason} (원문값 표시 없음)`
          : "판정 결과가 없습니다.",
      );
    } catch (caught) {
      handleApiError(caught);
      if (caught instanceof ApiClientError && caught.status === 403) {
        setEvaluateResult(
          "차단: 권한이 없거나 미설정 조합입니다. 원문값은 표시하지 않습니다.",
        );
      }
    } finally {
      setEvaluating(false);
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
        : "개인정보 조회권한 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PRIVACY-ACCESS-PERMISSION-MGMT"
        data-testid="privacy-permission-page"
      >
        <PermissionState
          title="개인정보 조회권한 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PRIVACY-ACCESS-PERMISSION-MGMT"
      data-testid="privacy-permission-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">보안·감사 관리 / 개인정보 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              개인정보 조회권한
            </h1>
            <p className="mt-2 text-sm text-muted">
              역할별 원문 조회, 마스킹 조회, 출력, 계좌정보 조회 권한을
              독립적으로 관리합니다.
            </p>
          </div>
          <ShieldCheck className="h-10 w-10 text-primary" aria-hidden="true" />
        </div>
      </div>

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-3 lg:grid-cols-4">
          <label className="text-sm font-medium text-dark">
            역할 R01~R09
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-permission-role-filter-select"
              value={roleCode}
              onChange={(event) => setRoleCode(event.target.value)}
            >
              <option value="">전체</option>
              {roleCodes.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            field_key 검색
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-permission-field-key-filter-input"
              value={fieldKey}
              onChange={(event) => setFieldKey(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-permission-page-size-select"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value));
              }}
            >
              {pageSizes.map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              data-testid="privacy-permission-search-button"
              onClick={() => {
                setPage(0);
                void load();
              }}
              type="button"
            >
              <Search className="mr-2 h-4 w-4" aria-hidden="true" />
              조회
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="privacy-permission-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>
      </section>

      {successMessage ? <SuccessState title={successMessage} /> : null}
      {error ? (
        <ErrorState title="개인정보 조회권한 오류" message={error} />
      ) : null}
      {loading ? <LoadingState title="권한 목록 조회 중" /> : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(380px,0.7fr)]">
        <section className="rounded-md border border-ld bg-white p-5 shadow-md">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-dark">권한 matrix</h2>
              <p className="text-sm text-muted">
                총 {totalElements}건 · 기본 20건 표시
              </p>
            </div>
          </div>
          {!loading && permissions.length === 0 ? (
            <EmptyState
              title="조회된 개인정보 권한이 없습니다"
              message="역할 또는 field_key 조건을 변경하세요."
            />
          ) : null}
          {permissions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-xs font-semibold uppercase tracking-wide text-muted">
                  <tr>
                    <th className="px-3 py-3">role_code</th>
                    <th className="px-3 py-3">field_key</th>
                    <th className="px-3 py-3">원문조회</th>
                    <th className="px-3 py-3">마스킹조회</th>
                    <th className="px-3 py-3">출력</th>
                    <th className="px-3 py-3">계좌정보</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {permissions.map((permission) => (
                    <tr
                      key={permission.permissionId}
                      className="cursor-pointer hover:bg-lightprimary"
                      data-testid={`privacy-permission-row-${permission.permissionId}`}
                      onClick={() => selectRow(permission)}
                    >
                      <td className="px-3 py-3 font-medium text-dark">
                        {permission.roleCode}
                      </td>
                      <td className="px-3 py-3">{permission.fieldKey}</td>
                      <td className="px-3 py-3">
                        {permission.rawViewAllowedYn}
                      </td>
                      <td className="px-3 py-3">
                        {permission.maskedViewAllowedYn}
                      </td>
                      <td className="px-3 py-3">
                        {permission.exportAllowedYn}
                      </td>
                      <td className="px-3 py-3">
                        {permission.accountViewAllowedYn}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex items-center justify-between text-sm text-muted">
            <button
              className="rounded-md border border-ld px-3 py-1 disabled:opacity-40"
              data-testid="privacy-permission-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              이전
            </button>
            <span>{page + 1} 페이지</span>
            <button
              className="rounded-md border border-ld px-3 py-1 disabled:opacity-40"
              data-testid="privacy-permission-next-page-button"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((current) => current + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <aside className="space-y-6">
          <section
            className="rounded-md border border-ld bg-white p-5 shadow-md"
            data-testid="privacy-permission-editor-panel"
          >
            <h2 className="text-lg font-semibold text-dark">선택 권한 편집</h2>
            <p className="mt-1 text-sm text-muted">{selectedLabel}</p>
            <div className="mt-4 space-y-4">
              <label className="block text-sm font-medium text-dark">
                역할코드 <span className="text-error">*</span>
                <select
                  className="mt-1 w-full rounded-md border border-ld px-3 py-2"
                  data-testid="privacy-permission-role-select"
                  value={form.roleCode}
                  onChange={(event) =>
                    setForm({ ...form, roleCode: event.target.value })
                  }
                >
                  {roleCodes.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
                {fieldErrors.roleCode ? (
                  <FieldError message={fieldErrors.roleCode} />
                ) : null}
              </label>
              <label className="block text-sm font-medium text-dark">
                개인정보 필드 <span className="text-error">*</span>
                <input
                  className="mt-1 w-full rounded-md border border-ld px-3 py-2"
                  data-testid="privacy-permission-field-key-input"
                  value={form.fieldKey}
                  onChange={(event) =>
                    setForm({ ...form, fieldKey: event.target.value })
                  }
                />
                {fieldErrors.fieldKey ? (
                  <FieldError message={fieldErrors.fieldKey} />
                ) : null}
              </label>
              <div className="grid gap-3 sm:grid-cols-2">
                <PermissionSelect
                  label="원문 조회"
                  testId="privacy-permission-raw-select"
                  value={form.rawViewAllowedYn}
                  onChange={(value) =>
                    setForm({ ...form, rawViewAllowedYn: value })
                  }
                />
                <PermissionSelect
                  label="마스킹 조회"
                  testId="privacy-permission-masked-select"
                  value={form.maskedViewAllowedYn}
                  onChange={(value) =>
                    setForm({ ...form, maskedViewAllowedYn: value })
                  }
                />
                <PermissionSelect
                  label="출력"
                  testId="privacy-permission-export-select"
                  value={form.exportAllowedYn}
                  onChange={(value) =>
                    setForm({ ...form, exportAllowedYn: value })
                  }
                />
                <PermissionSelect
                  label="계좌정보"
                  testId="privacy-permission-account-select"
                  value={form.accountViewAllowedYn}
                  onChange={(value) =>
                    setForm({ ...form, accountViewAllowedYn: value })
                  }
                />
              </div>
              <label className="block text-sm font-medium text-dark">
                변경 사유 <span className="text-error">*</span>
                <textarea
                  className="mt-1 min-h-20 w-full rounded-md border border-ld px-3 py-2"
                  data-testid="privacy-permission-change-reason-input"
                  value={form.changeReason}
                  onChange={(event) =>
                    setForm({ ...form, changeReason: event.target.value })
                  }
                />
                {fieldErrors.changeReason ? (
                  <FieldError message={fieldErrors.changeReason} />
                ) : null}
              </label>
              <div className="rounded-md bg-lightsecondary p-3 text-sm text-muted">
                이 화면은 역할별 개인정보 조회·출력 권한만 저장하며 사용자에게
                역할을 부여하거나 회수하지 않습니다.
              </div>
              <button
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                data-testid="privacy-permission-save-button"
                disabled={saving || !form.fieldKey}
                onClick={() => void save()}
                type="button"
              >
                저장 전 확인
              </button>
            </div>
          </section>

          <section
            className="rounded-md border border-ld bg-white p-5 shadow-md"
            data-testid="privacy-permission-evaluate-panel"
          >
            <h2 className="text-lg font-semibold text-dark">판정 확인</h2>
            <div className="mt-4 space-y-3">
              <select
                className="w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-permission-evaluate-role-select"
                value={evaluateForm.roleCode}
                onChange={(event) =>
                  setEvaluateForm({
                    ...evaluateForm,
                    roleCode: event.target.value,
                  })
                }
              >
                {roleCodes.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
              <input
                className="w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-permission-evaluate-field-key-input"
                placeholder="field_key"
                value={evaluateForm.fieldKey}
                onChange={(event) =>
                  setEvaluateForm({
                    ...evaluateForm,
                    fieldKey: event.target.value,
                  })
                }
              />
              <select
                className="w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-permission-evaluate-access-type-select"
                value={evaluateForm.accessType}
                onChange={(event) =>
                  setEvaluateForm({
                    ...evaluateForm,
                    accessType: event.target.value as PrivacyAccessType,
                  })
                }
              >
                {accessTypes.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
              <input
                className="w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-permission-evaluate-purpose-input"
                placeholder="처리 목적"
                value={evaluateForm.processPurpose}
                onChange={(event) =>
                  setEvaluateForm({
                    ...evaluateForm,
                    processPurpose: event.target.value,
                  })
                }
              />
              <button
                className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
                data-testid="privacy-permission-evaluate-button"
                disabled={evaluating}
                onClick={() => void evaluate()}
                type="button"
              >
                확인
              </button>
              {fieldErrors.accessType ? (
                <FieldError message={fieldErrors.accessType} />
              ) : null}
              {fieldErrors.processPurpose ? (
                <FieldError message={fieldErrors.processPurpose} />
              ) : null}
              {evaluateResult ? (
                <div
                  className="rounded-md bg-lightsecondary p-3 text-sm text-dark"
                  data-testid="privacy-permission-evaluate-result"
                  role="status"
                >
                  {evaluateResult}
                </div>
              ) : null}
            </div>
          </section>
        </aside>
      </div>
    </section>
  );
}

function PermissionSelect({
  label,
  testId,
  value,
  onChange,
}: {
  label: string;
  testId: string;
  value: YesNo;
  onChange: (value: YesNo) => void;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label} <span className="text-error">*</span>
      <select
        className="mt-1 w-full rounded-md border border-ld px-3 py-2"
        data-testid={testId}
        value={value}
        onChange={(event) => onChange(event.target.value as YesNo)}
      >
        {yesNoValues.map((item) => (
          <option key={item} value={item}>
            {item}
          </option>
        ))}
      </select>
    </label>
  );
}

function FieldError({ message }: { message: string }) {
  return <p className="mt-1 text-xs text-error">{message}</p>;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    const key = field.field.includes(".")
      ? field.field.slice(field.field.lastIndexOf(".") + 1)
      : field.field;
    accumulator[key] = field.message;
    return accumulator;
  }, {});
}
