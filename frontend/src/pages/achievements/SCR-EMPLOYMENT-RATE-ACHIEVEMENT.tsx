import { RefreshCw, Save, Upload } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  SuccessState,
} from "../../components/States";

type Achievement = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string | null;
  certificationStatus: string;
};

type SearchResponse = {
  achievements: Achievement[];
};

/** Renders the individual employment-rate flow and the policy-safe R07 bulk entry points. */
export function EmploymentRateAchievementPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [achievementDate, setAchievementDate] = useState("");
  const [achievementName, setAchievementName] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      const response = await apiRequest<SearchResponse>(
        `/api/business/employment-rate-achievements?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const select = async (row: Achievement) => {
    const response = await apiRequest<Achievement>(
      `/api/business/employment-rate-achievements/${row.achievementId}`,
    );
    const detail = response.data ?? row;
    setSelected(detail);
    setManagementItemCode(detail.managementItemCode);
    setAchievementDate(detail.achievementDate);
    setAchievementName(detail.achievementName ?? "");
  };

  const save = async () => {
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) {
      return;
    }
    try {
      const path = selected
        ? `/api/business/employment-rate-achievements/${selected.achievementId}`
        : "/api/business/employment-rate-achievements";
      await apiRequest(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode,
          achievementDate,
          achievementName,
        }),
      });
      setMessage("취업률 실적이 저장되었습니다.");
      await load();
    } catch (caught) {
      setError(errorMessage(caught));
    }
  };

  const upload = async () => {
    if (
      !file ||
      !window.confirm(
        "검증 결과에 오류가 있으면 어떤 행도 반영되지 않습니다. 계속하시겠습니까?",
      )
    ) {
      return;
    }
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(
        "/api/business/employment-rate-achievements/excel-uploads",
        {
          method: "POST",
          credentials: "include",
          body,
        },
      );
      if (!response.ok) {
        throw new Error("Excel 업로드를 반영할 수 없습니다.");
      }
      setMessage("Excel 검증과 반영이 완료되었습니다.");
      await load();
    } catch (caught) {
      setError(errorMessage(caught));
    }
  };

  const requestBulk = async () => {
    try {
      await apiRequest("/api/business/employment-rate-achievements/bulk-jobs", {
        method: "POST",
        body: JSON.stringify({}),
      });
    } catch (caught) {
      setError(errorMessage(caught));
    }
  };

  const locked = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENT"
      data-testid="employment-rate-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">취업률 실적 관리</h1>
        <p className="mt-2 text-sm text-muted">
          개별 실적 조회·저장과 R07 Excel 및 일괄 처리 결과를 관리합니다.
        </p>
        <button
          className="mt-3 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
          data-testid="employment-rate-achievement-refresh-button"
          onClick={() => void load()}
          type="button"
        >
          <RefreshCw size={16} />
          새로고침
        </button>
      </header>

      {message ? <SuccessState title="처리 완료" message={message} /> : null}
      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievement-list-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">개별 실적 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-rate-achievement-page-size-select"
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
              value={pageSize}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 취업률 실적이 없습니다" />
        ) : null}
        <div className="mt-2 space-y-2">
          {rows.map((row) => (
            <button
              className="block w-full rounded border border-ld p-3 text-left"
              data-testid="employment-rate-achievement-row"
              key={row.achievementId}
              onClick={() => void select(row)}
              type="button"
            >
              {row.managementNo} · {row.certificationStatus}
            </button>
          ))}
        </div>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">실적 상세</h2>
        <input
          className="mt-3 w-full"
          data-testid="employment-rate-achievement-management-item-input"
          disabled={locked}
          onChange={(event) => setManagementItemCode(event.target.value)}
          placeholder="관리항목 *"
          value={managementItemCode}
        />
        <input
          className="mt-3 w-full"
          data-testid="employment-rate-achievement-date-input"
          disabled={locked}
          onChange={(event) => setAchievementDate(event.target.value)}
          type="date"
          value={achievementDate}
        />
        <input
          className="mt-3 w-full"
          data-testid="employment-rate-achievement-name-input"
          disabled={locked}
          onChange={(event) => setAchievementName(event.target.value)}
          placeholder="실적명"
          value={achievementName}
        />
        <button
          className="mt-3 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white disabled:opacity-50"
          data-testid="employment-rate-achievement-save-button"
          disabled={locked}
          onClick={() => void save()}
          type="button"
        >
          <Save size={16} />
          저장
        </button>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievement-excel-panel"
      >
        <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
        <p className="mt-2 text-sm text-muted">
          오류 또는 중복 행이 하나라도 있으면 전체가 반영되지 않습니다.
        </p>
        <input
          className="mt-3"
          data-testid="employment-rate-achievement-upload-input"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          type="file"
        />
        <button
          className="ml-2 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
          data-testid="employment-rate-achievement-upload-button"
          onClick={() => void upload()}
          type="button"
        >
          <Upload size={16} />
          검증 및 반영
        </button>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievement-bulk-panel"
      >
        <h2 className="text-lg font-semibold text-dark">일괄 처리 결과</h2>
        <p className="mt-2 text-sm text-muted">
          대상 미리보기·확인 정책은 승인 전이며 실행 결과를 임의로 생성하지
          않습니다.
        </p>
        <button
          className="mt-3 rounded-md border border-primary px-4 py-2 text-primary"
          data-testid="employment-rate-achievement-bulk-button"
          onClick={() => void requestBulk()}
          type="button"
        >
          일괄 작업 요청
        </button>
      </section>
    </section>
  );
}

function errorMessage(caught: unknown) {
  if (caught instanceof ApiClientError || caught instanceof Error) {
    return caught.message;
  }
  return "처리 중 오류가 발생했습니다.";
}
