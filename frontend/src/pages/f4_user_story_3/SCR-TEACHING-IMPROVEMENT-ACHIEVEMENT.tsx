import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";

type Row = {
  achievementId: number;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  certificationStatus: string;
};

type ListResponse = {
  achievements: Row[];
  page: number;
  pageSize: number;
  totalElements: number;
};
type Form = Omit<Row, "achievementId" | "certificationStatus">;
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: 2025,
  semester: 1,
};

/** 강의개선 실적을 API 데이터로 조회하고 확정 상태를 잠근 채 저장하는 업무 화면이다. */
export function TeachingImprovementAchievementPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [denied, setDenied] = useState(false);
  const locked = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setDenied(false);
      const result = await apiRequest<ListResponse>(
        `/api/business/lecture-improvements?page=0&pageSize=${pageSize}` as `/api/${string}`,
      );
      setRows(result.data?.achievements ?? []);
    } catch (error) {
      setDenied(error instanceof ApiClientError && error.status === 403);
      setMessage(
        error instanceof Error
          ? error.message
          : "강의개선 실적을 조회하지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const choose = async (row: Row) => {
    try {
      const result = await apiRequest<Row>(
        `/api/business/lecture-improvements/${row.achievementId}` as `/api/${string}`,
      );
      const detail = result.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        achievementContent: detail.achievementContent,
        academicYear: detail.academicYear,
        semester: detail.semester,
      });
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "상세를 조회하지 못했습니다.",
      );
    }
  };

  const save = async () => {
    if (
      !form.managementItemCode.trim() ||
      !form.achievementDate ||
      !form.achievementContent.trim()
    ) {
      setMessage("관리항목, 업적발생일, 강의개선 내용은 필수입니다.");
      return;
    }
    if (form.academicYear < 2000 || ![1, 2].includes(form.semester)) {
      setMessage("학년도와 학기를 확인하세요.");
      return;
    }
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      await apiRequest(path as `/api/${string}`, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(form),
      });
      setMessage("저장되었습니다.");
      await load();
    } catch (error) {
      setDenied(error instanceof ApiClientError && error.status === 403);
      setMessage(
        error instanceof Error ? error.message : "저장하지 못했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };

  if (denied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-IMPROVEMENTS"
        data-testid="teaching-improvement-page"
      >
        강의개선 실적 관리 권한이 없습니다.
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-IMPROVEMENTS"
      data-testid="teaching-improvement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">강의개선 실적 관리</h1>
        <p className="mt-2 text-sm text-muted">
          학년도와 학기별 강의개선 실적을 관리합니다.
        </p>
      </header>
      {message ? (
        <p
          className="text-sm text-error"
          data-testid="teaching-improvement-message"
        >
          {message}
        </p>
      ) : null}
      <section className="rounded-md border border-ld bg-white p-5">
        <label>
          표시 건수
          <select
            data-testid="teaching-improvement-page-size"
            value={pageSize}
            onChange={(event) =>
              setPageSize(Number(event.target.value) as 20 | 50 | 100)
            }
          >
            <option value={20}>20건</option>
            <option value={50}>50건</option>
            <option value={100}>100건</option>
          </select>
        </label>
        {loading ? <p>강의개선 실적 조회 중</p> : null}
        {!loading && rows.length === 0 ? (
          <p>조회된 강의개선 실적이 없습니다.</p>
        ) : null}
        {rows.map((row) => (
          <button
            className="mt-2 block w-full border p-2 text-left"
            data-testid="teaching-improvement-row"
            key={row.achievementId}
            onClick={() => void choose(row)}
            type="button"
          >
            {row.managementItemCode} / {row.academicYear}년 {row.semester}학기 /{" "}
            {row.certificationStatus}
          </button>
        ))}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="teaching-improvement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">상세 입력</h2>
        {locked ? (
          <p data-testid="teaching-improvement-confirmed-lock">
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          <input
            data-testid="teaching-improvement-management-item"
            disabled={locked}
            placeholder="관리항목"
            value={form.managementItemCode}
            onChange={(event) =>
              setForm({ ...form, managementItemCode: event.target.value })
            }
          />
          <input
            data-testid="teaching-improvement-date"
            disabled={locked}
            type="date"
            value={form.achievementDate}
            onChange={(event) =>
              setForm({ ...form, achievementDate: event.target.value })
            }
          />
          <input
            data-testid="teaching-improvement-academic-year"
            disabled={locked}
            min="2000"
            type="number"
            value={form.academicYear}
            onChange={(event) =>
              setForm({ ...form, academicYear: Number(event.target.value) })
            }
          />
          <select
            data-testid="teaching-improvement-semester"
            disabled={locked}
            value={form.semester}
            onChange={(event) =>
              setForm({ ...form, semester: Number(event.target.value) })
            }
          >
            <option value={1}>1학기</option>
            <option value={2}>2학기</option>
          </select>
          <textarea
            className="md:col-span-2"
            data-testid="teaching-improvement-content"
            disabled={locked}
            placeholder="강의개선 내용"
            value={form.achievementContent}
            onChange={(event) =>
              setForm({ ...form, achievementContent: event.target.value })
            }
          />
        </div>
        <button
          className="mt-4 rounded-md bg-primary px-4 py-2 text-white disabled:opacity-60"
          data-testid="teaching-improvement-save"
          disabled={saving || locked}
          onClick={() => void save()}
          type="button"
        >
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}
