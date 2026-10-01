import { RefreshCw, Save, Search } from "lucide-react";
import type React from "react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type LectureEvaluationAchievement,
  type PageSize,
  type SaveLectureEvaluationAchievementPayload,
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

type FormState = {
  achievementId?: number;
  evaluationYear: string;
  organizationCode: string;
  managementItemCode: string;
  occurredDate: string;
  achievementDetail: string;
  changeReason: string;
};

const emptyFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  occurredDateFrom: "",
  occurredDateTo: "",
  certificationStatus: "",
};

const emptyForm: FormState = {
  evaluationYear: String(new Date().getFullYear()),
  organizationCode: "",
  managementItemCode: "",
  occurredDate: "",
  achievementDetail: "",
  changeReason: "",
};

/** 강의평가 실적의 검색, 상세 입력, 저장을 제공하는 교육영역 업무 화면이다. */
export function LectureEvaluationAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [items, setItems] = useState<LectureEvaluationAchievement[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await lectureEvaluationAchievementApi.list({
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

  const search = async () => {
    setPage(0);
    await load();
  };

  const save = async () => {
    const errors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      errors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.occurredDate) errors.occurredDate = "업적발생일을 입력하세요.";
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;

    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload: SaveLectureEvaluationAchievementPayload = {
        achievementId: form.achievementId,
        evaluationYear: form.evaluationYear.trim() || undefined,
        organizationCode: form.organizationCode.trim() || undefined,
        managementItemCode: form.managementItemCode.trim(),
        occurredDate: form.occurredDate,
        achievementDetail: form.achievementDetail.trim()
          ? JSON.parse(form.achievementDetail)
          : undefined,
        changeReason: form.changeReason.trim() || undefined,
      };
      await lectureEvaluationAchievementApi.save(payload);
      setSuccess(
        "강의평가 실적을 저장했습니다. 목록을 최신 상태로 다시 조회했습니다.",
      );
      setForm(emptyForm);
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

  const selectItem = (item: LectureEvaluationAchievement) => {
    setForm({
      achievementId: item.achievementId,
      evaluationYear: item.evaluationYear,
      organizationCode: item.organizationCode,
      managementItemCode: item.managementItemCode,
      occurredDate: item.occurredDate,
      achievementDetail: item.achievementDetail ?? "",
      changeReason: "",
    });
    setSuccess(null);
  };

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
        : "강의평가 실적 처리 중 오류가 발생했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-testid="lecture-evaluation-achievement-page">
        <PermissionState
          title="강의평가 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04와 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 &gt; 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의평가 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강의평가 실적을 검색하고 작성중 상태로 등록·수정합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="lecture-evaluation-refresh-button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </header>
      {success ? <SuccessState title={success} /> : null}
      {error ? <ErrorState title="강의평가 실적 오류" message={error} /> : null}
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          {(
            [
              ["managementNo", "관리번호"],
              ["teacherName", "교원명"],
              ["managementItemCode", "관리항목코드"],
              ["occurredDateFrom", "발생일 시작"],
              ["occurredDateTo", "발생일 종료"],
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
                data-testid={`lecture-evaluation-filter-${key}`}
              />
            </label>
          ))}
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            onClick={() => void search()}
            data-testid="lecture-evaluation-search-button"
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
            data-testid="lecture-evaluation-reset-button"
          >
            초기화
          </button>
        </div>
      </section>
      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
          <label className="text-sm text-muted">
            표시 건수{" "}
            <select
              className="ml-2 rounded border border-ld p-2"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as PageSize);
                setPage(0);
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
          <LoadingState title="강의평가 실적을 조회 중입니다" />
        ) : items.length === 0 ? (
          <EmptyState
            title="조회된 강의평가 실적이 없습니다"
            message="검색 조건을 변경하거나 새 실적을 등록하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-ld text-muted">
                <tr>
                  <th className="p-2">관리번호</th>
                  <th className="p-2">교원</th>
                  <th className="p-2">관리항목</th>
                  <th className="p-2">발생일</th>
                  <th className="p-2">상태</th>
                  <th className="p-2">첨부여부</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.achievementId}
                    className="cursor-pointer border-b border-ld hover:bg-lightprimary"
                    onClick={() => selectItem(item)}
                    data-testid={`lecture-evaluation-row-${item.achievementId}`}
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
        )}
        <p className="mt-3 text-sm text-muted">총 {totalElements}건</p>
      </section>
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">상세 입력</h2>
        <p className="mt-2 text-sm text-muted">
          평가확정 상태의 실적은 수정할 수 없습니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="평가연도">
            <input
              className="form-input"
              value={form.evaluationYear}
              onChange={(event) =>
                setForm({ ...form, evaluationYear: event.target.value })
              }
              data-testid="lecture-evaluation-year-input"
            />
          </Field>
          <Field label="조직코드">
            <input
              className="form-input"
              value={form.organizationCode}
              onChange={(event) =>
                setForm({ ...form, organizationCode: event.target.value })
              }
              data-testid="lecture-evaluation-organization-input"
            />
          </Field>
          <Field label="관리항목코드 *" error={fieldErrors.managementItemCode}>
            <input
              className="form-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              data-testid="lecture-evaluation-management-item-input"
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
              data-testid="lecture-evaluation-occurred-date-input"
            />
          </Field>
          <Field label="상세정보(JSON)" error={fieldErrors.achievementDetail}>
            <textarea
              className="form-input min-h-24"
              value={form.achievementDetail}
              onChange={(event) =>
                setForm({ ...form, achievementDetail: event.target.value })
              }
              data-testid="lecture-evaluation-detail-input"
            />
          </Field>
          <Field label="변경사유">
            <textarea
              className="form-input min-h-24"
              value={form.changeReason}
              onChange={(event) =>
                setForm({ ...form, changeReason: event.target.value })
              }
              data-testid="lecture-evaluation-change-reason-input"
            />
          </Field>
        </div>
        <button
          type="button"
          className="mt-4 inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white disabled:opacity-50"
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
