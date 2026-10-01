import { RefreshCw, Save, Search } from "lucide-react";
import type React from "react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  degreeCompletionAchievementApi,
  type DegreeCompletionAchievement,
  type DegreeCompletionStudentPayload,
  type PageSize,
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
  certificationStatus: string;
};

type FormState = {
  achievementId?: number;
  evaluationYear: string;
  organizationCode: string;
  managementItemCode: string;
  occurredDate: string;
  achievementDetail: string;
  attachmentRef: string;
  changeReason: string;
  students: DegreeCompletionStudentPayload[];
};

const emptyFilters: Filters = {
  managementNo: "",
  teacherName: "",
  certificationStatus: "",
};

const emptyStudent = (): DegreeCompletionStudentPayload => ({
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});

const emptyForm = (): FormState => ({
  evaluationYear: String(new Date().getFullYear()),
  organizationCode: "",
  managementItemCode: "",
  occurredDate: "",
  achievementDetail: "",
  attachmentRef: "",
  changeReason: "",
  students: [emptyStudent()],
});

/** 석·박사 배출 실적의 검색, 헤더 입력, 지도학생 상세 저장과 재조회를 제공한다. */
export function MastersDoctoralGraduationAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [items, setItems] = useState<DegreeCompletionAchievement[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "석·박사 배출 실적 처리 중 오류가 발생했습니다.",
    );
  };

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await degreeCompletionAchievementApi.list({
        ...filters,
        page,
        size: pageSize,
      });
      setItems(response.data?.achievements ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const updateStudent = (
    index: number,
    field: keyof DegreeCompletionStudentPayload,
    value: string,
  ) => {
    setForm((current) => ({
      ...current,
      students: current.students.map((student, studentIndex) =>
        studentIndex === index ? { ...student, [field]: value } : student,
      ),
    }));
  };

  const save = async () => {
    const errors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      errors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.occurredDate) errors.occurredDate = "업적발생일을 입력하세요.";
    form.students.forEach((student, index) => {
      if (!student.degreeType)
        errors[`students.${index}.degreeType`] = "학위구분을 선택하세요.";
      if (!student.studentName.trim())
        errors[`students.${index}.studentName`] = "학생명을 입력하세요.";
      if (!student.degreeAwardedDate)
        errors[`students.${index}.degreeAwardedDate`] =
          "학위수여일을 입력하세요.";
    });
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    if (
      !window.confirm("석·박사 배출 실적과 지도학생 상세를 저장하시겠습니까?")
    )
      return;

    try {
      setSaving(true);
      setError(null);
      await degreeCompletionAchievementApi.save({
        achievementId: form.achievementId,
        evaluationYear: form.evaluationYear.trim() || undefined,
        organizationCode: form.organizationCode.trim() || undefined,
        managementItemCode: form.managementItemCode.trim(),
        occurredDate: form.occurredDate,
        achievementDetail: form.achievementDetail.trim() || undefined,
        attachmentRef: form.attachmentRef.trim() || undefined,
        changeReason: form.changeReason.trim() || undefined,
        students: form.students.map((student) => ({
          ...student,
          degreeType: student.degreeType.trim(),
          studentName: student.studentName.trim(),
          thesisTitle: student.thesisTitle?.trim() || undefined,
        })),
      });
      setSuccess(
        "석·박사 배출 실적과 지도학생 상세를 저장하고 목록을 최신 상태로 조회했습니다.",
      );
      setForm(emptyForm());
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const selectItem = (item: DegreeCompletionAchievement) => {
    setForm({
      achievementId: item.achievementId,
      evaluationYear: item.evaluationYear,
      organizationCode: item.organizationCode,
      managementItemCode: item.managementItemCode,
      occurredDate: item.occurredDate,
      achievementDetail: item.achievementDetail ?? "",
      attachmentRef: "",
      changeReason: "",
      students: item.students.map((student) => ({
        degreeType: student.degreeType,
        studentName: student.studentName,
        thesisTitle: student.thesisTitle ?? "",
        degreeAwardedDate: student.degreeAwardedDate,
      })),
    });
    setSuccess(null);
  };

  if (permissionDenied) {
    return (
      <section data-testid="masters-doctoral-graduation-page">
        <PermissionState
          title="석·박사 배출 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04와 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
      data-testid="masters-doctoral-graduation-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 &gt; 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              석·박사 배출 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              배출 실적과 지도학생 세부내역을 함께 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="masters-doctoral-graduation-refresh-button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </header>
      {success ? <SuccessState title={success} /> : null}
      {error ? (
        <ErrorState title="석·박사 배출 실적 오류" message={error} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          {(
            [
              ["managementNo", "관리번호"],
              ["teacherName", "교원명"],
              ["certificationStatus", "인증상태"],
            ] as const
          ).map(([key, label]) => (
            <label key={key} className="text-sm text-ld">
              {label}
              <input
                className="form-input mt-1"
                value={filters[key]}
                onChange={(event) =>
                  setFilters({ ...filters, [key]: event.target.value })
                }
                data-testid={`masters-doctoral-graduation-filter-${key}`}
              />
            </label>
          ))}
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="masters-doctoral-graduation-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="h-10 rounded-md border border-ld px-4 text-sm"
            onClick={() => {
              setFilters(emptyFilters);
              setPage(0);
            }}
            data-testid="masters-doctoral-graduation-reset-button"
          >
            초기화
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex items-center justify-between gap-4">
          <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded border border-ld p-2"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as PageSize);
                setPage(0);
              }}
              data-testid="masters-doctoral-graduation-page-size-select"
            >
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="석·박사 배출 실적을 조회 중입니다" />
        ) : null}
        {!loading && items.length === 0 ? (
          <EmptyState
            title="조회된 석·박사 배출 실적이 없습니다"
            message="검색 조건을 변경하거나 새 실적을 등록하세요."
          />
        ) : null}
        {!loading && items.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-ld text-muted">
                <tr>
                  <th className="p-2">관리번호</th>
                  <th className="p-2">교원</th>
                  <th className="p-2">관리항목</th>
                  <th className="p-2">발생일</th>
                  <th className="p-2">상태</th>
                  <th className="p-2">첨부</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.achievementId}
                    className="cursor-pointer border-b border-ld hover:bg-lightprimary"
                    onClick={() => selectItem(item)}
                    data-testid={`masters-doctoral-graduation-row-${item.achievementId}`}
                  >
                    <td className="p-2">{item.managementNo}</td>
                    <td className="p-2">{item.teacherName}</td>
                    <td className="p-2">{item.managementItemCode}</td>
                    <td className="p-2">{item.occurredDate}</td>
                    <td className="p-2">{item.certificationStatus}</td>
                    <td className="p-2">
                      {item.attachmentPresent ? "있음" : "없음"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-sm text-muted">총 {totalElements}건</p>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="masters-doctoral-graduation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">상세 입력</h2>
        <p className="mt-2 text-sm text-muted">
          평가확정 상태의 실적은 수정하거나 첨부를 변경할 수 없습니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="평가연도">
            <input
              className="form-input"
              value={form.evaluationYear}
              onChange={(event) =>
                setForm({ ...form, evaluationYear: event.target.value })
              }
              data-testid="masters-doctoral-graduation-year-input"
            />
          </Field>
          <Field label="조직코드">
            <input
              className="form-input"
              value={form.organizationCode}
              onChange={(event) =>
                setForm({ ...form, organizationCode: event.target.value })
              }
              data-testid="masters-doctoral-graduation-organization-input"
            />
          </Field>
          <Field label="관리항목코드 *" error={fieldErrors.managementItemCode}>
            <input
              className="form-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              data-testid="masters-doctoral-graduation-management-item-input"
            />
          </Field>
          <Field label="업적발생일 *" error={fieldErrors.occurredDate}>
            <input
              type="date"
              className="form-input"
              value={form.occurredDate}
              onChange={(event) =>
                setForm({ ...form, occurredDate: event.target.value })
              }
              data-testid="masters-doctoral-graduation-occurred-date-input"
            />
          </Field>
          <Field label="첨부 참조">
            <input
              className="form-input"
              value={form.attachmentRef}
              onChange={(event) =>
                setForm({ ...form, attachmentRef: event.target.value })
              }
              data-testid="masters-doctoral-graduation-attachment-input"
            />
          </Field>
          <Field label="상세정보(JSON)">
            <textarea
              className="form-input min-h-24"
              value={form.achievementDetail}
              onChange={(event) =>
                setForm({ ...form, achievementDetail: event.target.value })
              }
              data-testid="masters-doctoral-graduation-detail-input"
            />
          </Field>
        </div>

        <div
          className="mt-6"
          data-testid="masters-doctoral-graduation-students-panel"
        >
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-dark">
              지도학생 세부내역
            </h3>
            <button
              type="button"
              className="h-9 rounded-md border border-primary px-3 text-sm text-primary"
              onClick={() =>
                setForm((current) => ({
                  ...current,
                  students: [...current.students, emptyStudent()],
                }))
              }
              data-testid="masters-doctoral-graduation-add-student-button"
            >
              학생 추가
            </button>
          </div>
          <div className="mt-3 space-y-3">
            {form.students.map((student, index) => (
              <div
                key={`${student.studentName}-${index}`}
                className="grid gap-3 rounded border border-ld p-3 md:grid-cols-5"
                data-testid={`masters-doctoral-graduation-student-row-${index}`}
              >
                <Field
                  label="학위구분 *"
                  error={fieldErrors[`students.${index}.degreeType`]}
                >
                  <select
                    className="form-input"
                    value={student.degreeType}
                    onChange={(event) =>
                      updateStudent(index, "degreeType", event.target.value)
                    }
                    data-testid={`masters-doctoral-graduation-degree-type-${index}`}
                  >
                    <option value="MASTER">석사</option>
                    <option value="DOCTORAL">박사</option>
                  </select>
                </Field>
                <Field
                  label="학생명 *"
                  error={fieldErrors[`students.${index}.studentName`]}
                >
                  <input
                    className="form-input"
                    value={student.studentName}
                    onChange={(event) =>
                      updateStudent(index, "studentName", event.target.value)
                    }
                    data-testid={`masters-doctoral-graduation-student-name-${index}`}
                  />
                </Field>
                <Field label="논문제목">
                  <input
                    className="form-input"
                    value={student.thesisTitle ?? ""}
                    onChange={(event) =>
                      updateStudent(index, "thesisTitle", event.target.value)
                    }
                    data-testid={`masters-doctoral-graduation-thesis-title-${index}`}
                  />
                </Field>
                <Field
                  label="학위수여일 *"
                  error={fieldErrors[`students.${index}.degreeAwardedDate`]}
                >
                  <input
                    type="date"
                    className="form-input"
                    value={student.degreeAwardedDate}
                    onChange={(event) =>
                      updateStudent(
                        index,
                        "degreeAwardedDate",
                        event.target.value,
                      )
                    }
                    data-testid={`masters-doctoral-graduation-awarded-date-${index}`}
                  />
                </Field>
                <div className="flex items-end">
                  <button
                    type="button"
                    className="h-10 rounded-md border border-error px-3 text-sm text-error disabled:opacity-40"
                    disabled={form.students.length === 1}
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        students: current.students.filter(
                          (_, studentIndex) => studentIndex !== index,
                        ),
                      }))
                    }
                    data-testid={`masters-doctoral-graduation-remove-student-${index}`}
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
        <Field label="변경사유">
          <textarea
            className="form-input mt-4 min-h-24"
            value={form.changeReason}
            onChange={(event) =>
              setForm({ ...form, changeReason: event.target.value })
            }
            data-testid="masters-doctoral-graduation-change-reason-input"
          />
        </Field>
        <button
          type="button"
          className="mt-4 inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white disabled:opacity-60"
          disabled={saving}
          onClick={() => void save()}
          data-testid="masters-doctoral-graduation-save-button"
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
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="text-sm text-ld">
      {label}
      {children}
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
