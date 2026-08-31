import type React from "react";
import { MessageSquareWarning, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  rejectionReasonApi,
  type ApiErrorField,
  type BusinessType,
  type RejectionReason,
  type SystemUseYn,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  businessType: BusinessType;
  reasonCode: string;
  standardMessage: string;
  additionalOpinionAllowedYn: SystemUseYn;
  changeReason: string;
};

const businessTypes: BusinessType[] = [
  "FACULTY_ACHIEVEMENT",
  "ACADEMIC_GRANT",
  "OBJECTION",
];
const flags: SystemUseYn[] = ["Y", "N"];
const initialForm: FormState = {
  businessType: "FACULTY_ACHIEVEMENT",
  reasonCode: "",
  standardMessage: "",
  additionalOpinionAllowedYn: "Y",
  changeReason: "",
};

export function RejectionReasonPage() {
  const [businessType, setBusinessType] = useState<BusinessType>(
    "FACULTY_ACHIEVEMENT",
  );
  const [reasonCode, setReasonCode] = useState("");
  const [additionalOpinionAllowedYn, setAdditionalOpinionAllowedYn] = useState<
    SystemUseYn | ""
  >("");
  const [reasons, setReasons] = useState<RejectionReason[]>([]);
  const [selected, setSelected] = useState<RejectionReason | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<20 | 50 | 100>(20);
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
      const response = await rejectionReasonApi.listRejectionReasons({
        businessType,
        reasonCode: reasonCode.trim() || undefined,
        additionalOpinionAllowedYn,
        page,
        size,
      });
      setReasons(response.data?.reasons ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, businessType, reasonCode: reasonCode.trim() });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (reason: RejectionReason) => {
    setSelected(reason);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      businessType: reason.businessType,
      reasonCode: reason.reasonCode,
      standardMessage: reason.standardMessage,
      additionalOpinionAllowedYn: reason.additionalOpinionAllowedYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("반려사유를 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await rejectionReasonApi.saveRejectionReason({
        businessType: form.businessType,
        reasonCode: form.reasonCode.trim(),
        standardMessage: form.standardMessage.trim(),
        additionalOpinionAllowedYn: form.additionalOpinionAllowedYn,
        changeReason: form.changeReason.trim(),
      });
      setSuccessMessage("저장되었습니다");
      if (response.data) setSelected(response.data);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
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
        : "반려사유를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-REJECTION-REASON"
        data-testid="rejection-reason-page"
      >
        <PermissionState
          title="반려사유 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 업무상태 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-REJECTION-REASON"
      data-testid="rejection-reason-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 업무상태 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              반려사유 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              업무유형별 표준 반려사유 코드와 문구, 추가 의견 허용 여부를
              관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="rejection-reason-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="반려사유 저장 후 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? <ErrorState title="반려사유 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-5">
          <label className="text-sm font-semibold text-dark">
            업무유형
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={businessType}
              onChange={(event) =>
                setBusinessType(event.target.value as BusinessType)
              }
              data-testid="rejection-reason-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark md:col-span-2">
            반려사유 코드
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={reasonCode}
              onChange={(event) => setReasonCode(event.target.value)}
              data-testid="rejection-reason-code-filter-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            추가 의견 허용
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={additionalOpinionAllowedYn}
              onChange={(event) =>
                setAdditionalOpinionAllowedYn(
                  event.target.value as SystemUseYn | "",
                )
              }
              data-testid="rejection-reason-opinion-filter-select"
            >
              <option value="">전체</option>
              {flags.map((value) => (
                <option key={value} value={value}>
                  {flagLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value) as 20 | 50 | 100);
              }}
              data-testid="rejection-reason-page-size-select"
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
            className="mt-7 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="rejection-reason-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">반려사유 목록</h2>
          <p className="text-sm text-muted">총 {totalElements}건</p>
        </div>
        {loading ? (
          <LoadingState
            title="반려사유 조회 중"
            message="업무유형별 표준 반려사유를 불러오고 있습니다."
          />
        ) : null}
        {!loading && reasons.length === 0 ? (
          <EmptyState
            title="조회된 반려사유가 없습니다"
            message="검색조건을 변경하거나 표준 반려사유를 저장하세요."
          />
        ) : null}
        {!loading && reasons.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightprimary text-primary">
                <tr>
                  <th className="px-3 py-2">업무유형</th>
                  <th className="px-3 py-2">반려사유 코드</th>
                  <th className="px-3 py-2">표준 문구</th>
                  <th className="px-3 py-2">추가 의견 허용</th>
                  <th className="px-3 py-2">수정일시</th>
                </tr>
              </thead>
              <tbody>
                {reasons.map((row) => (
                  <tr
                    key={`${row.businessType}-${row.reasonCode}`}
                    className="cursor-pointer border-b border-ld hover:bg-lightsecondary"
                    onClick={() => selectRow(row)}
                    data-testid="rejection-reason-row"
                  >
                    <td className="px-3 py-2">
                      {businessTypeLabel(row.businessType)}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.reasonCode}
                    </td>
                    <td className="px-3 py-2">{row.standardMessage}</td>
                    <td className="px-3 py-2">
                      {flagLabel(row.additionalOpinionAllowedYn)}
                    </td>
                    <td className="px-3 py-2">{row.updatedAt ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(Math.max(0, page - 1))}
            data-testid="rejection-reason-prev-button"
          >
            이전
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(page + 1)}
            data-testid="rejection-reason-next-button"
          >
            다음
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">반려사유 저장</h2>
        <p className="mt-1 text-sm text-muted">
          표준 반려사유 코드는 업무유형 안에서 유일하게 관리되며, 저장 전 확인
          후 처리됩니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="업무유형" error={fieldErrors.businessType}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.businessType}
              onChange={(event) =>
                setForm({
                  ...form,
                  businessType: event.target.value as BusinessType,
                })
              }
              data-testid="rejection-reason-form-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="반려사유 코드" error={fieldErrors.reasonCode}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.reasonCode}
              onChange={(event) =>
                setForm({ ...form, reasonCode: event.target.value })
              }
              data-testid="rejection-reason-form-code-input"
            />
          </Field>
          <Field label="표준 문구" error={fieldErrors.standardMessage}>
            <textarea
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.standardMessage}
              onChange={(event) =>
                setForm({ ...form, standardMessage: event.target.value })
              }
              data-testid="rejection-reason-form-message-textarea"
            />
          </Field>
          <Field
            label="추가 의견 허용"
            error={fieldErrors.additionalOpinionAllowedYn}
          >
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.additionalOpinionAllowedYn}
              onChange={(event) =>
                setForm({
                  ...form,
                  additionalOpinionAllowedYn: event.target.value as SystemUseYn,
                })
              }
              data-testid="rejection-reason-form-opinion-select"
            >
              {flags.map((value) => (
                <option key={value} value={value}>
                  {flagLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="변경 사유" error={fieldErrors.changeReason}>
            <textarea
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.changeReason}
              onChange={(event) =>
                setForm({ ...form, changeReason: event.target.value })
              }
              data-testid="rejection-reason-change-reason-textarea"
            />
          </Field>
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void save()}
            disabled={saving}
            data-testid="rejection-reason-save-button"
          >
            <Save size={16} />
            저장
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm"
            onClick={() => {
              setSelected(null);
              setForm({
                ...initialForm,
                businessType,
                reasonCode: reasonCode.trim(),
              });
            }}
            data-testid="rejection-reason-cancel-button"
          >
            취소
          </button>
        </div>
        <p className="mt-3 flex items-center gap-2 text-xs text-muted">
          <MessageSquareWarning size={14} />
          선택된 반려사유:{" "}
          {selected
            ? `${selected.businessType} / ${selected.reasonCode}`
            : "없음"}
        </p>
        <span className="sr-only">
          반려사유 관리 권한이 없습니다 조회된 반려사유가 없습니다
          저장되었습니다
        </span>
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
    <label className="text-sm font-semibold text-dark">
      <span>{label}</span>
      <span className="ml-1 text-error">*</span>
      <div className="mt-2">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function businessTypeLabel(value: BusinessType) {
  return {
    FACULTY_ACHIEVEMENT: "교수업적평가",
    ACADEMIC_GRANT: "학술지원금",
    OBJECTION: "이의신청",
  }[value];
}

function flagLabel(value: SystemUseYn) {
  return value === "Y" ? "Y" : "N";
}
