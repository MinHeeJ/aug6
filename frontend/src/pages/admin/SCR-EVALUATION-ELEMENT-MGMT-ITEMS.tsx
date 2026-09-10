import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationElementManagementItemApi,
  type ActiveYn,
  type ApiErrorField,
  type EvaluationElementManagementItem,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type FormState = {
  ruleVersionId: string;
  evaluationYear: string;
  areaCode: string;
  elementCode: string;
  managementItemCode: string;
  managementItemName: string;
  teacherEditablePart: string;
  sortOrder: string;
  activeYn: ActiveYn;
  changeReason: string;
};

const activeFlags: ActiveYn[] = ["Y", "N"];

const initialForm: FormState = {
  ruleVersionId: "",
  evaluationYear: "",
  areaCode: "",
  elementCode: "",
  managementItemCode: "",
  managementItemName: "",
  teacherEditablePart: "",
  sortOrder: "1",
  activeYn: "Y",
  changeReason: "",
};

export function EvaluationElementManagementItemsPage() {
  const [evaluationYear, setEvaluationYear] = useState("");
  const [areaCode, setAreaCode] = useState("");
  const [elementCode, setElementCode] = useState("");
  const [items, setItems] = useState<EvaluationElementManagementItem[]>([]);
  const [selected, setSelected] =
    useState<EvaluationElementManagementItem | null>(null);
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
        await evaluationElementManagementItemApi.listEvaluationElementManagementItems(
          {
            evaluationYear: evaluationYear.trim() || undefined,
            areaCode: areaCode.trim() || undefined,
            elementCode: elementCode.trim() || undefined,
            page,
            pageSize,
          },
        );
      setItems(response.data?.evaluationElementManagementItems ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, evaluationYear, areaCode, elementCode });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = (item: EvaluationElementManagementItem) => {
    setSelected(item);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(item.ruleVersionId),
      evaluationYear: item.evaluationYear,
      areaCode: item.areaCode,
      elementCode: item.elementCode,
      managementItemCode: item.managementItemCode,
      managementItemName: item.managementItemName,
      teacherEditablePart: item.teacherEditablePart,
      sortOrder: String(item.sortOrder),
      activeYn: item.activeYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("평가요소별 관리항목을 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await evaluationElementManagementItemApi.saveEvaluationElementManagementItem(
          {
            ruleVersionId: Number(form.ruleVersionId),
            evaluationYear: form.evaluationYear.trim(),
            areaCode: form.areaCode.trim(),
            elementCode: form.elementCode.trim(),
            managementItemCode: form.managementItemCode.trim(),
            managementItemName: form.managementItemName.trim(),
            teacherEditablePart: form.teacherEditablePart.trim(),
            sortOrder: Number(form.sortOrder),
            activeYn: form.activeYn,
            changeReason: form.changeReason.trim(),
          },
        );
      setSuccessMessage("저장되었습니다");
      if (response.data) setSelected(response.data);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const downloadExcel = () => {
    downloadCsv("evaluation-element-management-items.csv", items, [
      { header: "평가연도", value: (item) => item.evaluationYear },
      { header: "평가영역", value: (item) => item.areaCode },
      { header: "평가요소", value: (item) => item.elementCode },
      { header: "관리항목코드", value: (item) => item.managementItemCode },
      { header: "항목명", value: (item) => item.managementItemName },
      {
        header: "교수입력 가능부분",
        value: (item) => item.teacherEditablePart,
      },
      { header: "정렬순서", value: (item) => item.sortOrder },
      { header: "사용여부", value: (item) => activeLabel(item.activeYn) },
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
        : "평가요소별 관리항목을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVALUATION-ELEMENT-MGMT-ITEMS"
        data-testid="evaluation-element-management-items-page"
      >
        <PermissionState
          title="평가요소별 관리항목 관리 권한이 없습니다"
          message="R04 업무담당자 또는 R09 시스템관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-ELEMENT-MGMT-ITEMS"
      data-testid="evaluation-element-management-items-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 평가요소별 관리항목 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가요소별 관리항목 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              평가요소에 적용할 관리항목, 교수입력 가능부분, 정렬순서,
              사용상태를 운영 설정으로 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="evaluation-element-management-items-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 목록을 재조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="평가요소별 관리항목 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-5">
          <TextInput
            label="평가연도"
            value={evaluationYear}
            field="evaluationYear"
            fieldErrors={{}}
            onChange={setEvaluationYear}
          />
          <TextInput
            label="평가영역 코드"
            value={areaCode}
            field="areaCode"
            fieldErrors={{}}
            onChange={setAreaCode}
          />
          <TextInput
            label="평가요소 코드"
            value={elementCode}
            field="elementCode"
            fieldErrors={{}}
            onChange={setElementCode}
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
              data-testid="evaluation-element-management-items-page-size-select"
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
            data-testid="evaluation-element-management-items-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">
            평가요소별 관리항목 목록
          </h2>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
            disabled={items.length === 0}
            onClick={downloadExcel}
            data-testid="evaluation-element-management-items-excel-button"
          >
            <Download size={16} /> 엑셀 내려받기
          </button>
        </div>
        {loading ? <LoadingState title="평가요소별 관리항목 조회 중" /> : null}
        {!loading && items.length === 0 ? (
          <EmptyState
            title="조회된 평가요소별 관리항목이 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 관리항목을 저장하세요."
          />
        ) : null}
        {!loading && items.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">코드</th>
                  <th className="px-3 py-2">항목명</th>
                  <th className="px-3 py-2">교수입력 가능부분</th>
                  <th className="px-3 py-2">정렬순서</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {items.map((item) => (
                  <tr
                    key={item.settingId}
                    className={
                      selected?.settingId === item.settingId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(item)}
                    data-testid="evaluation-element-management-items-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {item.managementItemCode}
                    </td>
                    <td className="px-3 py-2">{item.managementItemName}</td>
                    <td className="px-3 py-2">{item.teacherEditablePart}</td>
                    <td className="px-3 py-2">{item.sortOrder}</td>
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
        <h2 className="text-lg font-semibold text-dark">등록·수정 폼</h2>
        <p className="mt-2 text-sm text-muted">
          확정 규정버전은 수정할 수 없습니다. 저장 후 동일 조건으로 목록을
          재조회합니다.
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
            label="평가연도"
            value={form.evaluationYear}
            field="evaluationYear"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, evaluationYear: value })}
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
            label="평가요소 코드"
            value={form.elementCode}
            field="elementCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, elementCode: value })}
            required
          />
          <TextInput
            label="관리항목코드"
            value={form.managementItemCode}
            field="managementItemCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
            required
          />
          <TextInput
            label="항목명"
            value={form.managementItemName}
            field="managementItemName"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, managementItemName: value })
            }
            required
          />
          <TextInput
            label="교수입력 가능부분"
            value={form.teacherEditablePart}
            field="teacherEditablePart"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, teacherEditablePart: value })
            }
            required
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
              data-testid="evaluation-element-management-items-active-yn-select"
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
              data-testid="evaluation-element-management-items-change-reason-textarea"
            />
            {fieldErrors.changeReason ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.changeReason}
              </span>
            ) : null}
          </label>
        </div>
        <div className="mt-5 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            disabled={saving}
            onClick={() => void save()}
            data-testid="evaluation-element-management-items-save-button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
            onClick={() => {
              setSelected(null);
              setFieldErrors({});
              setForm({
                ...initialForm,
                evaluationYear,
                areaCode,
                elementCode,
              });
            }}
            data-testid="evaluation-element-management-items-cancel-button"
          >
            취소
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
        data-testid={`evaluation-element-management-items-${String(field).replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
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
