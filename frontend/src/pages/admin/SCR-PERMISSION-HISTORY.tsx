import { History, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  permissionHistoryApi,
  type PermissionChangeHistory,
  type PermissionHistoryTargetType,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const targetTypes: Array<PermissionHistoryTargetType | ""> = [
  "",
  "ROLE",
  "MENU",
  "FUNCTION",
  "DATA_SCOPE",
  "TEMPORARY",
];

export function PermissionHistoryPage() {
  const [targetType, setTargetType] = useState<
    PermissionHistoryTargetType | ""
  >("");
  const [targetId, setTargetId] = useState("");
  const [history, setHistory] = useState<PermissionChangeHistory[]>([]);
  const [selected, setSelected] = useState<PermissionChangeHistory | null>(
    null,
  );
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedLabel = useMemo(() => {
    if (!selected) return "선택된 이력이 없습니다";
    return `${selected.targetType} / ${selected.targetId}`;
  }, [selected]);

  const load = async (showSuccess = false) => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await permissionHistoryApi.listPermissionChangeHistory({
        targetType,
        targetId,
        page,
        size,
      });
      const data = response.data;
      setHistory(data?.history ?? []);
      setTotal(data?.total ?? 0);
      setSelected(null);
      setSuccessMessage(showSuccess ? "검색이 완료되었습니다" : null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page]);

  const search = () => {
    setPage(0);
    void load(true);
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
        setError(caught.message);
        return;
      }
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "권한 변경 이력을 조회하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PERMISSION-HISTORY"
        data-testid="permission-history-page"
      >
        <PermissionState
          title="권한 변경 이력 조회 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PERMISSION-HISTORY"
      data-testid="permission-history-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 역할·권한 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              권한 변경 이력 조회
            </h1>
            <p className="mt-2 text-sm text-muted">
              역할·메뉴·기능·데이터 범위·임시 권한 변경 전후 값과 처리자, 사유,
              변경일시를 조회합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            data-testid="permission-history-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        권한 변경 이력 조회 권한이 없습니다 조회된 권한 변경 이력이 없습니다
        검색이 완료되었습니다
      </div>
      {error ? (
        <ErrorState title="권한 변경 이력 조회 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="permission-history-search-panel"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto]">
          <label className="text-sm font-medium text-dark">
            유형
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="permission-history-target-type-select"
              value={targetType}
              onChange={(event) =>
                setTargetType(
                  event.target.value as PermissionHistoryTargetType | "",
                )
              }
            >
              {targetTypes.map((value) => (
                <option key={value || "ALL"} value={value}>
                  {value || "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            대상 ID
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="permission-history-target-id-input"
              value={targetId}
              onChange={(event) => setTargetId(event.target.value)}
            />
          </label>
          <button
            className="mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="permission-history-search-button"
            onClick={search}
            type="button"
          >
            <Search size={16} /> 검색
          </button>
        </div>
      </section>

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-8"
          data-testid="permission-history-list-panel"
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">이력 목록</h2>
            <span className="text-sm text-muted">총 {total}건</span>
          </div>
          {loading ? <LoadingState title="권한 변경 이력 조회 중" /> : null}
          {!loading && history.length === 0 ? (
            <EmptyState
              title="조회된 권한 변경 이력이 없습니다"
              message="유형과 대상 ID 조건을 확인한 뒤 검색하세요."
            />
          ) : null}
          {!loading && history.length > 0 ? (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">유형</th>
                    <th className="px-3 py-2">대상</th>
                    <th className="px-3 py-2">변경 전 값</th>
                    <th className="px-3 py-2">변경 후 값</th>
                    <th className="px-3 py-2">처리자</th>
                    <th className="px-3 py-2">사유</th>
                    <th className="px-3 py-2">변경일시</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {history.map((item) => (
                    <tr
                      className="cursor-pointer hover:bg-lightprimary/40"
                      data-testid={`permission-history-row-${item.permissionHistoryId}`}
                      key={item.permissionHistoryId}
                      onClick={() => setSelected(item)}
                    >
                      <td className="px-3 py-2 font-medium text-dark">
                        {item.targetType}
                      </td>
                      <td className="px-3 py-2">{item.targetId}</td>
                      <td className="max-w-[220px] truncate px-3 py-2 font-mono text-xs">
                        {item.beforeValue ?? "-"}
                      </td>
                      <td className="max-w-[220px] truncate px-3 py-2 font-mono text-xs">
                        {item.afterValue ?? "-"}
                      </td>
                      <td className="px-3 py-2">{item.changedBy}</td>
                      <td className="px-3 py-2">{item.reason}</td>
                      <td className="px-3 py-2 text-muted">{item.changedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex justify-end gap-2">
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="permission-history-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="permission-history-next-page-button"
              disabled={(page + 1) * size >= total}
              onClick={() => setPage((value) => value + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <aside
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-4"
          data-testid="permission-history-detail-panel"
        >
          <div className="flex items-center gap-2">
            <History className="text-primary" size={18} />
            <h2 className="text-lg font-semibold text-dark">읽기 전용 상세</h2>
          </div>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          <div className="mt-4 rounded-md bg-lightsecondary p-4 text-sm text-link">
            이 화면에는 권한 변경, 이력 수정, 이력 삭제 버튼이 없습니다.
          </div>
          <div className="mt-4 space-y-3 text-sm">
            <ReadOnlyField
              label="변경 전 값"
              value={selected?.beforeValue ?? "-"}
            />
            <ReadOnlyField
              label="변경 후 값"
              value={selected?.afterValue ?? "-"}
            />
            <ReadOnlyField
              label="처리자"
              value={selected ? String(selected.changedBy) : "-"}
            />
            <ReadOnlyField label="사유" value={selected?.reason ?? "-"} />
            <ReadOnlyField
              label="변경일시"
              value={selected?.changedAt ?? "-"}
            />
          </div>
        </aside>
      </div>
    </section>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="font-semibold text-dark">{label}</p>
      <pre className="mt-1 max-h-36 overflow-auto rounded-md border border-ld bg-white p-3 text-xs text-muted whitespace-pre-wrap">
        {value}
      </pre>
    </div>
  );
}
