import type React from "react";
import { GitBranch, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  menuStructureApi,
  type ApiErrorField,
  type MenuTreeNode,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  parentMenuId: string;
  changeReason: string;
};

export function MenuStructureManagementPage() {
  const [filter, setFilter] = useState("");
  const [tree, setTree] = useState<MenuTreeNode[]>([]);
  const [selected, setSelected] = useState<MenuTreeNode | null>(null);
  const [form, setForm] = useState<FormState>({
    parentMenuId: "",
    changeReason: "",
  });
  const [reorderReason, setReorderReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const flattenedMenus = useMemo(() => flattenMenus(tree), [tree]);
  const siblingMenus = useMemo(() => {
    if (!selected) return [];
    return flattenedMenus
      .filter(
        (menu) =>
          (menu.parentMenuId ?? null) === (selected.parentMenuId ?? null),
      )
      .sort(
        (left, right) =>
          left.displayOrder - right.displayOrder || left.menuId - right.menuId,
      );
  }, [flattenedMenus, selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await menuStructureApi.getMenuTree({ filter });
      setTree(response.data ?? []);
      setSelected(null);
      setForm({ parentMenuId: "", changeReason: "" });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const selectMenu = (menu: MenuTreeNode) => {
    setSelected(menu);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      parentMenuId: menu.parentMenuId ? String(menu.parentMenuId) : "",
      changeReason: "",
    });
  };

  const saveParent = async () => {
    if (!selected) return;
    const nextParentMenuId = Number(form.parentMenuId);
    if (!Number.isFinite(nextParentMenuId)) {
      setFieldErrors({ parentMenuId: "부모 메뉴를 선택하세요." });
      return;
    }
    const parentName =
      flattenedMenus.find((menu) => menu.menuId === nextParentMenuId)
        ?.menuName ?? nextParentMenuId;
    if (
      !window.confirm(
        `${selected.menuName}의 부모 메뉴를 ${parentName}(으)로 변경합니까?`,
      )
    )
      return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await menuStructureApi.updateMenuParent(selected.menuId, {
        parentMenuId: nextParentMenuId,
        changeReason: form.changeReason,
      });
      setSuccessMessage("저장되었습니다");
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const reorderSelectedSiblings = async () => {
    if (!selected || siblingMenus.length === 0) return;
    const orderedMenuIds = siblingMenus.map((menu) => menu.menuId);
    if (
      !window.confirm(
        `${siblingMenus.length}개 메뉴의 표시 순서를 현재 목록 순서로 저장합니까?`,
      )
    )
      return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await menuStructureApi.reorderMenus({
        parentMenuId: selected.parentMenuId ?? null,
        orderedMenuIds,
        changeReason: reorderReason || "메뉴 구조 관리 화면 표시순서 저장",
      });
      setSuccessMessage("저장되었습니다");
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const moveSibling = (menuId: number, direction: -1 | 1) => {
    const index = siblingMenus.findIndex((menu) => menu.menuId === menuId);
    const targetIndex = index + direction;
    if (index < 0 || targetIndex < 0 || targetIndex >= siblingMenus.length)
      return;
    const reordered = [...siblingMenus];
    const [moved] = reordered.splice(index, 1);
    reordered.splice(targetIndex, 0, moved);
    setTree(
      replaceSiblingOrder(
        tree,
        selected?.parentMenuId ?? null,
        reordered.map((menu, orderIndex) => ({
          ...menu,
          displayOrder: orderIndex + 1,
        })),
      ),
    );
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
        : "메뉴 구조 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-MENU-STRUCTURE-MGMT">
        <PermissionState
          title="메뉴 구조 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section className="space-y-6" data-screen-id="SCR-MENU-STRUCTURE-MGMT">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 메뉴 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              메뉴 구조 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              대메뉴·중메뉴·소메뉴의 부모-자식 관계와 표시 순서를 관리합니다.
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

      <div className="sr-only">
        메뉴 구조 관리 권한이 없습니다 조회된 메뉴가 없습니다 저장되었습니다
      </div>
      {error ? (
        <ErrorState title="메뉴 구조 처리 오류" message={error} />
      ) : null}
      {successMessage ? (
        <SuccessState title="처리 완료" message={successMessage} />
      ) : null}

      <div className="rounded-md border border-ld bg-white p-6 shadow-md">
        <h2 className="card-title mb-4 text-lg font-semibold text-dark">
          검색조건
        </h2>
        <div className="grid grid-cols-12 gap-5 md:gap-6">
          <Field label="메뉴 검색" className="col-span-12 md:col-span-6">
            <div className="relative">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 pl-10 text-sm"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
                placeholder="메뉴명/화면ID/URL"
              />
            </div>
          </Field>
          <div className="col-span-12 flex items-end gap-3 md:col-span-6">
            <button
              className="h-10 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              disabled={loading}
              onClick={() => void load()}
              type="button"
            >
              조회
            </button>
            <button
              className="h-10 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              onClick={() => setFilter("")}
              type="button"
            >
              초기화
            </button>
          </div>
        </div>
        <p className="mt-4 text-xs text-muted">
          부모 메뉴 변경은 자기 자신과 하위 메뉴를 부모로 지정할 수 없으며, 서버
          검증 결과를 field-level 오류로 표시합니다.
        </p>
      </div>

      {loading ? (
        <LoadingState
          title="메뉴 구조를 불러오는 중입니다"
          message="menus 테이블의 부모-자식 관계와 표시 순서를 조회하고 있습니다."
        />
      ) : null}

      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-7">
          <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="card-title flex items-center gap-2 text-lg font-semibold text-dark">
              <GitBranch size={18} /> 메뉴 tree
            </h2>
            <p className="text-sm text-muted">
              총 {flattenedMenus.length}개 메뉴
            </p>
          </div>
          <div className="rounded-md border border-border p-4">
            {tree.length > 0
              ? tree.map((menu) => (
                  <MenuNode
                    key={menu.menuId}
                    menu={menu}
                    selectedId={selected?.menuId}
                    onSelect={selectMenu}
                  />
                ))
              : null}
            {!loading && tree.length === 0 ? (
              <EmptyState
                title="조회된 메뉴가 없습니다"
                message="검색어를 변경하거나 seed 메뉴 데이터를 확인하세요."
              />
            ) : null}
          </div>
        </div>

        <div className="col-span-12 space-y-6 lg:col-span-5">
          <div className="rounded-md border border-ld bg-white p-6 shadow-md">
            <h2 className="card-title mb-4 text-lg font-semibold text-dark">
              부모 메뉴 변경
            </h2>
            {!selected ? (
              <EmptyState
                title="메뉴를 선택하세요"
                message="메뉴 tree에서 행을 선택하면 부모 메뉴를 변경할 수 있습니다."
              />
            ) : (
              <div className="space-y-4">
                <Readonly
                  label="선택 메뉴"
                  value={`${selected.menuName} (${selected.menuId})`}
                />
                <Readonly
                  label="screenId / URL"
                  value={selected.screenId ?? selected.url ?? "-"}
                />
                <Field
                  label="부모 메뉴"
                  error={fieldErrors.parentMenuId}
                  required
                >
                  <select
                    className="h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
                    value={form.parentMenuId}
                    onChange={(event) =>
                      setForm({ ...form, parentMenuId: event.target.value })
                    }
                  >
                    <option value="">부모 메뉴 선택</option>
                    {flattenedMenus
                      .filter((menu) => menu.menuId !== selected.menuId)
                      .map((menu) => (
                        <option key={menu.menuId} value={menu.menuId}>
                          {menu.menuName} ({menu.menuId})
                        </option>
                      ))}
                  </select>
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
                    placeholder="메뉴 구조 변경 사유"
                  />
                </Field>
                <button
                  className="inline-flex rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled={saving}
                  onClick={() => void saveParent()}
                  type="button"
                >
                  <Save className="mr-2" size={16} />
                  저장
                </button>
              </div>
            )}
          </div>

          <div className="rounded-md border border-ld bg-white p-6 shadow-md">
            <h2 className="card-title mb-4 text-lg font-semibold text-dark">
              표시 순서 재정렬
            </h2>
            {!selected ? (
              <EmptyState
                title="형제 메뉴 목록 없음"
                message="메뉴를 선택하면 같은 부모 아래 메뉴의 표시 순서를 조정할 수 있습니다."
              />
            ) : (
              <div className="space-y-3">
                {siblingMenus.map((menu, index) => (
                  <div
                    key={menu.menuId}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                  >
                    <span>
                      <strong>{index + 1}</strong>. {menu.menuName}
                    </span>
                    <span className="flex gap-2">
                      <button
                        className="rounded-md border border-primary px-2 py-1 text-primary disabled:opacity-50"
                        disabled={index === 0}
                        onClick={() => moveSibling(menu.menuId, -1)}
                        type="button"
                      >
                        위
                      </button>
                      <button
                        className="rounded-md border border-primary px-2 py-1 text-primary disabled:opacity-50"
                        disabled={index === siblingMenus.length - 1}
                        onClick={() => moveSibling(menu.menuId, 1)}
                        type="button"
                      >
                        아래
                      </button>
                    </span>
                  </div>
                ))}
                <Field label="정렬 변경 사유">
                  <input
                    className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                    value={reorderReason}
                    onChange={(event) => setReorderReason(event.target.value)}
                    placeholder="표시순서 변경 사유"
                  />
                </Field>
                {fieldErrors.orderedMenuIds ? (
                  <p className="text-xs text-error">
                    {fieldErrors.orderedMenuIds}
                  </p>
                ) : null}
                <button
                  className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled={saving || siblingMenus.length === 0}
                  onClick={() => void reorderSelectedSiblings()}
                  type="button"
                >
                  순서 저장
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

function MenuNode({
  menu,
  selectedId,
  onSelect,
  depth = 0,
}: {
  menu: MenuTreeNode;
  selectedId?: number;
  onSelect: (menu: MenuTreeNode) => void;
  depth?: number;
}) {
  return (
    <div className="mt-1">
      <button
        className={`flex w-full items-center justify-between rounded-md p-3 text-left text-sm transition-all duration-200 ease-in-out hover:translate-x-1 hover:bg-lightprimary hover:text-primary ${selectedId === menu.menuId ? "bg-lightprimary text-primary" : ""}`}
        onClick={() => onSelect(menu)}
        style={{ paddingLeft: `${12 + depth * 16}px` }}
        type="button"
      >
        <span>
          <span className="font-semibold text-dark">{menu.menuName}</span>
          <span className="ml-2 text-xs text-muted">
            {menu.menuType} / order {menu.displayOrder}
          </span>
        </span>
        <span className="text-xs text-muted">
          {menu.screenId ?? menu.url ?? menu.menuId}
        </span>
      </button>
      {menu.children.length > 0 ? (
        <div className="border-l border-ld pl-2">
          {menu.children.map((child) => (
            <MenuNode
              key={child.menuId}
              menu={child}
              selectedId={selectedId}
              onSelect={onSelect}
              depth={depth + 1}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function Field({
  label,
  error,
  required,
  className,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <label className={`block text-sm font-semibold text-ld ${className ?? ""}`}>
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

function flattenMenus(nodes: MenuTreeNode[]): MenuTreeNode[] {
  return nodes.flatMap((node) => [node, ...flattenMenus(node.children)]);
}

function replaceSiblingOrder(
  nodes: MenuTreeNode[],
  parentMenuId: number | null,
  siblings: MenuTreeNode[],
): MenuTreeNode[] {
  return nodes
    .map((node) => {
      if ((node.parentMenuId ?? null) === parentMenuId) {
        return (
          siblings.find((sibling) => sibling.menuId === node.menuId) ?? node
        );
      }
      return {
        ...node,
        children: replaceSiblingOrder(node.children, parentMenuId, siblings),
      };
    })
    .sort(
      (left, right) =>
        left.displayOrder - right.displayOrder || left.menuId - right.menuId,
    );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    accumulator[field.field] = field.message;
    return accumulator;
  }, {});
}
