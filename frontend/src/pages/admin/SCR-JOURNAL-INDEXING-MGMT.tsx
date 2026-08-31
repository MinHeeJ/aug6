import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  journalIndexingInfoApi,
  type ActiveYn,
  type ApiErrorField,
  type JournalIndexingInfo,
  type JournalIndexingType,
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
  issn: string;
  journalName: string;
  indexingType: JournalIndexingType;
  publicationCountry: string;
  validStartDate: string;
  validEndDate: string;
  sourceName: string;
  sourceUpdatedAt: string;
  activeYn: ActiveYn;
  changeReason: string;
};

const activeFlags: ActiveYn[] = ["Y", "N"];
const indexingTypes: JournalIndexingType[] = [
  "KCI",
  "CANDIDATE",
  "INTERNATIONAL",
  "OTHER",
];

const initialForm: FormState = {
  ruleVersionId: "",
  issn: "",
  journalName: "",
  indexingType: "KCI",
  publicationCountry: "",
  validStartDate: "",
  validEndDate: "",
  sourceName: "",
  sourceUpdatedAt: "",
  activeYn: "Y",
  changeReason: "",
};

export function JournalIndexingInfoManagementPage() {
  const [ruleVersionId, setRuleVersionId] = useState("");
  const [issn, setIssn] = useState("");
  const [journalName, setJournalName] = useState("");
  const [indexingType, setIndexingType] = useState<JournalIndexingType | "">(
    "",
  );
  const [publicationCountry, setPublicationCountry] = useState("");
  const [activeYn, setActiveYn] = useState<ActiveYn | "">("");
  const [keyword, setKeyword] = useState("");
  const [infos, setInfos] = useState<JournalIndexingInfo[]>([]);
  const [selected, setSelected] = useState<JournalIndexingInfo | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalInfos, setTotalInfos] = useState(0);
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
      const response = await journalIndexingInfoApi.listJournalIndexingInfos({
        ruleVersionId: ruleVersionId ? Number(ruleVersionId) : undefined,
        issn: issn.trim() || undefined,
        journalName: journalName.trim() || undefined,
        indexingType,
        publicationCountry: publicationCountry.trim() || undefined,
        activeYn,
        keyword: keyword.trim() || undefined,
        page,
        pageSize: pageSize as 20 | 50 | 100,
      });
      const nextInfos = response.data?.journalIndexingInfos ?? [];
      setInfos(nextInfos);
      setTotalInfos(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        ruleVersionId,
        issn,
        journalName,
        indexingType: indexingType || "KCI",
        publicationCountry,
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

  const selectRow = (info: JournalIndexingInfo) => {
    setSelected(info);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(info.ruleVersionId),
      issn: info.issn,
      journalName: info.journalName,
      indexingType: info.indexingType,
      publicationCountry: info.publicationCountry,
      validStartDate: info.validStartDate,
      validEndDate: info.validEndDate,
      sourceName: info.sourceName,
      sourceUpdatedAt: toDatetimeLocal(info.sourceUpdatedAt),
      activeYn: info.activeYn,
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
      "학술지·후보지 등재정보를 저장하시겠습니까?",
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await journalIndexingInfoApi.saveJournalIndexingInfo({
        ruleVersionId: Number(form.ruleVersionId),
        issn: form.issn.trim(),
        journalName: form.journalName.trim(),
        indexingType: form.indexingType,
        publicationCountry: form.publicationCountry.trim(),
        validStartDate: form.validStartDate,
        validEndDate: form.validEndDate,
        sourceName: form.sourceName.trim(),
        sourceUpdatedAt: form.sourceUpdatedAt.trim(),
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
        : "학술지·후보지 등재정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-JOURNAL-INDEXING-MGMT"
        data-testid="journal-indexing-info-page"
      >
        <PermissionState
          title="학술지·후보지 등재정보 관리 권한이 없습니다"
          message="R04, R08 또는 R09 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-JOURNAL-INDEXING-MGMT"
      data-testid="journal-indexing-info-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 학술지·후보지 등재정보 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              학술지·후보지 등재정보 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              ISSN·학술지명·등재구분·발행국가와 출처 갱신일시를 유효기간
              기준으로 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="journal-indexing-info-refresh-button"
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
        <ErrorState title="학술지·후보지 등재정보 오류" message={error} />
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
            label="ISSN"
            value={issn}
            field="issn"
            fieldErrors={{}}
            onChange={setIssn}
          />
          <TextInput
            label="학술지명"
            value={journalName}
            field="journalName"
            fieldErrors={{}}
            onChange={setJournalName}
          />
          <SelectInput
            label="등재구분"
            value={indexingType}
            values={indexingTypes}
            onChange={(value) =>
              setIndexingType(value as JournalIndexingType | "")
            }
            testId="journal-indexing-info-indexing-type-filter-select"
            includeAll
          />
          <TextInput
            label="발행국가"
            value={publicationCountry}
            field="publicationCountry"
            fieldErrors={{}}
            onChange={setPublicationCountry}
          />
          <SelectInput
            label="사용여부"
            value={activeYn}
            values={activeFlags}
            onChange={(value) => setActiveYn(value as ActiveYn | "")}
            testId="journal-indexing-info-active-filter-select"
            includeAll
          />
          <label className="text-sm font-semibold text-dark">
            조회조건
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="ISSN/학술지명/출처"
              data-testid="journal-indexing-info-keyword-input"
            />
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="journal-indexing-info-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            학술지 등재정보 목록
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
              data-testid="journal-indexing-info-page-size-select"
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
          <LoadingState title="학술지·후보지 등재정보 조회 중" />
        ) : null}
        {!loading && infos.length === 0 ? (
          <EmptyState
            title="조회된 학술지 등재정보가 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 신규 정보를 저장하세요."
          />
        ) : null}
        {!loading && infos.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">ISSN</th>
                  <th className="px-3 py-2">학술지명</th>
                  <th className="px-3 py-2">등재구분</th>
                  <th className="px-3 py-2">발행국가</th>
                  <th className="px-3 py-2">규정버전</th>
                  <th className="px-3 py-2">유효기간</th>
                  <th className="px-3 py-2">출처/갱신일시</th>
                  <th className="px-3 py-2">사용여부</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {infos.map((info) => (
                  <tr
                    key={`${info.ruleVersionId}-${info.issn}-${info.validStartDate}-${info.validEndDate}`}
                    className={
                      selected?.journalIndexingInfoId ===
                      info.journalIndexingInfoId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(info)}
                    data-testid="journal-indexing-info-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {info.issn}
                    </td>
                    <td className="px-3 py-2">{info.journalName}</td>
                    <td className="px-3 py-2">
                      {indexingTypeLabel(info.indexingType)}
                    </td>
                    <td className="px-3 py-2">{info.publicationCountry}</td>
                    <td className="px-3 py-2">
                      {info.versionCode} /{" "}
                      {versionStatusLabel(info.versionStatus)}
                    </td>
                    <td className="px-3 py-2">
                      {info.validStartDate} ~ {info.validEndDate}
                    </td>
                    <td className="px-3 py-2">
                      {info.sourceName} / {info.sourceUpdatedAt}
                    </td>
                    <td className="px-3 py-2">{activeLabel(info.activeYn)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {totalInfos}건 / {page + 1}페이지
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">
          학술지 등재정보 저장
        </h2>
        <p className="mt-2 text-sm text-muted">
          작성중 규정버전에서만 저장하며, 같은 ISSN과 유효기간이 중복되는
          등재정보는 등록할 수 없습니다.
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
            label="ISSN"
            value={form.issn}
            field="issn"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, issn: value })}
            required
          />
          <TextInput
            label="학술지명"
            value={form.journalName}
            field="journalName"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, journalName: value })}
            required
          />
          <SelectInput
            label="등재구분"
            value={form.indexingType}
            values={indexingTypes}
            onChange={(value) =>
              setForm({ ...form, indexingType: value as JournalIndexingType })
            }
            testId="journal-indexing-info-indexing-type-select"
            required
          />
          <TextInput
            label="발행국가"
            value={form.publicationCountry}
            field="publicationCountry"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, publicationCountry: value })
            }
            required
          />
          <SelectInput
            label="사용여부"
            value={form.activeYn}
            values={activeFlags}
            onChange={(value) =>
              setForm({ ...form, activeYn: value as ActiveYn })
            }
            testId="journal-indexing-info-active-yn-select"
            required
          />
          <TextInput
            label="유효시작일"
            value={form.validStartDate}
            field="validStartDate"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, validStartDate: value })}
            required
          />
          <TextInput
            label="유효종료일"
            value={form.validEndDate}
            field="validEndDate"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, validEndDate: value })}
            required
          />
          <TextInput
            label="출처"
            value={form.sourceName}
            field="sourceName"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, sourceName: value })}
            required
          />
          <TextInput
            label="갱신일시"
            value={form.sourceUpdatedAt}
            field="sourceUpdatedAt"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, sourceUpdatedAt: value })}
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
              data-testid="journal-indexing-info-change-reason-textarea"
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
          data-testid="journal-indexing-info-save-button"
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
        <div className="mt-4 rounded-md bg-lightsecondary p-4 text-xs text-muted">
          파일럿 seed와 R04 Excel 갱신 출처를 보존합니다. 개별 논문 실적 연결,
          평가점수·배분율·계산식 변경, 확정 평가결과 소급 변경은 제공하지
          않습니다.
        </div>
      </section>
    </section>
  );
}

function validateForm(form: FormState): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!form.ruleVersionId.trim())
    errors.ruleVersionId = "규정버전을 선택하세요.";
  if (!form.issn.trim()) errors.issn = "ISSN을 입력하세요.";
  if (!form.journalName.trim()) errors.journalName = "학술지명을 입력하세요.";
  if (!form.publicationCountry.trim())
    errors.publicationCountry = "발행국가를 입력하세요.";
  if (!form.validStartDate.trim())
    errors.validStartDate = "유효시작일을 입력하세요.";
  if (!form.validEndDate.trim())
    errors.validEndDate = "유효종료일을 입력하세요.";
  if (!form.sourceName.trim()) errors.sourceName = "출처를 입력하세요.";
  if (!form.sourceUpdatedAt.trim())
    errors.sourceUpdatedAt = "갱신일시를 입력하세요.";
  if (!form.changeReason.trim())
    errors.changeReason = "변경 사유를 입력하세요.";
  if (
    form.validStartDate &&
    form.validEndDate &&
    form.validEndDate < form.validStartDate
  ) {
    errors.validEndDate = "유효종료일은 시작일 이후여야 합니다.";
  }
  return errors;
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
  const inputType = field.toLowerCase().includes("date")
    ? field === "sourceUpdatedAt"
      ? "datetime-local"
      : "date"
    : "text";
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <input
        type={inputType}
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={`journal-indexing-info-${kebab(field)}-input`}
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
        {values.map((nextValue) => (
          <option key={nextValue} value={nextValue}>
            {labelForValue(nextValue)}
          </option>
        ))}
      </select>
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    accumulator[field.field] = field.message;
    return accumulator;
  }, {});
}

function labelForValue(value: string) {
  if (value === "Y") return "사용";
  if (value === "N") return "미사용";
  return indexingTypeLabel(value);
}

function indexingTypeLabel(value: string) {
  const labels: Record<string, string> = {
    KCI: "등재지",
    CANDIDATE: "후보지",
    INTERNATIONAL: "국제등재",
    OTHER: "기타",
  };
  return labels[value] ?? value;
}

function activeLabel(value: ActiveYn) {
  return value === "Y" ? "사용" : "미사용";
}

function versionStatusLabel(value: string) {
  const labels: Record<string, string> = {
    DRAFT: "작성중",
    CONFIRMED: "확정",
    DISCARDED: "폐기",
  };
  return labels[value] ?? value;
}

function kebab(value: string) {
  return value
    .replace(/[A-Z]/g, (character) => `-${character.toLowerCase()}`)
    .replace(/^-/, "");
}

function toDatetimeLocal(value: string) {
  return value.length >= 16 ? value.slice(0, 16) : value;
}
