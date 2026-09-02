import type React from "react";
import { Download, RefreshCw, Save, Search, XCircle } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  objectionOpinionApi,
  type ApiErrorField,
  type ObjectionOpinionTarget,
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

type ObjectionAction = "ACCEPTED" | "REJECTED" | "NEEDS_REVIEW";

type TransitionForm = {
  actionType: ObjectionAction;
  reasonCode: string;
  opinion: string;
};

const initialForm: TransitionForm = {
  actionType: "ACCEPTED",
  reasonCode: "",
  opinion: "",
};

export function ObjectionOpinionManagementPage() {
  const [filters, setFilters] = useState({
    evaluationYear: "2026",
    decisionResult: "",
    applicant: "",
  });
  const [opinions, setOpinions] = useState<ObjectionOpinionTarget[]>([]);
  const [selected, setSelected] = useState<ObjectionOpinionTarget | null>(null);
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
    return `${selected.evaluationYear} / 이의신청 ${selected.objectionId} / 신청자 ${selected.applicantUserId}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await objectionOpinionApi.listObjectionOpinions({
        evaluationYear: filters.evaluationYear,
        decisionResult: filters.decisionResult,
        applicantName: filters.applicant,
        page,
        size: pageSize,
      });
      setOpinions(response.data?.opinions ?? []);
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

  const selectTarget = (target: ObjectionOpinionTarget) => {
    setSelected(target);
    setForm(initialForm);
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
    if (!window.confirm(`${selectedLabel} 의견 처리를 저장하시겠습니까?`))
      return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await objectionOpinionApi.saveObjectionOpinionsTransition(
        selected!.objectionId,
        {
          decisionResult: form.actionType,
          reviewerOpinion: form.opinion.trim(),
          reasonCode: form.reasonCode.trim() || undefined,
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

  const cancel = () => {
    setSelected(null);
    setForm(initialForm);
    setFieldErrors({});
    setSuccessMessage(null);
  };

  const exportRows = () => {
    downloadCsv("objection-opinions.csv", opinions, [
      { header: "평가연도", value: (row) => row.evaluationYear },
      { header: "이의신청번호", value: (row) => row.objectionId },
      { header: "신청자", value: (row) => row.applicantUserId },
      { header: "신청자 의견", value: (row) => row.applicantOpinionSnapshot },
      { header: "이의신청 내용", value: (row) => row.objectionContentSnapshot },
      { header: "검토자 의견", value: (row) => row.reviewerOpinion },
      { header: "결정결과", value: (row) => statusLabel(row.decisionResult) },
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
        : "이의신청 의견을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-OBJECTION-OPINION-MGMT"
        data-testid="objection-opinion-page"
      >
        <PermissionState
          title="이의신청 의견 관리 권한이 없습니다"
          message="R09 임시 운영 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-OBJECTION-OPINION-MGMT"
      data-testid="objection-opinion-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 의견·반려 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              이의신청 의견 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              이의신청 내용과 신청자 의견 snapshot을 조회하고 검토자 의견과
              인용·기각·추가검토 결정을 기록합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={exportRows}
              data-testid="objection-opinion-excel-button"
            >
              <Download size={16} />
              엑셀 다운로드
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void load()}
              data-testid="objection-opinion-refresh-button"
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
          message="저장 후 이의신청 의견 목록을 재조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="이의신청 의견 처리 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-4">
          <TextInput
            label="평가연도"
            value={filters.evaluationYear}
            onChange={(value) =>
              setFilters({ ...filters, evaluationYear: value })
            }
            testId="objection-opinion-year-input"
          />
          <TextInput
            label="결정결과"
            value={filters.decisionResult}
            onChange={(value) =>
              setFilters({ ...filters, decisionResult: value })
            }
            testId="objection-opinion-status-input"
          />
          <TextInput
            label="신청자/이의신청번호"
            value={filters.applicant}
            onChange={(value) => setFilters({ ...filters, applicant: value })}
            testId="objection-opinion-applicant-input"
          />
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="objection-opinion-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>

      <section className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-8">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">
              이의신청 대상 목록
            </h2>
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as PageSize);
                  setPage(0);
                }}
                data-testid="objection-opinion-size-select"
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
              message="이의신청 의견 목록을 불러오고 있습니다."
            />
          ) : opinions.length === 0 ? (
            <EmptyState
              title="조회된 이의신청 의견 대상이 없습니다"
              message="검색조건을 변경한 뒤 다시 조회하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">이의신청번호</th>
                    <th className="px-3 py-2">신청자</th>
                    <th className="px-3 py-2">신청자 의견</th>
                    <th className="px-3 py-2">이의신청 내용</th>
                    <th className="px-3 py-2">결정결과</th>
                    <th className="px-3 py-2">검토자 의견</th>
                    <th className="px-3 py-2">처리일시</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {opinions.map((target) => (
                    <tr
                      key={`${target.objectionOpinionId}-${target.objectionId}`}
                      className={
                        selected?.objectionId === target.objectionId
                          ? "bg-lightprimary"
                          : "hover:bg-lightsecondary"
                      }
                      onClick={() => selectTarget(target)}
                      data-testid="objection-opinion-row"
                    >
                      <td className="px-3 py-2 font-semibold text-link">
                        {target.objectionId}
                      </td>
                      <td className="px-3 py-2">{target.applicantUserId}</td>
                      <td className="px-3 py-2">
                        {target.applicantOpinionSnapshot}
                      </td>
                      <td className="px-3 py-2">
                        {target.objectionContentSnapshot}
                      </td>
                      <td className="px-3 py-2">
                        {statusLabel(target.decisionResult)}
                      </td>
                      <td className="px-3 py-2">{target.reviewerOpinion}</td>
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
                data-testid="objection-opinion-prev-button"
              >
                이전
              </button>
              <button
                type="button"
                className="rounded-md border border-ld px-3 py-1"
                disabled={(page + 1) * pageSize >= totalElements}
                onClick={() => setPage(page + 1)}
                data-testid="objection-opinion-next-button"
              >
                다음
              </button>
            </div>
          </div>
        </div>

        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4">
          <h2 className="text-lg font-semibold text-dark">의견 처리</h2>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          <p className="mt-3 rounded-md bg-lightsecondary p-3 text-xs text-muted">
            이 화면은 검토자 의견과 결정 결과만 기록합니다. 접수기간·처리담당자
            설정과 원평가 점수 직접 변경은 수행하지 않습니다.
          </p>
          {fieldErrors.targetId ? (
            <FieldError message={fieldErrors.targetId} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            결정결과<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.actionType}
              onChange={(event) =>
                setForm({
                  ...form,
                  actionType: event.target.value as ObjectionAction,
                })
              }
              data-testid="objection-opinion-action-select"
            >
              <option value="ACCEPTED">인용</option>
              <option value="REJECTED">기각</option>
              <option value="NEEDS_REVIEW">추가검토</option>
            </select>
          </label>
          {fieldErrors.actionType ? (
            <FieldError message={fieldErrors.actionType} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            기각 사유
            {form.actionType === "REJECTED" ? (
              <span className="ms-1 text-error">*</span>
            ) : null}
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.reasonCode}
              onChange={(event) =>
                setForm({ ...form, reasonCode: event.target.value })
              }
              data-testid="objection-opinion-reason-input"
            />
          </label>
          {fieldErrors.reasonCode ? (
            <FieldError message={fieldErrors.reasonCode} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            검토자 의견<span className="ms-1 text-error">*</span>
            <textarea
              className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.opinion}
              onChange={(event) =>
                setForm({ ...form, opinion: event.target.value })
              }
              data-testid="objection-opinion-textarea"
            />
          </label>
          {fieldErrors.opinion ? (
            <FieldError message={fieldErrors.opinion} />
          ) : null}
          <div className="mt-5 flex gap-2">
            <button
              type="button"
              className="inline-flex flex-1 items-center justify-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
              onClick={cancel}
              data-testid="objection-opinion-cancel-button"
            >
              <XCircle size={16} />
              취소
            </button>
            <button
              type="button"
              className="inline-flex flex-1 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              disabled={saving}
              onClick={() => void save()}
              data-testid="objection-opinion-save-button"
            >
              <Save size={16} />
              {saving ? "처리 중" : "처리"}
            </button>
          </div>
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
  if (!form.actionType) errors.actionType = "결정결과를 선택하세요.";
  if (!form.opinion.trim()) errors.opinion = "검토자 의견을 입력하세요.";
  if (form.actionType === "REJECTED" && !form.reasonCode.trim()) {
    errors.reasonCode = "기각 사유를 입력하세요.";
  }
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    ACCEPTED: "인용",
    REJECTED: "기각",
    NEEDS_REVIEW: "추가검토",
  };
  return labels[status] ?? status;
}
