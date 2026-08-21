import { useEffect, useState } from "react";
import { RefreshCw, Save, Search, PlayCircle } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type MenuExposureSetting = {
  menuId: number;
  menuName: string;
  systemUseYn: "Y" | "N";
  exposureStartAt?: string | null;
  exposureEndAt?: string | null;
  changeReason?: string | null;
};

type DetailCodeUsageSetting = {
  groupId: string;
  codeValue: string;
  codeName: string;
  systemUseYn: "Y" | "N";
  validStartDate?: string | null;
  validEndDate?: string | null;
  usageChangeReason?: string | null;
};

type CommonSettingRow = {
  settingKey: string;
  settingValue: string;
  settingUnit: string;
  changeReason?: string | null;
};

type BaseYearSetting = {
  baseYear: number;
  currentEvaluationYear: number;
  defaultSearchYear: number;
  copyRequestedYn: "Y" | "N";
  initializeRequestedYn: "Y" | "N";
  changeReason?: string | null;
};

type StandardPreparationHistory = {
  preparationId: number;
  baseYear: number;
  copyRequestedYn: "Y" | "N";
  initializeRequestedYn: "Y" | "N";
  preparedAt: string;
};

export const commonOperationsApi = {
  listMenuExposureSettings() {
    return apiRequest<MenuExposureSetting[]>("/api/admin/menus/exposure");
  },
  saveMenuExposureSettings(
    settings: MenuExposureSetting[],
    changeReason: string,
  ) {
    return apiRequest<MenuExposureSetting[]>("/api/admin/menus/exposure-save", {
      method: "PUT",
      body: JSON.stringify({ settings, changeReason }),
    });
  },
  listDetailCodeUsageSettings(groupId: string) {
    return apiRequest<DetailCodeUsageSetting[]>(
      `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes-usage` as `/api/${string}`,
    );
  },
  updateDetailCodeUsageSetting(
    groupId: string,
    codeValue: string,
    payload: Pick<
      DetailCodeUsageSetting,
      "systemUseYn" | "validStartDate" | "validEndDate"
    > & { changeReason: string },
  ) {
    return apiRequest<DetailCodeUsageSetting>(
      `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes/${encodeURIComponent(codeValue)}/usage` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  listCommonSettings() {
    return apiRequest<CommonSettingRow[]>("/api/admin/system-settings/common");
  },
  saveCommonSettings(settings: CommonSettingRow[], changeReason: string) {
    return apiRequest<CommonSettingRow[]>(
      "/api/admin/system-settings/common-values",
      {
        method: "PUT",
        body: JSON.stringify({ settings, changeReason }),
      },
    );
  },
  listBaseYearSettings() {
    return apiRequest<BaseYearSetting[]>(
      "/api/admin/system-settings/base-years",
    );
  },
  saveBaseYearSettings(payload: BaseYearSetting & { changeReason: string }) {
    return apiRequest<BaseYearSetting>(
      "/api/admin/system-settings/base-year-current",
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  prepareBaseYearStandards(
    baseYear: number,
    payload: {
      copyRequestedYn: "Y" | "N";
      initializeRequestedYn: "Y" | "N";
      changeReason: string;
    },
  ) {
    return apiRequest<StandardPreparationHistory>(
      `/api/admin/system-settings/base-years/${baseYear}/standards-preparation` as `/api/${string}`,
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export function getBasic7RouteContracts() {
  return [
    {
      route: "/admin/menu-usage",
      screenId: "SCR-MENU-USAGE-MGMT",
      operations: ["listMenuExposureSettings", "saveMenuExposureSettings"],
    },
    {
      route: "/admin/detail-code-usage",
      screenId: "SCR-CODE-USAGE-MGMT",
      operations: [
        "listDetailCodeUsageSettings",
        "updateDetailCodeUsageSetting",
      ],
    },
    {
      route: "/admin/common-settings",
      screenId: "SCR-COMMON-SETTINGS",
      operations: ["listCommonSettings", "saveCommonSettings"],
    },
    {
      route: "/admin/base-years",
      screenId: "SCR-BASE-YEAR-MGMT",
      operations: [
        "listBaseYearSettings",
        "saveBaseYearSettings",
        "prepareBaseYearStandards",
      ],
    },
  ] as const;
}

export function MenuUsageManagementPage() {
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [rows, setRows] = useState<MenuExposureSetting[]>([]);
  const [message, setMessage] = useState("");
  const [changeReason, setChangeReason] = useState("");

  const load = async () =>
    run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.listMenuExposureSettings();
      setRows(response.data ?? []);
      setStatus((response.data ?? []).length ? "loaded" : "empty");
    });

  useEffect(() => {
    void load();
  }, []);

  const save = async () => {
    if (!window.confirm("메뉴 노출 설정을 저장하시겠습니까?")) return;
    await run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.saveMenuExposureSettings(
        rows,
        changeReason,
      );
      setRows(response.data ?? []);
      setMessage("메뉴 노출 설정을 저장했습니다.");
      setStatus("success");
    });
  };

  return (
    <section
      data-testid="menu-usage-screen"
      data-screen-id="SCR-MENU-USAGE-MGMT"
      className="space-y-6"
    >
      <ScreenHeader
        title="메뉴 사용 관리"
        subtitle="시스템 관리 > 메뉴 관리 > 메뉴 사용 관리"
        onReload={load}
      />
      <StateBanner status={status} message={message} />
      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead>
              <tr className="text-left text-muted">
                <th className="py-3">메뉴</th>
                <th>사용여부</th>
                <th>노출 시작</th>
                <th>노출 종료</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.menuId}
                  data-testid={`menu-usage-row-${row.menuId}`}
                >
                  <td className="py-3 font-medium text-dark">{row.menuName}</td>
                  <td>
                    <select
                      data-testid={`menu-usage-select-${row.menuId}`}
                      className="rounded-md border border-ld px-3 py-2"
                      value={row.systemUseYn}
                      onChange={(event) =>
                        setRows(
                          update(rows, row.menuId, {
                            systemUseYn: event.target.value as "Y" | "N",
                          }),
                        )
                      }
                    >
                      <option value="Y">사용</option>
                      <option value="N">미사용</option>
                    </select>
                  </td>
                  <td>
                    <input
                      data-testid={`menu-exposure-start-${row.menuId}`}
                      className="rounded-md border border-ld px-3 py-2"
                      type="datetime-local"
                      value={toLocalInput(row.exposureStartAt)}
                      onChange={(event) =>
                        setRows(
                          update(rows, row.menuId, {
                            exposureStartAt: fromLocalInput(event.target.value),
                          }),
                        )
                      }
                    />
                  </td>
                  <td>
                    <input
                      data-testid={`menu-exposure-end-${row.menuId}`}
                      className="rounded-md border border-ld px-3 py-2"
                      type="datetime-local"
                      value={toLocalInput(row.exposureEndAt)}
                      onChange={(event) =>
                        setRows(
                          update(rows, row.menuId, {
                            exposureEndAt: fromLocalInput(event.target.value),
                          }),
                        )
                      }
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {status === "empty" ? (
            <EmptyState title="메뉴 없음" message="관리할 메뉴가 없습니다." />
          ) : null}
        </div>
        <SaveBar
          changeReason={changeReason}
          onChangeReason={setChangeReason}
          onSave={save}
          saveTestId="menu-usage-save-button"
        />
      </section>
    </section>
  );
}

export function DetailCodeUsageManagementPage() {
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [groupId, setGroupId] = useState("COMMON_STATUS");
  const [rows, setRows] = useState<DetailCodeUsageSetting[]>([]);
  const [message, setMessage] = useState("");
  const [changeReason, setChangeReason] = useState("");

  const load = async () =>
    run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.listDetailCodeUsageSettings(
        groupId.trim().toUpperCase(),
      );
      setRows(response.data ?? []);
      setStatus((response.data ?? []).length ? "loaded" : "empty");
    });

  useEffect(() => {
    void load();
  }, []);

  const saveRow = async (row: DetailCodeUsageSetting) => {
    if (!window.confirm("상세코드 사용 설정을 저장하시겠습니까?")) return;
    await run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.updateDetailCodeUsageSetting(
        row.groupId,
        row.codeValue,
        {
          systemUseYn: row.systemUseYn,
          validStartDate: row.validStartDate ?? undefined,
          validEndDate: row.validEndDate ?? undefined,
          changeReason,
        },
      );
      setRows(
        rows.map((item) =>
          item.codeValue === row.codeValue ? (response.data ?? item) : item,
        ),
      );
      setMessage(
        "상세코드 사용 설정을 저장했습니다. 과거 자료의 코드값은 변경하지 않습니다.",
      );
      setStatus("success");
    });
  };

  return (
    <section
      data-testid="detail-code-usage-screen"
      data-screen-id="SCR-CODE-USAGE-MGMT"
      className="space-y-6"
    >
      <ScreenHeader
        title="코드 사용 관리"
        subtitle="시스템 관리 > 공통코드 관리 > 코드 사용 관리"
        onReload={load}
      />
      <StateBanner status={status} message={message} />
      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <label
          className="text-sm font-semibold text-dark"
          htmlFor="usage-group-id"
        >
          코드그룹
        </label>
        <div className="mt-2 flex gap-2">
          <input
            id="usage-group-id"
            data-testid="detail-code-usage-group-input"
            className="rounded-md border border-ld px-3 py-2"
            value={groupId}
            onChange={(event) => setGroupId(event.target.value)}
          />
          <button
            data-testid="detail-code-usage-search-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
            onClick={() => void load()}
          >
            <Search size={16} />
            조회
          </button>
        </div>
        <div className="mt-6 overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead>
              <tr className="text-left text-muted">
                <th className="py-3">코드</th>
                <th>코드명</th>
                <th>사용여부</th>
                <th>적용 시작</th>
                <th>적용 종료</th>
                <th>저장</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.codeValue}
                  data-testid={`detail-code-usage-row-${row.codeValue}`}
                >
                  <td className="py-3 font-medium text-dark">
                    {row.codeValue}
                  </td>
                  <td>{row.codeName}</td>
                  <td>
                    <select
                      data-testid={`detail-code-usage-select-${row.codeValue}`}
                      className="rounded-md border border-ld px-3 py-2"
                      value={row.systemUseYn}
                      onChange={(event) =>
                        setRows(
                          rows.map((item) =>
                            item.codeValue === row.codeValue
                              ? {
                                  ...item,
                                  systemUseYn: event.target.value as "Y" | "N",
                                }
                              : item,
                          ),
                        )
                      }
                    >
                      <option value="Y">사용</option>
                      <option value="N">미사용</option>
                    </select>
                  </td>
                  <td>
                    <input
                      data-testid={`detail-code-start-${row.codeValue}`}
                      className="rounded-md border border-ld px-3 py-2"
                      type="date"
                      value={row.validStartDate ?? ""}
                      onChange={(event) =>
                        setRows(
                          rows.map((item) =>
                            item.codeValue === row.codeValue
                              ? {
                                  ...item,
                                  validStartDate: event.target.value || null,
                                }
                              : item,
                          ),
                        )
                      }
                    />
                  </td>
                  <td>
                    <input
                      data-testid={`detail-code-end-${row.codeValue}`}
                      className="rounded-md border border-ld px-3 py-2"
                      type="date"
                      value={row.validEndDate ?? ""}
                      onChange={(event) =>
                        setRows(
                          rows.map((item) =>
                            item.codeValue === row.codeValue
                              ? {
                                  ...item,
                                  validEndDate: event.target.value || null,
                                }
                              : item,
                          ),
                        )
                      }
                    />
                  </td>
                  <td>
                    <button
                      data-testid={`detail-code-usage-save-${row.codeValue}`}
                      type="button"
                      className="rounded-md bg-primary px-3 py-2 text-white"
                      onClick={() => void saveRow(row)}
                    >
                      저장
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {status === "empty" ? (
            <EmptyState
              title="상세코드 없음"
              message="조회된 상세코드가 없습니다."
            />
          ) : null}
        </div>
        <ReasonInput value={changeReason} onChange={setChangeReason} />
      </section>
    </section>
  );
}

export function CommonSettingsPage() {
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [rows, setRows] = useState<CommonSettingRow[]>([]);
  const [message, setMessage] = useState("");
  const [changeReason, setChangeReason] = useState("");
  const load = async () =>
    run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.listCommonSettings();
      setRows(response.data ?? []);
      setStatus((response.data ?? []).length ? "loaded" : "empty");
    });
  useEffect(() => {
    void load();
  }, []);
  const save = async () => {
    if (
      !window.confirm(
        "공통 환경설정을 저장하시겠습니까? 세션 유휴시간은 다음 로그인부터 적용됩니다.",
      )
    )
      return;
    await run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.saveCommonSettings(
        rows,
        changeReason,
      );
      setRows(response.data ?? []);
      setMessage(
        "공통 환경설정을 저장했습니다. 세션 유휴시간은 다음 로그인부터 적용됩니다.",
      );
      setStatus("success");
    });
  };
  return (
    <section
      data-testid="common-settings-screen"
      data-screen-id="SCR-COMMON-SETTINGS"
      className="space-y-6"
    >
      <ScreenHeader
        title="공통 환경설정"
        subtitle="시스템 관리 > 공통 운영 관리 > 공통 환경설정"
        onReload={load}
      />
      <StateBanner status={status} message={message} />
      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="grid gap-4 md:grid-cols-2">
          {rows.map((row) => (
            <label
              key={row.settingKey}
              data-testid={`common-setting-row-${row.settingKey}`}
              className="block rounded-md border border-ld p-4 text-sm"
            >
              <span className="font-semibold text-dark">
                {settingLabel(row.settingKey)}
              </span>
              <span className="ml-2 text-xs text-muted">
                {row.settingKey} · {row.settingUnit}
              </span>
              <input
                data-testid={`common-setting-input-${row.settingKey}`}
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={row.settingValue}
                onChange={(event) =>
                  setRows(
                    rows.map((item) =>
                      item.settingKey === row.settingKey
                        ? { ...item, settingValue: event.target.value }
                        : item,
                    ),
                  )
                }
              />
            </label>
          ))}
        </div>
        {status === "empty" ? (
          <EmptyState
            title="설정 없음"
            message="공통 설정 seed를 확인하세요."
          />
        ) : null}
        <SaveBar
          changeReason={changeReason}
          onChangeReason={setChangeReason}
          onSave={save}
          saveTestId="common-settings-save-button"
        />
      </section>
    </section>
  );
}

export function BaseYearManagementPage() {
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [rows, setRows] = useState<BaseYearSetting[]>([]);
  const [selected, setSelected] = useState<BaseYearSetting | null>(null);
  const [message, setMessage] = useState("");
  const [changeReason, setChangeReason] = useState("");
  const load = async () =>
    run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.listBaseYearSettings();
      const data = response.data ?? [];
      setRows(data);
      setSelected(data[0] ?? null);
      setStatus(data.length ? "loaded" : "empty");
    });
  useEffect(() => {
    void load();
  }, []);
  const save = async () => {
    if (!selected || !window.confirm("기준연도 설정을 저장하시겠습니까?"))
      return;
    await run(setStatus, setMessage, async () => {
      const response = await commonOperationsApi.saveBaseYearSettings({
        ...selected,
        changeReason,
      });
      setSelected(response.data ?? selected);
      setRows([
        response.data ?? selected,
        ...rows.filter((row) => row.baseYear !== selected.baseYear),
      ]);
      setMessage("기준연도 설정을 저장했습니다.");
      setStatus("success");
    });
  };
  const prepare = async () => {
    if (
      !selected ||
      !window.confirm(
        "연도별 기준정보 준비 이력을 생성하시겠습니까? 기존 평가자료는 변경하지 않습니다.",
      )
    )
      return;
    await run(setStatus, setMessage, async () => {
      await commonOperationsApi.prepareBaseYearStandards(selected.baseYear, {
        copyRequestedYn: selected.copyRequestedYn,
        initializeRequestedYn: selected.initializeRequestedYn,
        changeReason,
      });
      setMessage(
        "기준정보 준비 이력을 생성했습니다. 기존 평가자료는 변경하지 않습니다.",
      );
      setStatus("success");
    });
  };
  return (
    <section
      data-testid="base-years-screen"
      data-screen-id="SCR-BASE-YEAR-MGMT"
      className="space-y-6"
    >
      <ScreenHeader
        title="기준연도 관리"
        subtitle="시스템 관리 > 공통 운영 관리 > 기준연도 관리"
        onReload={load}
      />
      <StateBanner status={status} message={message} />
      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        {selected ? (
          <div className="grid gap-4 md:grid-cols-3">
            <YearInput
              label="기준연도"
              testId="base-year-input"
              value={selected.baseYear}
              onChange={(value) =>
                setSelected({ ...selected, baseYear: value })
              }
            />
            <YearInput
              label="현재 평가연도"
              testId="current-evaluation-year-input"
              value={selected.currentEvaluationYear}
              onChange={(value) =>
                setSelected({ ...selected, currentEvaluationYear: value })
              }
            />
            <YearInput
              label="기본 조회연도"
              testId="default-search-year-input"
              value={selected.defaultSearchYear}
              onChange={(value) =>
                setSelected({ ...selected, defaultSearchYear: value })
              }
            />
            <label className="text-sm font-semibold text-dark">
              복사 여부
              <select
                data-testid="copy-requested-select"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={selected.copyRequestedYn}
                onChange={(event) =>
                  setSelected({
                    ...selected,
                    copyRequestedYn: event.target.value as "Y" | "N",
                  })
                }
              >
                <option value="Y">복사</option>
                <option value="N">복사 안함</option>
              </select>
            </label>
            <label className="text-sm font-semibold text-dark">
              초기화 여부
              <select
                data-testid="initialize-requested-select"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={selected.initializeRequestedYn}
                onChange={(event) =>
                  setSelected({
                    ...selected,
                    initializeRequestedYn: event.target.value as "Y" | "N",
                  })
                }
              >
                <option value="Y">초기화</option>
                <option value="N">초기화 안함</option>
              </select>
            </label>
          </div>
        ) : (
          <EmptyState
            title="기준연도 없음"
            message="기준연도 seed를 확인하세요."
          />
        )}
        <ReasonInput value={changeReason} onChange={setChangeReason} />
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            data-testid="base-year-save-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
            onClick={() => void save()}
          >
            <Save size={16} />
            저장
          </button>
          <button
            data-testid="base-year-prepare-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-warning px-4 py-2 text-white"
            onClick={() => void prepare()}
          >
            <PlayCircle size={16} />
            기준정보 준비
          </button>
        </div>
      </section>
    </section>
  );
}

function ScreenHeader({
  title,
  subtitle,
  onReload,
}: {
  title: string;
  subtitle: string;
  onReload: () => Promise<void>;
}) {
  return (
    <div className="rounded-md bg-lightsecondary p-6 shadow-none">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-dark">{title}</h1>
          <p className="mt-2 text-sm text-muted">{subtitle}</p>
        </div>
        <button
          data-testid={`${title.replaceAll(" ", "-")}-reload-button`}
          type="button"
          className="inline-flex items-center gap-2 rounded-md border border-ld bg-white px-4 py-2 text-sm text-link"
          onClick={() => void onReload()}
        >
          <RefreshCw size={16} />
          새로고침
        </button>
      </div>
    </div>
  );
}

function StateBanner({
  status,
  message,
}: {
  status: ScreenStatus;
  message: string;
}) {
  if (status === "loading")
    return (
      <LoadingState title="조회 중" message="데이터를 불러오고 있습니다." />
    );
  if (status === "error")
    return (
      <ErrorState
        title="오류"
        message={message || "요청 처리 중 오류가 발생했습니다."}
      />
    );
  if (status === "permission")
    return (
      <PermissionState
        title="권한 없음"
        message="이 화면에 접근할 수 없습니다."
      />
    );
  if (status === "success")
    return <SuccessState title="처리 완료" message={message} />;
  return null;
}

function SaveBar({
  changeReason,
  onChangeReason,
  onSave,
  saveTestId,
}: {
  changeReason: string;
  onChangeReason: (value: string) => void;
  onSave: () => Promise<void>;
  saveTestId: string;
}) {
  return (
    <div className="mt-5 flex flex-col gap-3 border-t border-ld pt-5 md:flex-row md:items-end">
      <ReasonInput value={changeReason} onChange={onChangeReason} />
      <button
        data-testid={saveTestId}
        type="button"
        className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
        onClick={() => void onSave()}
      >
        <Save size={16} />
        저장
      </button>
    </div>
  );
}

function ReasonInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="min-w-0 flex-1 text-sm font-semibold text-dark">
      변경 사유
      <input
        data-testid="change-reason-input"
        className="mt-2 w-full rounded-md border border-ld px-3 py-2"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

function YearInput({
  label,
  testId,
  value,
  onChange,
}: {
  label: string;
  testId: string;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <input
        data-testid={testId}
        className="mt-2 w-full rounded-md border border-ld px-3 py-2"
        type="number"
        value={value}
        onChange={(event) =>
          onChange(Number.parseInt(event.target.value || "0", 10))
        }
      />
    </label>
  );
}

async function run(
  setStatus: (status: ScreenStatus) => void,
  setMessage: (message: string) => void,
  task: () => Promise<void>,
) {
  try {
    setStatus("loading");
    await task();
  } catch (caught) {
    if (caught instanceof ApiClientError && caught.status === 403)
      setStatus("permission");
    else setStatus("error");
    setMessage(
      caught instanceof Error
        ? caught.message
        : "요청 처리 중 오류가 발생했습니다.",
    );
  }
}

function update(
  rows: MenuExposureSetting[],
  menuId: number,
  patch: Partial<MenuExposureSetting>,
) {
  return rows.map((row) =>
    row.menuId === menuId ? { ...row, ...patch } : row,
  );
}

function toLocalInput(value?: string | null) {
  return value ? value.slice(0, 16) : "";
}

function fromLocalInput(value: string) {
  return value ? `${value}:00` : null;
}

function settingLabel(key: string) {
  const labels: Record<string, string> = {
    SESSION_IDLE_MINUTES: "세션 유휴시간",
    PAGE_SIZE_DEFAULT: "페이지당 조회건수",
    DEFAULT_SEARCH_PERIOD_DAYS: "기본 검색기간",
    LARGE_QUERY_THRESHOLD: "대량조회 기준건수",
    LONG_RUNNING_TASK_THRESHOLD: "장시간작업 안내 기준",
  };
  return labels[key] ?? key;
}
