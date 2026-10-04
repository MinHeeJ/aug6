import { FormEvent, useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type EmploymentRateImprovement = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
};

type SearchResponse = {
  achievements: EmploymentRateImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type FormValues = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
};

const emptyForm: FormValues = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
};

/** Search, detail, and R01 save workflow for employment-rate improvement achievements. */
export function EmploymentRateImprovementsPage() {
  const [rows, setRows] = useState<EmploymentRateImprovement[]>([]);
  const [form, setForm] = useState<FormValues>(emptyForm);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiRequest<SearchResponse>(
        `/api/business/employment-rate-improvements?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const selectRow = async (achievementId: number) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiRequest<EmploymentRateImprovement>(
        `/api/business/employment-rate-improvements/${achievementId}`,
      );
      const item = response.data;
      if (!item) return;
      setSelectedId(item.achievementId);
      setForm({
        managementItemCode: item.managementItemCode,
        achievementDate: item.achievementDate,
        specialLectureStartDate: item.specialLectureStartDate ?? "",
        specialLectureEndDate: item.specialLectureEndDate ?? "",
        mockExamQuestionPeriod: item.mockExamQuestionPeriod ?? "",
      });
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.managementItemCode.trim() || !form.achievementDate) {
      setError("관리항목과 업적발생일은 필수입니다.");
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const path = selectedId
        ? `/api/business/employment-rate-improvements/${selectedId}`
        : "/api/business/employment-rate-improvements";
      const response = await apiRequest<EmploymentRateImprovement>(
        path as `/api/${string}`,
        {
          method: selectedId ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode.trim(),
            achievementDate: form.achievementDate,
            specialLectureStartDate: form.specialLectureStartDate || undefined,
            specialLectureEndDate: form.specialLectureEndDate || undefined,
            mockExamQuestionPeriod:
              form.mockExamQuestionPeriod.trim() || undefined,
          }),
        },
      );
      const saved = response.data;
      if (saved) {
        setSelectedId(saved.achievementId);
        setForm({
          managementItemCode: saved.managementItemCode,
          achievementDate: saved.achievementDate,
          specialLectureStartDate: saved.specialLectureStartDate ?? "",
          specialLectureEndDate: saved.specialLectureEndDate ?? "",
          mockExamQuestionPeriod: saved.mockExamQuestionPeriod ?? "",
        });
      }
      setSuccess(
        selectedId
          ? "취업률 제고 실적을 수정했습니다."
          : "취업률 제고 실적을 저장했습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const startCreate = () => {
    setSelectedId(null);
    setForm(emptyForm);
    setError(null);
    setSuccess(null);
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-testid="employment-rate-improvements-page">
        <PermissionState
          title="취업률 제고 실적 권한이 없습니다"
          message="R01, R02 또는 R04 역할이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-rate-improvements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          특강 및 모의시험 출제 실적을 조회하고 저장합니다.
        </p>
      </header>
      {loading ? (
        <LoadingState
          title="실적 조회 중"
          message="취업률 제고 실적을 불러오고 있습니다."
        />
      ) : null}
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-improvements-list"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
          <div className="flex items-center gap-2">
            <label
              className="text-sm text-muted"
              htmlFor="employment-rate-page-size"
            >
              표시 건수
            </label>
            <select
              data-testid="employment-rate-page-size"
              id="employment-rate-page-size"
              onChange={(event) => setPageSize(Number(event.target.value))}
              value={pageSize}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
            <button
              className="rounded-md bg-primary px-3 py-2 text-sm font-semibold text-white"
              data-testid="employment-rate-create-button"
              onClick={startCreate}
              type="button"
            >
              신규 등록
            </button>
          </div>
        </div>
        {rows.length === 0 && !loading ? (
          <div className="mt-4">
            <EmptyState
              title="등록된 실적이 없습니다"
              message="신규 등록으로 실적을 입력하세요."
            />
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>관리항목</th>
                  <th>발생일</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    className="cursor-pointer border-t border-ld"
                    data-testid={`employment-rate-improvement-row-${row.achievementId}`}
                    key={row.achievementId}
                    onClick={() => void selectRow(row.achievementId)}
                  >
                    <td className="py-3">{row.managementNo}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementStatus}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <form
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-improvement-form"
        onSubmit={save}
      >
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            {selectedId ? "실적 상세·수정" : "실적 등록"}
          </h2>
          {selectedId ? (
            <span className="text-sm text-muted">ID {selectedId}</span>
          ) : null}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="text-sm text-dark">
            관리항목 *
            <input
              data-testid="employment-rate-management-item-input"
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </label>
          <label className="text-sm text-dark">
            업적발생일 *
            <input
              data-testid="employment-rate-achievement-date-input"
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </label>
          <label className="text-sm text-dark">
            특강 시작일
            <input
              data-testid="employment-rate-special-lecture-start-input"
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value,
                })
              }
              type="date"
              value={form.specialLectureStartDate}
            />
          </label>
          <label className="text-sm text-dark">
            특강 종료일
            <input
              data-testid="employment-rate-special-lecture-end-input"
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
              type="date"
              value={form.specialLectureEndDate}
            />
          </label>
          <label className="text-sm text-dark md:col-span-2">
            모의시험 출제기간
            <input
              data-testid="employment-rate-mock-exam-period-input"
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
              value={form.mockExamQuestionPeriod}
            />
          </label>
        </div>
        <button
          className="mt-5 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          data-testid="employment-rate-save-button"
          disabled={saving}
          type="submit"
        >
          {saving ? "저장 중" : "저장"}
        </button>
      </form>
    </section>
  );
}
