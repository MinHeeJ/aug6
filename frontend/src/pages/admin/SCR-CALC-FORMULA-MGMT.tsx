import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  calculationFormulaApi,
  type ActiveYn,
  type ApiErrorField,
  type CalculationFormula,
  type CalculationType,
  type EvaluationRuleVersionStatus,
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
  formulaCode: string;
  calculationType: CalculationType;
  variableDefinition: string;
  roundingRule: string;
  lowerBoundScore: string;
  upperBoundScore: string;
  evaluationYear: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason: string;
};

const activeFlags: ActiveYn[] = ["Y", "N"];
const calculationTypes: CalculationType[] = [
  "FIXED_SCORE",
  "DISTRIBUTION_RATE",
  "CAP",
  "LADDER",
];
const versionStatuses: EvaluationRuleVersionStatus[] = [
  "DRAFT",
  "CONFIRMED",
  "DISCARDED",
];

const initialForm: FormState = {
  ruleVersionId: "",
  formulaCode: "",
  calculationType: "FIXED_SCORE",
  variableDefinition: "{}",
  roundingRule: "",
  lowerBoundScore: "",
  upperBoundScore: "",
  evaluationYear: "",
  effectiveStartDate: "",
  effectiveEndDate: "",
  activeYn: "Y",
  changeReason: "",
};

export function CalculationFormulaManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [formulaCode, setFormulaCode] = useState("");
  const [calculationType, setCalculationType] = useState<CalculationType | "">(
    "",
  );
  const [evaluationYear, setEvaluationYear] = useState("");
  const [roundingRule, setRoundingRule] = useState("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [calculationFormulas, setCalculationFormulas] = useState<
    CalculationFormula[]
  >([]);
  const [selected, setSelected] = useState<CalculationFormula | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalFormulas, setTotalFormulas] = useState(0);
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
      const response = await calculationFormulaApi.listCalculationFormulas({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        formulaCode: formulaCode.trim() || undefined,
        calculationType,
        evaluationYear: evaluationYear.trim() || undefined,
        roundingRule: roundingRule.trim() || undefined,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        pageSize: pageSize as 20 | 50 | 100,
      });
      const nextFormulas = response.data?.calculationFormulas ?? [];
      setCalculationFormulas(nextFormulas);
      setTotalFormulas(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        ruleVersionId,
        formulaCode,
        calculationType: calculationType || "FIXED_SCORE",
        evaluationYear,
        roundingRule,
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

  const selectRow = (formula: CalculationFormula) => {
    setSelected(formula);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(formula.ruleVersionId),
      formulaCode: formula.formulaCode,
      calculationType: formula.calculationType,
      variableDefinition: formula.variableDefinition,
      roundingRule: formula.roundingRule,
      lowerBoundScore:
        formula.lowerBoundScore == null ? "" : String(formula.lowerBoundScore),
      upperBoundScore:
        formula.upperBoundScore == null ? "" : String(formula.upperBoundScore),
      evaluationYear: formula.evaluationYear,
      effectiveStartDate: formula.effectiveStartDate,
      effectiveEndDate: formula.effectiveEndDate,
      activeYn: formula.activeYn,
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
    const confirmed = window.confirm("계산식을 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await calculationFormulaApi.saveCalculationFormula({
        ruleVersionId: Number(form.ruleVersionId),
        formulaCode: form.formulaCode.trim(),
        calculationType: form.calculationType,
        variableDefinition: form.variableDefinition.trim(),
        roundingRule: form.roundingRule.trim(),
        lowerBoundScore: optionalNumber(form.lowerBoundScore),
        upperBoundScore: optionalNumber(form.upperBoundScore),
        evaluationYear: form.evaluationYear.trim(),
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
        : "계산식을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-CALC-FORMULA-MGMT"
        data-testid="calculation-formula-page"
      >
        <PermissionState
          title="계산식 관리 권한이 없습니다"
          message="R04, R08 또는 R09 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-CALC-FORMULA-MGMT"
      data-testid="calculation-formula-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 계산식 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              계산식 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              산식 ID·변수·반올림 기준·상한·하한·적용연도를 버전별로 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="calculation-formula-refresh-button"
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
      {error ? <ErrorState title="계산식 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-8">
          <TextInput
            label="규정버전 ID"
            value={ruleVersionId}
            field="ruleVersionId"
            fieldErrors={{}}
            onChange={setRuleVersionId}
          />
          <TextInput
            label="산식 ID"
            value={formulaCode}
            field="formulaCode"
            fieldErrors={{}}
            onChange={setFormulaCode}
          />
          <label className="text-sm font-semibold text-dark">
            계산 유형
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={calculationType}
              onChange={(event) =>
                setCalculationType(event.target.value as CalculationType | "")
              }
              data-testid="calculation-formula-calculation-type-filter-select"
            >
              <option value="">전체</option>
              {calculationTypes.map((value) => (
                <option key={value} value={value}>
                  {calculationTypeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <TextInput
            label="적용연도"
            value={evaluationYear}
            field="evaluationYear"
            fieldErrors={{}}
            onChange={setEvaluationYear}
          />
          <TextInput
            label="반올림 기준"
            value={roundingRule}
            field="roundingRule"
            fieldErrors={{}}
            onChange={setRoundingRule}
          />
          <label className="text-sm font-semibold text-dark">
            사용여부
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={activeYn}
              onChange={(event) =>
                setActiveYn(event.target.value as ActiveYn | "")
              }
              data-testid="calculation-formula-active-filter-select"
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
              placeholder="산식/계산유형/변수"
              data-testid="calculation-formula-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="calculation-formula-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">계산식 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setPage(0);
              }}
              data-testid="calculation-formula-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="계산식 조회 중" /> : null}
        {!loading && calculationFormulas.length === 0 ? (
          <EmptyState
            title="조회된 계산식이 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 계산식을 저장하세요."
          />
        ) : null}
        {!loading && calculationFormulas.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">산식 ID</th>
                  <th className="px-3 py-2">계산 유형</th>
                  <th className="px-3 py-2">변수 정의</th>
                  <th className="px-3 py-2">반올림</th>
                  <th className="px-3 py-2">하한/상한</th>
                  <th className="px-3 py-2">적용연도</th>
                  <th className="px-3 py-2">적용기간</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {calculationFormulas.map((formula) => (
                  <tr
                    key={`${formula.ruleVersionId}-${formula.formulaCode}-${formula.evaluationYear}-${formula.effectiveStartDate}`}
                    className={
                      selected?.formulaVersionId === formula.formulaVersionId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(formula)}
                    data-testid="calculation-formula-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {formula.formulaCode}
                    </td>
                    <td className="px-3 py-2">
                      {formula.calculationType} /{" "}
                      {formula.calculationTypeName ??
                        calculationTypeLabel(formula.calculationType)}
                    </td>
                    <td className="max-w-[280px] truncate px-3 py-2">
                      {formula.variableDefinition}
                    </td>
                    <td className="px-3 py-2">{formula.roundingRule}</td>
                    <td className="px-3 py-2">
                      {formula.lowerBoundScore ?? "-"} /{" "}
                      {formula.upperBoundScore ?? "-"}
                    </td>
                    <td className="px-3 py-2">{formula.evaluationYear}</td>
                    <td className="px-3 py-2">
                      {formula.effectiveStartDate} ~ {formula.effectiveEndDate}
                    </td>
                    <td className="px-3 py-2">
                      {activeLabel(formula.activeYn)}
                    </td>
                    <td className="px-3 py-2">
                      {versionStatusLabel(formula.versionStatus)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalFormulas}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">계산식 저장</h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장할 수 있으며 확정 규정버전의 계산식은
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
            label="산식 ID"
            value={form.formulaCode}
            field="formulaCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, formulaCode: value })}
            required
          />
          <label className="text-sm font-semibold text-dark">
            계산 유형<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.calculationType}
              onChange={(event) =>
                setForm({
                  ...form,
                  calculationType: event.target.value as CalculationType,
                })
              }
              data-testid="calculation-formula-calculation-type-select"
            >
              {calculationTypes.map((value) => (
                <option key={value} value={value}>
                  {calculationTypeLabel(value)}
                </option>
              ))}
            </select>
            {fieldErrors.calculationType ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.calculationType}
              </span>
            ) : null}
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-3">
            변수 정의<span className="ms-1 text-error">*</span>
            <textarea
              className="mt-2 min-h-[92px] w-full rounded-md border border-ld px-3 py-2 font-mono text-sm"
              value={form.variableDefinition}
              onChange={(event) =>
                setForm({ ...form, variableDefinition: event.target.value })
              }
              data-testid="calculation-formula-variable-definition-textarea"
            />
            {fieldErrors.variableDefinition ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.variableDefinition}
              </span>
            ) : null}
          </label>
          <TextInput
            label="반올림 기준"
            value={form.roundingRule}
            field="roundingRule"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, roundingRule: value })}
            required
          />
          <TextInput
            label="하한"
            value={form.lowerBoundScore}
            field="lowerBoundScore"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, lowerBoundScore: value })}
          />
          <TextInput
            label="상한"
            value={form.upperBoundScore}
            field="upperBoundScore"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, upperBoundScore: value })}
          />
          <TextInput
            label="적용연도"
            value={form.evaluationYear}
            field="evaluationYear"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, evaluationYear: value })}
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
              data-testid="calculation-formula-active-yn-select"
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
              data-testid="calculation-formula-change-reason-textarea"
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
          data-testid="calculation-formula-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          자유 코드 실행이 아니라 승인된 계산 유형과 변수 조합을 저장합니다.
          실제 점수 재계산과 교원별 산출근거 변경은 이 화면 범위가 아닙니다.
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
        data-testid={`calculation-formula-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
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

function calculationTypeLabel(value: CalculationType) {
  if (value === "FIXED_SCORE") return "정액배점";
  if (value === "DISTRIBUTION_RATE") return "배분율 적용";
  if (value === "CAP") return "상한 적용";
  return "구간별 배점";
}

function validateForm(form: FormState) {
  const errors: Record<string, string> = {};
  if (!form.ruleVersionId.trim())
    errors.ruleVersionId = "규정버전을 선택하세요.";
  if (!form.formulaCode.trim()) errors.formulaCode = "산식 ID를 입력하세요.";
  if (!form.variableDefinition.trim())
    errors.variableDefinition = "변수 정의를 입력하세요.";
  if (!form.roundingRule.trim())
    errors.roundingRule = "반올림 기준을 입력하세요.";
  if (form.lowerBoundScore.trim() && Number.isNaN(Number(form.lowerBoundScore)))
    errors.lowerBoundScore = "하한은 숫자여야 합니다.";
  if (form.upperBoundScore.trim() && Number.isNaN(Number(form.upperBoundScore)))
    errors.upperBoundScore = "상한은 숫자여야 합니다.";
  if (
    form.lowerBoundScore.trim() &&
    form.upperBoundScore.trim() &&
    Number(form.upperBoundScore) < Number(form.lowerBoundScore)
  )
    errors.upperBoundScore = "상한은 하한 이상이어야 합니다.";
  if (!form.evaluationYear.trim())
    errors.evaluationYear = "적용연도를 입력하세요.";
  if (!form.effectiveStartDate.trim())
    errors.effectiveStartDate = "적용시작일을 입력하세요.";
  if (!form.effectiveEndDate.trim())
    errors.effectiveEndDate = "적용종료일을 입력하세요.";
  if (!form.changeReason.trim())
    errors.changeReason = "변경 사유를 입력하세요.";
  return errors;
}

function optionalNumber(value: string) {
  return value.trim() ? Number(value) : null;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
