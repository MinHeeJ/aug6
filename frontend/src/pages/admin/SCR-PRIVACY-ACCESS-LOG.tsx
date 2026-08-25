import { Eye, RefreshCw, Search, ShieldAlert } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  privacyAccessLogApi,
  type PrivacyAccessLog,
  type PrivacyProcessType,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";

const pageSizes = [20, 50, 100];
const processTypes: Array<PrivacyProcessType | ""> = [
  "",
  "VIEW",
  "PRINT",
  "DOWNLOAD",
];

export function PrivacyAccessLogPage() {
  const [actorUserIdInput, setActorUserIdInput] = useState("");
  const [targetRef, setTargetRef] = useState("");
  const [processType, setProcessType] = useState<PrivacyProcessType | "">("");
  const [processedFrom, setProcessedFrom] = useState("");
  const [processedTo, setProcessedTo] = useState("");
  const [logs, setLogs] = useState<PrivacyAccessLog[]>([]);
  const [selected, setSelected] = useState<PrivacyAccessLog | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const actorUserId = useMemo(() => {
    const trimmed = actorUserIdInput.trim();
    if (!trimmed) return undefined;
    const numeric = Number(trimmed);
    return Number.isFinite(numeric) ? numeric : undefined;
  }, [actorUserIdInput]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await privacyAccessLogApi.searchPrivacyAccessLogs({
        actorUserId,
        targetRef,
        processType,
        processedFrom,
        processedTo,
        page,
        size,
      });
      setLogs(response.data?.logs ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectLog = async (historyId: number) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response = await privacyAccessLogApi.getPrivacyAccessLog(historyId);
      setSelected(response.data ?? null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
      }
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "개인정보 처리이력 정보를 조회하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PRIVACY-ACCESS-LOG"
        data-testid="privacy-access-log-page"
      >
        <PermissionState
          title="개인정보 처리이력 권한이 없습니다"
          message="R09 시스템관리자 또는 개인정보 처리이력 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PRIVACY-ACCESS-LOG"
      data-testid="privacy-access-log-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">보안·감사 관리 / 개인정보 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              개인정보 처리이력
            </h1>
            <p className="mt-2 text-sm text-muted">
              개인정보 조회·출력·다운로드 처리 목적, 대상 참조, 처리일시, IP와
              결과를 조회합니다. 이 화면은 조회 전용이며 보호 대상 개인정보
              원문은 표시하지 않습니다.
            </p>
          </div>
          <ShieldAlert className="h-10 w-10 text-primary" aria-hidden="true" />
        </div>
      </div>

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-3 lg:grid-cols-6">
          <label className="text-sm font-medium text-dark">
            사용자 ID
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-access-log-actor-input"
              inputMode="numeric"
              value={actorUserIdInput}
              onChange={(event) => setActorUserIdInput(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            대상자 참조
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-access-log-target-input"
              value={targetRef}
              onChange={(event) => setTargetRef(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            처리유형
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-access-log-process-type-select"
              value={processType}
              onChange={(event) =>
                setProcessType(event.target.value as PrivacyProcessType | "")
              }
            >
              {processTypes.map((value) => (
                <option key={value || "ALL"} value={value}>
                  {value || "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            시작일
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-access-log-from-input"
              type="date"
              value={processedFrom}
              onChange={(event) => setProcessedFrom(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            종료일
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-access-log-to-input"
              type="date"
              value={processedTo}
              onChange={(event) => setProcessedTo(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-access-log-page-size-select"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value));
              }}
            >
              {pageSizes.map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="privacy-access-log-search-button"
            onClick={() => {
              setPage(0);
              void load();
            }}
            type="button"
          >
            <Search className="mr-2 h-4 w-4" aria-hidden="true" />
            조회
          </button>
          <button
            className="inline-flex items-center rounded-md border border-ld px-4 py-2 text-sm"
            data-testid="privacy-access-log-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw className="mr-2 h-4 w-4" aria-hidden="true" />
            새로고침
          </button>
        </div>
      </section>

      {error ? (
        <ErrorState title="개인정보 처리이력 오류" message={error} />
      ) : null}
      {loading ? <LoadingState title="처리이력 조회 중" /> : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.65fr)]">
        <section className="rounded-md border border-ld bg-white p-5 shadow-md">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-dark">
                개인정보 처리이력 목록
              </h2>
              <p className="text-sm text-muted">
                총 {totalElements}건 · 기본 20건 표시
              </p>
            </div>
          </div>
          {!loading && logs.length === 0 ? (
            <EmptyState
              title="조회된 처리이력이 없습니다"
              message="사용자, 대상자, 처리유형 또는 기간 조건을 변경하세요."
            />
          ) : null}
          {logs.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-xs font-semibold uppercase tracking-wide text-muted">
                  <tr>
                    <th className="px-3 py-3">처리유형</th>
                    <th className="px-3 py-3">사용자</th>
                    <th className="px-3 py-3">대상자</th>
                    <th className="px-3 py-3">처리목적</th>
                    <th className="px-3 py-3">처리일시</th>
                    <th className="px-3 py-3">IP</th>
                    <th className="px-3 py-3">결과</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {logs.map((log) => (
                    <tr
                      key={log.historyId}
                      className="cursor-pointer hover:bg-lightprimary"
                      data-testid={`privacy-access-log-row-${log.historyId}`}
                      onClick={() => void selectLog(log.historyId)}
                    >
                      <td className="px-3 py-3 font-medium text-dark">
                        {log.processType}
                      </td>
                      <td className="px-3 py-3">
                        {log.actorLoginId ?? log.actorUserId}
                      </td>
                      <td className="px-3 py-3">{log.targetRef}</td>
                      <td className="px-3 py-3">{log.processPurpose}</td>
                      <td className="px-3 py-3 text-muted">
                        {log.processedAt}
                      </td>
                      <td className="px-3 py-3">{log.requestIp}</td>
                      <td className="px-3 py-3">{log.processResult}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex items-center justify-between text-sm text-muted">
            <button
              className="rounded-md border border-ld px-3 py-1 disabled:opacity-40"
              data-testid="privacy-access-log-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              이전
            </button>
            <span>{page + 1} 페이지</span>
            <button
              className="rounded-md border border-ld px-3 py-1 disabled:opacity-40"
              data-testid="privacy-access-log-next-page-button"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((current) => current + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <section
          className="rounded-md border border-ld bg-white p-5 shadow-md"
          data-testid="privacy-access-log-detail-region"
        >
          <div className="flex items-center gap-2">
            <Eye className="h-5 w-5 text-primary" aria-hidden="true" />
            <h2 className="text-lg font-semibold text-dark">
              상세 영역 (조회 전용)
            </h2>
          </div>
          {detailLoading ? <LoadingState title="상세 조회 중" /> : null}
          {!selected && !detailLoading ? (
            <EmptyState
              title="선택된 처리이력이 없습니다"
              message="목록에서 처리이력을 선택하세요."
            />
          ) : null}
          {selected ? (
            <dl className="mt-4 space-y-3 text-sm">
              <DetailItem label="이력 ID" value={String(selected.historyId)} />
              <DetailItem label="처리유형" value={selected.processType} />
              <DetailItem
                label="처리자"
                value={`${selected.actorLoginId ?? "-"} (${selected.actorUserId})`}
              />
              <DetailItem label="대상자 참조" value={selected.targetRef} />
              <DetailItem label="처리목적" value={selected.processPurpose} />
              <DetailItem label="처리일시" value={selected.processedAt} />
              <DetailItem label="IP" value={selected.requestIp} />
              <DetailItem label="처리결과" value={selected.processResult} />
            </dl>
          ) : null}
          <div className="mt-4 rounded-md bg-lightsecondary p-3 text-sm text-muted">
            보호 대상 개인정보 원문은 표시하지 않으며, 수정 버튼과 삭제 버튼이
            없는 조회 전용 이력입니다.
          </div>
        </section>
      </div>
    </section>
  );
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-ld p-3">
      <dt className="text-xs font-semibold text-muted">{label}</dt>
      <dd className="mt-1 break-words text-dark">{value}</dd>
    </div>
  );
}
