import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationItemApi,
  type ActiveYn,
  type ApiErrorField,
  type EvaluationItem,
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
  itemName: string;
  parentItemCode: string;
  sortOrder: string;
  activeYn: ActiveYn;
  scoreApplyMethod: string;
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
  itemName: "",
  parentItemCode: "",
  sortOrder: "1",
  activeYn: "Y",
  scoreApplyMethod: "FIXED",
  changeReason: "",
};

export function EvaluationItemManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [areaCode, setAreaCode] = useState("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [items, setItems] = useState<EvaluationItem[]>([]);
  const [selected, setSelected] = useState<EvaluationItem | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
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
      const response = await evaluationItemApi.listEvaluationItems({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        areaCode: areaCode.trim() || undefined,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        size: size as 20 | 50 | 100,
      });
      const nextItems = response.data?.evaluationItems ?? [];
      setItems(nextItems);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, ruleVersionId, areaCode });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (item: EvaluationItem) => {
    setSelected(item);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(item.ruleVersionId),
      areaCode: item.areaCode,
      itemCode: item.itemCode,
      itemName: item.itemName,
      parentItemCode: item.parentItemCode ?? "",
      sortOrder: String(item.sortOrder),
      activeYn: item.activeYn,
      scoreApplyMethod: item.scoreApplyMethod,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("평가항목을 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await evaluationItemApi.saveEvaluationItem({
        ruleVersionId: Number(form.ruleVersionId),
        areaCode: form.areaCode.trim(),
        itemCode: form.itemCode.trim(),
        itemName: form.itemName.trim(),
        parentItemCode: form.parentItemCode.trim() || null,
        sortOrder: Number(form.sortOrder),
        activeYn: form.activeYn,
        scoreApplyMethod: form.scoreApplyMethod.trim(),
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
        : "평가항목을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVALUATION-ITEM-MGMT"
        data-testid="evaluation-item-page"
      >
        <PermissionState
          title="평가항목 관리 권한이 없습니다"
          message="R04 업무담당자 또는 R09 시스템관리자 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-ITEM-MGMT"
      data-testid="evaluation-item-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 평가항목 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가항목 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              평가영역별 평가항목·지표 코드, 상위항목 계층, 정렬순서, 사용여부,
              배점 적용방식을 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="evaluation-item-refresh-button"
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
      {error ? <ErrorState title="평가항목 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-6">
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
          <label className="text-sm font-semibold text-dark">
            사용여부
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={activeYn}
              onChange={(event) =>
                setActiveYn(event.target.value as ActiveYn | "")
              }
              data-testid="evaluation-item-active-filter-select"
            >
              <option value="">전체</option>
              {activeFlags.map((value) => (
                <option key={value} value={value}>
                  {activeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            조회조건
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="영역/항목 코드 또는 명칭"
              data-testid="evaluation-item-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="evaluation-item-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">평가항목 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={size}
              onChange={(event) => {
                setSize(Number(event.target.value));
                setPage(0);
              }}
              data-testid="evaluation-item-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="평가항목 조회 중" /> : null}
        {!loading && items.length === 0 ? (
          <EmptyState
            title="조회된 평가항목이 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 항목을 저장하세요."
          />
        ) : null}
        {!loading && items.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">영역</th>
                  <th className="px-3 py-2">항목 코드</th>
                  <th className="px-3 py-2">항목명</th>
                  <th className="px-3 py-2">상위항목</th>
                  <th className="px-3 py-2">정렬순서</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">배점 적용방식</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {items.map((item) => (
                  <tr
                    key={`${item.ruleVersionId}-${item.areaCode}-${item.itemCode}`}
                    className={
                      selected?.itemId === item.itemId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(item)}
                    data-testid="evaluation-item-row"
                  >
                    <td className="px-3 py-2">
                      {item.areaCode} / {item.areaName}
                    </td>
                    <td className="px-3 py-2 font-semibold text-dark">
                      {item.itemCode}
                    </td>
                    <td className="px-3 py-2">{item.itemName}</td>
                    <td className="px-3 py-2">{item.parentItemCode ?? "-"}</td>
                    <td className="px-3 py-2">{item.sortOrder}</td>
                    <td className="px-3 py-2">{activeLabel(item.activeYn)}</td>
                    <td className="px-3 py-2">{item.scoreApplyMethod}</td>
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
        <h2 className="text-lg font-semibold text-dark">평가항목 저장</h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장할 수 있으며 확정 규정버전은 수정·삭제할 수
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
            label="평가항목명"
            value={form.itemName}
            field="itemName"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, itemName: value })}
            required
          />
          <TextInput
            label="상위항목 코드"
            value={form.parentItemCode}
            field="parentItemCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, parentItemCode: value })}
          />
          <TextInput
            label="정렬순서"
            value={form.sortOrder}
            field="sortOrder"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, sortOrder: value })}
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
              data-testid="evaluation-item-active-select"
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
          <TextInput
            label="배점 적용방식"
            value={form.scoreApplyMethod}
            field="scoreApplyMethod"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, scoreApplyMethod: value })}
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
              data-testid="evaluation-item-change-reason-textarea"
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
          data-testid="evaluation-item-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          평가요소·관리항목 상세 입력필드와 실제 평가점수·최대점수 입력은 이
          화면 범위가 아닙니다. 규정상태:{" "}
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
        data-testid={`evaluation-item-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
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
