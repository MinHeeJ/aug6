import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  basic60Api,
  courseAreaGroupGradeApi,
  type ActiveYn,
  type Basic60OperationalSetting,
  type CourseAreaGroupGrade,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type SettingKind = "element" | "participation" | "score";

type SettingConfig = {
  kind: SettingKind;
  screenId: string;
  testId: string;
  title: string;
  menuPath: string;
  description: string;
};

const settingConfigs: Record<SettingKind, SettingConfig> = {
  element: {
    kind: "element",
    screenId: "SCR-EVALUATION-ELEMENT-MANAGEMENT-ITEM-SETTINGS",
    testId: "evaluation-element-management-item-settings-page",
    title: "평가요소별 관리항목 설정",
    menuPath: "평가 기준 관리 / 평가 기준정보 관리 / 평가요소별 관리항목 설정",
    description:
      "평가요소별 관리항목의 적용대상, 교원 입력 가능 여부, 정렬순서와 사용여부를 관리합니다.",
  },
  participation: {
    kind: "participation",
    screenId: "SCR-PARTICIPATION-ALLOCATION-RATE-SETTINGS",
    testId: "participation-allocation-rate-settings-page",
    title: "참여구분별 배분율 설정",
    menuPath: "평가 기준 관리 / 평가 기준정보 관리 / 참여구분별 배분율 설정",
    description:
      "관리항목·연구자 수·참여구분별 배분율을 적용기간과 상태별로 관리합니다.",
  },
  score: {
    kind: "score",
    screenId: "SCR-MANAGEMENT-ITEM-EVALUATION-SCORE-SETTINGS",
    testId: "management-item-evaluation-score-settings-page",
    title: "관리항목별 평가점수 설정",
    menuPath: "평가 기준 관리 / 평가 기준정보 관리 / 관리항목별 평가점수 설정",
    description:
      "관리항목·소속대학별 평가점수와 상한점수를 적용기간과 상태별로 관리합니다.",
  },
};

type SearchState = {
  ruleVersionId: string;
  targetScope: string;
  areaCode: string;
  itemCode: string;
  evaluationYear: string;
  elementCode: string;
  managementItemCode: string;
  organizationCode: string;
  researcherCount: string;
  participationType: string;
  activeYn: ActiveYn | "";
  keyword: string;
};

type FormState = {
  ruleVersionId: string;
  targetScope: string;
  areaCode: string;
  itemCode: string;
  evaluationYear: string;
  elementCode: string;
  managementItemCode: string;
  managementItemName: string;
  organizationCode: string;
  organizationName: string;
  researcherCount: string;
  participationType: string;
  allocationRate: string;
  evaluationScore: string;
  maxScore: string;
  sortOrder: string;
  activeYn: ActiveYn;
  teacherEditableYn: ActiveYn;
  effectiveStartDate: string;
  effectiveEndDate: string;
  changeReason: string;
};

const initialSearch: SearchState = {
  ruleVersionId: "",
  targetScope: "",
  areaCode: "",
  itemCode: "",
  evaluationYear: "2026",
  elementCode: "",
  managementItemCode: "",
  organizationCode: "",
  researcherCount: "",
  participationType: "",
  activeYn: "",
  keyword: "",
};

const initialForm: FormState = {
  ruleVersionId: "",
  targetScope: "COLLEGE_EDU",
  areaCode: "EDUCATION",
  itemCode: "LECTURE",
  evaluationYear: "2026",
  elementCode: "COURSE_GROUP",
  managementItemCode: "",
  managementItemName: "",
  organizationCode: "",
  organizationName: "",
  researcherCount: "1",
  participationType: "SOLE",
  allocationRate: "1",
  evaluationScore: "0",
  maxScore: "",
  sortOrder: "1",
  activeYn: "Y",
  teacherEditableYn: "Y",
  effectiveStartDate: "2026-01-01",
  effectiveEndDate: "2026-12-31",
  changeReason: "",
};

export function EvaluationElementManagementItemSettingsPage() {
  return <OperationalSettingPage config={settingConfigs.element} />;
}

export function ParticipationAllocationRateSettingsPage() {
  return <OperationalSettingPage config={settingConfigs.participation} />;
}

export function ManagementItemEvaluationScoreSettingsPage() {
  return <OperationalSettingPage config={settingConfigs.score} />;
}

function OperationalSettingPage({ config }: { config: SettingConfig }) {
  const [search, setSearch] = useState<SearchState>(initialSearch);
  const [form, setForm] = useState<FormState>(initialForm);
  const [rows, setRows] = useState<Basic60OperationalSetting[]>([]);
  const [selected, setSelected] = useState<Basic60OperationalSetting | null>(
    null,
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
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
      const params = {
        ruleVersionId: search.ruleVersionId
          ? Number(search.ruleVersionId)
          : undefined,
        targetScope: search.targetScope || undefined,
        areaCode: search.areaCode || undefined,
        itemCode: search.itemCode || undefined,
        evaluationYear: search.evaluationYear || undefined,
        elementCode: search.elementCode || undefined,
        managementItemCode: search.managementItemCode || undefined,
        organizationCode: search.organizationCode || undefined,
        researcherCount: search.researcherCount
          ? Number(search.researcherCount)
          : undefined,
        participationType: search.participationType || undefined,
        activeYn: search.activeYn,
        keyword: search.keyword || undefined,
        page,
        pageSize,
      };
      const response =
        config.kind === "element"
          ? await basic60Api.listEvaluationElementManagementItemSettings(params)
          : config.kind === "participation"
            ? await basic60Api.listParticipationAllocationRateSettings(params)
            : await basic60Api.listManagementItemEvaluationScoreSettings(
                params,
              );
      const data = response.data;
      const nextRows =
        config.kind === "element"
          ? ((
              data as {
                evaluationElementManagementItemSettings?: Basic60OperationalSetting[];
              }
            )?.evaluationElementManagementItemSettings ?? [])
          : config.kind === "participation"
            ? ((
                data as {
                  participationAllocationRateSettings?: Basic60OperationalSetting[];
                }
              )?.participationAllocationRateSettings ?? [])
            : ((
                data as {
                  managementItemEvaluationScoreSettings?: Basic60OperationalSetting[];
                }
              )?.managementItemEvaluationScoreSettings ?? []);
      setRows(nextRows);
      setTotalElements(data?.totalElements ?? 0);
      setSelected(null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = (row: Basic60OperationalSetting) => {
    setSelected(row);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(row.ruleVersionId),
      targetScope: row.targetScope,
      areaCode: row.areaCode,
      itemCode: row.itemCode,
      evaluationYear: row.evaluationYear,
      elementCode: row.elementCode,
      managementItemCode: row.managementItemCode,
      managementItemName: row.managementItemName ?? "",
      organizationCode: row.organizationCode ?? "",
      organizationName: row.organizationName ?? "",
      researcherCount:
        row.researcherCount == null ? "1" : String(row.researcherCount),
      participationType: row.participationType ?? "SOLE",
      allocationRate:
        row.allocationRate == null ? "1" : String(row.allocationRate),
      evaluationScore:
        row.evaluationScore == null ? "0" : String(row.evaluationScore),
      maxScore: row.maxScore == null ? "" : String(row.maxScore),
      sortOrder: row.sortOrder == null ? "1" : String(row.sortOrder),
      activeYn: row.activeYn,
      teacherEditableYn: row.teacherEditableYn ?? "Y",
      effectiveStartDate: row.effectiveStartDate,
      effectiveEndDate: row.effectiveEndDate,
      changeReason: "",
    });
  };

  const save = async () => {
    const errors = validateSettingForm(config.kind, form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      setError("필수 입력값을 확인하세요.");
      return;
    }
    if (!window.confirm(`${config.title} 값을 저장하시겠습니까?`)) return;
    try {
      setSaving(true);
      setError(null);
      const common = {
        ruleVersionId: Number(form.ruleVersionId),
        targetScope: form.targetScope.trim(),
        areaCode: form.areaCode.trim(),
        itemCode: form.itemCode.trim(),
        evaluationYear: form.evaluationYear.trim(),
        elementCode: form.elementCode.trim(),
        managementItemCode: form.managementItemCode.trim(),
        activeYn: form.activeYn,
        effectiveStartDate: form.effectiveStartDate,
        effectiveEndDate: form.effectiveEndDate,
        changeReason: form.changeReason.trim(),
      };
      const response =
        config.kind === "element"
          ? await basic60Api.saveEvaluationElementManagementItemSetting({
              ...common,
              managementItemName: form.managementItemName.trim(),
              sortOrder: Number(form.sortOrder),
              teacherEditableYn: form.teacherEditableYn,
            })
          : config.kind === "participation"
            ? await basic60Api.saveParticipationAllocationRateSetting({
                ...common,
                researcherCount: Number(form.researcherCount),
                participationType: form.participationType.trim(),
                allocationRate: Number(form.allocationRate),
              })
            : await basic60Api.saveManagementItemEvaluationScoreSetting({
                ...common,
                organizationCode: form.organizationCode.trim(),
                organizationName: form.organizationName.trim(),
                evaluationScore: Number(form.evaluationScore),
                maxScore: form.maxScore.trim() ? Number(form.maxScore) : null,
                sortOrder: Number(form.sortOrder),
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
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      return;
    }
    setError(caught instanceof Error ? caught.message : "처리하지 못했습니다.");
  };

  if (permissionDenied) {
    return (
      <section data-screen-id={config.screenId} data-testid={config.testId}>
        <PermissionState
          title={`${config.title} 권한이 없습니다`}
          message="R04 업무담당자 또는 R09 시스템관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id={config.screenId}
      data-testid={config.testId}
    >
      <Header config={config} onRefresh={() => void load()} />
      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 목록 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title={`${config.title} 오류`} message={error} />
      ) : null}
      <SearchPanel
        search={search}
        setSearch={setSearch}
        kind={config.kind}
        onSearch={() => {
          setPage(0);
          void load();
        }}
      />
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            목록 ({totalElements}건)
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
              data-testid={`${config.kind}-page-size-select`}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="목록 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회 결과가 없습니다"
            message="조회조건을 변경하거나 작성중 규정버전에서 값을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <SettingsTable
            rows={rows}
            selected={selected}
            selectRow={selectRow}
            kind={config.kind}
          />
        ) : null}
      </section>
      <SettingForm
        kind={config.kind}
        form={form}
        setForm={setForm}
        fieldErrors={fieldErrors}
        onSave={() => void save()}
        saving={saving}
      />
    </section>
  );
}

function Header({
  config,
  onRefresh,
}: {
  config: SettingConfig;
  onRefresh: () => void;
}) {
  return (
    <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm text-link">{config.menuPath}</p>
          <h1 className="mt-2 text-xl font-semibold text-dark">
            {config.title}
          </h1>
          <p className="mt-2 text-sm text-muted">{config.description}</p>
        </div>
        <button
          type="button"
          className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          onClick={onRefresh}
          data-testid={`${config.kind}-refresh-button`}
        >
          <RefreshCw size={16} /> 새로고침
        </button>
      </div>
    </div>
  );
}

function SearchPanel({
  search,
  setSearch,
  kind,
  onSearch,
}: {
  search: SearchState;
  setSearch: (next: SearchState) => void;
  kind: SettingKind;
  onSearch: () => void;
}) {
  const update = (field: keyof SearchState) => (value: string) =>
    setSearch({ ...search, [field]: value });
  return (
    <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
      <div className="grid gap-4 md:grid-cols-8">
        <TextInput
          label="규정버전 ID"
          value={search.ruleVersionId}
          field="ruleVersionId"
          fieldErrors={{}}
          onChange={update("ruleVersionId")}
        />
        <TextInput
          label="적용대상"
          value={search.targetScope}
          field="targetScope"
          fieldErrors={{}}
          onChange={update("targetScope")}
        />
        <TextInput
          label="평가영역"
          value={search.areaCode}
          field="areaCode"
          fieldErrors={{}}
          onChange={update("areaCode")}
        />
        <TextInput
          label="평가항목"
          value={search.itemCode}
          field="itemCode"
          fieldErrors={{}}
          onChange={update("itemCode")}
        />
        <TextInput
          label="평가연도"
          value={search.evaluationYear}
          field="evaluationYear"
          fieldErrors={{}}
          onChange={update("evaluationYear")}
        />
        {kind === "score" ? (
          <TextInput
            label="소속대학"
            value={search.organizationCode}
            field="organizationCode"
            fieldErrors={{}}
            onChange={update("organizationCode")}
          />
        ) : null}
        {kind === "participation" ? (
          <TextInput
            label="참여구분"
            value={search.participationType}
            field="participationType"
            fieldErrors={{}}
            onChange={update("participationType")}
          />
        ) : null}
        <TextInput
          label="조회조건"
          value={search.keyword}
          field="keyword"
          fieldErrors={{}}
          onChange={update("keyword")}
        />
        <button
          type="button"
          className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
          onClick={onSearch}
          data-testid={`${kind}-search-button`}
        >
          <Search size={16} /> 조회
        </button>
      </div>
    </section>
  );
}

function SettingsTable({
  rows,
  selected,
  selectRow,
  kind,
}: {
  rows: Basic60OperationalSetting[];
  selected: Basic60OperationalSetting | null;
  selectRow: (row: Basic60OperationalSetting) => void;
  kind: SettingKind;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-ld text-sm">
        <thead className="bg-lightsecondary text-left text-muted">
          <tr>
            <th className="px-3 py-2">적용대상</th>
            <th className="px-3 py-2">평가체계</th>
            <th className="px-3 py-2">관리항목</th>
            {kind === "participation" ? (
              <th className="px-3 py-2">연구자/참여/배분율</th>
            ) : null}
            {kind === "score" ? <th className="px-3 py-2">소속/점수</th> : null}
            {kind === "element" ? (
              <th className="px-3 py-2">입력/정렬</th>
            ) : null}
            <th className="px-3 py-2">적용기간</th>
            <th className="px-3 py-2">상태</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-ld">
          {rows.map((row) => (
            <tr
              key={row.settingId}
              className={
                selected?.settingId === row.settingId
                  ? "bg-lightprimary"
                  : "hover:bg-lightgray"
              }
              onClick={() => selectRow(row)}
              data-testid={`${kind}-settings-row`}
            >
              <td className="px-3 py-2">{row.targetScope}</td>
              <td className="px-3 py-2">
                {row.areaCode} / {row.itemCode} / {row.evaluationYear} /{" "}
                {row.elementCode}
              </td>
              <td className="px-3 py-2">
                {row.managementItemCode}
                {row.managementItemName ? ` / ${row.managementItemName}` : ""}
              </td>
              {kind === "participation" ? (
                <td className="px-3 py-2">
                  {row.researcherCount}명 / {row.participationType} /{" "}
                  {row.allocationRate}
                </td>
              ) : null}
              {kind === "score" ? (
                <td className="px-3 py-2">
                  {row.organizationCode} / {row.evaluationScore} (상한{" "}
                  {row.maxScore ?? "-"})
                </td>
              ) : null}
              {kind === "element" ? (
                <td className="px-3 py-2">
                  교원입력 {ynLabel(row.teacherEditableYn ?? "N")} /{" "}
                  {row.sortOrder}
                </td>
              ) : null}
              <td className="px-3 py-2">
                {row.effectiveStartDate} ~ {row.effectiveEndDate}
              </td>
              <td className="px-3 py-2">
                {ynLabel(row.activeYn)} / {row.ruleVersionStatus}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SettingForm({
  kind,
  form,
  setForm,
  fieldErrors,
  onSave,
  saving,
}: {
  kind: SettingKind;
  form: FormState;
  setForm: (next: FormState) => void;
  fieldErrors: Record<string, string>;
  onSave: () => void;
  saving: boolean;
}) {
  const update = (field: keyof FormState) => (value: string) =>
    setForm({ ...form, [field]: value });
  return (
    <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-dark">설정 상세</h2>
        <button
          type="button"
          className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          onClick={onSave}
          disabled={saving}
          data-testid={`${kind}-save-button`}
        >
          <Save size={16} /> {saving ? "저장 중" : "저장"}
        </button>
      </div>
      <div className="grid gap-4 md:grid-cols-4">
        <TextInput
          label="규정버전 ID *"
          value={form.ruleVersionId}
          field="ruleVersionId"
          fieldErrors={fieldErrors}
          onChange={update("ruleVersionId")}
        />
        <TextInput
          label="적용대상 *"
          value={form.targetScope}
          field="targetScope"
          fieldErrors={fieldErrors}
          onChange={update("targetScope")}
        />
        <TextInput
          label="평가영역 *"
          value={form.areaCode}
          field="areaCode"
          fieldErrors={fieldErrors}
          onChange={update("areaCode")}
        />
        <TextInput
          label="평가항목 *"
          value={form.itemCode}
          field="itemCode"
          fieldErrors={fieldErrors}
          onChange={update("itemCode")}
        />
        <TextInput
          label="평가연도 *"
          value={form.evaluationYear}
          field="evaluationYear"
          fieldErrors={fieldErrors}
          onChange={update("evaluationYear")}
        />
        <TextInput
          label="평가요소 *"
          value={form.elementCode}
          field="elementCode"
          fieldErrors={fieldErrors}
          onChange={update("elementCode")}
        />
        <TextInput
          label="관리항목 코드 *"
          value={form.managementItemCode}
          field="managementItemCode"
          fieldErrors={fieldErrors}
          onChange={update("managementItemCode")}
        />
        {kind === "element" ? (
          <TextInput
            label="관리항목명 *"
            value={form.managementItemName}
            field="managementItemName"
            fieldErrors={fieldErrors}
            onChange={update("managementItemName")}
          />
        ) : null}
        {kind === "participation" ? (
          <TextInput
            label="연구자 수 *"
            value={form.researcherCount}
            field="researcherCount"
            fieldErrors={fieldErrors}
            onChange={update("researcherCount")}
          />
        ) : null}
        {kind === "participation" ? (
          <TextInput
            label="참여구분 *"
            value={form.participationType}
            field="participationType"
            fieldErrors={fieldErrors}
            onChange={update("participationType")}
          />
        ) : null}
        {kind === "participation" ? (
          <TextInput
            label="배분율 *"
            value={form.allocationRate}
            field="allocationRate"
            fieldErrors={fieldErrors}
            onChange={update("allocationRate")}
          />
        ) : null}
        {kind === "score" ? (
          <TextInput
            label="소속대학 코드 *"
            value={form.organizationCode}
            field="organizationCode"
            fieldErrors={fieldErrors}
            onChange={update("organizationCode")}
          />
        ) : null}
        {kind === "score" ? (
          <TextInput
            label="소속대학명"
            value={form.organizationName}
            field="organizationName"
            fieldErrors={fieldErrors}
            onChange={update("organizationName")}
          />
        ) : null}
        {kind === "score" ? (
          <TextInput
            label="평가점수 *"
            value={form.evaluationScore}
            field="evaluationScore"
            fieldErrors={fieldErrors}
            onChange={update("evaluationScore")}
          />
        ) : null}
        {kind === "score" ? (
          <TextInput
            label="상한점수"
            value={form.maxScore}
            field="maxScore"
            fieldErrors={fieldErrors}
            onChange={update("maxScore")}
          />
        ) : null}
        {kind !== "participation" ? (
          <TextInput
            label="정렬순서 *"
            value={form.sortOrder}
            field="sortOrder"
            fieldErrors={fieldErrors}
            onChange={update("sortOrder")}
          />
        ) : null}
        {kind === "element" ? (
          <SelectInput
            label="교원 입력 가능"
            value={form.teacherEditableYn}
            onChange={update("teacherEditableYn")}
            testId="teacher-editable-select"
          />
        ) : null}
        <SelectInput
          label="사용여부"
          value={form.activeYn}
          onChange={update("activeYn")}
          testId="active-yn-select"
        />
        <TextInput
          label="적용시작일 *"
          value={form.effectiveStartDate}
          field="effectiveStartDate"
          fieldErrors={fieldErrors}
          onChange={update("effectiveStartDate")}
          type="date"
        />
        <TextInput
          label="적용종료일 *"
          value={form.effectiveEndDate}
          field="effectiveEndDate"
          fieldErrors={fieldErrors}
          onChange={update("effectiveEndDate")}
          type="date"
        />
        <TextInput
          label="변경 사유 *"
          value={form.changeReason}
          field="changeReason"
          fieldErrors={fieldErrors}
          onChange={update("changeReason")}
        />
      </div>
    </section>
  );
}

function validateSettingForm(kind: SettingKind, form: FormState) {
  const errors: Record<string, string> = {};
  [
    "ruleVersionId",
    "targetScope",
    "areaCode",
    "itemCode",
    "evaluationYear",
    "elementCode",
    "managementItemCode",
    "effectiveStartDate",
    "effectiveEndDate",
    "changeReason",
  ].forEach((field) => {
    if (!String(form[field as keyof FormState]).trim())
      errors[field] = "필수 입력입니다.";
  });
  if (kind === "element" && !form.managementItemName.trim())
    errors.managementItemName = "관리항목명을 입력하세요.";
  if (kind === "participation") {
    if (!form.researcherCount.trim())
      errors.researcherCount = "연구자 수를 입력하세요.";
    if (!form.participationType.trim())
      errors.participationType = "참여구분을 입력하세요.";
    if (!form.allocationRate.trim())
      errors.allocationRate = "배분율을 입력하세요.";
  }
  if (kind === "score") {
    if (!form.organizationCode.trim())
      errors.organizationCode = "소속대학 코드를 입력하세요.";
    if (!form.evaluationScore.trim())
      errors.evaluationScore = "평가점수를 입력하세요.";
  }
  return errors;
}

export function CourseAreaGroupGradeQueryPage() {
  const [completionType, setCompletionType] = useState("");
  const [semester, setSemester] = useState("2026-1");
  const [courseArea, setCourseArea] = useState("");
  const [keyword, setKeyword] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [rows, setRows] = useState<CourseAreaGroupGrade[]>([]);
  const [selected, setSelected] = useState<CourseAreaGroupGrade | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await courseAreaGroupGradeApi.listCourseAreaGroupGrades({
        completionType,
        semester,
        courseArea,
        keyword,
        pageSize,
      });
      setRows(response.data?.courseAreaGroupGrades ?? []);
      setSelected(null);
      setSuccessMessage("조회가 완료되었습니다");
    } catch (caught) {
      if (caught instanceof ApiClientError) {
        if (caught.status === 403) setPermissionDenied(true);
        setError(caught.message);
      } else {
        setError(
          caught instanceof Error
            ? caught.message
            : "성적을 조회하지 못했습니다.",
        );
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const summary = useMemo(() => selected ?? rows[0] ?? null, [selected, rows]);

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-COURSE-AREA-GROUP-GRADE-QUERY"
        data-testid="course-area-group-grade-query-page"
      >
        <PermissionState
          title="교과영역 그룹평가 성적 조회 권한이 없습니다"
          message="본인 또는 허용된 데이터 범위만 조회할 수 있습니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-AREA-GROUP-GRADE-QUERY"
      data-testid="course-area-group-grade-query-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">
          교원 포털 / 성적 조회 / 교과영역 그룹평가 성적 조회
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          교과영역 그룹평가 성적 조회
        </h1>
        <p className="mt-2 text-sm text-muted">
          이수구분·학기·교과영역 조건으로 확정 공개된 그룹평가 성적을
          조회합니다. 이 화면에서는 성적을 생성·수정·삭제할 수 없습니다.
        </p>
      </div>
      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="조회 이력이 기록됩니다."
        />
      ) : null}
      {error ? <ErrorState title="성적 조회 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-6">
          <TextInput
            label="이수구분"
            value={completionType}
            field="completionType"
            fieldErrors={{}}
            onChange={setCompletionType}
          />
          <TextInput
            label="학기"
            value={semester}
            field="semester"
            fieldErrors={{}}
            onChange={setSemester}
          />
          <TextInput
            label="교과영역"
            value={courseArea}
            field="courseArea"
            fieldErrors={{}}
            onChange={setCourseArea}
          />
          <TextInput
            label="조회조건"
            value={keyword}
            field="keyword"
            fieldErrors={{}}
            onChange={setKeyword}
          />
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
              data-testid="course-grade-page-size-select"
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
            onClick={() => void load()}
            data-testid="course-grade-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>
      <section className="grid gap-6 lg:grid-cols-3">
        <div className="rounded-md border border-ld bg-white p-6 shadow-sm lg:col-span-2">
          <h2 className="mb-4 text-lg font-semibold text-dark">성적 목록</h2>
          {loading ? <LoadingState title="성적 조회 중" /> : null}
          {!loading && rows.length === 0 ? (
            <EmptyState
              title="조회된 성적이 없습니다"
              message="조회조건을 변경하세요."
            />
          ) : null}
          {!loading && rows.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">교원</th>
                    <th className="px-3 py-2">이수구분</th>
                    <th className="px-3 py-2">학기</th>
                    <th className="px-3 py-2">교과영역</th>
                    <th className="px-3 py-2">성적</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {rows.map((row) => (
                    <tr
                      key={row.resultId}
                      className={
                        selected?.resultId === row.resultId
                          ? "bg-lightprimary"
                          : "hover:bg-lightgray"
                      }
                      onClick={() => setSelected(row)}
                      data-testid="course-grade-row"
                    >
                      <td className="px-3 py-2">
                        {row.facultyName} ({row.employeeNo})
                      </td>
                      <td className="px-3 py-2">{row.completionType}</td>
                      <td className="px-3 py-2">{row.semester}</td>
                      <td className="px-3 py-2">{row.courseArea}</td>
                      <td className="px-3 py-2 font-semibold text-primary">
                        {row.groupGrade}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
        <aside className="rounded-md border border-ld bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-dark">상세</h2>
          {summary ? (
            <dl className="mt-4 space-y-3 text-sm">
              <Detail
                label="교원"
                value={`${summary.facultyName} (${summary.employeeNo})`}
              />
              <Detail label="이수구분" value={summary.completionType} />
              <Detail label="학기" value={summary.semester} />
              <Detail label="교과영역" value={summary.courseArea} />
              <Detail
                label="그룹평가 성적"
                value={String(summary.groupGrade)}
              />
              <Detail label="산출근거" value={summary.detailSummary} />
            </dl>
          ) : (
            <p className="mt-4 text-sm text-muted">
              목록 행을 선택하면 상세를 표시합니다.
            </p>
          )}
        </aside>
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
  type = "text",
}: {
  label: string;
  value: string;
  field: string;
  fieldErrors: Record<string, string>;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <input
        type={type}
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={Boolean(fieldErrors[field])}
        data-testid={`${field}-input`}
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
  onChange,
  testId,
}: {
  label: string;
  value: ActiveYn;
  onChange: (value: string) => void;
  testId: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <select
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      >
        <option value="Y">사용</option>
        <option value="N">미사용</option>
      </select>
    </label>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-muted">{label}</dt>
      <dd className="mt-1 font-semibold text-dark">{value}</dd>
    </div>
  );
}

function ynLabel(value: ActiveYn) {
  return value === "Y" ? "사용" : "미사용";
}
