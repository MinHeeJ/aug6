import { RotateCcw, Search } from "lucide-react";
import { useState } from "react";
import {
  ApiClientError,
  schoolInfoApi,
  type SchoolInfoRow,
} from "../../api/apiClient";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";

type Filters = {
  schoolName: string;
  educationOfficeCode: string;
};

type Status = "idle" | "loading" | "success" | "empty" | "error";

const defaultFilters: Filters = {
  schoolName: "",
  educationOfficeCode: "",
};

const columns: Array<{ key: keyof SchoolInfoRow; label: string }> = [
  { key: "educationOfficeName", label: "교육청명" },
  { key: "schoolName", label: "학교명" },
  { key: "schoolKindName", label: "학교종류" },
  { key: "locationName", label: "소재지" },
  { key: "foundationName", label: "설립구분" },
  { key: "roadAddress", label: "도로명주소" },
  { key: "telephoneNumber", label: "전화번호" },
];

export function SchoolInfoLookupPage() {
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [rows, setRows] = useState<SchoolInfoRow[]>([]);
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<string | null>(null);

  const search = async () => {
    try {
      setStatus("loading");
      setError(null);
      const response = await schoolInfoApi.search({
        schoolName: filters.schoolName,
        educationOfficeCode: filters.educationOfficeCode,
        page: 1,
        size: 100,
      });
      const nextRows = response.data?.rows ?? [];
      setRows(nextRows);
      setStatus(nextRows.length === 0 ? "empty" : "success");
    } catch (caught) {
      setRows([]);
      setError(toErrorMessage(caught));
      setStatus("error");
    }
  };

  const reset = () => {
    setFilters(defaultFilters);
    setRows([]);
    setError(null);
    setStatus("idle");
  };

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-SCHOOL-INFO-LOOKUP"
      data-testid="school-info-lookup-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">
          시스템 관리 / 외부 연동 / 학교 정보 조회
        </p>
        <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-xl font-semibold text-dark">학교정보 조회</h1>
            <p className="mt-2 text-sm text-muted">
              NEIS 학교기본정보 공개 API를 호출해 결과를 저장하지 않고 화면에서
              확인합니다. 인증키가 없어도 표본 응답으로 연동을 검증할 수
              있습니다.
            </p>
          </div>
          <div
            className="rounded-xl bg-lightprimary px-4 py-2 text-sm font-semibold text-primary"
            data-testid="school-info-displayed-count"
          >
            표시 {rows.length}건
          </div>
        </div>
      </div>

      {status === "error" && error ? (
        <div role="alert">
          <ErrorState title="NEIS 학교정보 조회 오류" message={error} />
        </div>
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-md"
        data-testid="school-info-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-5">
          <label className="md:col-span-2">
            <span className="text-sm font-semibold text-link">학교명</span>
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="school-info-school-name-input"
              placeholder="예: 가락"
              value={filters.schoolName}
              onChange={(event) =>
                setFilters({ ...filters, schoolName: event.target.value })
              }
            />
          </label>
          <label className="md:col-span-2">
            <span className="text-sm font-semibold text-link">
              시도교육청 코드
            </span>
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="school-info-office-code-input"
              placeholder="예: B10"
              value={filters.educationOfficeCode}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  educationOfficeCode: event.target.value,
                })
              }
            />
          </label>
          <div className="flex items-end gap-2">
            <button
              className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
              data-testid="school-info-search-button"
              disabled={status === "loading"}
              onClick={() => void search()}
              type="button"
            >
              <Search size={16} /> {status === "loading" ? "조회 중" : "조회"}
            </button>
            <button
              className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link hover:bg-lightprimary"
              data-testid="school-info-reset-button"
              disabled={status === "loading"}
              onClick={reset}
              type="button"
            >
              <RotateCcw size={16} /> 조건 초기화
            </button>
          </div>
        </div>
      </section>

      {status === "loading" ? (
        <LoadingState
          title="조회 중"
          message="NEIS 학교기본정보를 조회하고 있습니다."
        />
      ) : null}

      {status === "idle" ? (
        <EmptyState
          title="검색 조건을 입력하거나 비운 상태로 조회하세요"
          message="학교명과 시도교육청 코드는 선택 입력입니다. 페이지네이션과 저장 기능은 제공하지 않습니다."
        />
      ) : null}

      {status === "empty" ? (
        <EmptyState
          title="조회 결과가 없습니다"
          message="INFO-200 결과 없음 응답은 오류가 아니므로 조건을 바꾸어 다시 조회할 수 있습니다."
        />
      ) : null}

      {status === "success" ? (
        <section
          className="overflow-hidden rounded-md border border-ld bg-white shadow-md"
          data-testid="school-info-result-table"
        >
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr>
                  {columns.map((column) => (
                    <th key={column.key}>{column.label}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr
                    className="border-b border-ld last:border-0"
                    data-testid="school-info-result-row"
                    key={`${row.educationOfficeName}-${row.schoolName}-${index}`}
                  >
                    {columns.map((column) => (
                      <td key={column.key}>{row[column.key] || "-"}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </section>
  );
}

function toErrorMessage(caught: unknown) {
  if (caught instanceof ApiClientError) {
    return caught.message;
  }
  return caught instanceof Error
    ? caught.message
    : "NEIS 학교정보 조회에 실패했습니다.";
}
