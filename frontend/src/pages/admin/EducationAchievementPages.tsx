import { ChangeEvent, useState } from "react";
import {
  ApiClientError,
  educationAchievementApi,
  type EducationAchievement,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  SuccessState,
} from "../../components/States";

type ListKind = "lecture" | "graduate";

/** Reusable faculty-facing list/form screen for the BASIC-72 education achievement routes. */
export function EducationAchievementPage({ kind }: { kind: ListKind }) {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [status, setStatus] = useState<
    "idle" | "loading" | "success" | "empty" | "error"
  >("idle");
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    managementItemCode: "",
    occurredOn: "",
    detailContent: "",
  });
  const lecture = kind === "lecture";
  const title = lecture ? "강의평가 실적 관리" : "석·박사 배출 실적 관리";

  const load = async () => {
    setStatus("loading");
    setError(null);
    try {
      const response = lecture
        ? await educationAchievementApi.listLectureEvaluations()
        : await educationAchievementApi.listGraduateAchievements();
      setItems(response.data?.items ?? []);
      setStatus((response.data?.items.length ?? 0) ? "success" : "empty");
    } catch (caught) {
      setStatus("error");
      setError(message(caught));
    }
  };
  const save = async () => {
    if (!form.managementItemCode || !form.occurredOn || !form.detailContent) {
      setError("필수 입력 항목을 확인하세요.");
      setStatus("error");
      return;
    }
    try {
      await educationAchievementApi.saveLectureEvaluation(form);
      setForm({ managementItemCode: "", occurredOn: "", detailContent: "" });
      await load();
    } catch (caught) {
      setError(message(caught));
      setStatus("error");
    }
  };
  return (
    <section
      className="space-y-6"
      data-testid={`${kind}-achievement-page`}
      data-screen-id={
        lecture
          ? "SCR-LECTURE-EVALUATION-ACHIEVEMENT"
          : "SCR-GRADUATE-ACHIEVEMENT"
      }
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">교수업적 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
      </div>
      {status === "error" && error ? (
        <div role="alert">
          <ErrorState title="처리 오류" message={error} />
        </div>
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5 shadow-md"
        data-testid={`${kind}-search-panel`}
      >
        <button
          type="button"
          data-testid={`${kind}-search-button`}
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          onClick={() => void load()}
        >
          조회
        </button>
      </section>
      {lecture ? (
        <section
          className="rounded-md border border-ld bg-white p-5 shadow-md"
          data-testid="lecture-achievement-form"
        >
          <h2 className="font-semibold text-dark">강의평가 등록</h2>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <input
              aria-label="관리항목"
              data-testid="lecture-management-item-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              placeholder="관리항목 코드"
            />
            <input
              aria-label="업적발생일"
              data-testid="lecture-occurred-on-input"
              type="date"
              value={form.occurredOn}
              onChange={(event) =>
                setForm({ ...form, occurredOn: event.target.value })
              }
            />
            <input
              aria-label="실적내역"
              data-testid="lecture-detail-content-input"
              value={form.detailContent}
              onChange={(event) =>
                setForm({ ...form, detailContent: event.target.value })
              }
              placeholder="실적내역"
            />
          </div>
          <button
            type="button"
            data-testid="lecture-save-button"
            className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void save()}
          >
            저장
          </button>
        </section>
      ) : null}
      {status === "loading" ? (
        <LoadingState title="조회 중" message="실적을 조회하고 있습니다." />
      ) : null}
      {status === "empty" || status === "idle" ? (
        <EmptyState
          title={
            status === "empty"
              ? "조회 결과가 없습니다"
              : "조회 버튼을 눌러 실적을 확인하세요"
          }
        />
      ) : null}
      {status === "success" ? (
        <section
          className="overflow-x-auto rounded-md border border-ld bg-white shadow-md"
          data-testid={`${kind}-achievement-list`}
        >
          <table className="min-w-full text-sm">
            <thead>
              <tr>
                <th>관리항목</th>
                <th>발생일</th>
                <th>실적내역</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr
                  key={item.achievementId}
                  data-testid={`${kind}-achievement-row`}
                >
                  <td>{item.managementItemCode}</td>
                  <td>{item.occurredOn}</td>
                  <td>{item.detailContent}</td>
                  <td>{item.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}
    </section>
  );
}

/** Excel validation UI intentionally disables apply when the server reports an error row. */
export function StudentGuidanceExcelPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<{
    totalCount: number;
    successCount: number;
    failureCount: number;
    errorFileRef?: string | null;
  } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const upload = async () => {
    if (!file) {
      setError("업로드할 Excel 파일을 선택하세요.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response =
        await educationAchievementApi.uploadStudentGuidance(file);
      setResult(response.data ?? null);
    } catch (caught) {
      setError(message(caught));
    } finally {
      setLoading(false);
    }
  };
  return (
    <section
      className="space-y-6"
      data-testid="student-guidance-excel-page"
      data-screen-id="SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">교수업적 / 교육영역 / 학생지도</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 Excel 일괄등록
        </h1>
      </div>
      {error ? (
        <div role="alert">
          <ErrorState title="업로드 오류" message={error} />
        </div>
      ) : null}
      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <input
          data-testid="student-guidance-file-input"
          type="file"
          accept=".xlsx,.xls"
          onChange={(event: ChangeEvent<HTMLInputElement>) =>
            setFile(event.target.files?.[0] ?? null)
          }
        />
        <button
          data-testid="student-guidance-upload-button"
          type="button"
          disabled={loading}
          className="ml-3 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          onClick={() => void upload()}
        >
          {loading ? "검증 중" : "업로드 및 검증"}
        </button>
      </section>
      {result ? (
        <section
          className="rounded-md border border-ld bg-white p-5 shadow-md"
          data-testid="student-guidance-upload-result"
        >
          <SuccessState
            title="검증 완료"
            message={`전체 ${result.totalCount}건 / 정상 ${result.successCount}건 / 오류 ${result.failureCount}건`}
          />
          {result.errorFileRef ? (
            <p className="mt-3 text-sm text-error">
              오류 행이 있어 전체 반영할 수 없습니다.
            </p>
          ) : null}
          <button
            type="button"
            data-testid="student-guidance-apply-button"
            disabled={result.failureCount > 0}
            className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            반영
          </button>
        </section>
      ) : null}
    </section>
  );
}
function message(caught: unknown) {
  return caught instanceof ApiClientError || caught instanceof Error
    ? caught.message
    : "요청 처리에 실패했습니다.";
}
