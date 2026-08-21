import type React from "react";
import { useEffect, useState } from "react";
import { RefreshCw, Save, Search } from "lucide-react";
import {
  ApiClientError,
  operationsApi,
  type DataScopeRule,
  type DataScopeRulePayload,
  type DataScopeType,
  type DutyAssignment,
  type DutyAssignmentPayload,
  type PageSize,
  type PositionAssignment,
  type PositionAssignmentPayload,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type ScreenStatus =
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";
type FieldErrors = Record<string, string>;

const pageSizes: PageSize[] = [20, 50, 100];
const dataScopeOptions: { value: DataScopeType; label: string }[] = [
  { value: "SELF", label: "본인" },
  { value: "DEPARTMENT", label: "소속학과" },
  { value: "COLLEGE", label: "단과대학" },
  { value: "DUTY", label: "담당업무" },
  { value: "ALL", label: "전체" },
];

const emptyPositionForm: PositionAssignmentPayload = {
  positionCode: "",
  userId: "",
  organizationCode: "",
  effectiveStartDate: "",
  effectiveEndDate: "",
  changeReason: "",
};

const emptyDutyForm: DutyAssignmentPayload = {
  dutyOrganization: "",
  userId: "",
  dutyArea: "",
  validStartDate: "",
  validEndDate: "",
  dataScopeType: "DUTY",
  processingPermission: "",
  changeReason: "",
};

const emptyScopeForm: DataScopeRulePayload = {
  roleCode: "",
  dataScopeType: "SELF",
  organizationCode: "",
  dutyArea: "",
  changeReason: "",
};

export function PositionAssignmentManagementPage() {
  const [rows, setRows] = useState<PositionAssignment[]>([]);
  const [status, setStatus] = useState<ScreenStatus>("loading");
  const [message, setMessage] = useState("");
  const [filter, setFilter] = useState("");
  const [referenceDate, setReferenceDate] = useState("");
  const [size, setSize] = useState<PageSize>(20);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] =
    useState<PositionAssignmentPayload>(emptyPositionForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const load = async () => {
    setStatus("loading");
    setFieldErrors({});
    try {
      const response = await operationsApi.searchPositionAssignments({
        filter,
        referenceDate,
        size,
      });
      const assignments = response.data?.assignments ?? [];
      setRows(assignments);
      setStatus(assignments.length === 0 ? "empty" : "loaded");
    } catch (caught) {
      handleError(caught, setStatus, setMessage, setFieldErrors);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const save = async () => {
    if (
      !validateRequired(
        form,
        [
          "positionCode",
          "userId",
          "organizationCode",
          "effectiveStartDate",
          "changeReason",
        ],
        setFieldErrors,
      )
    )
      return;
    if (!window.confirm("보직 지정 정보를 저장하시겠습니까?")) return;
    try {
      if (selectedId)
        await operationsApi.updatePositionAssignment(
          selectedId,
          normalizePositionPayload(form),
        );
      else
        await operationsApi.savePositionAssignment(
          normalizePositionPayload(form),
        );
      setStatus("success");
      setMessage("보직 지정 정보가 저장되었습니다.");
      setForm(emptyPositionForm);
      setSelectedId(null);
      await load();
    } catch (caught) {
      handleError(caught, setStatus, setMessage, setFieldErrors);
    }
  };

  return (
    <section
      data-testid="position-assignment-management-page"
      className="space-y-6"
    >
      <ScreenHeader
        title="보직 관리"
        menuPath="시스템 관리 > 사용자·조직 관리 > 보직 관리"
        description="보직코드별 대상 사용자·소속조직·유효기간을 관리합니다. 사용자 인사정보와 조직구조는 이 화면에서 변경하지 않습니다."
      />
      <FilterBar
        filter={filter}
        setFilter={setFilter}
        referenceDate={referenceDate}
        setReferenceDate={setReferenceDate}
        size={size}
        setSize={setSize}
        onSearch={load}
      />
      <StatusBlock status={status} message={message} />
      <div className="grid grid-cols-12 gap-6">
        <section className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-7">
          <h2 className="text-lg font-semibold text-dark">보직 대상자 목록</h2>
          {rows.length === 0 ? (
            <EmptyState
              title="조회 결과가 없습니다"
              message="검색조건을 변경하거나 신규 보직을 등록하세요."
            />
          ) : (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-lightsecondary text-muted">
                  <tr>
                    <th className="p-3">보직코드</th>
                    <th className="p-3">사용자</th>
                    <th className="p-3">조직</th>
                    <th className="p-3">유효기간</th>
                    <th className="p-3">상태</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.positionAssignmentId}
                      data-testid="position-assignment-row"
                      className="cursor-pointer border-b border-ld hover:bg-lightprimary"
                      onClick={() => {
                        setSelectedId(row.positionAssignmentId);
                        setForm({
                          positionCode: row.positionCode,
                          userId: String(row.userId),
                          organizationCode: row.organizationCode,
                          effectiveStartDate: row.effectiveStartDate,
                          effectiveEndDate: row.effectiveEndDate ?? "",
                          changeReason: "",
                        });
                      }}
                    >
                      <td className="p-3 font-semibold text-primary">
                        {row.positionCode}
                      </td>
                      <td className="p-3">{row.userName ?? row.userId}</td>
                      <td className="p-3">
                        {row.organizationName ?? row.organizationCode}
                      </td>
                      <td className="p-3">
                        {row.effectiveStartDate} ~{" "}
                        {row.effectiveEndDate ?? "종료일 없음"}
                      </td>
                      <td className="p-3">{row.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <PositionForm
          form={form}
          setForm={setForm}
          fieldErrors={fieldErrors}
          onSave={save}
          selectedId={selectedId}
        />
      </div>
    </section>
  );
}

export function DutyAssignmentManagementPage() {
  const [rows, setRows] = useState<DutyAssignment[]>([]);
  const [status, setStatus] = useState<ScreenStatus>("loading");
  const [message, setMessage] = useState("");
  const [filter, setFilter] = useState("");
  const [referenceDate, setReferenceDate] = useState("");
  const [size, setSize] = useState<PageSize>(20);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<DutyAssignmentPayload>(emptyDutyForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const load = async () => {
    setStatus("loading");
    setFieldErrors({});
    try {
      const response = await operationsApi.searchDutyAssignments({
        filter,
        referenceDate,
        size,
      });
      const assignments = response.data?.assignments ?? [];
      setRows(assignments);
      setStatus(assignments.length === 0 ? "empty" : "loaded");
    } catch (caught) {
      handleError(caught, setStatus, setMessage, setFieldErrors);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const save = async () => {
    if (
      !validateRequired(
        form,
        [
          "dutyOrganization",
          "userId",
          "dutyArea",
          "validStartDate",
          "dataScopeType",
          "processingPermission",
          "changeReason",
        ],
        setFieldErrors,
      )
    )
      return;
    if (
      !window.confirm(
        "업무담당자 지정 정보를 저장하시겠습니까? 겹치는 지정은 최신 확정 정보가 우선 적용됩니다.",
      )
    )
      return;
    try {
      if (selectedId)
        await operationsApi.updateDutyAssignment(
          selectedId,
          normalizeDutyPayload(form),
        );
      else await operationsApi.saveDutyAssignment(normalizeDutyPayload(form));
      setStatus("success");
      setMessage("업무담당자 지정 정보가 저장되었습니다.");
      setForm(emptyDutyForm);
      setSelectedId(null);
      await load();
    } catch (caught) {
      handleError(caught, setStatus, setMessage, setFieldErrors);
    }
  };

  return (
    <section
      data-testid="duty-assignment-management-page"
      className="space-y-6"
    >
      <ScreenHeader
        title="업무담당자 관리"
        menuPath="시스템 관리 > 사용자·조직 관리 > 업무담당자 관리"
        description="업무조직별 담당자와 담당영역, 지정기간, 데이터 범위·처리 권한을 관리합니다."
      />
      <div className="rounded-md border border-warning bg-lightwarning p-4 text-sm text-warning">
        겹치는 담당자 지정은 허용되며 서버 권한 판정에서는 confirmed_at이 가장
        최신인 확정 지정이 우선 적용됩니다.
      </div>
      <FilterBar
        filter={filter}
        setFilter={setFilter}
        referenceDate={referenceDate}
        setReferenceDate={setReferenceDate}
        size={size}
        setSize={setSize}
        onSearch={load}
      />
      <StatusBlock status={status} message={message} />
      <div className="grid grid-cols-12 gap-6">
        <section className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-7">
          <h2 className="text-lg font-semibold text-dark">업무담당자 목록</h2>
          {rows.length === 0 ? (
            <EmptyState
              title="조회 결과가 없습니다"
              message="검색조건을 변경하거나 신규 담당자를 등록하세요."
            />
          ) : (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-lightsecondary text-muted">
                  <tr>
                    <th className="p-3">업무조직</th>
                    <th className="p-3">담당자</th>
                    <th className="p-3">업무영역</th>
                    <th className="p-3">지정기간</th>
                    <th className="p-3">범위/권한</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.dutyAssignmentId}
                      data-testid="duty-assignment-row"
                      className="cursor-pointer border-b border-ld hover:bg-lightprimary"
                      onClick={() => {
                        setSelectedId(row.dutyAssignmentId);
                        setForm({
                          dutyOrganization: row.dutyOrganization,
                          userId: String(row.userId),
                          dutyArea: row.dutyArea,
                          validStartDate: row.validStartDate,
                          validEndDate: row.validEndDate ?? "",
                          dataScopeType: row.dataScopeType,
                          processingPermission: row.processingPermission,
                          changeReason: "",
                        });
                      }}
                    >
                      <td className="p-3 font-semibold text-primary">
                        {row.dutyOrganization}
                      </td>
                      <td className="p-3">{row.userName ?? row.userId}</td>
                      <td className="p-3">{row.dutyArea}</td>
                      <td className="p-3">
                        {row.validStartDate} ~{" "}
                        {row.validEndDate ?? "종료일 없음"}
                      </td>
                      <td className="p-3">
                        {row.dataScopeType} / {row.processingPermission}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <DutyForm
          form={form}
          setForm={setForm}
          fieldErrors={fieldErrors}
          onSave={save}
          selectedId={selectedId}
        />
      </div>
    </section>
  );
}

export function DataScopeRuleManagementPage() {
  const [rows, setRows] = useState<DataScopeRule[]>([]);
  const [status, setStatus] = useState<ScreenStatus>("loading");
  const [message, setMessage] = useState("");
  const [filter, setFilter] = useState("");
  const [size, setSize] = useState<PageSize>(20);
  const [form, setForm] = useState<DataScopeRulePayload>(emptyScopeForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const load = async () => {
    setStatus("loading");
    setFieldErrors({});
    try {
      const response = await operationsApi.searchDataScopeRules({
        filter,
        size,
      });
      const rules = response.data?.rules ?? [];
      setRows(rules);
      setStatus(rules.length === 0 ? "empty" : "loaded");
    } catch (caught) {
      handleError(caught, setStatus, setMessage, setFieldErrors);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const save = async () => {
    const required =
      form.dataScopeType === "DUTY"
        ? ["roleCode", "dataScopeType", "dutyArea"]
        : ["roleCode", "dataScopeType"];
    if (!validateRequired(form, required, setFieldErrors)) return;
    if (
      !window.confirm(
        "데이터 범위 권한 규칙을 저장하시겠습니까? 여러 역할의 범위는 합집합으로 적용됩니다.",
      )
    )
      return;
    try {
      await operationsApi.saveDataScopeRules(normalizeScopePayload(form));
      setStatus("success");
      setMessage("데이터 범위 규칙이 저장되었습니다.");
      setForm(emptyScopeForm);
      await load();
    } catch (caught) {
      handleError(caught, setStatus, setMessage, setFieldErrors);
    }
  };

  return (
    <section
      data-testid="data-scope-rule-management-page"
      className="space-y-6"
    >
      <ScreenHeader
        title="데이터 범위 권한"
        menuPath="시스템 관리 > 역할·권한 관리 > 데이터 범위 권한"
        description="역할별 데이터 범위 유형을 서버 조회조건에 적용하도록 관리합니다. 역할 자체와 전체 권한 매트릭스는 변경하지 않습니다."
      />
      <FilterBar
        filter={filter}
        setFilter={setFilter}
        size={size}
        setSize={setSize}
        onSearch={load}
      />
      <div className="rounded-md border border-ld bg-white p-4 text-sm text-muted">
        범위 밖 데이터 조건은 서버에서 제외됩니다. 한 사용자의 여러 역할 범위는
        SELF, DEPARTMENT, COLLEGE, DUTY, ALL 규칙의 합집합으로 해석됩니다.
      </div>
      <StatusBlock status={status} message={message} />
      <div className="grid grid-cols-12 gap-6">
        <section className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-7">
          <h2 className="text-lg font-semibold text-dark">
            역할별 데이터 범위 목록
          </h2>
          {rows.length === 0 ? (
            <EmptyState
              title="조회 결과가 없습니다"
              message="검색조건을 변경하거나 규칙을 저장하세요."
            />
          ) : (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-lightsecondary text-muted">
                  <tr>
                    <th className="p-3">역할</th>
                    <th className="p-3">범위</th>
                    <th className="p-3">조직 조건</th>
                    <th className="p-3">업무영역</th>
                    <th className="p-3">수정일시</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.dataScopeRuleId}
                      data-testid="data-scope-rule-row"
                      className="cursor-pointer border-b border-ld hover:bg-lightprimary"
                      onClick={() =>
                        setForm({
                          roleCode: row.roleCode,
                          dataScopeType: row.dataScopeType,
                          organizationCode: row.organizationCode ?? "",
                          dutyArea: row.dutyArea ?? "",
                          changeReason: "",
                        })
                      }
                    >
                      <td className="p-3 font-semibold text-primary">
                        {row.roleCode} {row.roleName ?? ""}
                      </td>
                      <td className="p-3">{scopeLabel(row.dataScopeType)}</td>
                      <td className="p-3">
                        {row.organizationName ?? row.organizationCode ?? "전체"}
                      </td>
                      <td className="p-3">{row.dutyArea ?? "-"}</td>
                      <td className="p-3">{row.updatedAt ?? "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <ScopeForm
          form={form}
          setForm={setForm}
          fieldErrors={fieldErrors}
          onSave={save}
        />
      </div>
    </section>
  );
}

function ScreenHeader({
  title,
  menuPath,
  description,
}: {
  title: string;
  menuPath: string;
  description: string;
}) {
  return (
    <header className="rounded-md bg-lightsecondary p-6 shadow-none">
      <p className="text-sm font-semibold text-primary">{menuPath}</p>
      <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
      <p className="mt-2 text-sm text-muted">{description}</p>
    </header>
  );
}

function FilterBar({
  filter,
  setFilter,
  referenceDate,
  setReferenceDate,
  size,
  setSize,
  onSearch,
}: {
  filter: string;
  setFilter: (value: string) => void;
  referenceDate?: string;
  setReferenceDate?: (value: string) => void;
  size: PageSize;
  setSize: (value: PageSize) => void;
  onSearch: () => void;
}) {
  return (
    <section className="grid gap-3 rounded-md border border-ld bg-white p-4 md:grid-cols-4">
      <label className="text-sm font-semibold text-dark">
        검색어
        <input
          data-testid="assignment-filter-input"
          className="mt-1 w-full rounded-md border border-ld p-2 font-normal"
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
          placeholder="코드, 사용자, 조직"
        />
      </label>
      {setReferenceDate ? (
        <label className="text-sm font-semibold text-dark">
          기준일
          <input
            data-testid="assignment-reference-date-input"
            type="date"
            className="mt-1 w-full rounded-md border border-ld p-2 font-normal"
            value={referenceDate ?? ""}
            onChange={(event) => setReferenceDate(event.target.value)}
          />
        </label>
      ) : null}
      <label className="text-sm font-semibold text-dark">
        표시 건수
        <select
          data-testid="assignment-page-size-select"
          className="mt-1 w-full rounded-md border border-ld p-2 font-normal"
          value={size}
          onChange={(event) => setSize(Number(event.target.value) as PageSize)}
        >
          {pageSizes.map((value) => (
            <option key={value} value={value}>
              {value}건
            </option>
          ))}
        </select>
      </label>
      <button
        data-testid="assignment-search-button"
        type="button"
        className="mt-6 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
        onClick={onSearch}
      >
        <Search size={16} />
        조회
      </button>
    </section>
  );
}

function StatusBlock({
  status,
  message,
}: {
  status: ScreenStatus;
  message: string;
}) {
  if (status === "loading")
    return (
      <LoadingState
        title="목록 조회 중"
        message="서버에서 최신 정보를 가져오고 있습니다."
      />
    );
  if (status === "error")
    return (
      <ErrorState
        title="처리 오류"
        message={message || "요청 처리 중 오류가 발생했습니다."}
      />
    );
  if (status === "permission")
    return (
      <PermissionState
        title="권한이 없습니다"
        message="R09 시스템관리자 권한이 필요합니다."
      />
    );
  if (status === "success")
    return <SuccessState title="처리 완료" message={message} />;
  return null;
}

function PositionForm({
  form,
  setForm,
  fieldErrors,
  onSave,
  selectedId,
}: {
  form: PositionAssignmentPayload;
  setForm: (value: PositionAssignmentPayload) => void;
  fieldErrors: FieldErrors;
  onSave: () => void;
  selectedId: number | null;
}) {
  return (
    <FormShell
      title={selectedId ? "보직 지정 수정" : "보직 지정 등록"}
      onSave={onSave}
    >
      <TextInput
        label="보직코드 *"
        testId="position-code-input"
        value={form.positionCode}
        error={fieldErrors.positionCode}
        onChange={(positionCode) => setForm({ ...form, positionCode })}
      />
      <TextInput
        label="대상 사용자 ID *"
        testId="position-user-id-input"
        value={form.userId}
        error={fieldErrors.userId}
        onChange={(userId) => setForm({ ...form, userId })}
      />
      <TextInput
        label="소속조직 코드 *"
        testId="position-organization-code-input"
        value={form.organizationCode}
        error={fieldErrors.organizationCode}
        onChange={(organizationCode) => setForm({ ...form, organizationCode })}
      />
      <TextInput
        label="유효 시작일 *"
        testId="position-start-date-input"
        type="date"
        value={form.effectiveStartDate}
        error={fieldErrors.effectiveStartDate}
        onChange={(effectiveStartDate) =>
          setForm({ ...form, effectiveStartDate })
        }
      />
      <TextInput
        label="유효 종료일"
        testId="position-end-date-input"
        type="date"
        value={form.effectiveEndDate ?? ""}
        onChange={(effectiveEndDate) => setForm({ ...form, effectiveEndDate })}
      />
      <TextInput
        label="변경 사유 *"
        testId="position-change-reason-input"
        value={form.changeReason}
        error={fieldErrors.changeReason}
        onChange={(changeReason) => setForm({ ...form, changeReason })}
      />
    </FormShell>
  );
}

function DutyForm({
  form,
  setForm,
  fieldErrors,
  onSave,
  selectedId,
}: {
  form: DutyAssignmentPayload;
  setForm: (value: DutyAssignmentPayload) => void;
  fieldErrors: FieldErrors;
  onSave: () => void;
  selectedId: number | null;
}) {
  return (
    <FormShell
      title={selectedId ? "업무담당자 수정" : "업무담당자 등록"}
      onSave={onSave}
    >
      <TextInput
        label="업무조직 *"
        testId="duty-organization-input"
        value={form.dutyOrganization}
        error={fieldErrors.dutyOrganization}
        onChange={(dutyOrganization) => setForm({ ...form, dutyOrganization })}
      />
      <TextInput
        label="담당자 사용자 ID *"
        testId="duty-user-id-input"
        value={form.userId}
        error={fieldErrors.userId}
        onChange={(userId) => setForm({ ...form, userId })}
      />
      <TextInput
        label="담당 업무영역 *"
        testId="duty-area-input"
        value={form.dutyArea}
        error={fieldErrors.dutyArea}
        onChange={(dutyArea) => setForm({ ...form, dutyArea })}
      />
      <TextInput
        label="지정 시작일 *"
        testId="duty-start-date-input"
        type="date"
        value={form.validStartDate}
        error={fieldErrors.validStartDate}
        onChange={(validStartDate) => setForm({ ...form, validStartDate })}
      />
      <TextInput
        label="지정 종료일"
        testId="duty-end-date-input"
        type="date"
        value={form.validEndDate ?? ""}
        onChange={(validEndDate) => setForm({ ...form, validEndDate })}
      />
      <ScopeSelect
        value={form.dataScopeType}
        onChange={(dataScopeType) => setForm({ ...form, dataScopeType })}
        error={fieldErrors.dataScopeType}
      />
      <TextInput
        label="처리 권한 *"
        testId="duty-processing-permission-input"
        value={form.processingPermission}
        error={fieldErrors.processingPermission}
        onChange={(processingPermission) =>
          setForm({ ...form, processingPermission })
        }
      />
      <TextInput
        label="변경 사유 *"
        testId="duty-change-reason-input"
        value={form.changeReason}
        error={fieldErrors.changeReason}
        onChange={(changeReason) => setForm({ ...form, changeReason })}
      />
    </FormShell>
  );
}

function ScopeForm({
  form,
  setForm,
  fieldErrors,
  onSave,
}: {
  form: DataScopeRulePayload;
  setForm: (value: DataScopeRulePayload) => void;
  fieldErrors: FieldErrors;
  onSave: () => void;
}) {
  return (
    <FormShell title="데이터 범위 규칙 저장" onSave={onSave}>
      <TextInput
        label="역할코드 *"
        testId="scope-role-code-input"
        value={form.roleCode}
        error={fieldErrors.roleCode}
        onChange={(roleCode) => setForm({ ...form, roleCode })}
      />
      <ScopeSelect
        value={form.dataScopeType}
        onChange={(dataScopeType) => setForm({ ...form, dataScopeType })}
        error={fieldErrors.dataScopeType}
      />
      <TextInput
        label="조직 조건"
        testId="scope-organization-code-input"
        value={form.organizationCode ?? ""}
        onChange={(organizationCode) => setForm({ ...form, organizationCode })}
      />
      <TextInput
        label={form.dataScopeType === "DUTY" ? "업무영역 *" : "업무영역"}
        testId="scope-duty-area-input"
        value={form.dutyArea ?? ""}
        error={fieldErrors.dutyArea}
        onChange={(dutyArea) => setForm({ ...form, dutyArea })}
      />
      <TextInput
        label="변경 사유"
        testId="scope-change-reason-input"
        value={form.changeReason ?? ""}
        onChange={(changeReason) => setForm({ ...form, changeReason })}
      />
    </FormShell>
  );
}

function FormShell({
  title,
  onSave,
  children,
}: {
  title: string;
  onSave: () => void;
  children: React.ReactNode;
}) {
  return (
    <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-5">
      <h2 className="text-lg font-semibold text-dark">{title}</h2>
      <div className="mt-4 grid gap-4">{children}</div>
      <button
        data-testid="assignment-save-button"
        type="button"
        className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
        onClick={onSave}
      >
        <Save size={16} />
        저장
      </button>
      <button
        data-testid="assignment-refresh-button"
        type="button"
        className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
        onClick={() => window.location.reload()}
      >
        <RefreshCw size={16} />
        새로고침
      </button>
    </aside>
  );
}

function TextInput({
  label,
  testId,
  value,
  onChange,
  error,
  type = "text",
}: {
  label: string;
  testId: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  type?: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <input
        data-testid={testId}
        type={type}
        className="mt-1 w-full rounded-md border border-ld p-2 font-normal"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function ScopeSelect({
  value,
  onChange,
  error,
}: {
  value: DataScopeType;
  onChange: (value: DataScopeType) => void;
  error?: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      데이터 범위 유형 *
      <select
        data-testid="data-scope-type-select"
        className="mt-1 w-full rounded-md border border-ld p-2 font-normal"
        value={value}
        onChange={(event) => onChange(event.target.value as DataScopeType)}
      >
        {dataScopeOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function validateRequired<T extends object>(
  form: T,
  required: string[],
  setFieldErrors: (errors: FieldErrors) => void,
) {
  const errors: FieldErrors = {};
  const values = form as Record<string, unknown>;
  required.forEach((field) => {
    if (!String(values[field] ?? "").trim())
      errors[field] = "필수 입력 항목입니다.";
  });
  setFieldErrors(errors);
  return Object.keys(errors).length === 0;
}

function handleError(
  caught: unknown,
  setStatus: (status: ScreenStatus) => void,
  setMessage: (message: string) => void,
  setFieldErrors: (errors: FieldErrors) => void,
) {
  if (caught instanceof ApiClientError && caught.status === 403)
    setStatus("permission");
  else setStatus("error");
  if (caught instanceof ApiClientError && caught.apiError?.fields) {
    setFieldErrors(
      Object.fromEntries(
        caught.apiError.fields.map((field) => [field.field, field.message]),
      ),
    );
  }
  setMessage(
    caught instanceof Error
      ? caught.message
      : "요청 처리 중 오류가 발생했습니다.",
  );
}

function normalizePositionPayload(
  form: PositionAssignmentPayload,
): PositionAssignmentPayload {
  return { ...form, effectiveEndDate: form.effectiveEndDate || null };
}

function normalizeDutyPayload(
  form: DutyAssignmentPayload,
): DutyAssignmentPayload {
  return { ...form, validEndDate: form.validEndDate || null };
}

function normalizeScopePayload(
  form: DataScopeRulePayload,
): DataScopeRulePayload {
  return {
    ...form,
    organizationCode: form.organizationCode || null,
    dutyArea: form.dutyArea || null,
  };
}

function scopeLabel(value: DataScopeType) {
  return (
    dataScopeOptions.find((option) => option.value === value)?.label ?? value
  );
}
