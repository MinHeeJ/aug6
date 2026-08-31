import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationScoreApi,
  type ActiveYn,
  type ApiErrorField,
  type EvaluationRuleVersionStatus,
  type EvaluationScore,
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
  organizationCode: string;
  evaluationYear: string;
  baseScore: string;
  maxScore: string;
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
  organizationCode: "",
  evaluationYear: "",
  baseScore: "",
  maxScore: "",
  effectiveStartDate: "",
  effectiveEndDate: "",
  activeYn: "Y",
  changeReason: "",
};

export function EvaluationScoreManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [managementItemId, setManagementItemId] = useState("");
  const [organizationCode, setOrganizationCode] = useState("");
  const [evaluationYear, setEvaluationYear] = useState("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [evaluationScores, setEvaluationScores] = useState<EvaluationScore[]>(
    [],
  );
  const [selected, setSelected] = useState<EvaluationScore | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalScores, setTotalScores] = useState(0);
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
      const response = await evaluationScoreApi.listEvaluationScores({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        managementItemId: managementItemId
          ? Number(managementItemId)
          : undefined,
        organizationCode: organizationCode.trim() || undefined,
        evaluationYear: evaluationYear.trim() || undefined,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        pageSize: pageSize as 20 | 50 | 100,
      });
      const nextScores = response.data?.evaluationScores ?? [];
      setEvaluationScores(nextScores);
      setTotalScores(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        ruleVersionId,
        managementItemId,
        organizationCode,
        evaluationYear,
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

  const selectRow = (score: EvaluationScore) => {
    setSelected(score);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(score.ruleVersionId),
      managementItemId: String(score.managementItemId),
      organizationCode: score.organizationCode,
      evaluationYear: score.evaluationYear,
      baseScore: String(score.baseScore),
      maxScore:
        score.maxScore === null || score.maxScore === undefined
          ? ""
          : String(score.maxScore),
      effectiveStartDate: score.effectiveStartDate,
      effectiveEndDate: score.effectiveEndDate,
      activeYn: score.activeYn,
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
    const confirmed = window.confirm("평가점수를 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await evaluationScoreApi.saveEvaluationScore({
        ruleVersionId: Number(form.ruleVersionId),
        managementItemId: Number(form.managementItemId),
        organizationCode: form.organizationCode.trim(),
        evaluationYear: form.evaluationYear.trim(),
        baseScore: Number(form.baseScore),
        maxScore: form.maxScore.trim() ? Number(form.maxScore) : null,
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
        : "평가점수를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVAL-SCORE-MGMT"
        data-testid="evaluation-score-page"
      >
        <PermissionState
          title="평가점수 관리 권한이 없습니다"
          message="R04, R08 또는 R09 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVAL-SCORE-MGMT"
      data-testid="evaluation-score-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 평가점수 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가점수 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              관리항목·소속대학·평가연도·적용기간 조합별 기준점수와 최대점수를
              관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="evaluation-score-refresh-button"
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
      {error ? <ErrorState title="평가점수 오류" message={error} /> : null}

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
            label="소속대학 코드"
            value={organizationCode}
            field="organizationCode"
            fieldErrors={{}}
            onChange={setOrganizationCode}
          />
          <TextInput
            label="평가연도"
            value={evaluationYear}
            field="evaluationYear"
            fieldErrors={{}}
            onChange={setEvaluationYear}
          />
          <label className="text-sm font-semibold text-dark">
            사용여부
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={activeYn}
              onChange={(event) =>
                setActiveYn(event.target.value as ActiveYn | "")
              }
              data-testid="evaluation-score-active-filter-select"
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
              placeholder="영역/항목/요소/관리항목/조직"
              data-testid="evaluation-score-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="evaluation-score-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">평가점수 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setPage(0);
              }}
              data-testid="evaluation-score-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="평가점수 조회 중" /> : null}
        {!loading && evaluationScores.length === 0 ? (
          <EmptyState
            title="조회된 평가점수가 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 평가점수를 저장하세요."
          />
        ) : null}
        {!loading && evaluationScores.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">소속대학</th>
                  <th className="px-3 py-2">평가연도</th>
                  <th className="px-3 py-2">기준점수</th>
                  <th className="px-3 py-2">최대점수</th>
                  <th className="px-3 py-2">적용기간</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {evaluationScores.map((score) => (
                  <tr
                    key={`${score.ruleVersionId}-${score.managementItemId}-${score.organizationCode}-${score.evaluationYear}-${score.effectiveStartDate}`}
                    className={
                      selected?.scoreRuleId === score.scoreRuleId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(score)}
                    data-testid="evaluation-score-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {score.managementItemCode} / {score.managementItemName}
                    </td>
                    <td className="px-3 py-2">
                      {score.organizationCode} / {score.organizationName ?? "-"}
                    </td>
                    <td className="px-3 py-2">{score.evaluationYear}</td>
                    <td className="px-3 py-2">{score.baseScore}</td>
                    <td className="px-3 py-2">{score.maxScore ?? "-"}</td>
                    <td className="px-3 py-2">
                      {score.effectiveStartDate} ~ {score.effectiveEndDate}
                    </td>
                    <td className="px-3 py-2">{activeLabel(score.activeYn)}</td>
                    <td className="px-3 py-2">
                      {versionStatusLabel(score.versionStatus)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalScores}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">평가점수 저장</h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장할 수 있으며 확정 규정버전의 평가점수는
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
            label="소속대학 코드"
            value={form.organizationCode}
            field="organizationCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, organizationCode: value })}
            required
          />
          <TextInput
            label="평가연도"
            value={form.evaluationYear}
            field="evaluationYear"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, evaluationYear: value })}
            required
          />
          <TextInput
            label="평가점수"
            value={form.baseScore}
            field="baseScore"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, baseScore: value })}
            required
          />
          <TextInput
            label="최대점수"
            value={form.maxScore}
            field="maxScore"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, maxScore: value })}
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
              data-testid="evaluation-score-active-yn-select"
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
              data-testid="evaluation-score-change-reason-textarea"
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
          data-testid="evaluation-score-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          실제 업적의 참여자 배분율·계산식 설정과 교원별 평가결과 수정은 이 화면
          범위가 아닙니다. 규정상태:{" "}
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
        data-testid={`evaluation-score-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
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
  if (!form.organizationCode.trim())
    errors.organizationCode = "소속대학 코드를 입력하세요.";
  if (!form.evaluationYear.trim())
    errors.evaluationYear = "평가연도를 입력하세요.";
  if (!form.baseScore.trim()) errors.baseScore = "평가점수를 입력하세요.";
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
