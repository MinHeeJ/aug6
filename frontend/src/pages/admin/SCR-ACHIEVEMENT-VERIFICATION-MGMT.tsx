import type React from "react";
import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  achievementVerificationApi,
  ApiClientError,
  type AchievementVerificationTarget,
  type ApiErrorField,
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

type VerificationAction = "CERTIFY" | "RETURN" | "CANCEL_CERTIFICATION";

type TransitionForm = {
  actionType: VerificationAction;
  reasonCode: string;
  opinion: string;
  evidenceRef: string;
};

const initialForm: TransitionForm = {
  actionType: "CERTIFY",
  reasonCode: "",
  opinion: "",
  evidenceRef: "",
};

export function AchievementVerificationManagementPage() {
  const [filters, setFilters] = useState({
    evaluationYear: "2026",
    areaCode: "",
    verificationStatus: "DEPARTMENT_CONFIRMED",
  });
  const [targets, setTargets] = useState<AchievementVerificationTarget[]>([]);
  const [selected, setSelected] =
    useState<AchievementVerificationTarget | null>(null);
  const [form, setForm] = useState<TransitionForm>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedLabel = useMemo(() => {
    if (!selected) return "처리 대상을 선택하세요";
    return `${selected.evaluationYear} / 담당자 ${selected.handlerUserId ?? "공통"} / 업적 ${selected.achievementId}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await achievementVerificationApi.listAchievementVerificationTargets({
          ...filters,
          page,
          size: pageSize,
        });
      setTargets(response.data?.targets ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectTarget = (target: AchievementVerificationTarget) => {
    setSelected(target);
    setForm({ ...initialForm, evidenceRef: target.evidenceRef ?? "" });
    setFieldErrors({});
    setSuccessMessage(null);
  };

  const save = async () => {
    const clientErrors = validateForm(form);
    if (!selected) clientErrors.targetId = "목록에서 처리 대상을 선택하세요.";
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(`${selectedLabel} 담당자 인증 처리를 저장하시겠습니까?`)
    )
      return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await achievementVerificationApi.saveAchievementVerificationTargetsTransition(
        selected!.achievementId,
        {
          actionType: form.actionType,
          reasonCode: form.reasonCode.trim() || undefined,
          opinion: form.opinion.trim() || undefined,
          evidenceRef: form.evidenceRef.trim() || undefined,
        },
      );
      setSuccessMessage("처리되었습니다");
      await load();
      setSelected(null);
      setForm(initialForm);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const exportRows = () => {
    downloadCsv("achievement-verifications.csv", targets, [
      { header: "평가연도", value: (row) => row.evaluationYear },
      { header: "업적ID", value: (row) => row.achievementId },
      { header: "담당자", value: (row) => row.handlerUserId },
      { header: "처리구분", value: (row) => actionLabel(row.actionType) },
      { header: "상태", value: (row) => statusLabel(row.nextStatus) },
      { header: "의견", value: (row) => row.opinion },
      { header: "근거", value: (row) => row.evidenceRef },
      { header: "처리자", value: (row) => row.processedBy },
      { header: "처리일시", value: (row) => row.processedAt },
    ]);
    setSuccessMessage("엑셀 다운로드 파일을 생성했습니다");
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
        : "담당자 인증 대상을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-ACHIEVEMENT-VERIFICATION-MGMT"
        data-testid="achievement-verification-page"
      >
        <PermissionState
          title="담당자 인증 관리 권한이 없습니다"
          message="R09 임시 운영 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-ACHIEVEMENT-VERIFICATION-MGMT"
      data-testid="achievement-verification-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 확인·승인 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              담당자 인증 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              담당 범위의 학과장확인 실적을 조회하고 인증, 인증반려, 인증취소
              처리를 수행합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={exportRows}
              data-testid="achievement-verification-excel-button"
            >
              <Download size={16} />
              엑셀 다운로드
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void load()}
              data-testid="achievement-verification-refresh-button"
            >
              <RefreshCw size={16} />
              새로고침
            </button>
          </div>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 담당자 인증 대상 목록을 재조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="담당자 인증 처리 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-4">
          <TextInput
            label="평가연도"
            value={filters.evaluationYear}
            onChange={(value) =>
              setFilters({ ...filters, evaluationYear: value })
            }
            testId="achievement-verification-year-input"
          />
          <TextInput
            label="평가영역"
            value={filters.areaCode}
            onChange={(value) => setFilters({ ...filters, areaCode: value })}
            testId="achievement-verification-area-input"
          />
          <TextInput
            label="인증상태"
            value={filters.verificationStatus}
            onChange={(value) =>
              setFilters({ ...filters, verificationStatus: value })
            }
            testId="achievement-verification-status-input"
          />
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="achievement-verification-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>

      <section className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-8">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">인증 대상 목록</h2>
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as PageSize);
                  setPage(0);
                }}
                data-testid="achievement-verification-size-select"
              >
                <option value={20}>20건</option>
                <option value={50}>50건</option>
                <option value={100}>100건</option>
              </select>
            </label>
          </div>
          {loading ? (
            <LoadingState
              title="조회 중"
              message="담당자 인증 대상 목록을 불러오고 있습니다."
            />
          ) : targets.length === 0 ? (
            <EmptyState
              title="조회된 담당자 인증 대상이 없습니다"
              message="검색조건을 변경한 뒤 다시 조회하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">업적ID</th>
                    <th className="px-3 py-2">평가연도</th>
                    <th className="px-3 py-2">처리구분</th>
                    <th className="px-3 py-2">상태</th>
                    <th className="px-3 py-2">근거</th>
                    <th className="px-3 py-2">처리일시</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {targets.map((target) => (
                    <tr
                      key={`${target.verificationId}-${target.achievementId}`}
                      className={
                        selected?.achievementId === target.achievementId
                          ? "bg-lightprimary"
                          : "hover:bg-lightsecondary"
                      }
                      onClick={() => selectTarget(target)}
                      data-testid="achievement-verification-row"
                    >
                      <td className="px-3 py-2 font-semibold text-link">
                        {target.achievementId}
                      </td>
                      <td className="px-3 py-2">{target.evaluationYear}</td>
                      <td className="px-3 py-2">
                        {actionLabel(target.actionType)}
                      </td>
                      <td className="px-3 py-2">
                        {statusLabel(target.nextStatus)}
                      </td>
                      <td className="px-3 py-2">{target.evidenceRef ?? "-"}</td>
                      <td className="px-3 py-2">{target.processedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="mt-4 flex justify-between text-sm text-muted">
            <span>총 {totalElements}건</span>
            <div className="flex gap-2">
              <button
                type="button"
                className="rounded-md border border-ld px-3 py-1"
                disabled={page === 0}
                onClick={() => setPage(Math.max(0, page - 1))}
                data-testid="achievement-verification-prev-button"
              >
                이전
              </button>
              <button
                type="button"
                className="rounded-md border border-ld px-3 py-1"
                disabled={(page + 1) * pageSize >= totalElements}
                onClick={() => setPage(page + 1)}
                data-testid="achievement-verification-next-button"
              >
                다음
              </button>
            </div>
          </div>
        </div>

        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4">
          <h2 className="text-lg font-semibold text-dark">처리 입력</h2>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          {fieldErrors.targetId ? (
            <FieldError message={fieldErrors.targetId} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            처리구분<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.actionType}
              onChange={(event) =>
                setForm({
                  ...form,
                  actionType: event.target.value as VerificationAction,
                })
              }
              data-testid="achievement-verification-action-select"
            >
              <option value="CERTIFY">인증</option>
              <option value="RETURN">인증반려</option>
              <option value="CANCEL_CERTIFICATION">인증취소</option>
            </select>
          </label>
          {fieldErrors.actionType ? (
            <FieldError message={fieldErrors.actionType} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            처리 근거<span className="ms-1 text-error">*</span>
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.evidenceRef}
              onChange={(event) =>
                setForm({ ...form, evidenceRef: event.target.value })
              }
              data-testid="achievement-verification-evidence-input"
            />
          </label>
          {fieldErrors.evidenceRef ? (
            <FieldError message={fieldErrors.evidenceRef} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            반려 사유
            {form.actionType === "RETURN" ? (
              <span className="ms-1 text-error">*</span>
            ) : null}
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.reasonCode}
              onChange={(event) =>
                setForm({ ...form, reasonCode: event.target.value })
              }
              data-testid="achievement-verification-reason-input"
            />
          </label>
          {fieldErrors.reasonCode ? (
            <FieldError message={fieldErrors.reasonCode} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            의견
            {form.actionType === "RETURN" ? (
              <span className="ms-1 text-error">*</span>
            ) : null}
            <textarea
              className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.opinion}
              onChange={(event) =>
                setForm({ ...form, opinion: event.target.value })
              }
              data-testid="achievement-verification-opinion-textarea"
            />
          </label>
          {fieldErrors.opinion ? (
            <FieldError message={fieldErrors.opinion} />
          ) : null}
          <button
            type="button"
            className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            disabled={saving}
            onClick={() => void save()}
            data-testid="achievement-verification-save-button"
          >
            <Save size={16} />
            {saving ? "처리 중" : "저장"}
          </button>
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
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
    </label>
  );
}

function FieldError({ message }: { message: string }) {
  return <p className="mt-1 text-xs text-error">{message}</p>;
}

function validateForm(form: TransitionForm) {
  const errors: Record<string, string> = {};
  if (!form.actionType) errors.actionType = "처리구분을 선택하세요.";
  if (!form.evidenceRef.trim()) errors.evidenceRef = "처리 근거를 입력하세요.";
  if (form.actionType === "RETURN") {
    if (!form.reasonCode.trim())
      errors.reasonCode = "인증반려 사유를 입력하세요.";
    if (!form.opinion.trim()) errors.opinion = "인증반려 의견을 입력하세요.";
  }
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function actionLabel(actionType: string) {
  const labels: Record<string, string> = {
    CERTIFY: "인증",
    RETURN: "인증반려",
    CANCEL_CERTIFICATION: "인증취소",
  };
  return labels[actionType] ?? actionType;
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    DEPARTMENT_CONFIRMED: "학과장확인",
    CERTIFIED: "인증",
    CERTIFICATION_RETURNED: "인증반려",
  };
  return labels[status] ?? status;
}
