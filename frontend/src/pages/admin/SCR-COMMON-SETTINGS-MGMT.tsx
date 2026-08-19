import { RefreshCw, Save, Settings2 } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  commonSystemSettingsApi,
  type ApiErrorField,
  type CommonSystemSetting,
  type CommonSystemSettingKey,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormRow = {
  settingKey: CommonSystemSettingKey;
  settingValue: string;
  unit: string;
  changeReason: string;
};

const settingLabels: Record<CommonSystemSettingKey, string> = {
  SESSION_IDLE_MINUTES: "세션 유휴시간",
  PAGE_SIZE: "페이지당 조회건수",
  DEFAULT_SEARCH_PERIOD: "기본 검색기간",
  BULK_QUERY_THRESHOLD: "대량조회 기준건수",
  LONG_TASK_NOTICE_THRESHOLD: "장시간작업 안내 기준",
};

const settingDescriptions: Record<CommonSystemSettingKey, string> = {
  SESSION_IDLE_MINUTES:
    "세션 인증 방식은 변경하지 않고 전역 유휴 기준값만 저장합니다.",
  PAGE_SIZE: "사용자 조회 화면의 기본 페이지당 조회건수로 참조할 전역값입니다.",
  DEFAULT_SEARCH_PERIOD:
    "사용자 화면의 기본 검색기간 초기 조건으로 참조할 전역값입니다.",
  BULK_QUERY_THRESHOLD:
    "대량처리 구현방식은 결정하지 않고 안내 기준값만 저장합니다.",
  LONG_TASK_NOTICE_THRESHOLD:
    "장시간작업 안내 기준값만 저장하며 작업 처리 방식은 변경하지 않습니다.",
};

const orderedKeys = Object.keys(settingLabels) as CommonSystemSettingKey[];

export function CommonSettingsManagementPage() {
  const [settings, setSettings] = useState<CommonSystemSetting[]>([]);
  const [formRows, setFormRows] = useState<FormRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await commonSystemSettingsApi.getCommonSystemSettings();
      const rows = sortSettings(response.data?.settings ?? []);
      setSettings(rows);
      setFormRows(rows.map(toFormRow));
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const updateRow = (
    settingKey: CommonSystemSettingKey,
    patch: Partial<FormRow>,
  ) => {
    setFormRows((rows) =>
      rows.map((row) =>
        row.settingKey === settingKey ? { ...row, ...patch } : row,
      ),
    );
    setSuccessMessage(null);
  };

  const resetForm = () => {
    setFormRows(settings.map(toFormRow));
    setFieldErrors({});
  };

  const save = async () => {
    const localErrors = validateRows(formRows);
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) return;
    const confirmed = window.confirm("공통 환경설정을 저장합니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await commonSystemSettingsApi.saveCommonSystemSettings({
        settings: formRows.map((row) => ({
          settingKey: row.settingKey,
          settingValue: row.settingValue.trim(),
          unit: row.unit.trim(),
          changeReason: row.changeReason.trim(),
        })),
      });
      setSuccessMessage("저장되었습니다");
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
        return;
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "공통 환경설정 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-COMMON-SETTINGS-MGMT">
        <PermissionState
          title="공통 환경설정 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COMMON-SETTINGS-MGMT"
      data-testid="common-settings-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 환경설정 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              공통 환경설정
            </h1>
            <p className="mt-2 text-sm text-muted">
              세션 유휴시간, 페이지당 조회건수, 기본 검색기간, 대량조회
              기준건수, 장시간작업 안내 기준을 전역 설정으로 관리합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void load()}
            type="button"
            data-testid="common-settings-refresh-button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        공통 환경설정 정보를 불러오는 중입니다 조회된 공통 환경설정이 없습니다
        공통 환경설정 권한이 없습니다 저장되었습니다 세션 유휴시간 페이지당
        조회건수 기본 검색기간 대량조회 기준건수 장시간작업 안내 기준
      </div>
      {error ? (
        <ErrorState title="공통 환경설정 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md border border-warning/30 bg-warning/10 p-4 text-sm text-warning"
        data-testid="common-settings-oq-panel"
      >
        <p className="font-semibold">OQ-SET-001 단위/범위 확인 필요</p>
        <p className="mt-2">
          설정 단위와 세부 허용 범위가 미확정이므로 화면과 API는 양의 정수값과
          단위 표기만 저장합니다. 사용자별·업무별 개별 환경값은 생성하지 않고
          전역 설정만 저장합니다.
        </p>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md dark:bg-darkgray"
        data-testid="common-settings-form-panel"
      >
        <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
          <Settings2 size={18} />
          <h2 className="text-lg font-semibold">설정값 편집</h2>
        </div>
        {loading ? (
          <LoadingState title="공통 환경설정 정보를 불러오는 중입니다" />
        ) : formRows.length === 0 ? (
          <EmptyState title="조회된 공통 환경설정이 없습니다" />
        ) : (
          <div className="space-y-4">
            {formRows.map((row) => (
              <section
                key={row.settingKey}
                className="rounded-md border border-ld p-4"
                data-testid={`common-settings-row-${row.settingKey.toLowerCase().replace(/_/g, "-")}`}
              >
                <div className="mb-3">
                  <p className="font-semibold text-dark">
                    {settingLabels[row.settingKey]}
                  </p>
                  <p className="mt-1 text-xs text-muted">
                    {settingDescriptions[row.settingKey]}
                  </p>
                </div>
                <div className="grid gap-4 lg:grid-cols-4">
                  <label className="text-sm font-medium text-link">
                    설정값
                    <input
                      className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
                      inputMode="numeric"
                      value={row.settingValue}
                      onChange={(event) =>
                        updateRow(row.settingKey, {
                          settingValue: event.target.value,
                        })
                      }
                      data-testid={`common-settings-value-${row.settingKey.toLowerCase().replace(/_/g, "-")}`}
                    />
                    {fieldErrors[`${row.settingKey}.settingValue`] ? (
                      <p className="mt-1 text-xs text-error">
                        {fieldErrors[`${row.settingKey}.settingValue`]}
                      </p>
                    ) : null}
                  </label>
                  <label className="text-sm font-medium text-link">
                    unit
                    <input
                      className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
                      value={row.unit}
                      onChange={(event) =>
                        updateRow(row.settingKey, { unit: event.target.value })
                      }
                      data-testid={`common-settings-unit-${row.settingKey.toLowerCase().replace(/_/g, "-")}`}
                    />
                    {fieldErrors[`${row.settingKey}.unit`] ? (
                      <p className="mt-1 text-xs text-error">
                        {fieldErrors[`${row.settingKey}.unit`]}
                      </p>
                    ) : null}
                  </label>
                  <label className="text-sm font-medium text-link lg:col-span-2">
                    변경 사유
                    <input
                      className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
                      value={row.changeReason}
                      onChange={(event) =>
                        updateRow(row.settingKey, {
                          changeReason: event.target.value,
                        })
                      }
                      data-testid={`common-settings-reason-${row.settingKey.toLowerCase().replace(/_/g, "-")}`}
                    />
                    {fieldErrors[`${row.settingKey}.changeReason`] ? (
                      <p className="mt-1 text-xs text-error">
                        {fieldErrors[`${row.settingKey}.changeReason`]}
                      </p>
                    ) : null}
                  </label>
                </div>
              </section>
            ))}
            <div className="flex gap-2">
              <button
                className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                disabled={saving}
                onClick={() => void save()}
                type="button"
                data-testid="common-settings-save-button"
              >
                <Save size={16} /> {saving ? "저장 중" : "저장"}
              </button>
              <button
                className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link"
                onClick={resetForm}
                type="button"
                data-testid="common-settings-cancel-button"
              >
                취소
              </button>
            </div>
          </div>
        )}
      </section>
    </section>
  );
}

function toFormRow(setting: CommonSystemSetting): FormRow {
  return {
    settingKey: setting.settingKey,
    settingValue: setting.settingValue,
    unit: setting.unit ?? "",
    changeReason: "",
  };
}

function sortSettings(settings: CommonSystemSetting[]) {
  return [...settings].sort(
    (left, right) =>
      orderedKeys.indexOf(left.settingKey) -
      orderedKeys.indexOf(right.settingKey),
  );
}

function validateRows(rows: FormRow[]) {
  const errors: Record<string, string> = {};
  for (const row of rows) {
    const prefix = row.settingKey;
    if (!row.settingValue.trim()) {
      errors[`${prefix}.settingValue`] = "설정값을 입력하세요.";
    } else if (!/^[1-9][0-9]*$/.test(row.settingValue.trim())) {
      errors[`${prefix}.settingValue`] =
        "OQ-SET-001 기준에 따라 양의 정수값만 저장할 수 있습니다.";
    }
    if (!row.unit.trim()) {
      errors[`${prefix}.unit`] = "단위를 입력하세요.";
    }
    if (!row.changeReason.trim()) {
      errors[`${prefix}.changeReason`] = "변경 사유를 입력하세요.";
    }
  }
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    const keyMatch = field.field.match(
      /^settings\[\d+\]\.(settingValue|unit|changeReason)$/,
    );
    if (keyMatch) {
      accumulator[keyMatch[1]] = field.message;
      return accumulator;
    }
    accumulator[field.field.replace(/^settings\[\d+\]\./, "")] = field.message;
    return accumulator;
  }, {});
}
