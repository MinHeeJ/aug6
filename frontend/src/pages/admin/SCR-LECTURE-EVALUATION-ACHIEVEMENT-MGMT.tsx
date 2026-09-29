import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type ApiErrorField,
  type LectureEvaluationAchievement,
} from "../../api/apiClient";
import { downloadCsv } from "../../utils/exportCsv";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const pageSizes = [20, 50, 100] as const;

type FormState = {
  managementItemCode: string;
  occurredDate: string;
  achievementDetail: string;
};
const initialForm: FormState = {
  managementItemCode: "",
  occurredDate: "",
  achievementDetail: "",
};

/** Search, review, and persistence UI for the authorized lecture-evaluation achievement workflow. */
export function LectureEvaluationAchievementManagementPage() {
  const [evaluationYear, setEvaluationYear] = useState("");
  const [managementNo, setManagementNo] = useState("");
  const [teacherName, setTeacherName] = useState("");
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurredDateFrom, setOccurredDateFrom] = useState("");
  const [occurredDateTo, setOccurredDateTo] = useState("");
  const [certificationStatus, setCertificationStatus] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<(typeof pageSizes)[number]>(20);
  const [rows, setRows] = useState<LectureEvaluationAchievement[]>([]);
  const [selected, setSelected] = useState<LectureEvaluationAchievement | null>(
    null,
  );
  const [form, setForm] = useState<FormState>(initialForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await lectureEvaluationAchievementApi.list({
        evaluationYear,
        managementNo,
        teacherName,
        managementItemCode,
        occurredDateFrom,
        occurredDateTo,
        certificationStatus,
        page,
        pageSize,
      });
      setRows(response.data?.lectureEvaluationAchievements ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const save = async () => {
    const nextErrors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      nextErrors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.occurredDate)
      nextErrors.occurredDate = "업적발생일을 입력하세요.";
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      setError("필수 입력값을 확인하세요.");
      return;
    }
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const detail = form.achievementDetail.trim()
        ? JSON.parse(form.achievementDetail)
        : {};
      const response = await lectureEvaluationAchievementApi.save({
        managementItemCode: form.managementItemCode.trim(),
        occurredDate: form.occurredDate,
        achievementDetail: detail,
      });
      setSuccess(response.data?.warnings?.[0] ?? "저장되었습니다.");
      setForm(initialForm);
      await load();
    } catch (caught) {
      if (caught instanceof SyntaxError)
        setError("상세 입력값은 JSON 형식으로 입력하세요.");
      else handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrors(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의평가 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
        data-testid="lecture-evaluation-achievement-page"
      >
        <PermissionState
          title="강의평가 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한이 필요합니다."
        />
      </section>
    );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-achievement-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">
          업적 입력 관리 / 교육영역 / 강의평가 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의평가 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          강의평가 실적을 검색하고 저장합니다. 평가확정 데이터는 수정하거나
          삭제할 수 없습니다.
        </p>
      </div>
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의평가 실적 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-6">
          <Filter
            label="평가연도"
            value={evaluationYear}
            onChange={setEvaluationYear}
            testId="lecture-evaluation-year-filter"
          />
          <Filter
            label="관리번호"
            value={managementNo}
            onChange={setManagementNo}
            testId="lecture-evaluation-management-no-filter"
          />
          <Filter
            label="성명"
            value={teacherName}
            onChange={setTeacherName}
            testId="lecture-evaluation-teacher-filter"
          />
          <Filter
            label="관리항목"
            value={managementItemCode}
            onChange={setManagementItemCode}
            testId="lecture-evaluation-item-filter"
          />
          <DateFilter
            label="업적발생일(시작)"
            value={occurredDateFrom}
            onChange={setOccurredDateFrom}
            testId="lecture-evaluation-occurred-date-from-filter"
          />
          <DateFilter
            label="업적발생일(종료)"
            value={occurredDateTo}
            onChange={setOccurredDateTo}
            testId="lecture-evaluation-occurred-date-to-filter"
          />
          <label className="text-sm font-semibold text-dark">
            인증상태
            <select
              className="mt-2 w-full border border-ld px-3 py-2 text-sm"
              value={certificationStatus}
              onChange={(event) => setCertificationStatus(event.target.value)}
              data-testid="lecture-evaluation-status-filter"
            >
              <option value="">전체</option>
              <option value="DRAFTING">작성중</option>
              <option value="SUBMITTED">제출</option>
              <option value="EVALUATION_CONFIRMED">평가확정</option>
            </select>
          </label>
          <button
            type="button"
            className="btn-secondary mt-7"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="lecture-evaluation-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강의평가 실적 목록
          </h2>
          <div className="flex gap-2">
            <button
              type="button"
              className="btn-secondary"
              onClick={() =>
                downloadCsv("lecture-evaluation-achievements.csv", rows, [
                  { header: "관리번호", value: (row) => row.managementNo },
                  { header: "성명", value: (row) => row.teacherName },
                  {
                    header: "관리항목",
                    value: (row) => row.managementItemCode,
                  },
                  { header: "업적발생일", value: (row) => row.occurredDate },
                  {
                    header: "인증상태",
                    value: (row) => row.certificationStatus,
                  },
                ])
              }
              data-testid="lecture-evaluation-excel-button"
            >
              <Download size={16} />
              Excel 다운로드
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => void load()}
              data-testid="lecture-evaluation-refresh-button"
            >
              <RefreshCw size={16} />
              새로고침
            </button>
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 border border-ld px-2 py-1"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(
                    Number(event.target.value) as (typeof pageSizes)[number],
                  );
                  setPage(0);
                }}
                data-testid="lecture-evaluation-page-size-select"
              >
                {pageSizes.map((size) => (
                  <option key={size} value={size}>
                    {size}건
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
        {loading ? <LoadingState title="강의평가 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강의평가 실적이 없습니다"
            message="조회조건을 변경하거나 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
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
                    onClick={() => {
                      setSelected(row);
                      setForm({
                        managementItemCode: row.managementItemCode,
                        occurredDate: row.occurredDate,
                        achievementDetail: "",
                      });
                    }}
                    data-testid="lecture-evaluation-achievement-row"
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.occurredDate}</td>
                    <td>{statusLabel(row.certificationStatus)}</td>
                    <td>{row.hasAttachment ? "있음" : "없음"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">
          {selected ? "강의평가 실적 상세" : "강의평가 실적 등록"}
        </h2>
        <p className="mt-2 text-sm text-muted">
          필수 항목을 입력한 뒤 저장하면 목록을 다시 조회합니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Filter
            label="관리항목"
            value={form.managementItemCode}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
            testId="lecture-evaluation-management-item-input"
            required
            error={fieldErrors.managementItemCode}
          />
          <label className="text-sm font-semibold text-dark">
            업적발생일<span className="ms-1 text-error">*</span>
            <input
              type="date"
              className="mt-2 w-full border border-ld px-3 py-2 text-sm"
              value={form.occurredDate}
              onChange={(event) =>
                setForm({ ...form, occurredDate: event.target.value })
              }
              data-testid="lecture-evaluation-occurred-date-input"
            />
            {fieldErrors.occurredDate ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.occurredDate}
              </span>
            ) : null}
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-3">
            동적 상세 입력값 (JSON)
            <textarea
              className="mt-2 min-h-24 w-full border border-ld px-3 py-2 text-sm"
              value={form.achievementDetail}
              onChange={(event) =>
                setForm({ ...form, achievementDetail: event.target.value })
              }
              data-testid="lecture-evaluation-detail-textarea"
            />
          </label>
        </div>
        <button
          type="button"
          className="btn-primary mt-5"
          onClick={() => void save()}
          disabled={saving}
          data-testid="lecture-evaluation-save-button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}

function Filter({
  label,
  value,
  onChange,
  testId,
  required,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  required?: boolean;
  error?: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <input
        className="mt-2 w-full border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
function DateFilter({
  label,
  value,
  onChange,
  testId,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <input
        type="date"
        className="mt-2 w-full border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
    </label>
  );
}
function toFieldErrors(fields: ApiErrorField[]) {
  return Object.fromEntries(
    fields.map((field) => [field.field, field.message]),
  );
}
function statusLabel(status: string) {
  return (
    (
      {
        DRAFTING: "작성중",
        SUBMITTED: "제출",
        DEPARTMENT_CONFIRMED: "학과장확인",
        DEPARTMENT_REJECTED: "학과장미승인",
        CERTIFIED: "인증",
        CERTIFICATION_RETURNED: "인증반려",
        EVALUATION_CONFIRMED: "평가확정",
      } as Record<string, string>
    )[status] ?? status
  );
}
