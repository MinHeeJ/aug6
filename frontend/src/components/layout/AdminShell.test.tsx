import type React from "react";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AdminShell } from "./AdminShell";
import { I18nProvider, i18n } from "../../i18n";
import type { CurrentUser } from "../../api/apiClient";

const menus: CurrentUser["menus"] = [
  {
    menuId: 1,
    menuName: "교수업적평가",
    displayOrder: 1,
    children: [
      {
        menuId: 11,
        parentMenuId: 1,
        menuName: "연구자 프로필 관리",
        displayOrder: 1,
        children: [
          {
            menuId: 111,
            parentMenuId: 11,
            menuName: "연구자 프로필 목록",
            screenId: "SCR-RESEARCHER-PROFILE-LIST",
            url: "/researcher-profiles",
            displayOrder: 1,
            children: [],
          },
          {
            menuId: 112,
            parentMenuId: 11,
            menuName: "선행학위 미충족",
            screenId: "SCR-DEGREE-PREREQ-MISSING",
            url: "/admin/researcher-profiles/degree-prerequisite-missing",
            displayOrder: 2,
            children: [],
          },
        ],
      },
    ],
  },
  {
    menuId: 2,
    menuName: "파일·데이터 관리",
    displayOrder: 2,
    children: [
      {
        menuId: 21,
        parentMenuId: 2,
        menuName: "엑셀 관리",
        displayOrder: 1,
        children: [
          {
            menuId: 211,
            parentMenuId: 21,
            menuName: "업로드 양식 관리",
            screenId: "SCR-UPLOAD-TEMPLATE-MGMT",
            url: "/admin/excel-upload-templates",
            displayOrder: 1,
            children: [],
          },
        ],
      },
    ],
  },
];

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 1,
      loginId: "admin",
      name: "관리자",
      roles: ["R09"],
      menus,
    },
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

afterEach(() => {
  window.history.replaceState({}, "", "/admin/users");
  window.sessionStorage.clear();
  void i18n.changeLanguage("ko");
  vi.unstubAllGlobals();
});

function renderShell(children: React.ReactNode = "본문") {
  return render(
    <I18nProvider>
      <AdminShell>{children}</AdminShell>
    </I18nProvider>,
  );
}

describe("BASIC-56 AdminShell 헤더 다국어 전환", () => {
  it("SCR-COMMON-SHELL header language selector는 한국어 기본값과 English option을 표시한다", () => {
    renderShell();

    const selector = screen.getByTestId("header-language-selector");

    expect(selector).toHaveValue("ko");
    expect(screen.getByRole("option", { name: "한국어" })).toHaveValue("ko");
    expect(screen.getByRole("option", { name: "English" })).toHaveValue("en");
  });

  it("sidebar는 선택 언어를 getLocalizedMenuTree query로 전달하고 API displayName만 렌더링한다", async () => {
    const fetchMock = vi
      .fn()
      .mockImplementation(async (input: RequestInfo | URL) => {
        const url = String(input);
        return {
          ok: true,
          headers: { get: () => "application/json" },
          json: async () => ({
            success: true,
            data: url.includes("lang=en")
              ? {
                  lang: "en",
                  rows: [
                    {
                      menuId: 1,
                      menuName: "교수업적평가",
                      displayName: "Faculty Evaluation",
                      displayOrder: 1,
                      children: [
                        {
                          menuId: 11,
                          parentMenuId: 1,
                          menuName: "연구자 프로필 관리",
                          displayName: "Researcher Profiles",
                          displayOrder: 1,
                          children: [
                            {
                              menuId: 111,
                              parentMenuId: 11,
                              menuName: "연구자 프로필 목록",
                              displayName: "Researcher Profile List",
                              screenId: "SCR-RESEARCHER-PROFILE-LIST",
                              url: "/researcher-profiles",
                              displayOrder: 1,
                              children: [],
                            },
                          ],
                        },
                      ],
                    },
                  ],
                }
              : { lang: "ko", rows: menus },
            meta: {},
          }),
        };
      });
    vi.stubGlobal("fetch", fetchMock);
    renderShell();

    fireEvent.change(screen.getByTestId("header-language-selector"), {
      target: { value: "en" },
    });

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/admin/menus/localized-tree?lang=en",
        expect.objectContaining({ credentials: "include" }),
      ),
    );
    expect(
      await screen.findByRole("button", { name: "Faculty Evaluation" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "교수업적평가" }),
    ).not.toBeInTheDocument();
  });

  it("선택 언어는 browser session 동안 route 이동과 새 shell 렌더링 후에도 유지되고 DB 저장 API를 호출하지 않는다", () => {
    const fetchSpy = vi.spyOn(globalThis, "fetch");
    renderShell();

    fireEvent.change(screen.getByTestId("header-language-selector"), {
      target: { value: "en" },
    });
    expect(screen.getByTestId("header-language-selector")).toHaveValue("en");
    expect(window.sessionStorage.getItem("app.language")).toBe("en");

    fireEvent.mouseEnter(
      screen.getByRole("button", { name: "파일·데이터 관리" }),
    );
    fireEvent.click(screen.getByRole("link", { name: /업로드 양식 관리/ }));
    expect(window.location.pathname).toBe("/admin/excel-upload-templates");

    const { unmount } = renderShell();
    const selectors = screen.getAllByTestId("header-language-selector");
    expect(selectors[selectors.length - 1]).toHaveValue("en");
    unmount();
    expect(
      fetchSpy.mock.calls.some(([input, init]) => {
        const url = String(input);
        return (
          url.includes("language") ||
          url.includes("preference") ||
          url.includes("settings") ||
          init?.method === "PATCH" ||
          init?.method === "PUT" ||
          init?.method === "POST"
        );
      }),
    ).toBe(false);
    fetchSpy.mockRestore();
  });
});

describe("BASIC-38 AdminShell 메뉴 탐색", () => {
  it("대메뉴를 헤더 메뉴바에 노출하고 hover/focus로 하위 메뉴를 확인할 수 있다", () => {
    renderShell();

    const headerNav = screen.getByTestId("header-menu-bar");
    expect(
      within(headerNav).getByRole("button", { name: "교수업적평가" }),
    ).toBeInTheDocument();
    expect(
      within(headerNav).getByRole("button", { name: "파일·데이터 관리" }),
    ).toBeInTheDocument();

    fireEvent.mouseEnter(
      within(headerNav).getByRole("button", { name: "교수업적평가" }),
    );

    expect(
      screen.getByRole("link", { name: /연구자 프로필 목록/ }),
    ).toHaveAttribute("href", "/researcher-profiles");
    expect(
      screen.getByRole("link", { name: /선행학위 미충족/ }),
    ).toHaveAttribute(
      "href",
      "/admin/researcher-profiles/degree-prerequisite-missing",
    );
  });

  it("메뉴 검색 입력으로 실제 메뉴를 필터링하고 선택 시 해당 route로 이동한다", () => {
    renderShell();

    fireEvent.click(
      screen.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
    );
    const input = screen.getByTestId("menu-search-input");
    fireEvent.change(input, { target: { value: "업로드" } });

    expect(input).toHaveValue("업로드");
    const result = screen.getByRole("link", { name: /업로드 양식 관리/ });
    expect(result).toHaveAttribute("href", "/admin/excel-upload-templates");

    fireEvent.click(result);
    expect(window.location.pathname).toBe("/admin/excel-upload-templates");
  });
});
