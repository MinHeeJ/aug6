import { Download, Save, Upload } from "lucide-react";
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
  achievementName?: string | null;
  achievementStatus: string;
};

type SearchResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type ScreenStatus =
  | "loading"
  | "empty"
  | "ready"
  | "error"
  | "permission"
  | "success";

type ScreenState = { status: ScreenStatus; message?: string };

export function getEmploymentRateAchievementRouteContract() {
  return {
    route: "/faculty/education/employment-rate-achievements",
    screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
    operations: [
      "listEmploymentRateAchievements",
      "createEmploymentRateAchievement",
      "updateEmploymentRateAchievement",
      "downloadEmploymentRateAchievements",
      "uploadEmploymentRateAchievementsExcel",
      "createEmploymentRateBulkJob",
      "getEmploymentRateBulkJob",
    ],
  };
}

export function createEmploymentRateAchievementState(): ScreenState {
  return { status: "loading" };
}

export function reduceEmploymentRateAchievementState(
  state: ScreenState,
  action: { type: ScreenStatus; message?: string },
): ScreenState {
  if (action.type === "ready" || action.type === "empty")
    return { status: action.type };
  return { status: action.type, message: action.message ?? state.message };
}

export function EmploymentRateAchievementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [state, setState] = useState<ScreenState>(
    createEmploymentRateAchievementState(),
  );
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [achievementDate, setAchievementDate] = useState("");
  const [achievementName, setAchievementName] = useState("");
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [evaluationYear, setEvaluationYear] = useState("");
  const [jobResult, setJobResult] = useState<string | null>(null);

  const load = async () => {
    setState({ status: "loading" });
    try {
      const response = await apiRequest<SearchResponse>(
        "/api/business/employment-rate-achievements?page=0&pageSize=20",
      );
      const achievements = response.data?.achievements ?? [];
      setRows(achievements);
      setState({ status: achievements.length === 0 ? "empty" : "ready" });
    } catch (error) {
      setState(
        error instanceof ApiClientError && error.status === 403
          ? { status: "permission" }
          : {
              status: "error",
              message:
                error instanceof Error ? error.message : "조회에 실패했습니다.",
            },
      );
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const save = async () => {
    if (!managementItemCode.trim() || !achievementDate) {
      setState({
        status: "error",
        message: "관리항목과 업적발생일은 필수입니다.",
      });
      return;
    }
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    try {
      const path = selected
        ? `/api/business/employment-rate-achievements/${selected.achievementId}`
        : "/api/business/employment-rate-achievements";
      await apiRequest(path as `/api/${string}`, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: managementItemCode.trim(),
          achievementDate,
          achievementName: achievementName.trim() || undefined,
          attachmentIds: [],
        }),
      });
      setState({ status: "success", message: "저장되었습니다." });
      await load();
    } catch (error) {
      setState({
        status: "error",
        message:
          error instanceof Error ? error.message : "저장에 실패했습니다.",
      });
    }
  };

  const upload = async () => {
    if (!uploadFile) {
      setState({
        status: "error",
        message: "업로드할 Excel 파일을 선택하세요.",
      });
      return;
    }
    if (!window.confirm("파일을 검증하고 일괄등록 절차를 시작하시겠습니까?"))
      return;
    const formData = new FormData();
    formData.append("file", uploadFile);
    const response = await fetch(
      "/api/business/employment-rate-achievements/excel-uploads",
      {
        method: "POST",
        credentials: "include",
        body: formData,
      },
    );
    if (!response.ok) {
      setState({
        status: response.status === 403 ? "permission" : "error",
        message: "Excel 검증에 실패했습니다.",
      });
      return;
    }
    setState({
      status: "success",
      message:
        "검증 결과를 확인했습니다. 오류가 있으면 업무 데이터는 반영되지 않습니다.",
    });
  };

  const requestBatch = async () => {
    if (!evaluationYear.match(/^\d{4}$/)) {
      setState({
        status: "error",
        message: "평가연도는 YYYY 형식으로 입력하세요.",
      });
      return;
    }
    if (
      !window.confirm(
        "대상 미리보기와 조건을 확인한 뒤 일괄 작업을 요청하시겠습니까?",
      )
    )
      return;
    try {
      await apiRequest("/api/business/employment-rate-achievements/bulk-jobs", {
        method: "POST",
        body: JSON.stringify({
          evaluationYear,
          actionType: "GENERATE",
          targetCondition: {},
        }),
      });
      setJobResult("작업이 접수되었습니다.");
    } catch (error) {
      setJobResult(
        error instanceof Error
          ? error.message
          : "일괄 작업을 요청하지 못했습니다.",
      );
    }
  };

  if (state.status === "permission") {
    return (
      <section data-testid="employment-rate-achievements-page">
        <PermissionState
          title="취업률 실적 권한이 없습니다"
          message="조회는 R01/R02/R04, Excel 및 일괄 처리는 R07 권한이 필요합니다."
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
        <h1 className="text-xl font-semibold text-dark">취업률 실적 관리</h1>
        <p className="mt-2 text-sm text-muted">
          개별 입력, Excel 검증·등록, 일괄 처리 결과를 관리합니다.
        </p>
      </header>
      {state.status === "success" ? (
        <SuccessState title="처리 완료" message={state.message} />
      ) : null}
      {state.status === "error" ? (
        <ErrorState title="취업률 실적 오류" message={state.message} />
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievement-list-panel"
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
          <a
            className="text-sm text-link"
            data-testid="employment-rate-download-link"
            href="/api/business/employment-rate-achievements/download?page=0&pageSize=20"
          >
            <Download size={16} /> Excel 다운로드
          </a>
        </div>
        {state.status === "loading" ? (
          <LoadingState title="취업률 실적 조회 중" />
        ) : null}
        {state.status === "empty" ? (
          <EmptyState
            title="조회된 취업률 실적이 없습니다"
            message="상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {state.status === "ready" ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-sm">
              <tbody>
                {rows.map((row) => (
                  <tr
                    data-testid="employment-rate-achievement-row"
                    key={row.achievementId}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementName ?? "-"}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        data-testid="employment-rate-achievement-detail-button"
                        onClick={() => {
                          setSelected(row);
                          setManagementItemCode(row.managementItemCode);
                          setAchievementDate(row.achievementDate);
                          setAchievementName(row.achievementName ?? "");
                        }}
                        type="button"
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-achievement-form-panel"
      >
        <h2 className="text-lg font-semibold text-dark">개별 실적 입력</h2>
        <label>
          관리항목 *
          <input
            data-testid="employment-rate-management-item-input"
            value={managementItemCode}
            onChange={(event) => setManagementItemCode(event.target.value)}
          />
        </label>
        <label>
          업적발생일 *
          <input
            data-testid="employment-rate-achievement-date-input"
            type="date"
            value={achievementDate}
            onChange={(event) => setAchievementDate(event.target.value)}
          />
        </label>
        <label>
          실적명
          <input
            data-testid="employment-rate-achievement-name-input"
            value={achievementName}
            onChange={(event) => setAchievementName(event.target.value)}
          />
        </label>
        <button
          data-testid="employment-rate-save-button"
          onClick={() => void save()}
          type="button"
        >
          <Save size={16} /> 저장
        </button>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-excel-panel"
      >
        <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
        <input
          accept=".csv,.xls,.xlsx"
          data-testid="employment-rate-upload-input"
          onChange={(event) => setUploadFile(event.target.files?.[0] ?? null)}
          type="file"
        />
        <button
          data-testid="employment-rate-upload-button"
          onClick={() => void upload()}
          type="button"
        >
          <Upload size={16} /> 검증 및 등록
        </button>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-batch-panel"
      >
        <h2 className="text-lg font-semibold text-dark">일괄 생성·삭제</h2>
        <input
          data-testid="employment-rate-batch-year-input"
          placeholder="YYYY"
          value={evaluationYear}
          onChange={(event) => setEvaluationYear(event.target.value)}
        />
        <button
          data-testid="employment-rate-batch-request-button"
          onClick={() => void requestBatch()}
          type="button"
        >
          일괄 생성 요청
        </button>
        {jobResult ? (
          <p data-testid="employment-rate-batch-result">{jobResult}</p>
        ) : null}
      </section>
    </section>
  );
}
