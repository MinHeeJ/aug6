import { useEffect, useState } from "react";
import { apiRequest } from "../../api/apiClient";

type Row = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
};
type Results = {
  achievements: Row[];
  page: number;
  pageSize: number;
  totalElements: number;
};

/** 취업률 제고 실적의 API-backed search, detail selection, and create/update form. */
export function EmploymentRateImprovementsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState({
    managementItemCode: "",
    achievementDate: "",
    specialLectureStartDate: "",
    specialLectureEndDate: "",
    mockExamQuestionPeriod: "",
  });
  const [message, setMessage] = useState("");
  const load = async () => {
    try {
      const response = await apiRequest<Results>(
        "/api/business/employment-rate-improvements?page=0&pageSize=20",
      );
      setRows(response.data?.achievements ?? []);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "조회 중 오류가 발생했습니다.",
      );
    }
  };
  useEffect(() => {
    void load();
  }, []);
  const select = (row: Row) => {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      specialLectureStartDate: row.specialLectureStartDate ?? "",
      specialLectureEndDate: row.specialLectureEndDate ?? "",
      mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
    });
  };
  const save = async () => {
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    try {
      const path = selected
        ? `/api/business/employment-rate-improvements/${selected.achievementId}`
        : "/api/business/employment-rate-improvements";
      await apiRequest(path as `/api/${string}`, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(form),
      });
      setMessage("저장되었습니다.");
      await load();
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "저장 중 오류가 발생했습니다.",
      );
    }
  };
  const locked = selected?.achievementStatus === "EVALUATION_CONFIRMED";
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-rate-improvements-page"
    >
      <div>
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold">취업률 제고 실적 관리</h1>
      </div>
      {message ? (
        <p data-testid="employment-rate-improvements-message">{message}</p>
      ) : null}
      <section className="rounded-md p-5">
        <h2>취업률 제고 실적 목록</h2>
        {rows.length === 0 ? (
          <p>조회된 취업률 제고 실적이 없습니다.</p>
        ) : (
          <table>
            <tbody>
              {rows.map((row) => (
                <tr
                  data-testid="employment-rate-improvements-row"
                  key={row.achievementId}
                >
                  <td>{row.managementNo}</td>
                  <td>{row.achievementDate}</td>
                  <td>
                    <button
                      data-testid="employment-rate-improvements-detail-button"
                      onClick={() => select(row)}
                      type="button"
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
      <section
        className="rounded-md p-5"
        data-testid="employment-rate-improvements-form"
      >
        <h2>상세 입력</h2>
        {locked ? (
          <p data-testid="employment-rate-improvements-lock">
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <label>
          관리항목
          <input
            data-testid="employment-rate-improvements-management-item"
            value={form.managementItemCode}
            onChange={(event) =>
              setForm({ ...form, managementItemCode: event.target.value })
            }
          />
        </label>
        <label>
          업적발생일
          <input
            data-testid="employment-rate-improvements-date"
            type="date"
            value={form.achievementDate}
            onChange={(event) =>
              setForm({ ...form, achievementDate: event.target.value })
            }
          />
        </label>
        <label>
          특강 시작일
          <input
            type="date"
            value={form.specialLectureStartDate}
            onChange={(event) =>
              setForm({ ...form, specialLectureStartDate: event.target.value })
            }
          />
        </label>
        <label>
          특강 종료일
          <input
            type="date"
            value={form.specialLectureEndDate}
            onChange={(event) =>
              setForm({ ...form, specialLectureEndDate: event.target.value })
            }
          />
        </label>
        <label>
          모의시험 출제기간
          <input
            value={form.mockExamQuestionPeriod}
            onChange={(event) =>
              setForm({ ...form, mockExamQuestionPeriod: event.target.value })
            }
          />
        </label>
        <button
          data-testid="employment-rate-improvements-save-button"
          disabled={locked}
          onClick={() => void save()}
          type="button"
        >
          저장
        </button>
      </section>
    </section>
  );
}
