import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Achievement = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string;
  certificationStatus: string;
};

type Search = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type Upload = {
  uploadId: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  persistedCount: number;
  errors: {
    rowNumber: number;
    errorReason: string;
  }[];
};

/** Renders SCR-EMPLOYMENT-RATE-ACHIEVEMENTS with individual, Excel, and bulk-result tabs. */
export function EmploymentRateAchievementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [permission, setPermission] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [form, setForm] = useState({
    managementItemCode: "",
    achievementDate: "",
    achievementName: "",
  });
  const [upload, setUpload] = useState<Upload | null>(null);

  const handle = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermission(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "취업률 실적을 처리하지 못했습니다.",
    );
  };

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiRequest<Search>(
        `/api/business/employment-rate-achievements?page=0&pageSize=${pageSize}`,
      );
      setRows(result.data?.achievements ?? []);
    } catch (caught) {
      handle(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const save = async () => {
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) {
      return;
    }
    try {
      const response = await apiRequest<Achievement>(
        "/api/business/employment-rate-achievements",
        {
          method: "POST",
          body: JSON.stringify(form),
        },
      );
      setSuccess(
        `${response.data?.managementNo ?? "취업률 실적"}이 저장되었습니다.`,
      );
      await load();
    } catch (caught) {
      handle(caught);
    }
  };

  const uploadFile = async (file: File) => {
    try {
      const formData = new FormData();
      formData.append("file", file);
      const result = await apiRequest<Upload>(
        "/api/business/employment-rate-achievements/excel-uploads",
        {
          method: "POST",
          body: formData,
        },
      );
      setUpload(result.data ?? null);
      setSuccess("Excel 검증 결과를 확인하세요.");
    } catch (caught) {
      handle(caught);
    }
  };

  const runBulk = async () => {
    if (!window.confirm("일괄 처리를 실행하시겠습니까?")) {
      return;
    }
    try {
      await apiRequest("/api/business/employment-rate-achievements/bulk-jobs", {
        method: "POST",
        body: JSON.stringify({
          evaluationYear: new Date().getFullYear().toString(),
          actionType: "GENERATE",
          targetCondition: {},
        }),
      });
    } catch (caught) {
      handle(caught);
    }
  };

  if (permission) {
    return (
      <section data-testid="employment-rate-achievements-page">
        <PermissionState
          title="취업률 실적 관리 권한이 없습니다"
          message="개별 조회는 R01, R02, R04, Excel과 일괄 처리는 R07 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
      data-testid="employment-rate-achievements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          개별 실적, Excel 일괄등록과 정책 승인 후 일괄 처리 결과를 확인합니다.
        </p>
      </header>
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="mb-4 flex justify-between">
          <h2 className="text-lg font-semibold text-dark">취업률 실적 목록</h2>
          <label>
            표시 건수
            <select
              data-testid="employment-rate-page-size"
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
              value={pageSize}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 실적이 없습니다"
            message="상세 입력 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {rows.map((row) => (
          <article
            className="border-t border-ld py-3"
            data-testid="employment-rate-achievement-row"
            key={row.achievementId}
          >
            <strong>{row.managementItemCode}</strong>
            <span className="ml-3">{row.achievementDate}</span>
            <span className="ml-3">{row.achievementName}</span>
            <span className="ml-3">{row.certificationStatus}</span>
          </article>
        ))}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">개별 실적 입력</h2>
        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <label>
            관리항목 *
            <input
              data-testid="employment-rate-management-item"
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </label>
          <label>
            업적발생일 *
            <input
              data-testid="employment-rate-date"
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </label>
          <label>
            실적명
            <input
              data-testid="employment-rate-name"
              onChange={(event) =>
                setForm({ ...form, achievementName: event.target.value })
              }
              value={form.achievementName}
            />
          </label>
        </div>
        <button
          className="mt-4 rounded-md bg-primary px-4 py-2 text-white"
          data-testid="employment-rate-save"
          onClick={() => void save()}
          type="button"
        >
          저장
        </button>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-upload-panel"
      >
        <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
        <p className="text-sm text-muted">
          파일 업로드 후 정상 행과 오류 행을 확인합니다. 오류가 있으면 0건
          반영됩니다.
        </p>
        <input
          accept=".csv"
          data-testid="employment-rate-upload"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) {
              void uploadFile(file);
            }
          }}
          type="file"
        />
        {upload ? (
          <div data-testid="employment-rate-upload-result">
            정상 {upload.successCount}건 / 오류 {upload.errorCount}건 / 반영{" "}
            {upload.persistedCount}건
            {upload.errors.map((item) => (
              <p key={`${item.rowNumber}-${item.errorReason}`}>
                {item.rowNumber}행: {item.errorReason}
              </p>
            ))}
          </div>
        ) : null}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-bulk-panel"
      >
        <h2 className="text-lg font-semibold text-dark">일괄 생성·삭제</h2>
        <p className="text-sm text-muted">
          대상 미리보기와 실행 조건은 정책 승인 후 실행할 수 있습니다.
        </p>
        <button
          className="mt-3 rounded-md border border-primary px-4 py-2 text-primary"
          data-testid="employment-rate-bulk-run"
          onClick={() => void runBulk()}
          type="button"
        >
          일괄 처리 실행
        </button>
      </section>
    </section>
  );
}
