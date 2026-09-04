import type React from "react";
import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type PageSize = 20 | 50 | 100;
type ActiveYn = "Y" | "N";

type BusinessSetting = {
  settingId: number;
  evaluationYear: string;
  organizationCode: string;
  evaluationUnitCode: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  managerUserId: number;
  managerName: string;
  targetScope: string;
  activeYn: ActiveYn;
  changeReason?: string;
};

type Authority = BusinessSetting & {
  authorityId: number;
  inputAllowedYn: ActiveYn;
  outputAllowedYn: ActiveYn;
  modifyAllowedYn: ActiveYn;
  teacherUserId?: number | null;
  teacherName?: string | null;
};

type Criterion = {
  criterionId: number;
  areaCode: string;
  areaName: string;
  managementCriterionCode: string;
  managementCriterionName: string;
  parentCriterionCode?: string | null;
  activeYn: ActiveYn;
  classifiedAchievementCount: number;
};

type Achievement = {
  achievementId: number;
  evaluationYear: string;
  organizationCode: string;
  teacherUserId: number;
  teacherName: string;
  title: string;
  areaCode?: string | null;
  managementCriterionCode?: string | null;
  classificationCode?: string | null;
  confirmationStatus: "UNCONFIRMED" | "CONFIRMED";
  achievementDate: string;
  sourceSystem: string;
};

type ScoreResponse = {
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  totalScore: number;
  summaries: Array<{
    areaCode: string;
    areaName: string;
    subtotalScore: number;
  }>;
  items: Array<{
    scoreId: number;
    areaCode: string;
    areaName: string;
    itemCode: string;
    itemName: string;
    score: number;
    calculationDetail: string;
    ruleName: string;
    evidenceUrl: string;
  }>;
};

function query(params: Record<string, string | number | undefined>) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") q.set(key, String(value));
  });
  return q.toString();
}

function exportRowsToCsv<T extends object>(filename: string, rows: T[]) {
  const keys = Array.from(
    rows.reduce((set, row) => {
      Object.keys(row).forEach((key) => set.add(key));
      return set;
    }, new Set<string>()),
  );
  downloadCsv(
    filename,
    rows,
    keys.map((key) => ({
      header: key,
      value: (row) =>
        (row as Record<string, unknown>)[key] as
          | string
          | number
          | boolean
          | null
          | undefined,
    })),
  );
}

const businessApi = {
  listAuthorities: (params: Record<string, string | number | undefined>) =>
    apiRequest<{
      authorities: Authority[];
      page: number;
      pageSize: number;
      totalElements: number;
    }>(
      `/api/business/college-evaluation-unit-authorities?${query(params)}` as `/api/${string}`,
    ),
  saveAuthority: (body: Record<string, unknown>) =>
    apiRequest<Authority>(
      "/api/business/college-evaluation-unit-authorities/save",
      { method: "POST", body: JSON.stringify(body) },
    ),
  listSettings: (
    kind: "appeal" | "result",
    params: Record<string, string | number | undefined>,
  ) =>
    apiRequest<{
      settings: BusinessSetting[];
      page: number;
      pageSize: number;
      totalElements: number;
    }>(
      `/api/business/${kind === "appeal" ? "appeal" : "result-view"}-business-settings?${query(params)}` as `/api/${string}`,
    ),
  saveSetting: (kind: "appeal" | "result", body: Record<string, unknown>) =>
    apiRequest<BusinessSetting>(
      `/api/business/${kind === "appeal" ? "appeal" : "result-view"}-business-settings/save` as `/api/${string}`,
      { method: "POST", body: JSON.stringify(body) },
    ),
  listCriteria: (params: Record<string, string | number | undefined>) =>
    apiRequest<{
      criteria: Criterion[];
      page: number;
      pageSize: number;
      totalElements: number;
    }>(
      `/api/business/research-classification-criteria?${query(params)}` as `/api/${string}`,
    ),
  saveCriterion: (body: Record<string, unknown>) =>
    apiRequest<Criterion>(
      "/api/business/research-classification-criteria/save",
      { method: "POST", body: JSON.stringify(body) },
    ),
  listAchievements: (params: Record<string, string | number | undefined>) =>
    apiRequest<{
      achievements: Achievement[];
      page: number;
      pageSize: number;
      totalElements: number;
    }>(
      `/api/business/unconfirmed-research-achievements?${query(params)}` as `/api/${string}`,
    ),
  confirmAchievement: (achievementId: number, body: Record<string, unknown>) =>
    apiRequest<Achievement>(
      `/api/business/unconfirmed-research-achievements/${achievementId}/confirmation` as `/api/${string}`,
      { method: "POST", body: JSON.stringify(body) },
    ),
  scores: (params: Record<string, string | number | undefined>) =>
    apiRequest<ScoreResponse>(
      `/api/business/personal-achievement-scores?${query(params)}` as `/api/${string}`,
    ),
};

function useApiState() {
  const [loading, setLoading] = useState(true);
  const [permission, setPermission] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const handle = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403)
      setPermission(true);
    setError(
      caught instanceof Error
        ? caught.message
        : "요청 처리 중 오류가 발생했습니다.",
    );
  };
  return {
    loading,
    setLoading,
    permission,
    setPermission,
    error,
    setError,
    success,
    setSuccess,
    handle,
  };
}

const pageSizes: PageSize[] = [20, 50, 100];
const defaultFilters = {
  evaluationYear: "2027",
  organizationCode: "",
  evaluationUnitCode: "",
  activeYn: "",
  keyword: "",
};

export function CollegeEvaluationUnitAuthorityPage() {
  const api = useApiState();
  const [filters, setFilters] = useState(defaultFilters);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [rows, setRows] = useState<Authority[]>([]);
  const [form, setForm] = useState({
    authorityId: "",
    evaluationYear: "2027",
    organizationCode: "",
    evaluationUnitCode: "",
    managerUserId: "",
    inputAllowedYn: "Y",
    outputAllowedYn: "Y",
    modifyAllowedYn: "N",
    teacherUserId: "",
    effectiveStartDate: "",
    effectiveEndDate: "",
    activeYn: "Y",
    changeReason: "",
  });
  const load = async () => {
    try {
      api.setLoading(true);
      api.setPermission(false);
      api.setError(null);
      const res = await businessApi.listAuthorities({
        ...filters,
        size: pageSize,
      });
      setRows(res.data?.authorities ?? []);
    } catch (e) {
      api.handle(e);
    } finally {
      api.setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);
  const save = async () => {
    if (
      !form.evaluationYear ||
      !form.organizationCode ||
      !form.evaluationUnitCode ||
      !form.managerUserId ||
      !form.effectiveStartDate ||
      !form.effectiveEndDate ||
      !form.changeReason
    ) {
      api.setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("소속대학·평가단위 권한을 저장하시겠습니까?")) return;
    try {
      await businessApi.saveAuthority({
        ...form,
        authorityId: form.authorityId ? Number(form.authorityId) : null,
        managerUserId: Number(form.managerUserId),
        teacherUserId: form.teacherUserId ? Number(form.teacherUserId) : null,
      });
      api.setSuccess("저장 후 재조회되었습니다.");
      await load();
    } catch (e) {
      api.handle(e);
    }
  };
  return (
    <Screen
      title="소속대학·평가단위 권한 관리"
      screenId="SCR-COLLEGE-EVALUATION-UNIT-AUTHORITY"
      menuPath="평가 기준 관리 > 기간·권한 관리 > 소속대학·평가단위 권한 관리"
      state={api}
    >
      <SearchPanel
        filters={filters}
        setFilters={setFilters}
        pageSize={pageSize}
        setPageSize={setPageSize}
        onSearch={load}
        onCsv={() =>
          exportRowsToCsv("college-evaluation-unit-authorities.csv", rows)
        }
      />
      <DataTable
        rows={rows}
        onSelect={(r) =>
          setForm({
            authorityId: String(r.authorityId),
            evaluationYear: r.evaluationYear,
            organizationCode: r.organizationCode,
            evaluationUnitCode: r.evaluationUnitCode,
            managerUserId: String(r.managerUserId),
            inputAllowedYn: r.inputAllowedYn,
            outputAllowedYn: r.outputAllowedYn,
            modifyAllowedYn: r.modifyAllowedYn,
            teacherUserId: r.teacherUserId ? String(r.teacherUserId) : "",
            effectiveStartDate: r.effectiveStartDate,
            effectiveEndDate: r.effectiveEndDate,
            activeYn: r.activeYn,
            changeReason: "",
          })
        }
        columns={[
          "evaluationYear",
          "organizationCode",
          "evaluationUnitCode",
          "managerName",
          "inputAllowedYn",
          "outputAllowedYn",
          "modifyAllowedYn",
          "teacherName",
          "activeYn",
        ]}
      />
      <AuthorityForm form={form} setForm={setForm} onSave={save} />
    </Screen>
  );
}

export function AppealBusinessSettingPage() {
  return (
    <BusinessSettingPage
      kind="appeal"
      title="이의신청 기간·처리권한 관리"
      screenId="SCR-APPEAL-BUSINESS-SETTING"
      menuPath="평가 기준 관리 > 기간·권한 관리 > 이의신청 기간·처리권한 관리"
    />
  );
}
export function ResultViewBusinessSettingPage() {
  return (
    <BusinessSettingPage
      kind="result"
      title="개인평가결과 조회기간·처리권한 관리"
      screenId="SCR-RESULT-VIEW-BUSINESS-SETTING"
      menuPath="평가 기준 관리 > 기간·권한 관리 > 개인평가결과 조회기간·처리권한 관리"
    />
  );
}

function BusinessSettingPage({
  kind,
  title,
  screenId,
  menuPath,
}: {
  kind: "appeal" | "result";
  title: string;
  screenId: string;
  menuPath: string;
}) {
  const api = useApiState();
  const [filters, setFilters] = useState(defaultFilters);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [rows, setRows] = useState<BusinessSetting[]>([]);
  const [form, setForm] = useState({
    settingId: "",
    evaluationYear: "2027",
    organizationCode: "",
    evaluationUnitCode: "",
    managerUserId: "",
    targetScope: kind === "appeal" ? "COLLEGE" : "SELF",
    effectiveStartDate: "",
    effectiveEndDate: "",
    activeYn: "Y",
    changeReason: "",
  });
  const load = async () => {
    try {
      api.setLoading(true);
      api.setPermission(false);
      api.setError(null);
      const res = await businessApi.listSettings(kind, {
        ...filters,
        size: pageSize,
      });
      setRows(res.data?.settings ?? []);
    } catch (e) {
      api.handle(e);
    } finally {
      api.setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);
  const save = async () => {
    if (
      !form.evaluationYear ||
      !form.organizationCode ||
      !form.evaluationUnitCode ||
      !form.managerUserId ||
      !form.effectiveStartDate ||
      !form.effectiveEndDate ||
      !form.changeReason
    ) {
      api.setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm(`${title} 설정을 저장하시겠습니까?`)) return;
    try {
      await businessApi.saveSetting(kind, {
        ...form,
        settingId: form.settingId ? Number(form.settingId) : null,
        managerUserId: Number(form.managerUserId),
      });
      api.setSuccess("저장 후 재조회되었습니다.");
      await load();
    } catch (e) {
      api.handle(e);
    }
  };
  return (
    <Screen title={title} screenId={screenId} menuPath={menuPath} state={api}>
      <SearchPanel
        filters={filters}
        setFilters={setFilters}
        pageSize={pageSize}
        setPageSize={setPageSize}
        onSearch={load}
        onCsv={() => exportRowsToCsv(`${kind}-business-settings.csv`, rows)}
      />
      <DataTable
        rows={rows}
        onSelect={(r) =>
          setForm({
            settingId: String(r.settingId),
            evaluationYear: r.evaluationYear,
            organizationCode: r.organizationCode,
            evaluationUnitCode: r.evaluationUnitCode,
            managerUserId: String(r.managerUserId),
            targetScope: r.targetScope,
            effectiveStartDate: r.effectiveStartDate,
            effectiveEndDate: r.effectiveEndDate,
            activeYn: r.activeYn,
            changeReason: "",
          })
        }
        columns={[
          "evaluationYear",
          "organizationCode",
          "evaluationUnitCode",
          "effectiveStartDate",
          "effectiveEndDate",
          "managerName",
          "targetScope",
          "activeYn",
        ]}
      />
      <SettingForm form={form} setForm={setForm} onSave={save} />
    </Screen>
  );
}

export function PersonalAchievementScorePage() {
  const api = useApiState();
  const [filters, setFilters] = useState({
    teacherUserId: "",
    evaluationYear: "2027",
    areaCode: "",
  });
  const [data, setData] = useState<ScoreResponse | null>(null);
  const load = async () => {
    try {
      api.setLoading(true);
      api.setError(null);
      const res = await businessApi.scores({
        teacherUserId: filters.teacherUserId || undefined,
        evaluationYear: filters.evaluationYear,
        areaCode: filters.areaCode,
      });
      setData(res.data ?? null);
    } catch (e) {
      api.handle(e);
    } finally {
      api.setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);
  return (
    <Screen
      title="개인 업적점수·세부규정 조회"
      screenId="SCR-PERSONAL-ACHIEVEMENT-SCORE"
      menuPath="업적 평가 조회 > 개인 업적점수·세부규정 조회"
      state={api}
    >
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="교원 사용자 ID">
            <input
              data-testid="score-teacher-input"
              value={filters.teacherUserId}
              onChange={(e) =>
                setFilters({ ...filters, teacherUserId: e.target.value })
              }
              placeholder="R01은 본인 고정"
            />
          </Field>
          <Field label="평가연도 *">
            <input
              data-testid="score-year-input"
              value={filters.evaluationYear}
              onChange={(e) =>
                setFilters({ ...filters, evaluationYear: e.target.value })
              }
            />
          </Field>
          <Field label="영역">
            <input
              data-testid="score-area-input"
              value={filters.areaCode}
              onChange={(e) =>
                setFilters({ ...filters, areaCode: e.target.value })
              }
            />
          </Field>
          <button
            data-testid="score-search-button"
            className="mt-6 h-10 bg-primary px-4 text-white"
            onClick={load}
          >
            <Search className="inline h-4 w-4" /> 조회
          </button>
        </div>
      </section>
      {data ? (
        <>
          <section className="rounded-md border border-ld bg-white p-5">
            <h2 className="text-lg font-semibold">
              {data.teacherName} 총점 {data.totalScore}
            </h2>
            <div className="mt-4 grid gap-4 md:grid-cols-3">
              {data.summaries.map((s) => (
                <div
                  data-testid="score-summary-card"
                  key={s.areaCode}
                  className="rounded-xl bg-lightprimary p-4"
                >
                  <p>{s.areaName}</p>
                  <strong>{s.subtotalScore}</strong>
                </div>
              ))}
            </div>
          </section>
          <section className="rounded-md border border-ld bg-white p-5">
            <DataTable
              rows={data.items}
              columns={[
                "areaName",
                "itemName",
                "score",
                "calculationDetail",
                "ruleName",
              ]}
              renderExtra={(row) => (
                <a
                  data-testid="score-evidence-link"
                  className="text-primary underline"
                  href={String(row.evidenceUrl)}
                >
                  산출근거
                </a>
              )}
            />
          </section>
        </>
      ) : (
        <EmptyState
          title="조회 결과 없음"
          message="평가연도와 교원 조건으로 조회하세요."
        />
      )}
    </Screen>
  );
}

export function ResearchClassificationCriterionPage() {
  const api = useApiState();
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [filters, setFilters] = useState({
    areaCode: "",
    managementCriterionCode: "",
    activeYn: "",
    keyword: "",
  });
  const [rows, setRows] = useState<Criterion[]>([]);
  const [form, setForm] = useState({
    criterionId: "",
    areaCode: "RESEARCH",
    areaName: "연구",
    managementCriterionCode: "",
    managementCriterionName: "",
    parentCriterionCode: "",
    activeYn: "Y",
    changeReason: "",
  });
  const load = async () => {
    try {
      api.setLoading(true);
      api.setError(null);
      const res = await businessApi.listCriteria({
        ...filters,
        size: pageSize,
      });
      setRows(res.data?.criteria ?? []);
    } catch (e) {
      api.handle(e);
    } finally {
      api.setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);
  const save = async () => {
    if (
      !form.areaCode ||
      !form.areaName ||
      !form.managementCriterionCode ||
      !form.managementCriterionName ||
      !form.changeReason
    ) {
      api.setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("연구실적 분류기준을 저장하시겠습니까?")) return;
    try {
      await businessApi.saveCriterion({
        ...form,
        criterionId: form.criterionId ? Number(form.criterionId) : null,
        parentCriterionCode: form.parentCriterionCode || null,
      });
      api.setSuccess("저장 후 재조회되었습니다.");
      await load();
    } catch (e) {
      api.handle(e);
    }
  };
  return (
    <Screen
      title="연구실적 분류기준 설정"
      screenId="SCR-RESEARCH-CLASSIFICATION-CRITERION"
      menuPath="업적 평가 관리 > 연구실적 관리 > 연구실적 분류·미확인 목록 관리"
      state={api}
    >
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="grid gap-4 md:grid-cols-5">
          <Field label="영역">
            <input
              data-testid="criterion-area-filter"
              value={filters.areaCode}
              onChange={(e) =>
                setFilters({ ...filters, areaCode: e.target.value })
              }
            />
          </Field>
          <Field label="관리기준">
            <input
              data-testid="criterion-code-filter"
              value={filters.managementCriterionCode}
              onChange={(e) =>
                setFilters({
                  ...filters,
                  managementCriterionCode: e.target.value,
                })
              }
            />
          </Field>
          <PageSizeSelect value={pageSize} onChange={setPageSize} />
          <button
            data-testid="criterion-search-button"
            className="mt-6 h-10 bg-primary px-4 text-white"
            onClick={load}
          >
            조회
          </button>
          <button
            data-testid="criterion-csv-button"
            className="mt-6 h-10 border border-ld px-4"
            onClick={() =>
              exportRowsToCsv("research-classification-criteria.csv", rows)
            }
          >
            <Download className="inline h-4 w-4" /> Excel
          </button>
        </div>
      </section>
      <DataTable
        rows={rows}
        columns={[
          "areaCode",
          "areaName",
          "managementCriterionCode",
          "managementCriterionName",
          "parentCriterionCode",
          "activeYn",
          "classifiedAchievementCount",
        ]}
        onSelect={(r) =>
          setForm({
            criterionId: String(r.criterionId),
            areaCode: r.areaCode,
            areaName: r.areaName,
            managementCriterionCode: r.managementCriterionCode,
            managementCriterionName: r.managementCriterionName,
            parentCriterionCode: r.parentCriterionCode ?? "",
            activeYn: r.activeYn,
            changeReason: "",
          })
        }
      />
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="grid gap-4 md:grid-cols-4">
          {Object.keys(form)
            .filter((k) => k !== "criterionId")
            .map((key) => (
              <Field key={key} label={key}>
                <input
                  data-testid={`criterion-${key}-input`}
                  value={(form as Record<string, string>)[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                />
              </Field>
            ))}
          <button
            data-testid="criterion-save-button"
            className="mt-6 h-10 bg-primary px-4 text-white"
            onClick={save}
          >
            <Save className="inline h-4 w-4" /> 저장
          </button>
        </div>
      </section>
    </Screen>
  );
}

export function UnconfirmedResearchAchievementPage() {
  const api = useApiState();
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [filters, setFilters] = useState({
    evaluationYear: "2027",
    organizationCode: "",
    areaCode: "",
    confirmationStatus: "UNCONFIRMED",
    keyword: "",
  });
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [criterionCode, setCriterionCode] = useState("");
  const load = async () => {
    try {
      api.setLoading(true);
      api.setError(null);
      const res = await businessApi.listAchievements({
        ...filters,
        size: pageSize,
      });
      setRows(res.data?.achievements ?? []);
    } catch (e) {
      api.handle(e);
    } finally {
      api.setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);
  const confirm = async () => {
    if (!selected || !criterionCode) {
      api.setError("선택 행과 분류기준을 확인하세요.");
      return;
    }
    if (!window.confirm("선택한 연구실적을 확인완료로 전환하시겠습니까?"))
      return;
    try {
      await businessApi.confirmAchievement(selected.achievementId, {
        managementCriterionCode: criterionCode,
        changeReason: "미확인 연구실적 확인",
      });
      api.setSuccess("확인완료 처리 후 미확인 목록을 재조회했습니다.");
      setSelected(null);
      await load();
    } catch (e) {
      api.handle(e);
    }
  };
  return (
    <Screen
      title="미확인 연구실적 목록"
      screenId="SCR-UNCONFIRMED-RESEARCH-ACHIEVEMENT"
      menuPath="업적 평가 관리 > 연구실적 관리 > 미확인 연구실적 목록"
      state={api}
    >
      <SearchPanel
        filters={filters}
        setFilters={setFilters}
        pageSize={pageSize}
        setPageSize={setPageSize}
        onSearch={load}
        onCsv={() =>
          exportRowsToCsv("unconfirmed-research-achievements.csv", rows)
        }
      />
      <DataTable
        rows={rows}
        columns={[
          "evaluationYear",
          "organizationCode",
          "teacherName",
          "title",
          "areaCode",
          "confirmationStatus",
          "achievementDate",
          "sourceSystem",
        ]}
        onSelect={setSelected}
      />
      <section
        data-testid="achievement-detail-panel"
        className="rounded-md border border-ld bg-white p-5"
      >
        <h2 className="text-lg font-semibold">선택 행 상세 확인</h2>
        {selected ? (
          <div className="mt-3 space-y-3 text-sm">
            <p>
              {selected.teacherName} / {selected.title}
            </p>
            <Field label="적용 분류기준 *">
              <input
                data-testid="achievement-criterion-input"
                value={criterionCode}
                onChange={(e) => setCriterionCode(e.target.value)}
              />
            </Field>
            <button
              data-testid="achievement-confirm-button"
              className="bg-primary px-4 py-2 text-white"
              onClick={confirm}
            >
              확인상태 전환
            </button>
          </div>
        ) : (
          <p className="mt-3 text-sm text-muted">미확인 행을 선택하세요.</p>
        )}
      </section>
    </Screen>
  );
}

function Screen({
  title,
  screenId,
  menuPath,
  state,
  children,
}: {
  title: string;
  screenId: string;
  menuPath: string;
  state: ReturnType<typeof useApiState>;
  children: React.ReactNode;
}) {
  if (state.permission)
    return (
      <section data-screen-id={screenId}>
        <PermissionState
          title="권한이 없습니다"
          message={`${title} 접근 권한이 없습니다.`}
        />
      </section>
    );
  return (
    <section data-screen-id={screenId} className="space-y-6">
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm font-semibold text-primary">{screenId}</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
        <p className="mt-2 text-sm text-muted">{menuPath}</p>
      </div>
      {state.success ? (
        <SuccessState title="처리 완료" message={state.success} />
      ) : null}
      {state.error ? (
        <ErrorState title="처리 오류" message={state.error} />
      ) : null}
      {state.loading ? (
        <LoadingState
          title="조회 중"
          message="업무 데이터를 불러오고 있습니다."
        />
      ) : (
        children
      )}
    </section>
  );
}

function SearchPanel({
  filters,
  setFilters,
  pageSize,
  setPageSize,
  onSearch,
  onCsv,
}: {
  filters: Record<string, string>;
  setFilters: (v: Record<string, string>) => void;
  pageSize: PageSize;
  setPageSize: (v: PageSize) => void;
  onSearch: () => void;
  onCsv: () => void;
}) {
  return (
    <section className="rounded-md border border-ld bg-white p-5">
      <div className="grid gap-4 md:grid-cols-6">
        {Object.keys(filters).map((key) => (
          <Field key={key} label={key}>
            <input
              data-testid={`${key}-filter-input`}
              value={filters[key]}
              onChange={(e) =>
                setFilters({ ...filters, [key]: e.target.value })
              }
            />
          </Field>
        ))}
        <PageSizeSelect value={pageSize} onChange={setPageSize} />
        <button
          data-testid="business-search-button"
          className="mt-6 h-10 bg-primary px-4 text-white"
          onClick={onSearch}
        >
          <Search className="inline h-4 w-4" /> 조회
        </button>
        <button
          data-testid="business-csv-button"
          className="mt-6 h-10 border border-ld px-4"
          onClick={onCsv}
        >
          <Download className="inline h-4 w-4" /> Excel
        </button>
      </div>
    </section>
  );
}

function SettingForm({
  form,
  setForm,
  onSave,
}: {
  form: Record<string, string>;
  setForm: (v: any) => void;
  onSave: () => void;
}) {
  return (
    <section className="rounded-md border border-ld bg-white p-5">
      <h2 className="text-lg font-semibold">선택 행 등록·수정</h2>
      <div className="mt-4 grid gap-4 md:grid-cols-4">
        {Object.keys(form)
          .filter((k) => k !== "settingId")
          .map((key) => (
            <Field
              key={key}
              label={`${key}${["evaluationYear", "organizationCode", "evaluationUnitCode", "managerUserId", "effectiveStartDate", "effectiveEndDate", "changeReason"].includes(key) ? " *" : ""}`}
            >
              <input
                data-testid={`setting-${key}-input`}
                type={key.includes("Date") ? "date" : "text"}
                value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
              />
            </Field>
          ))}
        <button
          data-testid="setting-save-button"
          className="mt-6 h-10 bg-primary px-4 text-white"
          onClick={onSave}
        >
          <Save className="inline h-4 w-4" /> 저장
        </button>
        <button
          data-testid="setting-reset-button"
          className="mt-6 h-10 border border-ld px-4"
          onClick={() => window.location.reload()}
        >
          <RefreshCw className="inline h-4 w-4" /> 초기화
        </button>
      </div>
    </section>
  );
}

function AuthorityForm({
  form,
  setForm,
  onSave,
}: {
  form: Record<string, string>;
  setForm: (v: any) => void;
  onSave: () => void;
}) {
  return (
    <section className="rounded-md border border-ld bg-white p-5">
      <h2 className="text-lg font-semibold">
        [담당자 지정] [평가단위별 권한] [개별 교원 수정 권한]
      </h2>
      <div className="mt-4 grid gap-4 md:grid-cols-4">
        {Object.keys(form)
          .filter((k) => k !== "authorityId")
          .map((key) => (
            <Field key={key} label={key}>
              <input
                data-testid={`authority-${key}-input`}
                type={key.includes("Date") ? "date" : "text"}
                value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
              />
            </Field>
          ))}
        <button
          data-testid="authority-save-button"
          className="mt-6 h-10 bg-primary px-4 text-white"
          onClick={onSave}
        >
          <Save className="inline h-4 w-4" /> 저장
        </button>
      </div>
    </section>
  );
}

function DataTable<T extends object>({
  rows,
  columns,
  onSelect,
  renderExtra,
}: {
  rows: T[];
  columns: string[];
  onSelect?: (row: T) => void;
  renderExtra?: (row: T) => React.ReactNode;
}) {
  if (rows.length === 0)
    return (
      <EmptyState
        title="조회 결과가 없습니다"
        message="검색 조건을 변경한 뒤 다시 조회하세요."
      />
    );
  return (
    <section className="overflow-x-auto rounded-md border border-ld bg-white p-5">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c}>{c}</th>
            ))}
            {renderExtra ? <th>action</th> : null}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr
              data-testid="business-row"
              key={index}
              onClick={() => onSelect?.(row)}
            >
              {columns.map((c) => (
                <td key={c}>
                  {String((row as Record<string, unknown>)[c] ?? "-")}
                </td>
              ))}
              {renderExtra ? <td>{renderExtra(row)}</td> : null}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold text-ld">
      <span>{label}</span>
      <div className="mt-2">{children}</div>
    </label>
  );
}
function PageSizeSelect({
  value,
  onChange,
}: {
  value: PageSize;
  onChange: (v: PageSize) => void;
}) {
  return (
    <Field label="표시 건수">
      <select
        data-testid="page-size-select"
        value={value}
        onChange={(e) => onChange(Number(e.target.value) as PageSize)}
      >
        {pageSizes.map((s) => (
          <option key={s} value={s}>
            {s}건
          </option>
        ))}
      </select>
    </Field>
  );
}
