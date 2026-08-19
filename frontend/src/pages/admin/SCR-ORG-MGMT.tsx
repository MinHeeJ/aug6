import type React from "react";
import { Building2, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  organizationApi,
  type ApiErrorField,
  type Organization,
  type OrganizationTreeNode,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  parentOrganizationCode: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  changeReason: string;
};

const emptyForm: FormState = {
  parentOrganizationCode: "",
  effectiveStartDate: "",
  effectiveEndDate: "",
  changeReason: "",
};

export function OrganizationManagementPage() {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [tree, setTree] = useState<OrganizationTreeNode[]>([]);
  const [filter, setFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [selected, setSelected] = useState<Organization | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const visibleOrganizations = useMemo(() => {
    if (!typeFilter) return organizations;
    return organizations.filter((item) => item.organizationType === typeFilter);
  }, [organizations, typeFilter]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const [organizationResponse, treeResponse] = await Promise.all([
        organizationApi.searchOrganizations({
          organizationCodeFilter: filter,
          page: 0,
          size: 10,
        }),
        organizationApi.getOrganizationTree(),
      ]);
      setOrganizations(organizationResponse.data ?? []);
      setTree(treeResponse.data ?? []);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const selectOrganization = (organization: Organization) => {
    setSelected(organization);
    setFieldErrors({});
    setForm({
      parentOrganizationCode: organization.parentOrganizationCode ?? "",
      effectiveStartDate: organization.effectiveStartDate ?? "",
      effectiveEndDate: organization.effectiveEndDate ?? "",
      changeReason: "",
    });
  };

  const resetForm = () => {
    if (selected) {
      selectOrganization(selected);
    } else {
      setForm(emptyForm);
    }
  };

  const save = async () => {
    if (!selected) return;
    const confirmed = window.confirm(
      `${selected.organizationCode} 조직의 상위관계를 저장하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await organizationApi.saveOrganizationParentRelation(
        selected.organizationCode,
        {
          parentOrganizationCode: form.parentOrganizationCode,
          effectiveStartDate: form.effectiveStartDate,
          effectiveEndDate: form.effectiveEndDate || null,
          changeReason: form.changeReason,
        },
      );
      setSuccessMessage("조직 관계가 저장되었습니다");
      const updated = response.data ?? selected;
      setSelected(updated);
      setForm({
        parentOrganizationCode:
          updated.parentOrganizationCode ?? form.parentOrganizationCode,
        effectiveStartDate:
          updated.effectiveStartDate ?? form.effectiveStartDate,
        effectiveEndDate: updated.effectiveEndDate ?? form.effectiveEndDate,
        changeReason: "",
      });
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
        return;
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "조직 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <PermissionState
        title="조직 관리 권한이 없습니다"
        message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
      />
    );
  }

  return (
    <section className="space-y-6" data-screen-id="SCR-ORG-MGMT">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 사용자·조직 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">조직 관리</h1>
            <p className="mt-2 text-sm text-muted">
              대학·대학원·단과대학·학과·부서의 상하위 관계와 적용기간을
              관리합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      {error ? <ErrorState title="조직 처리 오류" message={error} /> : null}
      {successMessage ? (
        <SuccessState title="처리 완료" message={successMessage} />
      ) : null}

      <div className="rounded-md border border-ld bg-white p-6 shadow-md">
        <h2 className="card-title mb-4 text-lg font-semibold text-dark">
          검색조건
        </h2>
        <div className="grid grid-cols-12 gap-5 md:gap-6">
          <label className="col-span-12 text-sm font-semibold text-ld md:col-span-5">
            조직코드
            <div className="relative mt-2">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 pl-10 text-sm focus-visible:border-primary focus-visible:outline-0"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
                placeholder="조직코드"
              />
            </div>
          </label>
          <label className="col-span-12 text-sm font-semibold text-ld md:col-span-4">
            조직유형
            <select
              className="mt-2 h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value)}
            >
              <option value="">전체</option>
              <option value="UNIVERSITY">대학</option>
              <option value="GRADUATE_SCHOOL">대학원</option>
              <option value="COLLEGE">단과대학</option>
              <option value="DEPARTMENT">학과</option>
              <option value="OFFICE">부서</option>
            </select>
          </label>
          <div className="col-span-12 flex items-end gap-3 md:col-span-3">
            <button
              className="h-10 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void load()}
              type="button"
            >
              조회
            </button>
            <button
              className="h-10 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              onClick={() => {
                setFilter("");
                setTypeFilter("");
              }}
              type="button"
            >
              초기화
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <LoadingState
          title="조직 정보를 불러오는 중입니다"
          message="조직 목록과 계층을 조회하고 있습니다."
        />
      ) : null}

      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-5">
          <h2 className="card-title mb-4 flex items-center gap-2 text-lg font-semibold text-dark">
            <Building2 size={18} /> 조직 목록
          </h2>
          <div className="overflow-x-auto rounded-md border border-border">
            <table className="w-full caption-bottom text-sm">
              <thead className="border-b border-ld bg-lightgray">
                <tr>
                  <th className="px-4 py-3 text-left">조직코드</th>
                  <th className="px-4 py-3 text-left">조직명</th>
                  <th className="px-4 py-3 text-left">조직유형</th>
                  <th className="px-4 py-3 text-left">사용여부</th>
                  <th className="px-4 py-3 text-left">상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {visibleOrganizations.map((organization) => (
                  <tr
                    key={organization.organizationCode}
                    className={`cursor-pointer transition-colors hover:bg-lightprimary ${selected?.organizationCode === organization.organizationCode ? "bg-lightprimary" : ""}`}
                    onClick={() => selectOrganization(organization)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 font-semibold text-dark">
                      {organization.organizationCode}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {organization.organizationName}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {organization.organizationType}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {organization.systemUseYn}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <span className="rounded-full bg-lightsuccess px-2.5 py-0.5 text-xs font-semibold text-success">
                        {organization.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && visibleOrganizations.length === 0 ? (
            <EmptyState
              title="조회된 조직이 없습니다"
              message="조직코드 검색조건을 변경해 다시 조회하세요."
            />
          ) : null}
        </div>

        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-3">
          <h2 className="card-title mb-4 text-lg font-semibold text-dark">
            조직 계층
          </h2>
          {tree.length === 0 && !loading ? (
            <EmptyState
              title="계층 정보 없음"
              message="표시할 상하위 조직 관계가 없습니다."
            />
          ) : (
            <Tree
              nodes={tree}
              selectedCode={selected?.organizationCode}
              onSelect={(code) => {
                const found = organizations.find(
                  (item) => item.organizationCode === code,
                );
                if (found) selectOrganization(found);
              }}
            />
          )}
          <p className="mt-4 text-xs text-muted">
            OQ-UI-031: 이력 조회 전용 route는 확정 전 제공하지 않습니다.
          </p>
        </div>

        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-4">
          <h2 className="card-title mb-4 text-lg font-semibold text-dark">
            선택 조직 관계 편집
          </h2>
          {!selected ? (
            <div className="space-y-4">
              <EmptyState
                title="조직을 선택하세요"
                message="목록 또는 계층에서 조직을 선택하면 상위관계를 편집할 수 있습니다."
              />
              <div className="flex gap-3">
                <button
                  className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled
                  type="button"
                >
                  상위관계 저장
                </button>
                <button
                  className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
                  disabled
                  type="button"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <Readonly label="조직코드" value={selected.organizationCode} />
              <Readonly label="조직명" value={selected.organizationName} />
              <Readonly label="조직유형" value={selected.organizationType} />
              <Field
                label="상위조직코드"
                error={fieldErrors.parentOrganizationCode}
                required
              >
                <input
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.parentOrganizationCode}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      parentOrganizationCode: event.target.value,
                    })
                  }
                />
              </Field>
              <Field
                label="적용 시작일"
                error={fieldErrors.effectiveStartDate}
                required
              >
                <input
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  type="date"
                  value={form.effectiveStartDate}
                  onChange={(event) =>
                    setForm({ ...form, effectiveStartDate: event.target.value })
                  }
                />
              </Field>
              <Field label="적용 종료일" error={fieldErrors.effectiveEndDate}>
                <input
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  type="date"
                  value={form.effectiveEndDate}
                  onChange={(event) =>
                    setForm({ ...form, effectiveEndDate: event.target.value })
                  }
                />
              </Field>
              <Field
                label="변경 사유"
                error={fieldErrors.changeReason}
                required
              >
                <textarea
                  className="min-h-24 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.changeReason}
                  onChange={(event) =>
                    setForm({ ...form, changeReason: event.target.value })
                  }
                />
              </Field>
              <div className="flex gap-3">
                <button
                  className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled={saving}
                  onClick={() => void save()}
                  type="button"
                >
                  상위관계 저장
                </button>
                <button
                  className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
                  onClick={resetForm}
                  type="button"
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

function Tree({
  nodes,
  selectedCode,
  onSelect,
}: {
  nodes: OrganizationTreeNode[];
  selectedCode?: string;
  onSelect: (code: string) => void;
}) {
  return (
    <ul className="space-y-2">
      {nodes.map((node) => (
        <TreeNode
          key={node.organizationCode}
          node={node}
          selectedCode={selectedCode}
          onSelect={onSelect}
        />
      ))}
    </ul>
  );
}

function TreeNode({
  node,
  selectedCode,
  onSelect,
}: {
  node: OrganizationTreeNode;
  selectedCode?: string;
  onSelect: (code: string) => void;
}) {
  return (
    <li>
      <button
        className={`w-full rounded-md px-3 py-2 text-left text-sm hover:bg-lightprimary ${selectedCode === node.organizationCode ? "bg-primary text-white" : "text-link"}`}
        onClick={() => onSelect(node.organizationCode)}
        type="button"
      >
        {node.organizationName}
      </button>
      {node.children.length ? (
        <div className="ml-4 mt-2 border-l border-ld pl-3">
          <Tree
            nodes={node.children}
            selectedCode={selectedCode}
            onSelect={onSelect}
          />
        </div>
      ) : null}
    </li>
  );
}

function Field({
  label,
  error,
  required,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold text-ld">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <div className="mt-2">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function Readonly({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold text-muted">{label}</p>
      <p className="mt-1 rounded-lg bg-lightgray px-3 py-2 text-sm text-dark">
        {value}
      </p>
    </div>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    accumulator[field.field] = field.message;
    return accumulator;
  }, {});
}
