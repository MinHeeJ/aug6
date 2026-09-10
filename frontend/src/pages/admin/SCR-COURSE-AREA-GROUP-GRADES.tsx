import { Download, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  courseAreaGroupGradeApi,
  type CurrentUser,
  type CourseAreaGroupGrade,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type CourseAreaGroupGradesPageProps = {
  currentUser?: CurrentUser | null;
};

export function CourseAreaGroupGradesPage({
  currentUser,
}: CourseAreaGroupGradesPageProps = {}) {
  const [teacherUserId, setTeacherUserId] = useState("");
  const [completionTypeCode, setCompletionTypeCode] = useState("");
  const [semesterCode, setSemesterCode] = useState("");
  const [courseAreaCode, setCourseAreaCode] = useState("");
  const [grades, setGrades] = useState<CourseAreaGroupGrade[]>([]);
  const [selected, setSelected] = useState<CourseAreaGroupGrade | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isProfessorSelfScope = currentUser?.roles.includes("R01") === true;
  const effectiveTeacherUserId = isProfessorSelfScope
    ? String(currentUser?.userId ?? "")
    : teacherUserId;

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const parsedTeacherUserId = Number(effectiveTeacherUserId);
      const response = await courseAreaGroupGradeApi.listCourseAreaGroupGrades({
        teacherUserId:
          effectiveTeacherUserId.trim() && Number.isFinite(parsedTeacherUserId)
            ? parsedTeacherUserId
            : undefined,
        completionTypeCode: completionTypeCode.trim() || undefined,
        semesterCode: semesterCode.trim() || undefined,
        courseAreaCode: courseAreaCode.trim() || undefined,
        page,
        pageSize,
      });
      setGrades(response.data?.courseAreaGroupGrades ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize, effectiveTeacherUserId]);

  const selectGrade = async (grade: CourseAreaGroupGrade) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response =
        await courseAreaGroupGradeApi.getCourseAreaGroupGradeDetail(
          grade.gradeId,
        );
      setSelected(response.data ?? grade);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setDetailLoading(false);
    }
  };

  const downloadExcel = () => {
    downloadCsv("course-area-group-grades.csv", grades, [
      { header: "평가연도", value: (grade) => grade.evaluationYear },
      {
        header: "교원",
        value: (grade) => `${grade.teacherName} (${grade.teacherUserId})`,
      },
      { header: "소속대학", value: (grade) => grade.collegeCode },
      { header: "학과", value: (grade) => grade.departmentCode },
      { header: "이수구분", value: (grade) => grade.completionTypeCode },
      { header: "학기", value: (grade) => grade.semesterCode },
      { header: "교과영역", value: (grade) => grade.courseAreaName },
      { header: "성적", value: (grade) => groupGradeLabel(grade.groupGrade) },
      { header: "총점", value: (grade) => grade.totalScore },
      {
        header: "확정상태",
        value: (grade) => finalizationLabel(grade.finalizationStatus),
      },
    ]);
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "교과영역 그룹평가 성적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-COURSE-AREA-GROUP-GRADES"
        data-testid="course-area-group-grades-page"
      >
        <PermissionState
          title="교과영역 그룹평가 성적 조회 권한이 없습니다"
          message="R01 교원 또는 R04 업무담당자 권한이 필요합니다. R01 교원은 본인 성적만 조회할 수 있습니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-AREA-GROUP-GRADES"
      data-testid="course-area-group-grades-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              업적 평가 조회 / 교원 성적 조회 / 교과영역 그룹평가 성적 조회
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              교과영역 그룹평가 성적 조회
            </h1>
            <p className="mt-2 text-sm text-muted">
              교원, 이수구분, 학기, 교과영역 조건으로 공개된 그룹평가 성적
              목록과 상세를 조회합니다. R01 교원은 본인 성적만 조회할 수
              있습니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="course-area-group-grades-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {error ? (
        <ErrorState title="교과영역 그룹평가 성적 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-6">
          <TextInput
            label="교원 식별자"
            value={effectiveTeacherUserId}
            onChange={setTeacherUserId}
            testId="course-area-group-grades-teacher-input"
            disabled={isProfessorSelfScope}
            helperText={
              isProfessorSelfScope
                ? "R01 교원은 본인으로 고정됩니다."
                : undefined
            }
          />
          <TextInput
            label="이수구분"
            value={completionTypeCode}
            onChange={setCompletionTypeCode}
            testId="course-area-group-grades-completion-input"
          />
          <TextInput
            label="학기"
            value={semesterCode}
            onChange={setSemesterCode}
            testId="course-area-group-grades-semester-input"
          />
          <TextInput
            label="교과영역"
            value={courseAreaCode}
            onChange={setCourseAreaCode}
            testId="course-area-group-grades-course-area-input"
          />
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
              data-testid="course-area-group-grades-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="course-area-group-grades-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-3">
        <div className="rounded-md border border-ld bg-white p-6 shadow-sm lg:col-span-2">
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="text-lg font-semibold text-dark">
              그룹평가 성적 목록
            </h2>
            <button
              type="button"
              className="inline-flex items-center justify-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
              disabled={grades.length === 0}
              onClick={downloadExcel}
              data-testid="course-area-group-grades-excel-button"
            >
              <Download size={16} /> 엑셀 내려받기
            </button>
          </div>
          {loading ? (
            <LoadingState title="교과영역 그룹평가 성적 조회 중" />
          ) : null}
          {!loading && grades.length === 0 ? (
            <EmptyState
              title="조회된 성적이 없습니다"
              message="교원·이수구분·학기·교과영역 조건을 변경해 보세요."
            />
          ) : null}
          {!loading && grades.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">교원</th>
                    <th className="px-3 py-2">이수구분</th>
                    <th className="px-3 py-2">학기</th>
                    <th className="px-3 py-2">교과영역</th>
                    <th className="px-3 py-2">성적</th>
                    <th className="px-3 py-2">총점</th>
                    <th className="px-3 py-2">상태</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {grades.map((grade) => (
                    <tr
                      key={grade.gradeId}
                      className={
                        selected?.gradeId === grade.gradeId
                          ? "bg-lightprimary"
                          : "hover:bg-lightgray"
                      }
                      onClick={() => void selectGrade(grade)}
                      data-testid="course-area-group-grades-row"
                    >
                      <td className="px-3 py-2 font-semibold text-dark">
                        {grade.teacherName} ({grade.teacherUserId})
                      </td>
                      <td className="px-3 py-2">{grade.completionTypeCode}</td>
                      <td className="px-3 py-2">{grade.semesterCode}</td>
                      <td className="px-3 py-2">{grade.courseAreaName}</td>
                      <td className="px-3 py-2">
                        {groupGradeLabel(grade.groupGrade)}
                      </td>
                      <td className="px-3 py-2">{grade.totalScore}</td>
                      <td className="px-3 py-2">
                        {finalizationLabel(grade.finalizationStatus)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <p className="mt-3 text-xs text-muted">
            총 {totalElements}건 / {page + 1}페이지
          </p>
        </div>

        <aside
          className="rounded-md border border-ld bg-white p-6 shadow-sm"
          data-testid="course-area-group-grades-detail-panel"
        >
          <h2 className="text-lg font-semibold text-dark">상세</h2>
          {detailLoading ? <LoadingState title="상세 조회 중" /> : null}
          {!detailLoading && !selected ? (
            <EmptyState
              title="선택된 성적이 없습니다"
              message="목록 행을 선택하면 상세 성적을 조회합니다."
            />
          ) : null}
          {!detailLoading && selected ? (
            <dl className="mt-4 space-y-3 text-sm">
              <DetailItem label="평가연도" value={selected.evaluationYear} />
              <DetailItem
                label="교원"
                value={`${selected.teacherName} (${selected.teacherUserId})`}
              />
              <DetailItem
                label="소속"
                value={`${selected.collegeCode} / ${selected.departmentCode}`}
              />
              <DetailItem
                label="이수구분"
                value={selected.completionTypeCode}
              />
              <DetailItem label="학기" value={selected.semesterCode} />
              <DetailItem
                label="교과영역"
                value={`${selected.courseAreaName} (${selected.courseAreaCode})`}
              />
              <DetailItem
                label="성적"
                value={groupGradeLabel(selected.groupGrade)}
              />
              <DetailItem label="총점" value={String(selected.totalScore)} />
              <DetailItem
                label="확정상태"
                value={finalizationLabel(selected.finalizationStatus)}
              />
            </dl>
          ) : null}
        </aside>
      </section>
    </section>
  );
}

function TextInput({
  label,
  value,
  onChange,
  testId,
  disabled = false,
  helperText,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  disabled?: boolean;
  helperText?: string;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <input
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm disabled:bg-lightgray"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
        disabled={disabled}
      />
      {helperText ? (
        <span className="mt-1 block text-xs text-muted">{helperText}</span>
      ) : null}
    </label>
  );
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-lightsecondary p-3">
      <dt className="text-xs font-semibold text-muted">{label}</dt>
      <dd className="mt-1 text-dark">{value}</dd>
    </div>
  );
}

function groupGradeLabel(value: string) {
  const labels: Record<string, string> = {
    A_PLUS: "A+",
    A: "A",
    B_PLUS: "B+",
    B: "B",
    C: "C",
  };
  return labels[value] ?? value;
}

function finalizationLabel(value: string) {
  const labels: Record<string, string> = {
    CERTIFIED: "인증",
    EVALUATION_CONFIRMED: "평가확정",
    CANCELLED: "확정취소",
  };
  return labels[value] ?? value;
}
