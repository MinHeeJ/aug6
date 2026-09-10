import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  managementItemEvaluationScoreApi,
  type ActiveYn,
  type ApiErrorField,
  type ManagementItemEvaluationScore,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type FormState = {
  ruleVersionId: string;
  achievementAreaCode: string;
  achievementCategoryCode: string;
  managementItemCode: string;
  collegeCode: string;
  evaluationScore: string;
  sortOrder: string;
  activeYn: ActiveYn;
  changeReason: string;
};

const initialForm: FormState = {
  ruleVersionId: "",
  achievementAreaCode: "",
  achievementCategoryCode: "",
  managementItemCode: "",
  collegeCode: "",
  evaluationScore: "",
  sortOrder: "",
  activeYn: "Y",
  changeReason: "",
};

export function ManagementItemEvaluationScoresPage() {
  const [achievementAreaCode, setAchievementAreaCode] = useState("");
  const [achievementCategoryCode, setAchievementCategoryCode] = useState("");
  const [collegeCode, setCollegeCode] = useState("");
  const [scores, setScores] = useState<ManagementItemEvaluationScore[]>([]);
  const [selected, setSelected] =
    useState<ManagementItemEvaluationScore | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await managementItemEvaluationScoreApi.listManagementItemEvaluationScores(
          {
            achievementAreaCode: achievementAreaCode.trim() || undefined,
            achievementCategoryCode:
              achievementCategoryCode.trim() || undefined,
            collegeCode: collegeCode.trim() || undefined,
            page,
            pageSize,
          },
        );
      setScores(response.data?.managementItemEvaluationScores ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        achievementAreaCode,
        achievementCategoryCode,
        collegeCode,
      });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = (item: ManagementItemEvaluationScore) => {
    setSelected(item);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      ruleVersionId: String(item.ruleVersionId),
      achievementAreaCode: item.achievementAreaCode,
      achievementCategoryCode: item.achievementCategoryCode,
      managementItemCode: item.managementItemCode,
      collegeCode: item.collegeCode,
      evaluationScore: String(item.evaluationScore),
      sortOrder: String(item.sortOrder),
      activeYn: item.activeYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("관리항목별 평가점수를 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await managementItemEvaluationScoreApi.saveManagementItemEvaluationScore(
          {
            ruleVersionId: Number(form.ruleVersionId),
            achievementAreaCode: form.achievementAreaCode.trim(),
            achievementCategoryCode: form.achievementCategoryCode.trim(),
            managementItemCode: form.managementItemCode.trim(),
            collegeCode: form.collegeCode.trim(),
            evaluationScore:
              form.evaluationScore.trim() === ""
                ? Number.NaN
                : Number(form.evaluationScore),
            sortOrder:
              form.sortOrder.trim() === ""
                ? Number.NaN
                : Number(form.sortOrder),
            activeYn: form.activeYn,
            changeReason: form.changeReason.trim(),
          },
        );
      setSuccessMessage("저장되었습니다");
      if (response.data) setSelected(response.data);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const downloadExcel = () => {
    downloadCsv("management-item-evaluation-scores.csv", scores, [
      { header: "평가연도", value: (item) => item.evaluationYear },
      { header: "업적영역", value: (item) => item.achievementAreaCode },
      { header: "업적분류", value: (item) => item.achievementCategoryCode },
      { header: "관리항목", value: (item) => item.managementItemCode },
      { header: "소속대학", value: (item) => item.collegeCode },
      { header: "평가점수", value: (item) => item.evaluationScore },
      { header: "정렬순서", value: (item) => item.sortOrder },
      { header: "사용상태", value: (item) => activeLabel(item.activeYn) },
      {
        header: "규정상태",
        value: (item) => versionStatusLabel(item.versionStatus),
      },
    ]);
    setSuccessMessage("엑셀 내려받기 파일을 생성했습니다");
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "관리항목별 평가점수를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-MANAGEMENT-ITEM-EVAL-SCORES"
        data-testid="management-item-evaluation-scores-page"
      >
        <PermissionState
          title="관리항목별 평가점수 관리 권한이 없습니다"
          message="R04 업무담당자 또는 R09 시스템관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MANAGEMENT-ITEM-EVAL-SCORES"
      data-testid="management-item-evaluation-scores-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              평가 기준 관리 / 평가 기준정보 관리 / 관리항목별 평가점수 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              관리항목별 평가점수 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              업적영역·업적분류·소속대학별 관리항목 평가점수, 정렬순서,
              사용상태를 운영 설정으로 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="management-item-evaluation-scores-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 목록을 재조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="관리항목별 평가점수 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-5">
          <TextInput
            label="업적영역 코드"
            value={achievementAreaCode}
            field="achievementAreaCode"
            fieldErrors={{}}
            onChange={setAchievementAreaCode}
          />
          <TextInput
            label="업적분류 코드"
            value={achievementCategoryCode}
            field="achievementCategoryCode"
            fieldErrors={{}}
            onChange={setAchievementCategoryCode}
          />
          <TextInput
            label="소속대학 코드"
            value={collegeCode}
            field="collegeCode"
            fieldErrors={{}}
            onChange={setCollegeCode}
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
              data-testid="management-item-evaluation-scores-page-size-select"
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
            data-testid="management-item-evaluation-scores-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">
            관리항목별 평가점수 목록
          </h2>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
            disabled={scores.length === 0}
            onClick={downloadExcel}
            data-testid="management-item-evaluation-scores-excel-button"
          >
            <Download size={16} /> 엑셀 내려받기
          </button>
        </div>
        {loading ? <LoadingState title="관리항목별 평가점수 조회 중" /> : null}
        {!loading && scores.length === 0 ? (
          <EmptyState
            title="조회된 평가점수가 없습니다"
            message="업적영역·업적분류·소속대학 조건을 변경하거나 작성중 규정버전에서 설정을 저장하세요."
          />
        ) : null}
        {!loading && scores.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">소속대학</th>
                  <th className="px-3 py-2">평가점수</th>
                  <th className="px-3 py-2">정렬순서</th>
                  <th className="px-3 py-2">적용 대상</th>
                  <th className="px-3 py-2">사용상태</th>
                  <th className="px-3 py-2">규정상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {scores.map((item) => (
                  <tr
                    key={item.settingId}
                    className={
                      selected?.settingId === item.settingId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    onClick={() => selectRow(item)}
                    data-testid="management-item-evaluation-scores-row"
                  >
                    <td className="px-3 py-2 font-semibold text-dark">
                      {item.managementItemCode}
                    </td>
                    <td className="px-3 py-2">{item.collegeCode}</td>
                    <td className="px-3 py-2">{item.evaluationScore}</td>
                    <td className="px-3 py-2">{item.sortOrder}</td>
                    <td className="px-3 py-2">
                      {item.achievementAreaCode} /{" "}
                      {item.achievementCategoryCode}
                    </td>
                    <td className="px-3 py-2">{activeLabel(item.activeYn)}</td>
                    <td className="px-3 py-2">
                      {versionStatusLabel(item.versionStatus)}
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
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">등록·수정 폼</h2>
        <p className="mt-2 text-sm text-muted">
          확정 규정버전은 수정할 수 없습니다. 저장 후 동일 조건으로 목록을
          재조회합니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <TextInput
            label="규정버전 ID"
            value={form.ruleVersionId}
            field="ruleVersionId"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, ruleVersionId: value })}
            required
          />
          <TextInput
            label="업적영역 코드"
            value={form.achievementAreaCode}
            field="achievementAreaCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, achievementAreaCode: value })
            }
            required
          />
          <TextInput
            label="업적분류 코드"
            value={form.achievementCategoryCode}
            field="achievementCategoryCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, achievementCategoryCode: value })
            }
            required
          />
          <TextInput
            label="관리항목 코드"
            value={form.managementItemCode}
            field="managementItemCode"
            fieldErrors={fieldErrors}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
            required
          />
          <TextInput
            label="소속대학 코드"
            value={form.collegeCode}
            field="collegeCode"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, collegeCode: value })}
            required
          />
          <TextInput
            label="평가점수"
            value={form.evaluationScore}
            field="evaluationScore"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, evaluationScore: value })}
            required
          />
          <TextInput
            label="정렬순서"
            value={form.sortOrder}
            field="sortOrder"
            fieldErrors={fieldErrors}
            onChange={(value) => setForm({ ...form, sortOrder: value })}
            required
          />
          <label className="text-sm font-semibold text-dark">
            사용여부<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.activeYn}
              onChange={(event) =>
                setForm({ ...form, activeYn: event.target.value as ActiveYn })
              }
              data-testid="management-item-evaluation-scores-active-yn-select"
            >
              <option value="Y">사용</option>
              <option value="N">미사용</option>
            </select>
            {fieldErrors.activeYn ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.activeYn}
              </span>
            ) : null}
          </label>
        </div>
        <label className="mt-4 block text-sm font-semibold text-dark">
          변경 사유<span className="ms-1 text-error">*</span>
          <textarea
            className="mt-2 min-h-[92px] w-full rounded-md border border-ld px-3 py-2 text-sm"
            value={form.changeReason}
            onChange={(event) =>
              setForm({ ...form, changeReason: event.target.value })
            }
            data-testid="management-item-evaluation-scores-change-reason-textarea"
          />
          {fieldErrors.changeReason ? (
            <span className="mt-1 block text-xs text-error">
              {fieldErrors.changeReason}
            </span>
          ) : null}
        </label>
        <div className="mt-5 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            disabled={saving}
            onClick={() => void save()}
            data-testid="management-item-evaluation-scores-save-button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
            onClick={() => {
              setSelected(null);
              setFieldErrors({});
              setForm({
                ...initialForm,
                achievementAreaCode,
                achievementCategoryCode,
                collegeCode,
              });
            }}
            data-testid="management-item-evaluation-scores-cancel-button"
          >
            취소
          </button>
        </div>
      </section>
    </section>
  );
}

function TextInput({
  label,
  value,
  field,
  fieldErrors,
  onChange,
  required,
}: {
  label: string;
  value: string;
  field: string;
  fieldErrors: Record<string, string>;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <input
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={Boolean(fieldErrors[field])}
        data-testid={`management-item-evaluation-scores-${field
          .replace(/[^A-Za-z0-9]+/g, "-")
          .replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)
          .replace(/^-/, "")}-input`}
      />
      {fieldErrors[field] ? (
        <span className="mt-1 block text-xs text-error">
          {fieldErrors[field]}
        </span>
      ) : null}
    </label>
  );
}

function activeLabel(value: string) {
  return value === "Y" ? "사용" : "미사용";
}

function versionStatusLabel(value: string) {
  if (value === "DRAFT") return "작성중";
  if (value === "CONFIRMED") return "확정";
  return "폐기";
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
