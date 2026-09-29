import { Download, Save, Search } from "lucide-react";
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
  SuccessState,
} from "../../components/States";

type FormState = {
  achievementId?: number;
  managementItemCode: string;
  organizationCode: string;
  occurredDate: string;
  achievementDetail: string;
  attachmentRef: string;
  certificationStatus: string;
  changeReason: string;
};
const blankForm: FormState = {
  managementItemCode: "",
  organizationCode: "",
  occurredDate: "",
  achievementDetail: "",
  attachmentRef: "",
  certificationStatus: "DRAFTING",
  changeReason: "",
};

/** 강의평가 실적의 검색·목록·상세 저장을 제공하는 교육영역 업무 화면이다. */
export function LectureEvaluationAchievementManagementPage() {
  const [managementItemCode, setManagementItemCode] = useState("");
  const [certificationStatus, setCertificationStatus] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [rows, setRows] = useState<LectureEvaluationAchievement[]>([]);
  const [form, setForm] = useState<FormState>(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const confirmed = form.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await lectureEvaluationAchievementApi.list({
        managementItemCode: managementItemCode || undefined,
        certificationStatus: certificationStatus || undefined,
        pageSize,
      });
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
  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403)
      setPermissionDenied(true);
    setError(
      caught instanceof Error
        ? caught.message
        : "강의평가 실적을 처리하지 못했습니다.",
    );
  };
  const select = (row: LectureEvaluationAchievement) =>
    setForm({
      achievementId: row.achievementId,
      managementItemCode: row.managementItemCode,
      organizationCode: row.organizationCode,
      occurredDate: row.occurredDate,
      achievementDetail: row.achievementDetail,
      attachmentRef: row.attachmentRef ?? "",
      certificationStatus: row.certificationStatus,
      changeReason: "",
    });
  const save = async () => {
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      await lectureEvaluationAchievementApi.save({
        ...form,
        attachmentRef: form.attachmentRef || undefined,
      });
      setSuccess("저장 후 목록을 다시 조회했습니다.");
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };
  const download = () => {
    const csv = [
      "관리번호,성명,관리항목,업적발생일,인증상태,첨부여부",
      ...rows.map((row) =>
        [
          row.managementNo,
          row.teacherName,
          row.managementItemCode,
          row.occurredDate,
          row.certificationStatus,
          row.attachmentRef ? "Y" : "N",
        ].join(","),
      ),
    ].join("\n");
    const url = URL.createObjectURL(
      new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = "lecture-evaluation-achievements.csv";
    link.click();
    URL.revokeObjectURL(url);
  };
  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
        data-testid="lecture-evaluation-page"
      >
        <PermissionState
          title="강의평가 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">
          업적 입력 관리 / 교육영역 / 강의평가 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold">강의평가 실적 관리</h1>
      </div>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? <ErrorState title="강의평가 실적 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-4 md:grid-cols-4">
          <label>
            관리항목
            <input
              className="form-input mt-2 w-full"
              value={managementItemCode}
              onChange={(e) => setManagementItemCode(e.target.value)}
              data-testid="lecture-evaluation-management-item-filter"
            />
          </label>
          <label>
            인증상태
            <select
              className="form-input mt-2 w-full"
              value={certificationStatus}
              onChange={(e) => setCertificationStatus(e.target.value)}
              data-testid="lecture-evaluation-status-filter"
            >
              <option value="">전체</option>
              <option value="DRAFTING">작성중</option>
              <option value="SUBMITTED">제출</option>
              <option value="EVALUATION_CONFIRMED">평가확정</option>
            </select>
          </label>
          <button
            className="btn-primary mt-6"
            type="button"
            onClick={() => void load()}
            data-testid="lecture-evaluation-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="mb-4 flex justify-between">
          <h2 className="text-lg font-semibold">강의평가 실적 목록</h2>
          <div>
            <select
              value={pageSize}
              onChange={(e) =>
                setPageSize(Number(e.target.value) as 20 | 50 | 100)
              }
              data-testid="lecture-evaluation-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
            <button
              className="btn-secondary ml-2"
              type="button"
              onClick={download}
              data-testid="lecture-evaluation-excel-button"
            >
              <Download size={16} />
              Excel 다운로드
            </button>
          </div>
        </div>
        {loading ? (
          <LoadingState title="강의평가 실적 조회 중" />
        ) : rows.length === 0 ? (
          <EmptyState title="조회된 강의평가 실적이 없습니다" />
        ) : (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>인증상태</th>
                  <th>첨부여부</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    onClick={() => select(row)}
                    data-testid="lecture-evaluation-row"
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.occurredDate}</td>
                    <td>{row.certificationStatus}</td>
                    <td>{row.attachmentRef ? "첨부" : "없음"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold">상세</h2>
        {confirmed ? (
          <p
            className="mt-3 text-sm text-lighterror"
            role="status"
            data-testid="lecture-evaluation-confirmed-lock-message"
          >
            평가확정 실적은 수정·삭제하거나 첨부파일을 삭제할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목 *"
            value={form.managementItemCode}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
          />
          <Field
            label="소속 조직 *"
            value={form.organizationCode}
            onChange={(value) => setForm({ ...form, organizationCode: value })}
          />
          <label>
            업적발생일 *
            <input
              type="date"
              className="form-input mt-2 w-full"
              value={form.occurredDate}
              onChange={(e) =>
                setForm({ ...form, occurredDate: e.target.value })
              }
              data-testid="lecture-evaluation-occurred-date-input"
            />
          </label>
          <label>
            첨부파일 식별자
            <input
              className="form-input mt-2 w-full"
              value={form.attachmentRef}
              onChange={(e) =>
                setForm({ ...form, attachmentRef: e.target.value })
              }
              data-testid="lecture-evaluation-attachment-input"
            />
          </label>
          <label className="md:col-span-2">
            실적 내용 *
            <textarea
              className="form-input mt-2 w-full"
              value={form.achievementDetail}
              onChange={(e) =>
                setForm({ ...form, achievementDetail: e.target.value })
              }
              data-testid="lecture-evaluation-detail-input"
            />
          </label>
        </div>
        <button
          className="btn-primary mt-4"
          disabled={saving || confirmed}
          type="button"
          onClick={() => void save()}
          data-testid="lecture-evaluation-save-button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}
function Field({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label>
      {label}
      <input
        className="form-input mt-2 w-full"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        data-testid={`lecture-evaluation-${label.replace(/[^a-zA-Z]/g, "").toLowerCase()}-input`}
      />
    </label>
  );
}
