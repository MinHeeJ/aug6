import { RefreshCw, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import {
  educationAchievementApi,
  type EducationAchievement,
  type DegreeCompletionDetail,
} from "../../api/educationAchievementApi";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const emptyDetail: DegreeCompletionDetail = {
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
};
const statusLabel = (status: string) =>
  ({
    DRAFTING: "작성중",
    SUBMITTED: "제출",
    DEPARTMENT_CONFIRMED: "학과장확인",
    DEPARTMENT_REJECTED: "학과장미승인",
    CERTIFIED: "인증",
    CERTIFICATION_RETURNED: "인증반려",
    EVALUATION_CONFIRMED: "평가확정",
  })[status] ?? status;

/** Renders the degree-completion master/detail form and prevents edits to evaluation-confirmed rows. */
export function DegreeCompletionAchievementPage() {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurrenceDate, setOccurrenceDate] = useState("");
  const [detail, setDetail] = useState<DegreeCompletionDetail>(emptyDetail);
  const [selected, setSelected] = useState<EducationAchievement | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const locked = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await educationAchievementApi.list({
        achievementType: "DEGREE_COMPLETION",
      });
      setItems(response.data?.items ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);
  const select = (item: EducationAchievement) => {
    setSelected(item);
    setManagementItemCode(item.managementItemCode);
    setOccurrenceDate(item.occurrenceDate);
    setDetail(item.degreeCompletionDetails?.[0] ?? emptyDetail);
    setError(null);
    setSuccess(null);
  };
  const save = async () => {
    if (locked) {
      setError("평가확정된 실적은 수정할 수 없습니다.");
      return;
    }
    if (
      !managementItemCode.trim() ||
      !occurrenceDate ||
      !detail.studentName.trim() ||
      !detail.thesisTitle.trim() ||
      !detail.degreeAwardedDate
    ) {
      setError(
        "관리항목, 발생일, 학위구분, 학생명, 논문제목, 수여일을 입력하세요.",
      );
      return;
    }
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      await educationAchievementApi.save({
        achievementId: selected?.achievementId,
        achievementType: "DEGREE_COMPLETION",
        managementItemCode: managementItemCode.trim(),
        occurrenceDate,
        degreeCompletionDetails: [detail],
      });
      setSelected(null);
      setManagementItemCode("");
      setOccurrenceDate("");
      setDetail(emptyDetail);
      setSuccess("저장되었습니다. 목록을 재조회했습니다.");
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };
  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "석·박사 배출 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-DEGREE-COMPLETION-ACHIEVEMENT"
        data-testid="degree-completion-page"
      >
        <PermissionState
          title="석·박사 배출 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-DEGREE-COMPLETION-ACHIEVEMENT"
      data-testid="degree-completion-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              석·박사 배출 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              실적과 지도학생 세부내역을 함께 등록합니다.
            </p>
          </div>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => void load()}
            data-testid="degree-completion-refresh-button"
          >
            <RefreshCw size={16} /> 재조회
          </button>
        </div>
      </div>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="석·박사 배출 실적 처리 오류" message={error} />
      ) : null}
      {locked ? (
        <ErrorState
          title="수정 제한"
          message="평가확정된 실적은 수정할 수 없습니다."
        />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">
          석·박사 배출 상세 입력
        </h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <label className="text-sm font-semibold text-dark">
            관리항목<span className="ml-1 text-error">*</span>
            <input
              disabled={locked}
              className="form-input mt-2 w-full"
              data-testid="degree-completion-management-item-input"
              value={managementItemCode}
              onChange={(event) => setManagementItemCode(event.target.value)}
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            발생일<span className="ml-1 text-error">*</span>
            <input
              disabled={locked}
              type="date"
              className="form-input mt-2 w-full"
              data-testid="degree-completion-occurrence-date-input"
              value={occurrenceDate}
              onChange={(event) => setOccurrenceDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            학위구분<span className="ml-1 text-error">*</span>
            <select
              disabled={locked}
              className="form-input mt-2 w-full"
              data-testid="degree-completion-degree-type-select"
              value={detail.degreeType}
              onChange={(event) =>
                setDetail({
                  ...detail,
                  degreeType: event.target
                    .value as DegreeCompletionDetail["degreeType"],
                })
              }
            >
              <option value="MASTER">석사</option>
              <option value="DOCTOR">박사</option>
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            학생명<span className="ml-1 text-error">*</span>
            <input
              disabled={locked}
              className="form-input mt-2 w-full"
              data-testid="degree-completion-student-name-input"
              value={detail.studentName}
              onChange={(event) =>
                setDetail({ ...detail, studentName: event.target.value })
              }
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            논문제목<span className="ml-1 text-error">*</span>
            <input
              disabled={locked}
              className="form-input mt-2 w-full"
              data-testid="degree-completion-thesis-title-input"
              value={detail.thesisTitle}
              onChange={(event) =>
                setDetail({ ...detail, thesisTitle: event.target.value })
              }
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            수여일<span className="ml-1 text-error">*</span>
            <input
              disabled={locked}
              type="date"
              className="form-input mt-2 w-full"
              data-testid="degree-completion-awarded-date-input"
              value={detail.degreeAwardedDate}
              onChange={(event) =>
                setDetail({ ...detail, degreeAwardedDate: event.target.value })
              }
            />
          </label>
        </div>
        <button
          type="button"
          disabled={saving || locked}
          className="btn-primary mt-5"
          onClick={() => void save()}
          data-testid="degree-completion-save-button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">
          석·박사 배출 실적 목록
        </h2>
        {loading ? (
          <LoadingState
            title="석·박사 배출 실적 조회 중"
            message="목록을 불러오고 있습니다."
          />
        ) : items.length === 0 ? (
          <EmptyState
            title="조회된 석·박사 배출 실적이 없습니다"
            message="세부내역을 입력하여 새 실적을 등록하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리항목</th>
                  <th>발생일</th>
                  <th>인증상태</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.achievementId}
                    data-testid="degree-completion-row"
                    className="cursor-pointer"
                    onClick={() => select(item)}
                  >
                    <td>{item.managementItemCode}</td>
                    <td>{item.occurrenceDate}</td>
                    <td>{statusLabel(item.certificationStatus)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}
