import {
  Clock3,
  Download,
  History,
  RefreshCw,
  Search,
  ShieldCheck,
} from "lucide-react";
import type React from "react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  achievementDataHistoryApi,
  type AchievementDataAsOf,
  type AchievementDataHistory,
  type ChangeType,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

const changeTypes: Array<ChangeType | ""> = ["", "CREATE", "UPDATE", "DELETE"];
const achievementTypes = ["", "BASIC36_RESEARCHER_PROFILE"];

export function AchievementDataHistoryPage() {
  const [achievementType, setAchievementType] = useState("");
  const [achievementKey, setAchievementKey] = useState("");
  const [employeeNo, setEmployeeNo] = useState("");
  const [changedAtFrom, setChangedAtFrom] = useState("");
  const [changedAtTo, setChangedAtTo] = useState("");
  const [changeType, setChangeType] = useState<ChangeType | "">("");
  const [histories, setHistories] = useState<AchievementDataHistory[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await achievementDataHistoryApi.listHistories({
        achievementType,
        achievementKey,
        employeeNo,
        changedAtFrom: toIsoSecond(changedAtFrom),
        changedAtTo: toIsoSecond(changedAtTo),
        changeType,
        page,
        size,
      });
      setHistories(response.data?.histories ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(
        caught,
        setPermissionDenied,
        setError,
        "업적데이터 변경이력을 조회하지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-ACHIEVEMENT-DATA-HISTORY"
        data-testid="achievement-data-history-page"
      >
        <PermissionState
          title="업적데이터 변경이력 권한이 없습니다"
          message="R04 업무담당자, R08 감사담당자 또는 R09 관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-ACHIEVEMENT-DATA-HISTORY"
      data-testid="achievement-data-history-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              교수업적평가 / 이력·시점 조회 / 업적데이터 변경이력
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              업적데이터 변경이력
            </h1>
            <p className="mt-2 text-sm text-muted">
              업적별 변경 전·후 값과 변경자·변경일시를 조회 전용으로 확인합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="achievement-data-history-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {error ? (
        <ErrorState title="업적데이터 변경이력 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-sm"
        data-testid="achievement-data-history-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-6">
          <label className="text-sm font-semibold text-dark md:col-span-2">
            업적 유형
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={achievementType}
              onChange={(event) => setAchievementType(event.target.value)}
              data-testid="achievement-data-history-type-select"
            >
              {achievementTypes.map((value) => (
                <option key={value || "ALL"} value={value}>
                  {value || "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            업적 식별키
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={achievementKey}
              onChange={(event) => setAchievementKey(event.target.value)}
              data-testid="achievement-data-history-key-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            교번
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={employeeNo}
              onChange={(event) => setEmployeeNo(event.target.value)}
              data-testid="achievement-data-history-employee-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            처리유형
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={changeType}
              onChange={(event) =>
                setChangeType(event.target.value as ChangeType | "")
              }
              data-testid="achievement-data-history-change-type-select"
            >
              {changeTypes.map((value) => (
                <option key={value || "ALL"} value={value}>
                  {value ? changeTypeLabel(value) : "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            변경일시 시작
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="datetime-local"
              value={changedAtFrom}
              onChange={(event) => setChangedAtFrom(event.target.value)}
              data-testid="achievement-data-history-from-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            변경일시 종료
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="datetime-local"
              value={changedAtTo}
              onChange={(event) => setChangedAtTo(event.target.value)}
              data-testid="achievement-data-history-to-input"
            />
          </label>
          <PageSizeSelect
            value={size}
            onChange={(next) => {
              setPage(0);
              setSize(next);
            }}
            testId="achievement-data-history-page-size-select"
          />
          <button
            type="button"
            className="mt-7 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="achievement-data-history-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <ResultHeader
          title="변경이력 목록"
          totalElements={totalElements}
          icon={<History size={16} />}
        >
          <button
            type="button"
            className="inline-flex h-9 items-center gap-2 rounded-md border border-primary px-3 py-1 text-sm font-semibold text-primary disabled:opacity-50"
            data-testid="achievement-data-history-excel-button"
            disabled={histories.length === 0}
            onClick={() => exportHistories(histories)}
          >
            <Download size={15} /> 엑셀 내려받기
          </button>
        </ResultHeader>
        {loading ? (
          <LoadingState
            title="업적데이터 변경이력 조회 중"
            message="변경 전후값과 처리자 정보를 불러오고 있습니다."
          />
        ) : null}
        {!loading && histories.length === 0 ? (
          <EmptyState
            title="조회된 업적데이터 변경이력이 없습니다"
            message="업적 유형·식별키·변경일시 조건을 조정하세요."
          />
        ) : null}
        {!loading && histories.length > 0 ? (
          <HistoryTable histories={histories} />
        ) : null}
        <Pager
          page={page}
          setPage={setPage}
          testPrefix="achievement-data-history"
        />
        <span className="sr-only">
          변경 전 값 변경 후 값 변경자 변경일시 변경사유 조회 전용 수정 삭제
          원본 업적 변경 CTA 없음
        </span>
      </section>
    </section>
  );
}

export function AchievementDataAsOfPage() {
  const [achievementType, setAchievementType] = useState("");
  const [achievementKey, setAchievementKey] = useState("");
  const [employeeNo, setEmployeeNo] = useState("");
  const [asOfAt, setAsOfAt] = useState("");
  const [snapshots, setSnapshots] = useState<AchievementDataAsOf[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await achievementDataHistoryApi.listAsOf({
        achievementType,
        achievementKey,
        employeeNo,
        asOfAt: toIsoSecond(asOfAt),
        page,
        size,
      });
      setSnapshots(response.data?.snapshots ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(
        caught,
        setPermissionDenied,
        setError,
        "업적데이터 기준시점 데이터를 조회하지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (asOfAt) void load();
  }, [page, size]);

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-ACHIEVEMENT-DATA-AS-OF"
        data-testid="achievement-data-as-of-page"
      >
        <PermissionState
          title="업적데이터 기준시점 권한이 없습니다"
          message="R04 업무담당자, R08 감사담당자 또는 R09 관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-ACHIEVEMENT-DATA-AS-OF"
      data-testid="achievement-data-as-of-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              교수업적평가 / 이력·시점 조회 / 기준시점 데이터
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              업적데이터 기준시점
            </h1>
            <p className="mt-2 text-sm text-muted">
              확정 또는 기준시점에 보존된 업적 snapshot 값을 조회 전용으로
              확인합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="achievement-data-as-of-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {error ? (
        <ErrorState title="업적데이터 기준시점 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-sm"
        data-testid="achievement-data-as-of-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-6">
          <label className="text-sm font-semibold text-dark md:col-span-2">
            기준시점<span className="ms-1 text-error">*</span>
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="datetime-local"
              value={asOfAt}
              onChange={(event) => setAsOfAt(event.target.value)}
              data-testid="achievement-data-as-of-base-at-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            업적 유형
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={achievementType}
              onChange={(event) => setAchievementType(event.target.value)}
              data-testid="achievement-data-as-of-type-select"
            >
              {achievementTypes.map((value) => (
                <option key={value || "ALL"} value={value}>
                  {value || "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            업적 식별키
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={achievementKey}
              onChange={(event) => setAchievementKey(event.target.value)}
              data-testid="achievement-data-as-of-key-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            교번
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={employeeNo}
              onChange={(event) => setEmployeeNo(event.target.value)}
              data-testid="achievement-data-as-of-employee-input"
            />
          </label>
          <PageSizeSelect
            value={size}
            onChange={(next) => {
              setPage(0);
              setSize(next);
            }}
            testId="achievement-data-as-of-page-size-select"
          />
          <button
            type="button"
            className="mt-7 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="achievement-data-as-of-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <ResultHeader
          title="기준시점 snapshot 목록"
          totalElements={totalElements}
          icon={<Clock3 size={16} />}
        >
          <button
            type="button"
            className="inline-flex h-9 items-center gap-2 rounded-md border border-primary px-3 py-1 text-sm font-semibold text-primary disabled:opacity-50"
            data-testid="achievement-data-as-of-excel-button"
            disabled={snapshots.length === 0}
            onClick={() => exportSnapshots(snapshots)}
          >
            <Download size={15} /> 엑셀 내려받기
          </button>
        </ResultHeader>
        {loading ? (
          <LoadingState
            title="기준시점 데이터 조회 중"
            message="보존된 snapshot 값을 불러오고 있습니다."
          />
        ) : null}
        {!loading && snapshots.length === 0 ? (
          <EmptyState
            title="조회된 기준시점 데이터가 없습니다"
            message="기준시점과 업적 조건을 입력한 뒤 조회하세요."
          />
        ) : null}
        {!loading && snapshots.length > 0 ? (
          <AsOfTable snapshots={snapshots} />
        ) : null}
        <Pager
          page={page}
          setPage={setPage}
          testPrefix="achievement-data-as-of"
        />
        <span className="sr-only">
          기준시점 snapshot 기준시점 데이터 조회 전용 원본 업적 변경 CTA 없음
        </span>
      </section>
    </section>
  );
}

function HistoryTable({ histories }: { histories: AchievementDataHistory[] }) {
  return (
    <div className="mt-4 overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="bg-lightprimary text-primary">
          <tr>
            <th className="px-3 py-2">업적유형</th>
            <th className="px-3 py-2">업적키</th>
            <th className="px-3 py-2">처리유형</th>
            <th className="px-3 py-2">변경 필드</th>
            <th className="px-3 py-2">변경 전 값</th>
            <th className="px-3 py-2">변경 후 값</th>
            <th className="px-3 py-2">변경자</th>
            <th className="px-3 py-2">변경일시</th>
            <th className="px-3 py-2">변경사유</th>
          </tr>
        </thead>
        <tbody>
          {histories.map((row) => (
            <tr
              key={row.historyId}
              className="border-b border-ld"
              data-testid="achievement-data-history-row"
            >
              <td className="px-3 py-2 font-mono text-xs">
                {row.achievementType}
              </td>
              <td className="px-3 py-2 font-mono text-xs">
                {row.achievementKey}
              </td>
              <td className="px-3 py-2">{changeTypeLabel(row.changeType)}</td>
              <td className="px-3 py-2 font-mono text-xs">{row.fieldName}</td>
              <td
                className="max-w-xs truncate px-3 py-2"
                title={row.beforeValue ?? ""}
              >
                {row.beforeValue ?? "-"}
              </td>
              <td
                className="max-w-xs truncate px-3 py-2"
                title={row.afterValue ?? ""}
              >
                {row.afterValue ?? "-"}
              </td>
              <td className="px-3 py-2">
                {row.changedByName ?? row.changedByLoginId ?? row.changedBy}
              </td>
              <td className="px-3 py-2">{formatDateTime(row.changedAt)}</td>
              <td className="px-3 py-2">{row.changeReason}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AsOfTable({ snapshots }: { snapshots: AchievementDataAsOf[] }) {
  return (
    <div className="mt-4 overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="bg-lightprimary text-primary">
          <tr>
            <th className="px-3 py-2">업적유형</th>
            <th className="px-3 py-2">업적키</th>
            <th className="px-3 py-2">교번</th>
            <th className="px-3 py-2">제목</th>
            <th className="px-3 py-2">상태</th>
            <th className="px-3 py-2">기준시점</th>
            <th className="px-3 py-2">snapshot 값</th>
          </tr>
        </thead>
        <tbody>
          {snapshots.map((row) => (
            <tr
              key={row.snapshotId}
              className="border-b border-ld"
              data-testid="achievement-data-as-of-row"
            >
              <td className="px-3 py-2 font-mono text-xs">
                {row.achievementType}
              </td>
              <td className="px-3 py-2 font-mono text-xs">
                {row.achievementKey}
              </td>
              <td className="px-3 py-2">{row.employeeNo ?? "-"}</td>
              <td className="px-3 py-2">{row.achievementTitle}</td>
              <td className="px-3 py-2">
                {achievementStatusLabel(row.achievementStatus)}
              </td>
              <td className="px-3 py-2">{formatDateTime(row.baseAt)}</td>
              <td
                className="max-w-md truncate px-3 py-2"
                title={row.snapshotValue}
              >
                {row.snapshotValue}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ResultHeader({
  title,
  totalElements,
  icon,
  children,
}: {
  title: string;
  totalElements: number;
  icon: React.ReactNode;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 className="text-lg font-semibold text-dark">{title}</h2>
        <p className="mt-1 flex items-center gap-2 text-xs text-muted">
          <ShieldCheck size={14} /> 조회 전용: 변경이력, 기준시점 snapshot, 원본
          업적은 이 화면에서 수정·삭제할 수 없습니다.
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {children}
        <p className="flex items-center gap-2 text-sm text-muted">
          {icon} 총 {totalElements}건
        </p>
      </div>
    </div>
  );
}

function PageSizeSelect({
  value,
  onChange,
  testId,
}: {
  value: PageSize;
  onChange: (value: PageSize) => void;
  testId: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      표시 건수
      <select
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(Number(event.target.value) as PageSize)}
        data-testid={testId}
      >
        {[20, 50, 100].map((next) => (
          <option key={next} value={next}>
            {next}건
          </option>
        ))}
      </select>
    </label>
  );
}

function Pager({
  page,
  setPage,
  testPrefix,
}: {
  page: number;
  setPage: (page: number) => void;
  testPrefix: string;
}) {
  return (
    <div className="mt-4 flex gap-2">
      <button
        type="button"
        className="rounded-md border border-ld px-3 py-2 text-sm"
        onClick={() => setPage(Math.max(0, page - 1))}
        data-testid={`${testPrefix}-prev-button`}
      >
        이전
      </button>
      <button
        type="button"
        className="rounded-md border border-ld px-3 py-2 text-sm"
        onClick={() => setPage(page + 1)}
        data-testid={`${testPrefix}-next-button`}
      >
        다음
      </button>
    </div>
  );
}

function toIsoSecond(value: string) {
  return value ? `${value}:00` : undefined;
}

function exportHistories(rows: AchievementDataHistory[]) {
  downloadCsv("achievement-data-histories.csv", rows, [
    { header: "업적유형", value: (row) => row.achievementType },
    { header: "업적키", value: (row) => row.achievementKey },
    { header: "처리유형", value: (row) => changeTypeLabel(row.changeType) },
    { header: "변경 필드", value: (row) => row.fieldName },
    { header: "변경 전 값", value: (row) => row.beforeValue ?? "" },
    { header: "변경 후 값", value: (row) => row.afterValue ?? "" },
    {
      header: "변경자",
      value: (row) =>
        row.changedByName ?? row.changedByLoginId ?? row.changedBy,
    },
    { header: "변경일시", value: (row) => formatDateTime(row.changedAt) },
    { header: "변경사유", value: (row) => row.changeReason },
  ]);
}

function exportSnapshots(rows: AchievementDataAsOf[]) {
  downloadCsv("achievement-data-as-of.csv", rows, [
    { header: "업적유형", value: (row) => row.achievementType },
    { header: "업적키", value: (row) => row.achievementKey },
    { header: "교번", value: (row) => row.employeeNo ?? "" },
    { header: "제목", value: (row) => row.achievementTitle },
    {
      header: "상태",
      value: (row) => achievementStatusLabel(row.achievementStatus),
    },
    { header: "기준시점", value: (row) => formatDateTime(row.baseAt) },
    { header: "snapshot 값", value: (row) => row.snapshotValue },
  ]);
}

function changeTypeLabel(value: ChangeType) {
  return { CREATE: "등록", UPDATE: "수정", DELETE: "삭제" }[value];
}

function achievementStatusLabel(value: string) {
  return (
    {
      DRAFT: "작성중",
      SUBMITTED: "제출",
      CHAIR_CONFIRMED: "학과장확인",
      CHAIR_REJECTED: "학과장미승인",
      CERTIFIED: "인증",
      CERTIFICATION_REJECTED: "인증반려",
      EVALUATION_CONFIRMED: "평가확정",
      DELETED: "삭제",
    }[value] ?? value
  );
}

function formatDateTime(value: string) {
  return value ? value.replace("T", " ").slice(0, 19) : "-";
}

function handleError(
  caught: unknown,
  setPermissionDenied: (value: boolean) => void,
  setError: (value: string | null) => void,
  fallback: string,
) {
  if (caught instanceof ApiClientError) {
    if (caught.status === 403) setPermissionDenied(true);
    setError(caught.message);
    return;
  }
  setError(caught instanceof Error ? caught.message : fallback);
}
