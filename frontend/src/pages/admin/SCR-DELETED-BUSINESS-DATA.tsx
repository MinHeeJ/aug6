import { RefreshCw, Search, ShieldCheck, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  deletedBusinessDataApi,
  type BusinessType,
  type DeletedBusinessData,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";

const businessTypes: Array<BusinessType | ""> = [
  "",
  "FACULTY_ACHIEVEMENT",
  "ACADEMIC_GRANT",
  "OBJECTION",
];

export function DeletedBusinessDataPage() {
  const [businessType, setBusinessType] = useState<BusinessType | "">("");
  const [originalKey, setOriginalKey] = useState("");
  const [deletedBy, setDeletedBy] = useState("");
  const [deletedAtFrom, setDeletedAtFrom] = useState("");
  const [deletedAtTo, setDeletedAtTo] = useState("");
  const [deletedData, setDeletedData] = useState<DeletedBusinessData[]>([]);
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
      const response = await deletedBusinessDataApi.listDeletedBusinessData({
        businessType,
        originalKey,
        deletedBy,
        deletedAtFrom: toIsoMinute(deletedAtFrom),
        deletedAtTo: toIsoMinute(deletedAtTo),
        page,
        size,
      });
      setDeletedData(response.data?.deletedData ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      if (caught instanceof ApiClientError) {
        if (caught.status === 403) setPermissionDenied(true);
        setError(caught.message);
      } else {
        setError(
          caught instanceof Error
            ? caught.message
            : "삭제자료를 조회하지 못했습니다.",
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
        data-screen-id="SCR-DELETED-BUSINESS-DATA"
        data-testid="deleted-business-data-page"
      >
        <PermissionState
          title="삭제자료 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 삭제자료 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-DELETED-BUSINESS-DATA"
      data-testid="deleted-business-data-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              파일·데이터 관리 / 데이터 이력 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              삭제자료 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              논리삭제된 업무자료의 원본키와
              삭제자·삭제일시·삭제사유·복구가능여부를 조회합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="deleted-business-data-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {error ? <ErrorState title="삭제자료 조회 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-6">
          <label className="text-sm font-semibold text-dark md:col-span-2">
            삭제된 업무자료
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={businessType}
              onChange={(event) =>
                setBusinessType(event.target.value as BusinessType | "")
              }
              data-testid="deleted-business-data-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value || "ALL"} value={value}>
                  {value ? businessTypeLabel(value) : "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            원본키
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={originalKey}
              onChange={(event) => setOriginalKey(event.target.value)}
              data-testid="deleted-business-data-original-key-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            삭제자
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={deletedBy}
              onChange={(event) =>
                setDeletedBy(event.target.value.replace(/[^0-9]/g, ""))
              }
              inputMode="numeric"
              data-testid="deleted-business-data-deleted-by-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            삭제일시 시작
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="datetime-local"
              value={deletedAtFrom}
              onChange={(event) => setDeletedAtFrom(event.target.value)}
              data-testid="deleted-business-data-from-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            삭제일시 종료
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="datetime-local"
              value={deletedAtTo}
              onChange={(event) => setDeletedAtTo(event.target.value)}
              data-testid="deleted-business-data-to-input"
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
              data-testid="deleted-business-data-page-size-select"
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
            data-testid="deleted-business-data-search-button"
          >
            <Search size={16} /> 검색
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-semibold text-dark">
              <Trash2 size={18} />
              삭제자료 목록
            </h2>
            <p className="mt-1 flex items-center gap-2 text-xs text-muted">
              <ShieldCheck size={14} />
              복구/물리삭제 기능은 제공하지 않습니다. 이 화면은 조회 전용입니다.
            </p>
          </div>
          <p className="text-sm text-muted">총 {totalElements}건</p>
        </div>
        {loading ? (
          <LoadingState
            title="삭제자료 조회 중"
            message="논리삭제 자료의 삭제정보를 불러오고 있습니다."
          />
        ) : null}
        {!loading && deletedData.length === 0 ? (
          <EmptyState
            title="조회된 삭제자료가 없습니다"
            message="업무자료·원본키·삭제일시 조건을 조정하세요."
          />
        ) : null}
        {!loading && deletedData.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightprimary text-primary">
                <tr>
                  <th className="px-3 py-2">업무자료</th>
                  <th className="px-3 py-2">원본키</th>
                  <th className="px-3 py-2">삭제자</th>
                  <th className="px-3 py-2">삭제일시</th>
                  <th className="px-3 py-2">삭제사유</th>
                  <th className="px-3 py-2">복구가능여부</th>
                </tr>
              </thead>
              <tbody>
                {deletedData.map((row) => (
                  <tr
                    key={row.deletedDataId}
                    className="border-b border-ld"
                    data-testid="deleted-business-data-row"
                  >
                    <td className="px-3 py-2">
                      {businessTypeLabel(row.businessType)}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.originalKey}
                    </td>
                    <td className="px-3 py-2">
                      {row.deletedByName ??
                        row.deletedByLoginId ??
                        row.deletedBy}
                    </td>
                    <td className="px-3 py-2">
                      {formatDateTime(row.deletedAt)}
                    </td>
                    <td
                      className="max-w-xs truncate px-3 py-2"
                      title={row.deleteReason}
                    >
                      {row.deleteReason}
                    </td>
                    <td className="px-3 py-2">
                      {row.recoverableYn === "Y" ? "가능" : "불가"}
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
            data-testid="deleted-business-data-prev-button"
          >
            이전
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(page + 1)}
            data-testid="deleted-business-data-next-button"
          >
            다음
          </button>
        </div>
        <span className="sr-only">
          삭제자료 관리 권한이 없습니다 조회된 삭제자료가 없습니다 복구가능여부
          복구/물리삭제 기능은 제공하지 않습니다
        </span>
      </section>
    </section>
  );
}

function toIsoMinute(value: string) {
  return value ? `${value}:00` : undefined;
}

function businessTypeLabel(value: BusinessType) {
  return {
    FACULTY_ACHIEVEMENT: "교수업적평가",
    ACADEMIC_GRANT: "학술지원금",
    OBJECTION: "이의신청",
  }[value];
}

function formatDateTime(value: string) {
  return value ? value.replace("T", " ").slice(0, 19) : "-";
}
