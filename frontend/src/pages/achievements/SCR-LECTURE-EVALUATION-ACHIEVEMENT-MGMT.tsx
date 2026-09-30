import { Download, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type ApiErrorField,
  type LectureEvaluationAchievement,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

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
  achievementDetail: string;
  attachmentRef: string;
};

const emptyFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  occurredDateFrom: "",
  occurredDateTo: "",
  certificationStatus: "",
};
const emptyForm: Form = {
  managementItemCode: "",
  occurredDate: "",
  achievementDetail: "{}",
  attachmentRef: "",
};

/** Provides the searchable list and detail-save workflow for lecture-evaluation achievements. */
export function LectureEvaluationAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [rows, setRows] = useState<LectureEvaluationAchievement[]>([]);
  const [selected, setSelected] = useState<LectureEvaluationAchievement | null>(
    null,
  );
  const [form, setForm] = useState<Form>(emptyForm);
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [page, setPage] = useState(0);
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
        ...filters,
        page,
        size,
      });
      setRows(response.data?.achievements ?? []);
      setSelected(null);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (row: LectureEvaluationAchievement) => {
    setSelected(row);
    setFieldErrors({});
    setForm({
      managementItemCode: row.managementItemCode,
      occurredDate: row.occurredDate,
      achievementDetail: row.achievementDetail || "{}",
      attachmentRef: row.attachmentRef ?? "",
    });
  };

  const save = async () => {
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const achievementDetail = form.achievementDetail.trim()
        ? JSON.parse(form.achievementDetail)
        : {};
      await lectureEvaluationAchievementApi.save({
        achievementId: selected?.achievementId,
        managementItemCode: form.managementItemCode.trim(),
        occurredDate: form.occurredDate,
        achievementDetail,
        attachmentRef: form.attachmentRef.trim() || undefined,
      });
      setSuccess("강의평가 실적이 저장되었습니다. 목록을 갱신했습니다.");
      await load();
    } catch (caught) {
      if (caught instanceof SyntaxError) {
        setFieldErrors({
          achievementDetail: "상세정보는 올바른 JSON 형식이어야 합니다.",
        });
      } else {
        handleError(caught);
      }
    } finally {
      setSaving(false);
    }
  };

  const downloadExcel = () => {
    const headings = [
      "관리번호",
      "성명",
      "관리항목",
      "업적발생일",
      "인증상태",
      "첨부여부",
    ];
    const values = rows.map((row) => [
      row.managementNo,
      row.teacherName,
      row.managementItemCode,
      row.occurredDate,
      row.certificationStatus,
      row.attachmentExists ? "Y" : "N",
    ]);
    const csv = [headings, ...values]
      .map((columns) => columns.map(csvCell).join(","))
      .join("\n");
    const anchor = document.createElement("a");
    anchor.href = URL.createObjectURL(
      new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8" }),
    );
    anchor.download = "lecture-evaluation-achievements.csv";
    anchor.click();
    URL.revokeObjectURL(anchor.href);
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
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

  if (permissionDenied) {
    return (
      <section data-testid="lecture-evaluation-page">
        <PermissionState
          title="강의평가 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">
          업적 입력 관리 / 교육영역 / 강의평가 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의평가 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          강의평가 실적을 조회하고, 동적 세부정보·첨부 참조·인증상태를
          확인합니다.
        </p>
      </header>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? <ErrorState title="강의평가 실적 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
          <Filter
            label="관리번호"
            value={filters.managementNo}
            onChange={(value) =>
              setFilters({ ...filters, managementNo: value })
            }
            testId="lecture-evaluation-management-no-filter"
          />
          <Filter
            label="성명"
            value={filters.teacherName}
            onChange={(value) => setFilters({ ...filters, teacherName: value })}
            testId="lecture-evaluation-teacher-filter"
          />
          <Filter
            label="관리항목"
            value={filters.managementItemCode}
            onChange={(value) =>
              setFilters({ ...filters, managementItemCode: value })
            }
            testId="lecture-evaluation-item-filter"
          />
          <Filter
            label="발생일 시작"
            type="date"
            value={filters.occurredDateFrom}
            onChange={(value) =>
              setFilters({ ...filters, occurredDateFrom: value })
            }
            testId="lecture-evaluation-date-from-filter"
          />
          <Filter
            label="발생일 종료"
            type="date"
            value={filters.occurredDateTo}
            onChange={(value) =>
              setFilters({ ...filters, occurredDateTo: value })
            }
            testId="lecture-evaluation-date-to-filter"
          />
          <label className="text-sm font-semibold text-dark">
            인증상태
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={filters.certificationStatus}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  certificationStatus: event.target.value,
                })
              }
              data-testid="lecture-evaluation-status-filter"
            >
              <option value="">전체</option>
              <option value="DRAFTING">작성중</option>
              <option value="SUBMITTED">제출</option>
              <option value="DEPARTMENT_CONFIRMED">학과장확인</option>
              <option value="DEPARTMENT_REJECTED">학과장미승인</option>
              <option value="CERTIFIED">인증</option>
              <option value="EVALUATION_CONFIRMED">평가확정</option>
            </select>
          </label>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="lecture-evaluation-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm"
            onClick={() => setFilters(emptyFilters)}
            data-testid="lecture-evaluation-reset-button"
          >
            조건 초기화
          </button>
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm"
            onClick={downloadExcel}
            disabled={rows.length === 0}
            data-testid="lecture-evaluation-excel-button"
          >
            <Download size={16} />
            Excel 다운로드
          </button>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="lecture-evaluation-list-panel"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            강의평가 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수{" "}
            <select
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value) as 20 | 50 | 100);
              }}
              data-testid="lecture-evaluation-page-size-select"
            >
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState
            title="조회 중"
            message="강의평가 실적을 불러오고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회된 강의평가 실적이 없습니다"
            message="검색조건을 변경하거나 신규 실적을 입력하세요."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-ld text-muted">
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>인증상태</th>
                  <th>첨부여부</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.achievementId} className="border-b border-ld">
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.occurredDate}</td>
                    <td>{row.certificationStatus}</td>
                    <td>{row.attachmentExists ? "있음" : "없음"}</td>
                    <td>
                      <button
                        type="button"
                        className="text-primary"
                        onClick={() => selectRow(row)}
                        data-testid={`lecture-evaluation-detail-${row.achievementId}`}
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="lecture-evaluation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          {selected ? "강의평가 실적 상세" : "신규 강의평가 실적"}
        </h2>
        {selected ? (
          <p className="mt-2 text-sm text-muted">
            현재 인증상태: {selected.certificationStatus} · 첨부:{" "}
            {selected.attachmentExists ? "있음" : "없음"}
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Filter
            label="관리항목 *"
            value={form.managementItemCode}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
            testId="lecture-evaluation-management-item-input"
            error={fieldErrors.managementItemCode}
          />
          <Filter
            label="업적발생일 *"
            type="date"
            value={form.occurredDate}
            onChange={(value) => setForm({ ...form, occurredDate: value })}
            testId="lecture-evaluation-occurred-date-input"
            error={fieldErrors.occurredDate}
          />
          <Filter
            label="첨부 참조"
            value={form.attachmentRef}
            onChange={(value) => setForm({ ...form, attachmentRef: value })}
            testId="lecture-evaluation-attachment-input"
          />
          <label className="text-sm font-semibold text-dark md:col-span-2">
            FR-018 동적 세부정보 (JSON)
            <textarea
              className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2 font-mono text-sm"
              value={form.achievementDetail}
              onChange={(event) =>
                setForm({ ...form, achievementDetail: event.target.value })
              }
              data-testid="lecture-evaluation-detail-input"
            />
            {fieldErrors.achievementDetail ? (
              <span className="text-error">
                {fieldErrors.achievementDetail}
              </span>
            ) : null}
          </label>
        </div>
        <button
          type="button"
          className="mt-5 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          disabled={saving}
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

function Filter({
  label,
  value,
  onChange,
  testId,
  type = "text",
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  type?: "text" | "date";
  error?: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <input
        type={type}
        className="mt-2 w-full rounded-md border border-ld px-3 py-2"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
      {error ? <span className="text-error">{error}</span> : null}
    </label>
  );
}
function toFieldErrors(fields: ApiErrorField[]) {
  return Object.fromEntries(
    fields.map((field) => [field.field, field.message]),
  );
}
function csvCell(value: string) {
  return `"${value.replaceAll('"', '""')}"`;
}
