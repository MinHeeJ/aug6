import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  participationRateOperationSettingApi,
  type ApiErrorField,
  type ParticipationRateOperationRatePayload,
  type ParticipationRateOperationSetting,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type MatrixRow = {
  researcherCountBand: string;
  participationTypeCode: string;
  distributionRate: string;
};

type FormState = {
  ruleVersionId: string;
  achievementAreaCode: string;
  achievementCategoryCode: string;
  managementItemCode: string;
  rates: MatrixRow[];
  changeReason: string;
};

const initialMatrixRow: MatrixRow = {
  researcherCountBand: "",
  participationTypeCode: "",
  distributionRate: "",
};

const initialForm: FormState = {
  ruleVersionId: "",
  achievementAreaCode: "",
  achievementCategoryCode: "",
  managementItemCode: "",
  rates: [{ ...initialMatrixRow }],
  changeReason: "",
};

export function ParticipationRateOperationSettingsPage() {
  const [achievementAreaCode, setAchievementAreaCode] = useState("");
  const [achievementCategoryCode, setAchievementCategoryCode] = useState("");
  const [settings, setSettings] = useState<ParticipationRateOperationSetting[]>(
    [],
  );
  const [selected, setSelected] =
    useState<ParticipationRateOperationSetting | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [totalElements, setTotalElements] = useState(0);
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
      const response =
        await participationRateOperationSettingApi.listParticipationRateOperationSettings(
          {
            achievementAreaCode: achievementAreaCode.trim() || undefined,
            achievementCategoryCode:
              achievementCategoryCode.trim() || undefined,
            page,
            pageSize,
          },
        );
      setSettings(response.data?.participationRateOperationSettings ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        achievementAreaCode,
        achievementCategoryCode,
      });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = (item: ParticipationRateOperationSetting) => {
    setSelected(item);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(item.ruleVersionId),
      achievementAreaCode: item.achievementAreaCode,
      achievementCategoryCode: item.achievementCategoryCode,
      managementItemCode: item.managementItemCode,
      rates: matrixRowsFor(item, settings),
      changeReason: "",
    });
  };

  const updateRate = (index: number, field: keyof MatrixRow, value: string) => {
    setForm((current) => ({
      ...current,
      rates: current.rates.map((row, rowIndex) =>
        rowIndex === index ? { ...row, [field]: value } : row,
      ),
    }));
  };

  const save = async () => {
    const confirmed = window.confirm(
      "참여구분별 배분율 matrix를 일괄 저장하시겠습니까?",
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await participationRateOperationSettingApi.saveParticipationRateOperationSetting(
          {
            ruleVersionId: Number(form.ruleVersionId),
            achievementAreaCode: form.achievementAreaCode.trim(),
            achievementCategoryCode: form.achievementCategoryCode.trim(),
            managementItemCode: form.managementItemCode.trim(),
            rates: form.rates.map(toRatePayload),
            changeReason: form.changeReason.trim(),
          },
        );
      setSuccessMessage("저장되었습니다");
      if (response.data?.participationRateOperationSettings[0]) {
        setSelected(response.data.participationRateOperationSettings[0]);
      }
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const downloadExcel = () => {
    downloadCsv("participation-rate-operation-settings.csv", settings, [
      { header: "평가연도", value: (item) => item.evaluationYear },
      { header: "업적영역", value: (item) => item.achievementAreaCode },
      { header: "업적분류", value: (item) => item.achievementCategoryCode },
      { header: "관리항목", value: (item) => item.managementItemCode },
      { header: "연구자수", value: (item) => item.researcherCountBand },
      { header: "참여구분", value: (item) => item.participationTypeCode },
      { header: "배분율", value: (item) => item.distributionRate },
      { header: "사용상태", value: (item) => activeLabel(item.activeYn) },
      {
        header: "규정상태",
        value: (item) => versionStatusLabel(item.versionStatus),
      },
    ]);
    setSuccessMessage("엑셀 내려받기 파일을 생성했습니다");
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "참여구분별 배분율을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PARTICIPATION-RATE-OPERATION"
        data-testid="participation-rate-operation-settings-page"
      >
        <PermissionState
          title="참여구분별 배분율 관리 권한이 없습니다"
          message="R04 업무담당자 또는 R09 시스템관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PARTICIPATION-RATE-OPERATION"
      data-testid="participation-rate-operation-settings-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 참여구분별 배분율 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              참여구분별 배분율 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              업적영역·업적분류별 관리항목 x 연구자수 matrix의 참여구분별
              배분율과 사용상태를 운영 설정으로 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="participation-rate-operation-settings-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 matrix를 재조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="참여구분별 배분율 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-4">
          <TextInput
            label="업적영역 코드"
            value={achievementAreaCode}
            field="achievementAreaCode"
            fieldErrors={{}}
            onChange={setAchievementAreaCode}
          />
          <TextInput
            label="업적분류 코드"
            value={achievementCategoryCode}
            field="achievementCategoryCode"
            fieldErrors={{}}
            onChange={setAchievementCategoryCode}
          />
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
              data-testid="participation-rate-operation-settings-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="participation-rate-operation-settings-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">
            참여구분별 배분율 matrix
          </h2>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
            disabled={settings.length === 0}
            onClick={downloadExcel}
            data-testid="participation-rate-operation-settings-excel-button"
          >
            <Download size={16} /> 엑셀 내려받기
          </button>
        </div>
        {loading ? <LoadingState title="참여구분별 배분율 조회 중" /> : null}
        {!loading && settings.length === 0 ? (
          <EmptyState
            title="조회된 참여구분별 배분율이 없습니다"
            message="업적영역·업적분류 조건을 변경하거나 작성중 규정버전에서 matrix를 저장하세요."
          />
        ) : null}
        {!loading && settings.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리항목 x 연구자수</th>
                  <th className="px-3 py-2">참여구분</th>
                  <th className="px-3 py-2">배분율</th>
                  <th className="px-3 py-2">적용 대상</th>
                  <th className="px-3 py-2">사용상태</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {settings.map((item) => (
                  <tr
                    key={item.settingId}
                    className={
                      selected?.settingId === item.settingId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(item)}
                    data-testid="participation-rate-operation-settings-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {item.managementItemCode} / {item.researcherCountBand}
                    </td>
                    <td className="px-3 py-2">{item.participationTypeCode}</td>
                    <td className="px-3 py-2">{item.distributionRate}%</td>
                    <td className="px-3 py-2">
                      {item.achievementAreaCode} /{" "}
                      {item.achievementCategoryCode}
                    </td>
                    <td className="px-3 py-2">{activeLabel(item.activeYn)}</td>
                    <td className="px-3 py-2">
                      {versionStatusLabel(item.versionStatus)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalElements}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">일괄 저장</h2>
        <p className="mt-2 text-sm text-muted">
          확정 규정버전/확정 평가결과 소급 변경은 차단됩니다. 저장 후 동일
          조건으로 matrix를 재조회합니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <TextInput
            label="규정버전 ID"
            value={form.ruleVersionId}
            field="ruleVersionId"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, ruleVersionId: value })}
            required
          />
          <TextInput
            label="업적영역 코드"
            value={form.achievementAreaCode}
            field="achievementAreaCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, achievementAreaCode: value })
            }
            required
          />
          <TextInput
            label="업적분류 코드"
            value={form.achievementCategoryCode}
            field="achievementCategoryCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, achievementCategoryCode: value })
            }
            required
          />
          <TextInput
            label="관리항목 코드"
            value={form.managementItemCode}
            field="managementItemCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
            required
          />
        </div>
        <div
          className="mt-4 space-y-3"
          data-testid="participation-rate-operation-settings-rate-matrix"
        >
          {form.rates.map((rate, index) => (
            <div
              key={`${index}-${rate.researcherCountBand}-${rate.participationTypeCode}`}
              className="grid gap-4 rounded-md bg-lightgray p-4 md:grid-cols-3"
              data-testid="participation-rate-operation-settings-rate-row"
            >
              <TextInput
                label="연구자 수"
                value={rate.researcherCountBand}
                field={`rates[${index}].researcherCountBand`}
                fieldErrors={fieldErrors}
                onChange={(value) =>
                  updateRate(index, "researcherCountBand", value)
                }
                required
              />
              <TextInput
                label="참여구분"
                value={rate.participationTypeCode}
                field={`rates[${index}].participationTypeCode`}
                fieldErrors={fieldErrors}
                onChange={(value) =>
                  updateRate(index, "participationTypeCode", value)
                }
                required
              />
              <TextInput
                label="배분율"
                value={rate.distributionRate}
                field={`rates[${index}].distributionRate`}
                fieldErrors={fieldErrors}
                onChange={(value) =>
                  updateRate(index, "distributionRate", value)
                }
                required
              />
            </div>
          ))}
          {fieldErrors.rates ? (
            <span className="block text-xs text-error">
              {fieldErrors.rates}
            </span>
          ) : null}
        </div>
        <label className="mt-4 block text-sm font-semibold text-dark">
          변경 사유<span className="ms-1 text-error">*</span>
          <textarea
            className="mt-2 min-h-[92px] w-full rounded-md border border-ld px-3 py-2 text-sm"
            value={form.changeReason}
            onChange={(event) =>
              setForm({ ...form, changeReason: event.target.value })
            }
            data-testid="participation-rate-operation-settings-change-reason-textarea"
          />
          {fieldErrors.changeReason ? (
            <span className="mt-1 block text-xs text-error">
              {fieldErrors.changeReason}
            </span>
          ) : null}
        </label>
        <div className="mt-5 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            disabled={saving}
            onClick={() => void save()}
            data-testid="participation-rate-operation-settings-save-button"
          >
            <Save size={16} /> {saving ? "저장 중" : "일괄 저장"}
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
            onClick={() => {
              setSelected(null);
              setFieldErrors({});
              setForm({
                ...initialForm,
                achievementAreaCode,
                achievementCategoryCode,
              });
            }}
            data-testid="participation-rate-operation-settings-cancel-button"
          >
            취소
          </button>
          <button
            type="button"
            className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() =>
              setForm({
                ...form,
                rates: [...form.rates, { ...initialMatrixRow }],
              })
            }
            data-testid="participation-rate-operation-settings-add-rate-button"
          >
            행 추가
          </button>
        </div>
      </section>
    </section>
  );
}

function TextInput({
  label,
  value,
  field,
  fieldErrors,
  onChange,
  required,
}: {
  label: string;
  value: string;
  field: string;
  fieldErrors: Record<string, string>;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <input
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={Boolean(fieldErrors[field])}
        data-testid={`participation-rate-operation-settings-${field
          .replace(/[^A-Za-z0-9]+/g, "-")
          .replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)
          .replace(/^-/, "")}-input`}
      />
      {fieldErrors[field] ? (
        <span className="mt-1 block text-xs text-error">
          {fieldErrors[field]}
        </span>
      ) : null}
    </label>
  );
}

function toRatePayload(row: MatrixRow): ParticipationRateOperationRatePayload {
  return {
    researcherCountBand: row.researcherCountBand.trim(),
    participationTypeCode: row.participationTypeCode.trim(),
    distributionRate:
      row.distributionRate.trim() === ""
        ? Number.NaN
        : Number(row.distributionRate),
  };
}

function matrixRowsFor(
  selected: ParticipationRateOperationSetting,
  settings: ParticipationRateOperationSetting[],
): MatrixRow[] {
  const rows = settings
    .filter(
      (item) =>
        item.ruleVersionId === selected.ruleVersionId &&
        item.achievementAreaCode === selected.achievementAreaCode &&
        item.achievementCategoryCode === selected.achievementCategoryCode &&
        item.managementItemCode === selected.managementItemCode,
    )
    .map((item) => ({
      researcherCountBand: item.researcherCountBand,
      participationTypeCode: item.participationTypeCode,
      distributionRate: String(item.distributionRate),
    }));
  return rows.length > 0 ? rows : [{ ...initialMatrixRow }];
}

function activeLabel(value: string) {
  return value === "Y" ? "사용" : "미사용";
}

function versionStatusLabel(value: string) {
  if (value === "DRAFT") return "작성중";
  if (value === "CONFIRMED") return "확정";
  return "폐기";
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
