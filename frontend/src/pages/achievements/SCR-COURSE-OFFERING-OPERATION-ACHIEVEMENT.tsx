import { RefreshCw, Save } from "lucide-react";
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
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  certificationStatus: string;
};

type SearchResponse = {
  achievements: Achievement[];
};

type SaveResponse = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
};

const blankForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
};

/** Renders the approved course offering and operation list, detail, and save flow. */
export function CourseOfferingOperationAchievementPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(blankForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const locked = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiRequest<SearchResponse>(
        `/api/business/course-operations?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
      setDenied(false);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const select = async (row: Achievement) => {
    try {
      const response = await apiRequest<Achievement>(
        `/api/business/course-operations/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        performanceDetails: detail.performanceDetails,
      });
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")) {
      return;
    }
    try {
      setSaving(true);
      const path = selected
        ? `/api/business/course-operations/${selected.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(form),
      });
      if (response.data?.achievement) {
        await select(response.data.achievement);
      }
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강좌 개설·운영 실적을 처리하지 못했습니다.",
    );
  };

  if (denied) {
    return (
      <section data-testid="course-operation-page">
        <PermissionState
          title="강좌 개설·운영 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
      data-testid="course-operation-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강좌 개설·운영 실적 관리
            </h1>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
            data-testid="course-operation-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="강좌 개설·운영 실적 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-operation-list-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강좌 개설·운영 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="course-operation-page-size-select"
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
        {loading ? <LoadingState title="강좌 개설·운영 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 강좌 개설·운영 실적이 없습니다" />
        ) : null}
        <div className="mt-3 space-y-2">
          {rows.map((row) => (
            <button
              className="block w-full rounded border border-ld p-3 text-left"
              data-testid="course-operation-row"
              key={row.achievementId}
              onClick={() => void select(row)}
              type="button"
            >
              {row.managementNo} · {row.performanceDetails}
            </button>
          ))}
        </div>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-operation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          강좌 개설·운영 실적 상세
        </h2>
        {locked ? (
          <p className="mt-2 text-sm text-error">
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="text-sm font-semibold text-dark">
            관리항목 *
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              data-testid="course-operation-management-item-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            업적발생일 *
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              data-testid="course-operation-achievement-date-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            실적내역 *
            <textarea
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              data-testid="course-operation-performance-details-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, performanceDetails: event.target.value })
              }
              value={form.performanceDetails}
            />
          </label>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white disabled:opacity-50"
            data-testid="course-operation-save-button"
            disabled={locked || saving}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} />
            {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}
