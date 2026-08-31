import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  participationRateApi,
  type ActiveYn,
  type ApiErrorField,
  type EvaluationRuleVersionStatus,
  type ParticipationRate,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  ruleVersionId: string;
  managementItemId: string;
  researcherCount: string;
  participationType: string;
  distributionRate: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason: string;
};

const activeFlags: ActiveYn[] = ["Y", "N"];
const versionStatuses: EvaluationRuleVersionStatus[] = [
  "DRAFT",
  "CONFIRMED",
  "DISCARDED",
];

const initialForm: FormState = {
  ruleVersionId: "",
  managementItemId: "",
  researcherCount: "",
  participationType: "",
  distributionRate: "",
  effectiveStartDate: "",
  effectiveEndDate: "",
  activeYn: "Y",
  changeReason: "",
};

export function ParticipationRateManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [managementItemId, setManagementItemId] = useState("");
  const [researcherCount, setResearcherCount] = useState("");
  const [participationType, setParticipationType] = useState("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [participationRates, setParticipationRates] = useState<
    ParticipationRate[]
  >([]);
  const [selected, setSelected] = useState<ParticipationRate | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalRates, setTotalRates] = useState(0);
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
      const response = await participationRateApi.listParticipationRates({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        managementItemId: managementItemId
          ? Number(managementItemId)
          : undefined,
        researcherCount: researcherCount ? Number(researcherCount) : undefined,
        participationType: participationType.trim() || undefined,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        pageSize: pageSize as 20 | 50 | 100,
      });
      const nextRates = response.data?.participationRates ?? [];
      setParticipationRates(nextRates);
      setTotalRates(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        ruleVersionId,
        managementItemId,
        researcherCount,
        participationType,
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

  const selectRow = (rate: ParticipationRate) => {
    setSelected(rate);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(rate.ruleVersionId),
      managementItemId: String(rate.managementItemId),
      researcherCount: String(rate.researcherCount),
      participationType: rate.participationType,
      distributionRate: String(rate.distributionRate),
      effectiveStartDate: rate.effectiveStartDate,
      effectiveEndDate: rate.effectiveEndDate,
      activeYn: rate.activeYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const nextFieldErrors = validateForm(form);
    setFieldErrors(nextFieldErrors);
    if (Object.keys(nextFieldErrors).length > 0) {
      setError("필수 입력값을 확인하세요.");
      return;
    }
    const confirmed = window.confirm("참여구분 배분율을 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await participationRateApi.saveParticipationRate({
        ruleVersionId: Number(form.ruleVersionId),
        managementItemId: Number(form.managementItemId),
        researcherCount: Number(form.researcherCount),
        participationType: form.participationType.trim(),
        distributionRate: Number(form.distributionRate),
        effectiveStartDate: form.effectiveStartDate,
        effectiveEndDate: form.effectiveEndDate,
        activeYn: form.activeYn,
        changeReason: form.changeReason.trim(),
      });
      setSuccessMessage("저장되었습니다");
      if (response.data) setSelected(response.data);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
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
        : "배분율을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PARTICIPATION-RATE-MGMT"
        data-testid="participation-rate-page"
      >
        <PermissionState
          title="참여구분·배분율 관리 권한이 없습니다"
          message="R04, R08 또는 R09 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PARTICIPATION-RATE-MGMT"
      data-testid="participation-rate-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 참여구분·배분율 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              참여구분·배분율 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              관리항목·연구자 수·참여구분 조합별 배분율과 적용기간을 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="participation-rate-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 목록 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? <ErrorState title="배분율 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-7">
          <TextInput
            label="규정버전 ID"
            value={ruleVersionId}
            field="ruleVersionId"
            fieldErrors={{}}
            onChange={setRuleVersionId}
          />
          <TextInput
            label="관리항목 ID"
            value={managementItemId}
            field="managementItemId"
            fieldErrors={{}}
            onChange={setManagementItemId}
          />
          <TextInput
            label="연구자 수"
            value={researcherCount}
            field="researcherCount"
            fieldErrors={{}}
            onChange={setResearcherCount}
          />
          <TextInput
            label="참여구분"
            value={participationType}
            field="participationType"
            fieldErrors={{}}
            onChange={setParticipationType}
          />
          <label className="text-sm font-semibold text-dark">
            사용여부
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={activeYn}
              onChange={(event) =>
                setActiveYn(event.target.value as ActiveYn | "")
              }
              data-testid="participation-rate-active-filter-select"
            >
              <option value="">전체</option>
              {activeFlags.map((value) => (
                <option key={value} value={value}>
                  {activeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            조회조건
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="참여구분/영역/항목/요소/관리항목"
              data-testid="participation-rate-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="participation-rate-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">배분율 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setPage(0);
              }}
              data-testid="participation-rate-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="배분율 조회 중" /> : null}
        {!loading && participationRates.length === 0 ? (
          <EmptyState
            title="조회된 배분율이 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 배분율을 저장하세요."
          />
        ) : null}
        {!loading && participationRates.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">연구자 수</th>
                  <th className="px-3 py-2">참여구분</th>
                  <th className="px-3 py-2">배분율</th>
                  <th className="px-3 py-2">적용기간</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {participationRates.map((rate) => (
                  <tr
                    key={`${rate.ruleVersionId}-${rate.managementItemId}-${rate.researcherCount}-${rate.participationType}-${rate.effectiveStartDate}`}
                    className={
                      selected?.participationRateRuleId ===
                      rate.participationRateRuleId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(rate)}
                    data-testid="participation-rate-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {rate.managementItemCode} / {rate.managementItemName}
                    </td>
                    <td className="px-3 py-2">{rate.researcherCount}명</td>
                    <td className="px-3 py-2">
                      {rate.participationType} /{" "}
                      {rate.participationTypeName ?? "-"}
                    </td>
                    <td className="px-3 py-2">{rate.distributionRate}</td>
                    <td className="px-3 py-2">
                      {rate.effectiveStartDate} ~ {rate.effectiveEndDate}
                    </td>
                    <td className="px-3 py-2">{activeLabel(rate.activeYn)}</td>
                    <td className="px-3 py-2">
                      {versionStatusLabel(rate.versionStatus)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalRates}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">배분율 저장</h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장할 수 있으며 확정 규정버전의 배분율은
          수정·삭제할 수 없습니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <TextInput
            label="규정버전 ID"
            value={form.ruleVersionId}
            field="ruleVersionId"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, ruleVersionId: value })}
            required
          />
          <TextInput
            label="관리항목 ID"
            value={form.managementItemId}
            field="managementItemId"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, managementItemId: value })}
            required
          />
          <TextInput
            label="연구자 수"
            value={form.researcherCount}
            field="researcherCount"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, researcherCount: value })}
            required
          />
          <TextInput
            label="참여구분"
            value={form.participationType}
            field="participationType"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, participationType: value })}
            required
          />
          <TextInput
            label="배분율"
            value={form.distributionRate}
            field="distributionRate"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, distributionRate: value })}
            required
          />
          <TextInput
            label="적용시작일"
            value={form.effectiveStartDate}
            field="effectiveStartDate"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, effectiveStartDate: value })
            }
            required
          />
          <TextInput
            label="적용종료일"
            value={form.effectiveEndDate}
            field="effectiveEndDate"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, effectiveEndDate: value })}
            required
          />
          <label className="text-sm font-semibold text-dark">
            사용여부<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.activeYn}
              onChange={(event) =>
                setForm({ ...form, activeYn: event.target.value as ActiveYn })
              }
              data-testid="participation-rate-active-yn-select"
            >
              {activeFlags.map((value) => (
                <option key={value} value={value}>
                  {activeLabel(value)}
                </option>
              ))}
            </select>
            {fieldErrors.activeYn ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.activeYn}
              </span>
            ) : null}
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-3">
            변경 사유<span className="ms-1 text-error">*</span>
            <textarea
              className="mt-2 min-h-[92px] w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.changeReason}
              onChange={(event) =>
                setForm({ ...form, changeReason: event.target.value })
              }
              data-testid="participation-rate-change-reason-textarea"
            />
            {fieldErrors.changeReason ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.changeReason}
              </span>
            ) : null}
          </label>
        </div>
        <button
          type="button"
          className="mt-5 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          disabled={saving}
          onClick={() => void save()}
          data-testid="participation-rate-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          실제 점수 계산 실행과 교원별 산출근거 이력은 이 화면 범위가 아닙니다.
          규정상태: {versionStatuses.map(versionStatusLabel).join(" / ")}
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
  field: keyof FormState;
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
        data-testid={`participation-rate-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
      />
      {fieldErrors[field] ? (
        <span className="mt-1 block text-xs text-error">
          {fieldErrors[field]}
        </span>
      ) : null}
    </label>
  );
}

function activeLabel(value: ActiveYn) {
  return value === "Y" ? "사용" : "미사용";
}

function versionStatusLabel(value: EvaluationRuleVersionStatus) {
  if (value === "DRAFT") return "작성중";
  if (value === "CONFIRMED") return "확정";
  return "폐기";
}

function validateForm(form: FormState) {
  const errors: Record<string, string> = {};
  if (!form.ruleVersionId.trim())
    errors.ruleVersionId = "규정버전을 선택하세요.";
  if (!form.managementItemId.trim())
    errors.managementItemId = "관리항목을 선택하세요.";
  if (!form.researcherCount.trim())
    errors.researcherCount = "연구자 수를 입력하세요.";
  if (!form.participationType.trim())
    errors.participationType = "참여구분을 입력하세요.";
  if (!form.distributionRate.trim())
    errors.distributionRate = "배분율을 입력하세요.";
  if (!form.effectiveStartDate.trim())
    errors.effectiveStartDate = "적용시작일을 입력하세요.";
  if (!form.effectiveEndDate.trim())
    errors.effectiveEndDate = "적용종료일을 입력하세요.";
  if (!form.changeReason.trim())
    errors.changeReason = "변경 사유를 입력하세요.";
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
