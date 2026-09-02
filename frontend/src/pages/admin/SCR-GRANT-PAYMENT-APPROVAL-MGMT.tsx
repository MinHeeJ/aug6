import type React from "react";
import { Download, RefreshCw, Save, Search, XCircle } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  grantPaymentApprovalApi,
  type ApiErrorField,
  type GrantPaymentApprovalTarget,
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

type ApprovalAction = "APPROVE" | "REJECT" | "CANCEL_APPROVAL";

type TransitionForm = {
  actionType: ApprovalAction;
  reasonCode: string;
  opinion: string;
};

const initialForm: TransitionForm = {
  actionType: "APPROVE",
  reasonCode: "",
  opinion: "",
};

export function GrantPaymentApprovalManagementPage() {
  const [filters, setFilters] = useState({
    evaluationYear: "2026",
    approvalStatus: "",
    applicant: "",
  });
  const [approvals, setApprovals] = useState<GrantPaymentApprovalTarget[]>([]);
  const [selected, setSelected] = useState<GrantPaymentApprovalTarget | null>(
    null,
  );
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
    return `${selected.evaluationYear} / 신청 ${selected.grantApplicationId} / 업적 ${selected.linkedAchievementId ?? "미연계"}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await grantPaymentApprovalApi.listGrantPaymentApprovals({
        evaluationYear: filters.evaluationYear,
        approvalStatus: filters.approvalStatus,
        applicantName: filters.applicant,
        page,
        size: pageSize,
      });
      setApprovals(response.data?.approvals ?? []);
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

  const selectTarget = (target: GrantPaymentApprovalTarget) => {
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
    if (!window.confirm(`${selectedLabel} 지급승인 처리를 저장하시겠습니까?`))
      return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await grantPaymentApprovalApi.saveGrantPaymentApprovalsTransition(
        selected!.grantApplicationId,
        {
          actionType: form.actionType,
          reasonCode: form.reasonCode.trim() || undefined,
          opinion: form.opinion.trim() || undefined,
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
    downloadCsv("grant-payment-approvals.csv", approvals, [
      { header: "평가연도", value: (row) => row.evaluationYear },
      { header: "신청번호", value: (row) => row.grantApplicationId },
      { header: "신청금액", value: (row) => row.requestedAmountSnapshot },
      { header: "지급금액", value: (row) => row.paymentAmountSnapshot },
      { header: "계좌정보", value: (row) => row.accountSnapshotRef },
      { header: "업적연계정보", value: (row) => row.linkedAchievementId },
      { header: "승인상태", value: (row) => statusLabel(row.approvalStatus) },
      { header: "의견", value: (row) => row.opinion },
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
        : "지급승인 대상을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-GRANT-PAYMENT-APPROVAL-MGMT"
        data-testid="grant-payment-approval-page"
      >
        <PermissionState
          title="지급승인 관리 권한이 없습니다"
          message="R09 임시 운영 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-GRANT-PAYMENT-APPROVAL-MGMT"
      data-testid="grant-payment-approval-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 확인·승인 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              지급승인 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              학술지원금 신청의 신청금액, 지급금액, 계좌정보, 업적연계정보를
              확인하고 승인·반려·승인취소를 처리합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={exportRows}
              data-testid="grant-payment-approval-excel-button"
            >
              <Download size={16} />
              엑셀 다운로드
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void load()}
              data-testid="grant-payment-approval-refresh-button"
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
          message="저장 후 지급승인 대상 목록을 재조회했습니다."
        />
      ) : null}
      {error ? <ErrorState title="지급승인 처리 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-4">
          <TextInput
            label="평가연도"
            value={filters.evaluationYear}
            onChange={(value) =>
              setFilters({ ...filters, evaluationYear: value })
            }
            testId="grant-payment-approval-year-input"
          />
          <TextInput
            label="승인상태"
            value={filters.approvalStatus}
            onChange={(value) =>
              setFilters({ ...filters, approvalStatus: value })
            }
            testId="grant-payment-approval-status-input"
          />
          <TextInput
            label="신청자/신청번호"
            value={filters.applicant}
            onChange={(value) => setFilters({ ...filters, applicant: value })}
            testId="grant-payment-approval-applicant-input"
          />
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="grant-payment-approval-search-button"
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
              승인대상 신청 목록
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
                data-testid="grant-payment-approval-size-select"
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
              message="지급승인 대상 목록을 불러오고 있습니다."
            />
          ) : approvals.length === 0 ? (
            <EmptyState
              title="조회된 지급승인 대상이 없습니다"
              message="검색조건을 변경한 뒤 다시 조회하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">신청번호</th>
                    <th className="px-3 py-2">신청금액</th>
                    <th className="px-3 py-2">지급금액</th>
                    <th className="px-3 py-2">계좌정보</th>
                    <th className="px-3 py-2">업적연계정보</th>
                    <th className="px-3 py-2">승인상태</th>
                    <th className="px-3 py-2">처리일시</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {approvals.map((target) => (
                    <tr
                      key={`${target.approvalId}-${target.grantApplicationId}`}
                      className={
                        selected?.grantApplicationId ===
                        target.grantApplicationId
                          ? "bg-lightprimary"
                          : "hover:bg-lightsecondary"
                      }
                      onClick={() => selectTarget(target)}
                      data-testid="grant-payment-approval-row"
                    >
                      <td className="px-3 py-2 font-semibold text-link">
                        {target.grantApplicationId}
                      </td>
                      <td className="px-3 py-2">
                        {formatMoney(target.requestedAmountSnapshot)}
                      </td>
                      <td className="px-3 py-2">
                        {formatMoney(target.paymentAmountSnapshot)}
                      </td>
                      <td className="px-3 py-2">{target.accountSnapshotRef}</td>
                      <td className="px-3 py-2">
                        {target.linkedAchievementId ?? "-"}
                      </td>
                      <td className="px-3 py-2">
                        {statusLabel(target.approvalStatus)}
                      </td>
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
                data-testid="grant-payment-approval-prev-button"
              >
                이전
              </button>
              <button
                type="button"
                className="rounded-md border border-ld px-3 py-1"
                disabled={(page + 1) * pageSize >= totalElements}
                onClick={() => setPage(page + 1)}
                data-testid="grant-payment-approval-next-button"
              >
                다음
              </button>
            </div>
          </div>
        </div>

        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4">
          <h2 className="text-lg font-semibold text-dark">승인 처리</h2>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          <p className="mt-3 rounded-md bg-lightsecondary p-3 text-xs text-muted">
            실제 계좌이체·회계전표·예산집행은 실행하지 않습니다. 지원금
            신청정보와 대상 업적은 이 화면에서 수정하지 않습니다.
          </p>
          {fieldErrors.targetId ? (
            <FieldError message={fieldErrors.targetId} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            승인상태<span className="ms-1 text-error">*</span>
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.actionType}
              onChange={(event) =>
                setForm({
                  ...form,
                  actionType: event.target.value as ApprovalAction,
                })
              }
              data-testid="grant-payment-approval-action-select"
            >
              <option value="APPROVE">승인</option>
              <option value="REJECT">반려</option>
              <option value="CANCEL_APPROVAL">승인취소</option>
            </select>
          </label>
          {fieldErrors.actionType ? (
            <FieldError message={fieldErrors.actionType} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            반려 사유
            {form.actionType === "REJECT" ? (
              <span className="ms-1 text-error">*</span>
            ) : null}
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.reasonCode}
              onChange={(event) =>
                setForm({ ...form, reasonCode: event.target.value })
              }
              data-testid="grant-payment-approval-reason-input"
            />
          </label>
          {fieldErrors.reasonCode ? (
            <FieldError message={fieldErrors.reasonCode} />
          ) : null}
          <label className="mt-4 block text-sm font-semibold text-dark">
            처리 의견
            {form.actionType === "REJECT" ? (
              <span className="ms-1 text-error">*</span>
            ) : null}
            <textarea
              className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.opinion}
              onChange={(event) =>
                setForm({ ...form, opinion: event.target.value })
              }
              data-testid="grant-payment-approval-opinion-textarea"
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
              data-testid="grant-payment-approval-cancel-button"
            >
              <XCircle size={16} />
              취소
            </button>
            <button
              type="button"
              className="inline-flex flex-1 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              disabled={saving}
              onClick={() => void save()}
              data-testid="grant-payment-approval-save-button"
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
  if (!form.actionType) errors.actionType = "승인상태를 선택하세요.";
  if (form.actionType === "REJECT") {
    if (!form.reasonCode.trim())
      errors.reasonCode = "지급반려 사유를 입력하세요.";
    if (!form.opinion.trim()) errors.opinion = "지급반려 의견을 입력하세요.";
  }
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("ko-KR").format(value);
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    APPROVED: "지급승인",
    REJECTED: "지급반려",
    APPROVAL_CANCELLED: "승인취소",
  };
  return labels[status] ?? status;
}
