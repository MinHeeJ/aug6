import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureAchievementApi,
  type LectureAchievement,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type Filters = {
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  occurredDateFrom: string;
  occurredDateTo: string;
  certificationStatus: string;
};

type Form = {
  managementItemCode: string;
  occurredDate: string;
  attachmentRef: string;
  achievementDetail: string;
};

const initialFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  occurredDateFrom: "",
  occurredDateTo: "",
  certificationStatus: "",
};
const initialForm: Form = {
  managementItemCode: "",
  occurredDate: "",
  attachmentRef: "",
  achievementDetail: "{}",
};

export function LectureAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [rows, setRows] = useState<LectureAchievement[]>([]);
  const [selected, setSelected] = useState<LectureAchievement | null>(null);
  const [form, setForm] = useState<Form>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<PageSize>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const finalized = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await lectureAchievementApi.list({
        ...filters,
        page,
        size,
      });
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      showError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (row: LectureAchievement) => {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      occurredDate: row.occurredDate,
      attachmentRef: row.attachmentRef ?? "",
      achievementDetail: row.achievementDetail || "{}",
    });
    setFieldErrors({});
    setSuccess(null);
  };

  const save = async (actionType?: "SUBMIT") => {
    if (finalized) {
      setError("평가확정된 실적은 수정하거나 삭제할 수 없습니다.");
      return;
    }
    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    const confirmationMessage =
      actionType === "SUBMIT"
        ? "강의실적을 제출하시겠습니까?"
        : "강의실적을 저장하시겠습니까?";
    if (!window.confirm(confirmationMessage)) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const achievementDetail = JSON.parse(form.achievementDetail) as Record<
        string,
        unknown
      >;
      const response = await lectureAchievementApi.save({
        achievementId: selected?.achievementId,
        managementItemCode: form.managementItemCode.trim(),
        occurredDate: form.occurredDate,
        achievementDetail,
        attachmentRef: form.attachmentRef.trim() || undefined,
        actionType,
      });
      const saved = response.data;
      setSuccess(
        saved?.occurredDateOutOfRangeWarning
          ? "저장되었습니다. 업적발생일이 평가대상 기간 밖입니다."
          : "저장되었습니다.",
      );
      if (saved) selectRow(saved);
      await load();
    } catch (caught) {
      if (caught instanceof SyntaxError) {
        setFieldErrors({
          achievementDetail: "상세 입력값은 올바른 JSON 형식이어야 합니다.",
        });
      } else {
        showError(caught);
      }
    } finally {
      setSaving(false);
    }
  };

  const showError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-testid="lecture-achievement-page">
        <PermissionState
          title="강의실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 역할과 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-ACHIEVEMENT-MGMT"
      data-testid="lecture-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 &gt; 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강의실적을 조회하고 상세 정보를 저장합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
              data-testid="lecture-achievement-excel-button"
              onClick={() => {
                downloadCsv("lecture-achievements.csv", rows, [
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
                ]);
                setSuccess("엑셀 다운로드 파일을 생성했습니다.");
              }}
              type="button"
            >
              <Download size={16} /> 엑셀 다운로드
            </button>
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
              data-testid="lecture-achievement-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        </div>
      </header>

      {success ? (
        <SuccessState
          title={success}
          message="목록을 최신 저장값으로 갱신했습니다."
        />
      ) : null}
      {error ? <ErrorState title="강의실적 처리 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-4 md:grid-cols-4">
          <Field
            label="관리번호"
            value={filters.managementNo}
            onChange={(value) =>
              setFilters({ ...filters, managementNo: value })
            }
            testId="lecture-achievement-management-no-input"
          />
          <Field
            label="성명"
            value={filters.teacherName}
            onChange={(value) => setFilters({ ...filters, teacherName: value })}
            testId="lecture-achievement-teacher-name-input"
          />
          <Field
            label="관리항목"
            value={filters.managementItemCode}
            onChange={(value) =>
              setFilters({ ...filters, managementItemCode: value })
            }
            testId="lecture-achievement-management-item-filter"
          />
          <Field
            label="인증상태"
            value={filters.certificationStatus}
            onChange={(value) =>
              setFilters({ ...filters, certificationStatus: value })
            }
            testId="lecture-achievement-status-filter"
          />
          <DateField
            label="발생일 시작"
            value={filters.occurredDateFrom}
            onChange={(value) =>
              setFilters({ ...filters, occurredDateFrom: value })
            }
            testId="lecture-achievement-date-from-input"
          />
          <DateField
            label="발생일 종료"
            value={filters.occurredDateTo}
            onChange={(value) =>
              setFilters({ ...filters, occurredDateTo: value })
            }
            testId="lecture-achievement-date-to-input"
          />
          <button
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="lecture-achievement-search-button"
            onClick={() => {
              setPage(0);
              void load();
            }}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-5">
        <section className="rounded-md border border-ld bg-white p-6 xl:col-span-3">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">목록</h2>
            <select
              data-testid="lecture-achievement-page-size-select"
              onChange={(event) => {
                setSize(Number(event.target.value) as PageSize);
                setPage(0);
              }}
              value={size}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </div>
          {loading ? (
            <LoadingState title="강의실적을 조회하고 있습니다" />
          ) : null}
          {!loading && rows.length === 0 ? (
            <EmptyState title="조회된 강의실적이 없습니다" />
          ) : null}
          {!loading && rows.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-ld text-muted">
                    <th>관리번호</th>
                    <th>성명</th>
                    <th>관리항목</th>
                    <th>업적발생일</th>
                    <th>인증상태</th>
                    <th>첨부</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      className="border-b border-ld"
                      data-testid={`lecture-achievement-row-${row.achievementId}`}
                      key={row.achievementId}
                    >
                      <td>{row.managementNo}</td>
                      <td>{row.teacherName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.occurredDate}</td>
                      <td>{row.certificationStatus}</td>
                      <td>{row.attachmentRef ? "있음" : "없음"}</td>
                      <td>
                        <button
                          className="text-primary"
                          data-testid={`lecture-achievement-detail-${row.achievementId}`}
                          onClick={() => selectRow(row)}
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
          className="rounded-md border border-ld bg-white p-6 xl:col-span-2"
          data-testid="lecture-achievement-detail-panel"
        >
          <h2 className="text-lg font-semibold text-dark">상세</h2>
          <p className="mt-1 text-sm text-muted">
            FR-018 관리항목별 동적 필드를 JSON으로 입력합니다.
          </p>
          <div className="mt-4 space-y-4">
            <Field
              error={fieldErrors.managementItemCode}
              label="관리항목 *"
              value={form.managementItemCode}
              onChange={(value) =>
                setForm({ ...form, managementItemCode: value })
              }
              testId="lecture-achievement-management-item-input"
            />
            <DateField
              error={fieldErrors.occurredDate}
              label="업적발생일 *"
              value={form.occurredDate}
              onChange={(value) => setForm({ ...form, occurredDate: value })}
              testId="lecture-achievement-occurred-date-input"
            />
            <Field
              label="첨부파일 참조"
              value={form.attachmentRef}
              onChange={(value) => setForm({ ...form, attachmentRef: value })}
              testId="lecture-achievement-attachment-ref-input"
            />
            <label
              className="block text-sm font-medium text-dark"
              htmlFor="lecture-achievement-detail-input"
            >
              동적 상세 입력
            </label>
            <textarea
              className="min-h-28 w-full rounded-md border border-ld p-2 text-sm"
              data-testid="lecture-achievement-detail-input"
              id="lecture-achievement-detail-input"
              onChange={(event) =>
                setForm({ ...form, achievementDetail: event.target.value })
              }
              value={form.achievementDetail}
            />
            {fieldErrors.achievementDetail ? (
              <p className="text-sm text-error">
                {fieldErrors.achievementDetail}
              </p>
            ) : null}
            <div className="flex flex-wrap gap-2">
              <button
                className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white disabled:opacity-60"
                data-testid="lecture-achievement-save-button"
                disabled={saving || finalized}
                onClick={() => void save()}
                type="button"
              >
                <Save size={16} />
                {saving ? "저장 중" : "저장"}
              </button>
              <button
                className="inline-flex h-10 items-center rounded-md border border-primary px-4 text-sm font-semibold text-primary disabled:opacity-60"
                data-testid="lecture-achievement-submit-button"
                disabled={saving || finalized}
                onClick={() => void save("SUBMIT")}
                type="button"
              >
                제출
              </button>
            </div>
          </div>
        </section>
      </section>
    </section>
  );
}

function Field({
  label,
  value,
  onChange,
  testId,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  error?: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <input
        className="mt-1 h-10 w-full rounded-md border border-ld px-3"
        data-testid={testId}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      />
      {error ? (
        <span className="mt-1 block text-sm text-error">{error}</span>
      ) : null}
    </label>
  );
}

function DateField({
  label,
  value,
  onChange,
  testId,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  error?: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <input
        className="mt-1 h-10 w-full rounded-md border border-ld px-3"
        data-testid={testId}
        onChange={(event) => onChange(event.target.value)}
        type="date"
        value={value}
      />
      {error ? (
        <span className="mt-1 block text-sm text-error">{error}</span>
      ) : null}
    </label>
  );
}

function validateForm(form: Form): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목은 필수입니다.";
  if (!form.occurredDate) errors.occurredDate = "업적발생일은 필수입니다.";
  return errors;
}
