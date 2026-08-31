import { History, RefreshCw, Search, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  dataChangeHistoryApi,
  type ChangeType,
  type DataChangeHistory,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";

const changeTypes: Array<ChangeType | ""> = ["", "CREATE", "UPDATE", "DELETE"];

export function DataChangeHistoryPage() {
  const [targetBusiness, setTargetBusiness] = useState("");
  const [targetKey, setTargetKey] = useState("");
  const [changedBy, setChangedBy] = useState("");
  const [changedAtFrom, setChangedAtFrom] = useState("");
  const [changedAtTo, setChangedAtTo] = useState("");
  const [changeType, setChangeType] = useState<ChangeType | "">("");
  const [histories, setHistories] = useState<DataChangeHistory[]>([]);
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
      const response = await dataChangeHistoryApi.listDataChangeHistories({
        targetBusiness,
        targetKey,
        changedBy,
        changedAtFrom: toIsoMinute(changedAtFrom),
        changedAtTo: toIsoMinute(changedAtTo),
        changeType,
        page,
        size,
      });
      setHistories(response.data?.histories ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      if (caught instanceof ApiClientError) {
        if (caught.status === 403) setPermissionDenied(true);
        setError(caught.message);
      } else {
        setError(
          caught instanceof Error
            ? caught.message
            : "데이터 변경 이력을 조회하지 못했습니다.",
        );
      }
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
        data-screen-id="SCR-DATA-CHANGE-HISTORY"
        data-testid="data-change-history-page"
      >
        <PermissionState
          title="데이터 변경 이력 권한이 없습니다"
          message="R09 시스템관리자 또는 데이터 이력 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-DATA-CHANGE-HISTORY"
      data-testid="data-change-history-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              파일·데이터 관리 / 데이터 이력 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              데이터 변경 이력
            </h1>
            <p className="mt-2 text-sm text-muted">
              업무 운영 데이터의 등록·수정·삭제 전후값을 필드 단위로 조회합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="data-change-history-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {error ? (
        <ErrorState title="데이터 변경 이력 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-6">
          <label className="text-sm font-semibold text-dark md:col-span-2">
            대상 업무·기준정보
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={targetBusiness}
              onChange={(event) => setTargetBusiness(event.target.value)}
              placeholder="예: rejection_reasons"
              data-testid="data-change-history-target-business-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            대상 식별정보
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={targetKey}
              onChange={(event) => setTargetKey(event.target.value)}
              data-testid="data-change-history-target-key-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            처리자
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={changedBy}
              onChange={(event) =>
                setChangedBy(event.target.value.replace(/[^0-9]/g, ""))
              }
              inputMode="numeric"
              data-testid="data-change-history-changed-by-input"
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
              data-testid="data-change-history-change-type-select"
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
              data-testid="data-change-history-from-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            변경일시 종료
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="datetime-local"
              value={changedAtTo}
              onChange={(event) => setChangedAtTo(event.target.value)}
              data-testid="data-change-history-to-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value) as PageSize);
              }}
              data-testid="data-change-history-page-size-select"
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
            className="mt-7 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="data-change-history-search-button"
          >
            <Search size={16} /> 검색
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">변경 이력 목록</h2>
            <p className="mt-1 flex items-center gap-2 text-xs text-muted">
              <ShieldCheck size={14} />
              조회 전용: 원본자료와 변경이력은 이 화면에서 수정·삭제할 수
              없습니다.
            </p>
          </div>
          <p className="text-sm text-muted">총 {totalElements}건</p>
        </div>
        {loading ? (
          <LoadingState
            title="데이터 변경 이력 조회 중"
            message="필드 단위 변경 전후값을 불러오고 있습니다."
          />
        ) : null}
        {!loading && histories.length === 0 ? (
          <EmptyState
            title="조회된 데이터 변경 이력이 없습니다"
            message="대상 업무·식별정보·변경일시 조건을 조정하세요."
          />
        ) : null}
        {!loading && histories.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightprimary text-primary">
                <tr>
                  <th className="px-3 py-2">대상</th>
                  <th className="px-3 py-2">원본키</th>
                  <th className="px-3 py-2">처리유형</th>
                  <th className="px-3 py-2">변경 필드</th>
                  <th className="px-3 py-2">변경 전 값</th>
                  <th className="px-3 py-2">변경 후 값</th>
                  <th className="px-3 py-2">처리자</th>
                  <th className="px-3 py-2">변경일시</th>
                </tr>
              </thead>
              <tbody>
                {histories.map((row) => (
                  <tr
                    key={row.historyId}
                    className="border-b border-ld"
                    data-testid="data-change-history-row"
                  >
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.targetBusiness}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.targetKey}
                    </td>
                    <td className="px-3 py-2">
                      {changeTypeLabel(row.changeType)}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.fieldName}
                    </td>
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
                      {row.changedByName ??
                        row.changedByLoginId ??
                        row.changedBy}
                    </td>
                    <td className="px-3 py-2">
                      {formatDateTime(row.changedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(Math.max(0, page - 1))}
            data-testid="data-change-history-prev-button"
          >
            이전
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(page + 1)}
            data-testid="data-change-history-next-button"
          >
            다음
          </button>
        </div>
        <span className="sr-only">
          데이터 변경 이력 권한이 없습니다 조회된 데이터 변경 이력이 없습니다
          변경 전 값 변경 후 값 조회 전용
        </span>
      </section>
    </section>
  );
}

function toIsoMinute(value: string) {
  return value ? `${value}:00` : undefined;
}

function changeTypeLabel(value: ChangeType) {
  return {
    CREATE: "등록",
    UPDATE: "수정",
    DELETE: "삭제",
  }[value];
}

function formatDateTime(value: string) {
  return value ? value.replace("T", " ").slice(0, 19) : "-";
}
