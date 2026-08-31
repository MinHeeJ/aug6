import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationRuleSetApi,
  type ActiveYn,
  type ApiErrorField,
  type EvaluationRuleSet,
  type EvaluationRuleSetStatus,
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
  targetScope: string;
  ruleSetName: string;
  ruleSetStatus: EvaluationRuleSetStatus;
  activeYn: ActiveYn;
  effectiveStartDate: string;
  effectiveEndDate: string;
  changeReason: string;
};

const activeFlags: ActiveYn[] = ["Y", "N"];
const ruleSetStatuses: EvaluationRuleSetStatus[] = [
  "DRAFT",
  "CONFIRMED",
  "DISCARDED",
];
const versionStatuses: EvaluationRuleVersionStatus[] = [
  "DRAFT",
  "CONFIRMED",
  "DISCARDED",
];

const initialForm: FormState = {
  ruleVersionId: "",
  targetScope: "",
  ruleSetName: "",
  ruleSetStatus: "DRAFT",
  activeYn: "Y",
  effectiveStartDate: "",
  effectiveEndDate: "",
  changeReason: "",
};

export function EvaluationRuleSetManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [targetScope, setTargetScope] = useState("");
  const [ruleSetName, setRuleSetName] = useState("");
  const [ruleSetStatus, setRuleSetStatus] = useState<
    EvaluationRuleSetStatus | ""
  >("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [ruleSets, setRuleSets] = useState<EvaluationRuleSet[]>([]);
  const [selected, setSelected] = useState<EvaluationRuleSet | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalRuleSets, setTotalRuleSets] = useState(0);
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
      const response = await evaluationRuleSetApi.listEvaluationRuleSets({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        targetScope: targetScope.trim() || undefined,
        ruleSetName: ruleSetName.trim() || undefined,
        ruleSetStatus,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        pageSize: pageSize as 20 | 50 | 100,
      });
      const nextRuleSets = response.data?.evaluationRuleSets ?? [];
      setRuleSets(nextRuleSets);
      setTotalRuleSets(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        ruleVersionId,
        targetScope,
        ruleSetName,
        ruleSetStatus: ruleSetStatus || "DRAFT",
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

  const selectRow = (ruleSet: EvaluationRuleSet) => {
    setSelected(ruleSet);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(ruleSet.ruleVersionId),
      targetScope: ruleSet.targetScope,
      ruleSetName: ruleSet.ruleSetName,
      ruleSetStatus: ruleSet.ruleSetStatus,
      activeYn: ruleSet.activeYn,
      effectiveStartDate: ruleSet.effectiveStartDate,
      effectiveEndDate: ruleSet.effectiveEndDate,
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
    const confirmed = window.confirm(
      "업적평가 기준·점수규칙을 저장하시겠습니까?",
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await evaluationRuleSetApi.saveEvaluationRuleSet({
        ruleVersionId: Number(form.ruleVersionId),
        targetScope: form.targetScope.trim(),
        ruleSetName: form.ruleSetName.trim(),
        ruleSetStatus: form.ruleSetStatus,
        activeYn: form.activeYn,
        effectiveStartDate: form.effectiveStartDate,
        effectiveEndDate: form.effectiveEndDate,
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
        : "업적평가 기준·점수규칙을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVAL-RULE-SET-MGMT"
        data-testid="evaluation-rule-set-page"
      >
        <PermissionState
          title="업적평가 기준·점수규칙 관리 권한이 없습니다"
          message="R04, R08 또는 R09 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVAL-RULE-SET-MGMT"
      data-testid="evaluation-rule-set-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 업적평가 기준·점수규칙 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              업적평가 기준·점수규칙 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              업적평가 규정, 평가기준, 점수규칙의 적용 대상과 사용상태를 통합
              관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="evaluation-rule-set-refresh-button"
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
      {error ? (
        <ErrorState title="업적평가 기준·점수규칙 오류" message={error} />
      ) : null}

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
            label="적용 대상"
            value={targetScope}
            field="targetScope"
            fieldErrors={{}}
            onChange={setTargetScope}
          />
          <TextInput
            label="규칙명"
            value={ruleSetName}
            field="ruleSetName"
            fieldErrors={{}}
            onChange={setRuleSetName}
          />
          <SelectInput
            label="규칙 상태"
            value={ruleSetStatus}
            values={ruleSetStatuses}
            onChange={(value) =>
              setRuleSetStatus(value as EvaluationRuleSetStatus | "")
            }
            testId="evaluation-rule-set-status-filter-select"
            includeAll
          />
          <SelectInput
            label="사용여부"
            value={activeYn}
            values={activeFlags}
            onChange={(value) => setActiveYn(value as ActiveYn | "")}
            testId="evaluation-rule-set-active-filter-select"
            includeAll
          />
          <label className="text-sm font-semibold text-dark">
            조회조건
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="적용대상/규칙명/상태"
              data-testid="evaluation-rule-set-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="evaluation-rule-set-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            기준·점수규칙 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setPage(0);
              }}
              data-testid="evaluation-rule-set-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="업적평가 기준·점수규칙 조회 중" />
        ) : null}
        {!loading && ruleSets.length === 0 ? (
          <EmptyState
            title="조회된 기준·점수규칙이 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 기준을 저장하세요."
          />
        ) : null}
        {!loading && ruleSets.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">규칙명</th>
                  <th className="px-3 py-2">적용 대상</th>
                  <th className="px-3 py-2">규칙 상태</th>
                  <th className="px-3 py-2">규정버전</th>
                  <th className="px-3 py-2">적용기간</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">수정자</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {ruleSets.map((ruleSet) => (
                  <tr
                    key={`${ruleSet.ruleVersionId}-${ruleSet.targetScope}-${ruleSet.ruleSetName}-${ruleSet.effectiveStartDate}`}
                    className={
                      selected?.ruleSetId === ruleSet.ruleSetId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(ruleSet)}
                    data-testid="evaluation-rule-set-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {ruleSet.ruleSetName}
                    </td>
                    <td className="px-3 py-2">{ruleSet.targetScope}</td>
                    <td className="px-3 py-2">
                      {statusLabel(ruleSet.ruleSetStatus)}
                    </td>
                    <td className="px-3 py-2">
                      {ruleSet.versionCode} /{" "}
                      {versionStatusLabel(ruleSet.versionStatus)}
                    </td>
                    <td className="px-3 py-2">
                      {ruleSet.effectiveStartDate} ~ {ruleSet.effectiveEndDate}
                    </td>
                    <td className="px-3 py-2">
                      {activeLabel(ruleSet.activeYn)}
                    </td>
                    <td className="px-3 py-2">{ruleSet.updatedBy ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalRuleSets}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">기준·점수규칙 저장</h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장하며, 확정 평가결과와 개별 교원 업적자료는
          이 화면에서 변경하지 않습니다.
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
            label="적용 대상"
            value={form.targetScope}
            field="targetScope"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, targetScope: value })}
            required
          />
          <TextInput
            label="규칙명"
            value={form.ruleSetName}
            field="ruleSetName"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, ruleSetName: value })}
            required
          />
          <SelectInput
            label="규칙 상태"
            value={form.ruleSetStatus}
            values={ruleSetStatuses}
            onChange={(value) =>
              setForm({
                ...form,
                ruleSetStatus: value as EvaluationRuleSetStatus,
              })
            }
            testId="evaluation-rule-set-rule-set-status-select"
            required
          />
          <SelectInput
            label="사용여부"
            value={form.activeYn}
            values={activeFlags}
            onChange={(value) =>
              setForm({ ...form, activeYn: value as ActiveYn })
            }
            testId="evaluation-rule-set-active-yn-select"
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
          <label className="text-sm font-semibold text-dark md:col-span-3">
            변경 사유<span className="ms-1 text-error">*</span>
            <textarea
              className="mt-2 min-h-[92px] w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.changeReason}
              onChange={(event) =>
                setForm({ ...form, changeReason: event.target.value })
              }
              data-testid="evaluation-rule-set-change-reason-textarea"
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
          data-testid="evaluation-rule-set-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          CMN-FR-030~032 기준을 통합 조회·점검하는 상위 기준정보입니다.
          평가영역·항목·요소·관리항목 상세 편집 및 확정 평가결과 소급 변경은
          제공하지 않습니다. 규정상태:{" "}
          {versionStatuses.map(versionStatusLabel).join(" / ")}
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
        data-testid={`evaluation-rule-set-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
      />
      {fieldErrors[field] ? (
        <span className="mt-1 block text-xs text-error">
          {fieldErrors[field]}
        </span>
      ) : null}
    </label>
  );
}

function SelectInput({
  label,
  value,
  values,
  onChange,
  testId,
  includeAll,
  required,
}: {
  label: string;
  value: string;
  values: string[];
  onChange: (value: string) => void;
  testId: string;
  includeAll?: boolean;
  required?: boolean;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <select
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      >
        {includeAll ? <option value="">전체</option> : null}
        {values.map((item) => (
          <option key={item} value={item}>
            {item === "Y" || item === "N"
              ? activeLabel(item as ActiveYn)
              : statusLabel(item as EvaluationRuleSetStatus)}
          </option>
        ))}
      </select>
    </label>
  );
}

function activeLabel(value: ActiveYn) {
  return value === "Y" ? "사용" : "미사용";
}

function statusLabel(value: EvaluationRuleSetStatus) {
  if (value === "DRAFT") return "작성중";
  if (value === "CONFIRMED") return "확정";
  return "폐기";
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
  if (!form.targetScope.trim()) errors.targetScope = "적용 대상을 입력하세요.";
  if (!form.ruleSetName.trim())
    errors.ruleSetName = "기준·점수규칙명을 입력하세요.";
  if (!form.effectiveStartDate.trim())
    errors.effectiveStartDate = "적용시작일을 입력하세요.";
  if (!form.effectiveEndDate.trim())
    errors.effectiveEndDate = "적용종료일을 입력하세요.";
  if (
    form.effectiveStartDate &&
    form.effectiveEndDate &&
    form.effectiveEndDate < form.effectiveStartDate
  )
    errors.effectiveEndDate = "적용종료일은 시작일 이후여야 합니다.";
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
