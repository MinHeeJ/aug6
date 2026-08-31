import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  areaElementSystemApi,
  type ActiveYn,
  type ApiErrorField,
  type AreaElementSystem,
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
  areaCode: string;
  itemCode: string;
  evaluationYear: string;
  elementCode: string;
  targetScope: string;
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
  areaCode: "",
  itemCode: "",
  evaluationYear: "",
  elementCode: "",
  targetScope: "",
  activeYn: "Y",
  changeReason: "",
};

export function AreaElementSystemManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [areaCode, setAreaCode] = useState("");
  const [itemCode, setItemCode] = useState("");
  const [evaluationYear, setEvaluationYear] = useState("");
  const [elementCode, setElementCode] = useState("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [systems, setSystems] = useState<AreaElementSystem[]>([]);
  const [selected, setSelected] = useState<AreaElementSystem | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalSystems, setTotalSystems] = useState(0);
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
      const response = await areaElementSystemApi.listAreaElementSystems({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        areaCode: areaCode.trim() || undefined,
        itemCode: itemCode.trim() || undefined,
        evaluationYear: evaluationYear.trim() || undefined,
        elementCode: elementCode.trim() || undefined,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        size: size as 20 | 50 | 100,
      });
      const nextSystems = response.data?.areaElementSystems ?? [];
      setSystems(nextSystems);
      setTotalSystems(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        ruleVersionId,
        areaCode,
        itemCode,
        evaluationYear,
        elementCode,
      });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (system: AreaElementSystem) => {
    setSelected(system);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(system.ruleVersionId),
      areaCode: system.areaCode,
      itemCode: system.itemCode,
      evaluationYear: system.evaluationYear,
      elementCode: system.elementCode,
      targetScope: system.targetScope,
      activeYn: system.activeYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm(
      "영역별 평가요소 체계를 저장하시겠습니까?",
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await areaElementSystemApi.saveAreaElementSystem({
        ruleVersionId: Number(form.ruleVersionId),
        areaCode: form.areaCode.trim(),
        itemCode: form.itemCode.trim(),
        evaluationYear: form.evaluationYear.trim(),
        elementCode: form.elementCode.trim(),
        targetScope: form.targetScope.trim(),
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
        : "영역별 평가요소 체계를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-AREA-ELEMENT-SYSTEM-MGMT"
        data-testid="area-element-system-page"
      >
        <PermissionState
          title="영역별 평가요소 체계 관리 권한이 없습니다"
          message="R04 업무담당자 또는 R09 시스템관리자 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-AREA-ELEMENT-SYSTEM-MGMT"
      data-testid="area-element-system-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 영역별 평가요소 체계 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              영역별 평가요소 체계 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              업적영역별 평가항목·지표·평가요소 체계의 적용 대상과 사용상태를
              관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="area-element-system-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="영역별 평가요소 체계 오류" message={error} />
      ) : null}

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
            label="평가영역 코드"
            value={areaCode}
            field="areaCode"
            fieldErrors={{}}
            onChange={setAreaCode}
          />
          <TextInput
            label="평가항목 코드"
            value={itemCode}
            field="itemCode"
            fieldErrors={{}}
            onChange={setItemCode}
          />
          <TextInput
            label="평가연도"
            value={evaluationYear}
            field="evaluationYear"
            fieldErrors={{}}
            onChange={setEvaluationYear}
          />
          <TextInput
            label="평가요소 코드"
            value={elementCode}
            field="elementCode"
            fieldErrors={{}}
            onChange={setElementCode}
          />
          <label className="text-sm font-semibold text-dark">
            사용상태
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={activeYn}
              onChange={(event) =>
                setActiveYn(event.target.value as ActiveYn | "")
              }
              data-testid="area-element-system-active-filter-select"
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
              placeholder="영역/항목/요소/적용 대상"
              data-testid="area-element-system-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="area-element-system-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            영역별 평가요소 체계 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={size}
              onChange={(event) => {
                setSize(Number(event.target.value));
                setPage(0);
              }}
              data-testid="area-element-system-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="영역별 평가요소 체계 조회 중" /> : null}
        {!loading && systems.length === 0 ? (
          <EmptyState
            title="조회된 영역별 평가요소 체계가 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 체계를 저장하세요."
          />
        ) : null}
        {!loading && systems.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">영역</th>
                  <th className="px-3 py-2">항목</th>
                  <th className="px-3 py-2">평가연도</th>
                  <th className="px-3 py-2">요소</th>
                  <th className="px-3 py-2">적용 대상</th>
                  <th className="px-3 py-2">사용상태</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {systems.map((system) => (
                  <tr
                    key={`${system.ruleVersionId}-${system.areaCode}-${system.itemCode}-${system.evaluationYear}-${system.elementCode}-${system.targetScope}`}
                    className={
                      selected?.systemSettingId === system.systemSettingId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(system)}
                    data-testid="area-element-system-row"
                  >
                    <td className="px-3 py-2">
                      {system.areaCode} / {system.areaName}
                    </td>
                    <td className="px-3 py-2">
                      {system.itemCode} / {system.itemName}
                    </td>
                    <td className="px-3 py-2">{system.evaluationYear}</td>
                    <td className="px-3 py-2 font-semibold text-dark">
                      {system.elementCode} / {system.elementName}
                    </td>
                    <td className="px-3 py-2">{system.targetScope}</td>
                    <td className="px-3 py-2">
                      {activeLabel(system.activeYn)}
                    </td>
                    <td className="px-3 py-2">
                      {versionStatusLabel(system.versionStatus)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalSystems}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">
          영역별 평가요소 체계 저장
        </h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 소급 변경할 수
          없습니다.
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
            label="평가영역 코드"
            value={form.areaCode}
            field="areaCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, areaCode: value })}
            required
          />
          <TextInput
            label="평가항목 코드"
            value={form.itemCode}
            field="itemCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, itemCode: value })}
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
            label="평가요소 코드"
            value={form.elementCode}
            field="elementCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, elementCode: value })}
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
          <label className="text-sm font-semibold text-dark">
            사용상태<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.activeYn}
              onChange={(event) =>
                setForm({ ...form, activeYn: event.target.value as ActiveYn })
              }
              data-testid="area-element-system-active-select"
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
              data-testid="area-element-system-change-reason-textarea"
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
          data-testid="area-element-system-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          평가점수·배분율·계산식 설정과 관리항목 세부 입력필드 변경은 이 화면
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
        data-testid={`area-element-system-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
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

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
