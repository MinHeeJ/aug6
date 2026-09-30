import { Download } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type LectureEvaluationAchievement,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

const initialForm = {
  evaluationYear: "",
  organizationCode: "",
  managementItemCode: "",
  occurredDate: "",
  changeReason: "",
};

/** Evaluation-confirmed achievements are immutable on both the UI and server mutation boundaries. */
export function canEditLectureEvaluationAchievement(
  achievement: LectureEvaluationAchievement | null,
) {
  return achievement?.certificationStatus !== "EVALUATION_CONFIRMED";
}

/** 강의평가 실적의 검색·목록·상세 저장을 제공하는 BASIC-79 화면이다. */
export function LectureEvaluationAchievementManagementPage() {
  const [rows, setRows] = useState<LectureEvaluationAchievement[]>([]);
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [form, setForm] = useState(initialForm);
  const [selected, setSelected] = useState<LectureEvaluationAchievement | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [permission, setPermission] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermission(false);
      const result = await lectureEvaluationAchievementApi.list({
        size,
        evaluationYear: form.evaluationYear,
        organizationCode: form.organizationCode,
        managementItemCode: form.managementItemCode,
      });
      setRows(result.data?.achievements ?? []);
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403)
        setPermission(true);
      else
        setError(
          "강의평가 실적을 불러오지 못했습니다. 검색 조건을 확인한 뒤 다시 시도하세요.",
        );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [size]);
  const save = async () => {
    if (!canEditLectureEvaluationAchievement(selected)) {
      setError("평가확정된 실적은 수정할 수 없습니다.");
      return;
    }
    if (
      !form.evaluationYear ||
      !form.organizationCode ||
      !form.managementItemCode ||
      !form.occurredDate ||
      !form.changeReason
    ) {
      setError("필수 입력 항목을 모두 입력하세요.");
      return;
    }
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      const response = await lectureEvaluationAchievementApi.save({
        ...form,
        achievementId: selected?.achievementId,
        achievementDetail: {},
      });
      setMessage(
        response.data?.warnings?.join(" ") ||
          "저장 후 목록을 새로고침했습니다.",
      );
      await load();
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "저장에 실패했습니다.",
      );
    }
  };
  if (permission)
    return (
      <section data-testid="lecture-evaluation-achievement-page">
        <PermissionState
          title="강의평가 실적 접근 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">강의평가 실적 관리</h1>
        <p className="mt-2 text-sm text-muted">
          교육영역 / 강의평가 실적을 검색하고 등록·수정합니다.
        </p>
      </header>
      {error ? <ErrorState title="강의평가 실적 오류" message={error} /> : null}
      {message ? (
        <p
          className="rounded-md bg-lightsuccess p-4 text-sm text-success"
          role="status"
        >
          {message}
        </p>
      ) : null}
      <section className="grid gap-3 rounded-md border border-ld bg-white p-5 md:grid-cols-5">
        <label>
          평가연도
          <input
            data-testid="lecture-evaluation-year-input"
            className="mt-1 w-full rounded border border-ld p-2"
            value={form.evaluationYear}
            onChange={(event) =>
              setForm({ ...form, evaluationYear: event.target.value })
            }
          />
        </label>
        <label>
          소속 조직
          <input
            data-testid="lecture-evaluation-organization-input"
            className="mt-1 w-full rounded border border-ld p-2"
            value={form.organizationCode}
            onChange={(event) =>
              setForm({ ...form, organizationCode: event.target.value })
            }
          />
        </label>
        <label>
          관리항목
          <input
            data-testid="lecture-evaluation-item-input"
            className="mt-1 w-full rounded border border-ld p-2"
            value={form.managementItemCode}
            onChange={(event) =>
              setForm({ ...form, managementItemCode: event.target.value })
            }
          />
        </label>
        <label>
          업적발생일
          <input
            data-testid="lecture-evaluation-date-input"
            type="date"
            className="mt-1 w-full rounded border border-ld p-2"
            value={form.occurredDate}
            onChange={(event) =>
              setForm({ ...form, occurredDate: event.target.value })
            }
          />
        </label>
        <button
          data-testid="lecture-evaluation-search-button"
          className="self-end rounded-md bg-primary p-2 text-white"
          onClick={() => void load()}
          type="button"
        >
          조회
        </button>
      </section>
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="mb-4 flex flex-wrap justify-between gap-2">
          <h2 className="font-semibold">강의평가 실적 목록</h2>
          <div className="flex gap-2">
            <select
              data-testid="lecture-evaluation-page-size-select"
              value={size}
              onChange={(event) =>
                setSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
            <button
              data-testid="lecture-evaluation-excel-button"
              type="button"
              disabled={!rows.length}
              onClick={() =>
                downloadCsv("lecture-evaluation-achievements.csv", rows)
              }
            >
              {" "}
              <Download size={15} className="inline" /> Excel
            </button>
          </div>
        </div>
        {loading ? (
          <LoadingState
            title="강의평가 실적 조회 중"
            message="목록을 불러오고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회된 강의평가 실적이 없습니다"
            message="검색 조건을 변경하거나 새 실적을 등록하세요."
          />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr>
                <th>평가연도</th>
                <th>관리항목</th>
                <th>발생일</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  data-testid={`lecture-evaluation-row-${row.achievementId}`}
                  key={row.achievementId}
                  onClick={() => {
                    setSelected(row);
                    setForm({
                      evaluationYear: row.evaluationYear,
                      organizationCode: row.organizationCode,
                      managementItemCode: row.managementItemCode,
                      occurredDate: row.occurredDate,
                      changeReason: "강의평가 실적 수정",
                    });
                  }}
                >
                  <td>{row.evaluationYear}</td>
                  <td>{row.managementItemCode}</td>
                  <td>{row.occurredDate}</td>
                  <td>{row.certificationStatus}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
      <section className="rounded-md border border-ld bg-white p-5">
        <h2 className="font-semibold">상세 입력</h2>
        {selected?.certificationStatus === "EVALUATION_CONFIRMED" ? (
          <p className="mt-2 text-sm text-danger" role="status">
            평가확정된 실적은 수정하거나 첨부를 변경할 수 없습니다.
          </p>
        ) : null}
        <label className="mt-3 block">
          변경 사유
          <input
            data-testid="lecture-evaluation-change-reason-input"
            className="mt-1 w-full rounded border border-ld p-2"
            value={form.changeReason}
            onChange={(event) =>
              setForm({ ...form, changeReason: event.target.value })
            }
            disabled={!canEditLectureEvaluationAchievement(selected)}
          />
        </label>
        <button
          data-testid="lecture-evaluation-save-button"
          className="mt-4 rounded-md bg-primary px-4 py-2 text-white"
          onClick={() => void save()}
          type="button"
          disabled={!canEditLectureEvaluationAchievement(selected)}
        >
          {selected ? "수정 저장" : "저장"}
        </button>
      </section>
    </section>
  );
}
