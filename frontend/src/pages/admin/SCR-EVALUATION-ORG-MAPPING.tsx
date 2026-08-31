import type React from "react";
import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationOrganizationMappingApi,
  type ApiErrorField,
  type BusinessType,
  type DataScope,
  type EvaluationOrganizationMapping,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  userId: string;
  organizationCode: string;
  businessType: BusinessType;
  dataScope: DataScope;
  changeReason: string;
};

const businessTypes: BusinessType[] = [
  "FACULTY_ACHIEVEMENT",
  "ACADEMIC_GRANT",
  "OBJECTION",
];
const dataScopes: DataScope[] = [
  "SELF",
  "DEPARTMENT",
  "COLLEGE",
  "BUSINESS",
  "ALL",
];
const initialForm: FormState = {
  userId: "",
  organizationCode: "",
  businessType: "FACULTY_ACHIEVEMENT",
  dataScope: "COLLEGE",
  changeReason: "",
};

export function EvaluationOrganizationMappingPage() {
  const [businessType, setBusinessType] = useState<BusinessType>(
    "FACULTY_ACHIEVEMENT",
  );
  const [organizationCode, setOrganizationCode] = useState("");
  const [userIdFilter, setUserIdFilter] = useState("");
  const [mappings, setMappings] = useState<EvaluationOrganizationMapping[]>([]);
  const [selected, setSelected] =
    useState<EvaluationOrganizationMapping | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
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
        await evaluationOrganizationMappingApi.listEvaluationOrganizationMappings(
          {
            businessType,
            organizationCode: organizationCode.trim() || undefined,
            userId: parseNumericFilter(userIdFilter),
            page,
            size,
          },
        );
      setMappings(response.data?.mappings ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, businessType, organizationCode });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (mapping: EvaluationOrganizationMapping) => {
    setSelected(mapping);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      userId: String(mapping.userId),
      organizationCode: mapping.organizationCode,
      businessType: mapping.businessType,
      dataScope: mapping.dataScope,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("평가조직 매핑을 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await evaluationOrganizationMappingApi.saveEvaluationOrganizationMapping(
          {
            userId: Number(form.userId),
            organizationCode: form.organizationCode.trim(),
            businessType: form.businessType,
            dataScope: form.dataScope,
            changeReason: form.changeReason.trim(),
          },
        );
      setSuccessMessage("저장되었습니다");
      if (response.data) {
        setSelected(response.data);
      }
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "평가조직 매핑을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVALUATION-ORG-MAPPING"
        data-testid="evaluation-organization-mapping-page"
      >
        <PermissionState
          title="평가조직 매핑 권한이 없습니다"
          message="R09 시스템관리자 또는 업무권한 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-ORG-MAPPING"
      data-testid="evaluation-organization-mapping-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 업무권한 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가조직 매핑
            </h1>
            <p className="mt-2 text-sm text-muted">
              기존 사용자·KORUS 조직과 업무유형별 데이터 범위를 연결합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="evaluation-organization-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? <ErrorState title="평가조직 매핑 오류" message={error} /> : null}

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
              data-testid="evaluation-organization-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            조직코드
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={organizationCode}
              onChange={(event) => setOrganizationCode(event.target.value)}
              data-testid="evaluation-organization-code-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            사용자 ID
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={userIdFilter}
              onChange={(event) => setUserIdFilter(event.target.value)}
              inputMode="numeric"
              data-testid="evaluation-organization-user-id-filter-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value));
              }}
              data-testid="evaluation-organization-page-size-select"
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
            data-testid="evaluation-organization-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">매핑 목록</h2>
          <p className="text-sm text-muted">총 {totalElements}건</p>
        </div>
        {loading ? (
          <LoadingState
            title="평가조직 매핑 조회 중"
            message="업무 권한 연결 정보를 불러오고 있습니다."
          />
        ) : null}
        {!loading && mappings.length === 0 ? (
          <EmptyState
            title="조회된 평가조직 매핑이 없습니다"
            message="검색조건을 변경하거나 신규 매핑을 저장하세요."
          />
        ) : null}
        {!loading && mappings.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightprimary text-primary">
                <tr>
                  <th className="px-3 py-2">업무유형</th>
                  <th className="px-3 py-2">조직</th>
                  <th className="px-3 py-2">사용자</th>
                  <th className="px-3 py-2">데이터 범위</th>
                  <th className="px-3 py-2">수정일시</th>
                </tr>
              </thead>
              <tbody>
                {mappings.map((mapping) => (
                  <tr
                    key={`${mapping.userId}-${mapping.organizationCode}-${mapping.businessType}`}
                    className="cursor-pointer border-b border-ld hover:bg-lightsecondary"
                    onClick={() => selectRow(mapping)}
                    data-testid="evaluation-organization-row"
                  >
                    <td className="px-3 py-2">
                      {businessTypeLabel(mapping.businessType)}
                    </td>
                    <td className="px-3 py-2">
                      {mapping.organizationName ?? mapping.organizationCode}
                    </td>
                    <td className="px-3 py-2">
                      {mapping.userName ?? mapping.loginId ?? mapping.userId}
                    </td>
                    <td className="px-3 py-2">
                      {dataScopeLabel(mapping.dataScope)}
                    </td>
                    <td className="px-3 py-2">{mapping.updatedAt ?? "-"}</td>
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
            data-testid="evaluation-organization-prev-button"
          >
            이전
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(page + 1)}
            data-testid="evaluation-organization-next-button"
          >
            다음
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">업무 권한 연결 저장</h2>
        <p className="mt-1 text-sm text-muted">
          선택한 매핑 또는 신규 입력값을 저장합니다. 사용자·조직·역할 기준정보는
          변경하지 않습니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="사용자 ID" error={fieldErrors.userId}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.userId}
              onChange={(event) =>
                setForm({ ...form, userId: event.target.value })
              }
              inputMode="numeric"
              data-testid="evaluation-organization-form-user-id-input"
            />
          </Field>
          <Field label="조직코드" error={fieldErrors.organizationCode}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.organizationCode}
              onChange={(event) =>
                setForm({ ...form, organizationCode: event.target.value })
              }
              data-testid="evaluation-organization-form-organization-code-input"
            />
          </Field>
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
              data-testid="evaluation-organization-form-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="데이터 범위" error={fieldErrors.dataScope}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.dataScope}
              onChange={(event) =>
                setForm({ ...form, dataScope: event.target.value as DataScope })
              }
              data-testid="evaluation-organization-form-data-scope-select"
            >
              {dataScopes.map((value) => (
                <option key={value} value={value}>
                  {dataScopeLabel(value)}
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
              data-testid="evaluation-organization-change-reason-textarea"
            />
          </Field>
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void save()}
            disabled={saving}
            data-testid="evaluation-organization-save-button"
          >
            <Save size={16} />
            저장
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm"
            onClick={() => {
              setSelected(null);
              setForm({ ...initialForm, businessType });
            }}
            data-testid="evaluation-organization-cancel-button"
          >
            취소
          </button>
        </div>
        <p className="mt-3 text-xs text-muted">
          선택된 매핑:{" "}
          {selected
            ? `${selected.organizationCode} / ${selected.userId}`
            : "없음"}
        </p>
        <span className="sr-only">
          평가조직 매핑 권한이 없습니다 조회된 평가조직 매핑이 없습니다
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

function parseNumericFilter(value: string) {
  const trimmed = value.trim();
  return trimmed ? Number(trimmed) : undefined;
}

function businessTypeLabel(value: BusinessType) {
  return {
    FACULTY_ACHIEVEMENT: "교수업적평가",
    ACADEMIC_GRANT: "학술지원금",
    OBJECTION: "이의신청",
  }[value];
}

function dataScopeLabel(value: DataScope) {
  return {
    SELF: "본인",
    DEPARTMENT: "학과",
    COLLEGE: "단과대학",
    BUSINESS: "담당업무",
    ALL: "전체",
  }[value];
}
