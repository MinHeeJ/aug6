import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./AuthProvider";
import { AppRouter } from "./router";
import { ADMIN_ROUTES, canAccessAdminRoute } from "../pages/LoginPage";
import {
  ApiClientError,
  courseOperationApi,
  employmentRateAchievementApi,
  employmentRateImprovementApi,
  lectureImprovementApi,
  type CurrentUser,
} from "../api/apiClient";

const screens = [
  [
    "employment-rate-improvements",
    "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
  ],
  ["course-operations", "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"],
  ["lecture-improvements", "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"],
  ["employment-rate-achievements", "SCR-EMPLOYMENT-RATE-ACHIEVEMENT"],
] as const;

function principal(resource: string, roles = ["R01"]): CurrentUser {
  const route = ADMIN_ROUTES.find(
    (entry) => entry.path === `/faculty/education/${resource}`,
  )!;
  return {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles,
    menus: [
      {
        menuId: 991,
        menuName: route.label,
        screenId: route.screenId,
        url: route.path,
        displayOrder: 1,
        children: [],
      },
    ],
  };
}
const ok = (data: unknown) =>
  new Response(JSON.stringify({ success: true, data, meta: {} }), {
    headers: { "Content-Type": "application/json" },
  });
const fetchStub = vi.fn<typeof fetch>();

beforeEach(() => {
  window.history.replaceState({}, "", "/");
  fetchStub.mockReset();
  vi.stubGlobal("fetch", fetchStub);
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/");
});

function wire(user: CurrentUser) {
  fetchStub.mockImplementation(async (path) => {
    if (path === "/api/auth/me") return ok(user);
    if (String(path).includes("/histories")) return ok([]);
    return ok({ achievements: [], managementItems: [], totalElements: 0 });
  });
}

describe("education route wiring", () => {
  it.each(screens)(
    "menu entry reaches %s and its relative API",
    async (resource, screenId) => {
      const user = principal(resource);
      wire(user);
      render(
        <AuthProvider>
          <AppRouter />
        </AuthProvider>,
      );
      fireEvent.click(
        await screen.findByRole("button", { name: "모바일 메뉴" }),
      );
      fireEvent.click(
        await screen.findByRole("link", { name: user.menus[0].menuName }),
      );
      await waitFor(() =>
        expect(
          document.querySelector(`[data-screen-id="${screenId}"]`),
        ).not.toBeNull(),
      );
      expect(window.location.pathname).toBe(`/faculty/education/${resource}`);
      await waitFor(() =>
        expect(
          fetchStub.mock.calls.some(([path]) =>
            String(path).startsWith(`/api/business/${resource}?`),
          ),
        ).toBe(true),
      );
      expect(
        ADMIN_ROUTES.filter((route) => route.screenId === screenId),
      ).toHaveLength(1);
    },
  );

  it.each(screens)(
    "denies %s without the server menu before calling its API",
    async (resource) => {
      wire({ ...principal(resource), menus: [] });
      window.history.replaceState({}, "", `/faculty/education/${resource}`);
      render(
        <AuthProvider>
          <AppRouter />
        </AuthProvider>,
      );
      await screen.findByText("권한이 없습니다");
      expect(
        fetchStub.mock.calls.some(([path]) =>
          String(path).startsWith("/api/business/"),
        ),
      ).toBe(false);
    },
  );

  it("admits R07 only to the employment wizard, not ordinary achievement routes", async () => {
    const user = principal("employment-rate-achievements", ["R07"]);
    wire(user);
    window.history.replaceState({}, "", user.menus[0].url!);
    render(
      <AuthProvider>
        <AppRouter />
      </AuthProvider>,
    );
    await screen.findByText("업로드 이력이 없습니다.");
    expect(
      screen.queryByTestId("employment-individual-tab"),
    ).not.toBeInTheDocument();
    expect(
      fetchStub.mock.calls.some(([path]) =>
        String(path).startsWith("/api/business/employment-rate-achievements?"),
      ),
    ).toBe(false);
    for (const [resource] of screens.slice(0, 3)) {
      const batchUser = principal(resource, ["R07"]);
      expect(canAccessAdminRoute(batchUser, batchUser.menus[0].url!)).toBe(
        false,
      );
    }
  });

  it.each(screens)(
    "preserves R09 route admission with a menu for %s",
    (resource) => {
      const admin = principal(resource, ["R09"]);
      expect(canAccessAdminRoute(admin, admin.menus[0].url!)).toBe(true);
      const denied = principal(resource, ["R08"]);
      expect(canAccessAdminRoute(denied, denied.menus[0].url!)).toBe(false);
    },
  );
});

describe("education API registration", () => {
  const common = {
    managementItemCode: "selected-item",
    achievementDate: "2026-04-10",
    attachmentIds: [],
  };
  const clients = [
    {
      resource: "employment-rate-improvements",
      list: employmentRateImprovementApi.listEmploymentRateImprovements,
      get: employmentRateImprovementApi.getEmploymentRateImprovement,
      create: () =>
        employmentRateImprovementApi.createEmploymentRateImprovement(common),
      update: () =>
        employmentRateImprovementApi.updateEmploymentRateImprovement(
          431,
          common,
        ),
    },
    {
      resource: "course-operations",
      list: courseOperationApi.listCourseOperations,
      get: courseOperationApi.getCourseOperation,
      create: () =>
        courseOperationApi.createCourseOperation({
          ...common,
          performanceDetails: "운영 실적",
        }),
      update: () =>
        courseOperationApi.updateCourseOperation(431, {
          ...common,
          performanceDetails: "운영 실적",
        }),
    },
    {
      resource: "lecture-improvements",
      list: lectureImprovementApi.listLectureImprovements,
      get: lectureImprovementApi.getLectureImprovement,
      create: () =>
        lectureImprovementApi.createLectureImprovement({
          ...common,
          achievementContent: "개선 실적",
          academicYear: 2026,
          semester: 2,
        }),
      update: () =>
        lectureImprovementApi.updateLectureImprovement(431, {
          ...common,
          achievementContent: "개선 실적",
          academicYear: 2026,
          semester: 2,
        }),
    },
    {
      resource: "employment-rate-achievements",
      list: employmentRateAchievementApi.listEmploymentRateAchievements,
      get: employmentRateAchievementApi.getEmploymentRateAchievement,
      create: () =>
        employmentRateAchievementApi.createEmploymentRateAchievement(common),
      update: () =>
        employmentRateAchievementApi.updateEmploymentRateAchievement(
          431,
          common,
        ),
    },
  ];

  it.each(clients)(
    "binds CRUD methods and selected IDs for $resource",
    async (client) => {
      fetchStub.mockImplementation(async () => ok({}));
      await client.list({ page: 2, pageSize: 50, managementNo: "관리 번호" });
      await client.get(431);
      await client.create();
      await client.update();
      const base = `/api/business/${client.resource}`;
      const [list, detail, create, update] = fetchStub.mock.calls;
      expect(String(list[0]).split("?")[0]).toBe(base);
      expect(
        new URLSearchParams(String(list[0]).split("?")[1]).get("pageSize"),
      ).toBe("50");
      expect(detail[0]).toBe(`${base}/431`);
      expect(create[0]).toBe(base);
      expect(create[1]?.method).toBe("POST");
      expect(update[0]).toBe(`${base}/431`);
      expect(update[1]?.method).toBe("PUT");
      expect(JSON.parse(String(update[1]?.body))).not.toHaveProperty(
        "achievementId",
      );
      expect(
        fetchStub.mock.calls.every(
          ([, init]) => init?.credentials === "include",
        ),
      ).toBe(true);
    },
  );

  it("uploads multipart without a JSON Content-Type and keeps wizard IDs opaque", async () => {
    fetchStub.mockImplementation(async () => ok({}));
    const file = new File(["uploaded bytes"], "실적.xlsx");
    await employmentRateAchievementApi.uploadEmploymentRateAchievementsExcel(
      file,
    );
    const [path, init] = fetchStub.mock.calls[0];
    expect(path).toBe(
      "/api/business/employment-rate-achievements/excel-uploads",
    );
    expect(init?.method).toBe("POST");
    expect(new Headers(init?.headers).has("Content-Type")).toBe(false);
    expect((init?.body as FormData).get("file")).toBe(file);
    await employmentRateAchievementApi.commitUpload("opaque/id");
    await employmentRateAchievementApi.getUploadErrors("opaque/id");
    await employmentRateAchievementApi.listUploadHistories();
    await employmentRateAchievementApi.getEmploymentRateBulkJob("job/id");
    expect(fetchStub.mock.calls.slice(1).map(([url]) => url)).toEqual([
      "/api/business/employment-rate-achievements/excel-uploads/opaque%2Fid/commit",
      "/api/business/employment-rate-achievements/excel-uploads/opaque%2Fid/errors",
      "/api/business/employment-rate-achievements/excel-uploads/histories",
      "/api/business/employment-rate-achievements/bulk-jobs/job%2Fid",
    ]);
  });

  it("retains policy conflict status and field errors rather than inventing an accepted job", async () => {
    const error = {
      code: "POLICY_NOT_APPROVED",
      message: "정책 미승인",
      fields: [],
    };
    fetchStub.mockResolvedValue(
      new Response(JSON.stringify({ success: false, error, meta: {} }), {
        status: 409,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(
      employmentRateAchievementApi.createEmploymentRateBulkJob({
        evaluationYear: "2026",
        actionType: "DELETE",
        targetCondition: { organizationCode: "selected-org" },
      }),
    ).rejects.toMatchObject({ status: 409, apiError: error });
    expect(fetchStub.mock.calls[0][0]).toBe(
      "/api/business/employment-rate-achievements/bulk-jobs",
    );
  });

  it("returns actual download blobs and preserves download authorization errors", async () => {
    fetchStub.mockImplementation(async () => new Response("workbook bytes"));
    await employmentRateAchievementApi.downloadEmploymentRateAchievements({
      pageSize: 100,
    });
    await employmentRateAchievementApi.downloadTemplate();
    await employmentRateAchievementApi.downloadUploadErrors("upload/id");
    expect(fetchStub.mock.calls.map(([path]) => path)).toEqual([
      "/api/business/employment-rate-achievements/download?page=0&pageSize=100",
      "/api/business/employment-rate-achievements/excel-uploads/template",
      "/api/business/employment-rate-achievements/excel-uploads/upload%2Fid/errors/download",
    ]);
    fetchStub.mockResolvedValue(
      new Response(
        JSON.stringify({
          success: false,
          error: { code: "FORBIDDEN", message: "권한 없음", fields: [] },
          meta: {},
        }),
        { status: 403, headers: { "Content-Type": "application/json" } },
      ),
    );
    await expect(
      employmentRateAchievementApi.downloadTemplate(),
    ).rejects.toBeInstanceOf(ApiClientError);
  });
});
