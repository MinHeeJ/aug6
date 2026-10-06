import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "../../app/AuthProvider";
import { CourseOperationManagementPage } from "./SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";

const base = "/api/business/course-operations";
const row = {
  achievementId: 41,
  managementNo: "TEST-COURSE-41",
  teacherUserId: 7,
  teacherName: "테스트 교원",
  organizationCode: "TEST-ORG",
  evaluationYear: "2026",
  managementItemCode: "TEST-ITEM",
  achievementDate: "2026-04-10",
  performanceDetails: "목록의 요약",
  achievementStatus: "DRAFT",
  attachmentIds: ["attachment-101", "attachment-102"],
  createdAt: "2026-04-10T09:00:00",
  updatedAt: "2026-04-10T09:00:00",
};
const detail = { ...row, performanceDetails: "상세 API의 실적내역" };
let roles: string[];
let userId: number;
let transport: ReturnType<typeof vi.fn<typeof fetch>>;
function response(data: unknown, status = 200) {
  return new Response(
    JSON.stringify({ success: status < 400, data, meta: {} }),
    {
      status,
      headers: { "Content-Type": "application/json" },
    },
  );
}
function failure(
  status: number,
  code: string,
  fields: { field: string; message: string }[] = [],
) {
  return new Response(
    JSON.stringify({
      success: false,
      error: { code, message: "서버 거부", fields },
      meta: {},
    }),
    {
      status,
      headers: { "Content-Type": "application/json" },
    },
  );
}
function list(rows = [row], total = rows.length) {
  return response({
    achievements: rows,
    page: 0,
    pageSize: 20,
    totalElements: total,
  });
}
function mount() {
  render(
    <AuthProvider>
      <CourseOperationManagementPage />
    </AuthProvider>,
  );
}
function fill() {
  fireEvent.change(screen.getByTestId("course-managementItemCode"), {
    target: { value: "NEW-ITEM" },
  });
  fireEvent.change(screen.getByTestId("course-achievementDate"), {
    target: { value: "2026-05-01" },
  });
  fireEvent.change(screen.getByTestId("course-performanceDetails"), {
    target: { value: "새 실적내역" },
  });
}
function writes() {
  return transport.mock.calls.filter(
    ([, init]) => init?.method === "POST" || init?.method === "PUT",
  );
}

beforeEach(() => {
  roles = ["R01"];
  userId = 7;
  vi.spyOn(window, "confirm").mockReturnValue(true);
  transport = vi.fn<typeof fetch>(async (input, init) => {
    const url = String(input);
    if (url === "/api/auth/me")
      return response({
        userId,
        loginId: "test-user",
        name: "테스트 교원",
        roles,
        menus: [],
      });
    if (url === `${base}/41` && !init?.method) return response(detail);
    if (init?.method === "POST" || init?.method === "PUT") {
      return response({
        achievement: { ...detail, ...JSON.parse(String(init.body)) },
        occurredDateWarning: false,
        warningMessage: null,
      });
    }
    if (url.startsWith(`${base}?`)) return list();
    throw new Error(`Unexpected HTTP request: ${url}`);
  });
  vi.stubGlobal("fetch", transport);
});
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("강좌 개설·운영 실적", () => {
  it("loads detail before PUT and preserves attachments without owner/status fields", async () => {
    mount();
    await screen.findByText("TEST-COURSE-41");
    expect(
      transport.mock.calls.some(
        ([input]) => String(input) === `${base}?page=0&pageSize=20`,
      ),
    ).toBe(true);
    fireEvent.click(screen.getByTestId("course-detail-41"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
        "상세 API의 실적내역",
      ),
    );
    fireEvent.change(screen.getByTestId("course-performanceDetails"), {
      target: { value: "수정 실적" },
    });
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    const [url, init] = writes()[0];
    expect(url).toBe("/api/business/course-operations/41");
    expect(init?.method).toBe("PUT");
    expect(JSON.parse(String(init?.body))).toEqual({
      managementItemCode: "TEST-ITEM",
      achievementDate: "2026-04-10",
      performanceDetails: "수정 실적",
      attachmentIds: ["attachment-101", "attachment-102"],
    });
    const detailIndex = transport.mock.calls.findIndex(
      ([input]) => String(input) === `${base}/41`,
    );
    const putIndex = transport.mock.calls.findIndex(
      ([, options]) => options?.method === "PUT",
    );
    expect(detailIndex).toBeLessThan(putIndex);
    expect(screen.queryByTestId("course-upload")).not.toBeInTheDocument();
    expect(screen.getByTestId("course-attachment-gap")).toBeInTheDocument();
  });

  it("resets selected detail to a new entry and POSTs only the four writable fields", async () => {
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
        detail.performanceDetails,
      ),
    );
    fireEvent.click(screen.getByTestId("course-new"));
    expect(screen.getByTestId("course-managementItemCode")).toHaveValue("");
    expect(screen.getByTestId("course-attachment-refs")).toHaveTextContent(
      "없음",
    );
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    expect(writes()).toHaveLength(1);
    expect(writes()[0][0]).toBe(base);
    expect(writes()[0][1]?.method).toBe("POST");
    expect(JSON.parse(String(writes()[0][1]?.body))).toEqual({
      managementItemCode: "NEW-ITEM",
      achievementDate: "2026-05-01",
      performanceDetails: "새 실적내역",
      attachmentIds: [],
    });
  });

  it("blocks missing required values before confirmation or any write", async () => {
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-save"));
    expect(
      screen.getByTestId("course-error-managementItemCode"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("course-error-achievementDate"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("course-error-performanceDetails"),
    ).toBeInTheDocument();
    expect(window.confirm).not.toHaveBeenCalled();
    expect(writes()).toHaveLength(0);
  });

  it("does not write when save confirmation is cancelled", async () => {
    mount();
    await screen.findByText("TEST-COURSE-41");
    fill();
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(screen.getByTestId("course-save"));
    expect(window.confirm).toHaveBeenCalled();
    expect(writes()).toHaveLength(0);
  });

  it.each([
    "SUBMITTED",
    "DEPARTMENT_CONFIRMED",
    "CERTIFIED",
    "EVALUATION_CONFIRMED",
    "DELETED",
  ])("renders %s detail read-only", async (achievementStatus) => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      String(input) === `${base}/41`
        ? response({ ...detail, achievementStatus })
        : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
        detail.performanceDetails,
      ),
    );
    expect(screen.getByTestId("course-save")).toBeDisabled();
    expect(screen.getByTestId("course-performanceDetails")).toBeDisabled();
    expect(screen.getByTestId("course-readonly")).toBeInTheDocument();
    expect(writes()).toHaveLength(0);
  });

  it.each(["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"])(
    "allows the R01 owner to edit %s",
    async (achievementStatus) => {
      const original = transport.getMockImplementation()!;
      transport.mockImplementation(async (input, init) =>
        String(input) === `${base}/41`
          ? response({ ...detail, achievementStatus })
          : original(input, init),
      );
      mount();
      await screen.findByText("TEST-COURSE-41");
      fireEvent.click(screen.getByTestId("course-detail-41"));
      await waitFor(() =>
        expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
          detail.performanceDetails,
        ),
      );
      expect(screen.getByTestId("course-save")).toBeEnabled();
    },
  );

  it.each([
    { testRoles: ["R02"], testUser: 7 },
    { testRoles: ["R04"], testUser: 7 },
    { testRoles: ["R01"], testUser: 99 },
  ])(
    "prevents non-writers or another owner from editing: %j",
    async ({ testRoles, testUser }) => {
      roles = testRoles;
      userId = testUser;
      mount();
      await screen.findByText("TEST-COURSE-41");
      fireEvent.click(screen.getByTestId("course-detail-41"));
      await waitFor(() =>
        expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
          detail.performanceDetails,
        ),
      );
      expect(screen.getByTestId("course-save")).toBeDisabled();
      expect(writes()).toHaveLength(0);
      if (!roles.includes("R01"))
        expect(screen.getByTestId("course-new")).toBeDisabled();
    },
  );

  it.each(["list", "detail", "save"])(
    "shows the permission state after a %s 403",
    async (stage) => {
      const original = transport.getMockImplementation()!;
      transport.mockImplementation(async (input, init) => {
        if (
          (stage === "list" && String(input).startsWith(`${base}?`)) ||
          (stage === "detail" && String(input) === `${base}/41`) ||
          (stage === "save" && init?.method === "POST")
        )
          return failure(403, "FORBIDDEN");
        return original(input, init);
      });
      mount();
      if (stage !== "list") {
        await screen.findByText("TEST-COURSE-41");
        if (stage === "detail")
          fireEvent.click(screen.getByTestId("course-detail-41"));
        else {
          fill();
          fireEvent.click(screen.getByTestId("course-save"));
        }
      }
      await screen.findByText("강좌 개설·운영 실적 권한이 없습니다");
      expect(screen.queryByTestId("course-save")).not.toBeInTheDocument();
    },
  );

  it("sends applied filters and zero-based pagination with the chosen pageSize", async () => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      String(input).startsWith(`${base}?`)
        ? list([row], 121)
        : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.change(screen.getByTestId("course-filter-managementNo"), {
      target: { value: " TEST " },
    });
    fireEvent.change(screen.getByTestId("course-filter-teacherName"), {
      target: { value: "교원" },
    });
    fireEvent.change(screen.getByTestId("course-filter-managementItemCode"), {
      target: { value: "ITEM" },
    });
    fireEvent.change(screen.getByTestId("course-filter-achievementStatus"), {
      target: { value: "DRAFT" },
    });
    fireEvent.click(screen.getByTestId("course-search"));
    await waitFor(() =>
      expect(
        transport.mock.calls.some(([input]) =>
          String(input).includes("managementNo=TEST"),
        ),
      ).toBe(true),
    );
    fireEvent.change(screen.getByTestId("course-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(
        transport.mock.calls.some(([input]) =>
          String(input).includes("pageSize=50"),
        ),
      ).toBe(true),
    );
    await waitFor(() =>
      expect(screen.getByTestId("course-next")).toBeEnabled(),
    );
    fireEvent.click(screen.getByTestId("course-next"));
    await waitFor(() =>
      expect(
        transport.mock.calls.some(([input]) =>
          String(input).startsWith(`${base}?page=1&pageSize=50`),
        ),
      ).toBe(true),
    );
    const paths = transport.mock.calls.map(([input]) => String(input));
    expect(paths).toContain(
      `${base}?page=1&pageSize=50&managementNo=TEST` +
        "&teacherName=%EA%B5%90%EC%9B%90&managementItemCode=ITEM&achievementStatus=DRAFT",
    );
    fireEvent.change(screen.getByTestId("course-page-size"), {
      target: { value: "100" },
    });
    await waitFor(() =>
      expect(
        transport.mock.calls.some(([input]) =>
          String(input).startsWith(`${base}?page=0&pageSize=100`),
        ),
      ).toBe(true),
    );
  });

  it("ignores a late detail response after a newer row selection", async () => {
    let resolveOld!: (value: Response) => void;
    const pending = new Promise<Response>((resolve) => {
      resolveOld = resolve;
    });
    const second = {
      ...row,
      achievementId: 42,
      managementNo: "TEST-COURSE-42",
      performanceDetails: "두 번째 상세",
    };
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) => {
      if (String(input) === `${base}/41`) return pending;
      if (String(input) === `${base}/42`) return response(second);
      if (String(input).startsWith(`${base}?`)) return list([row, second]);
      return original(input, init);
    });
    mount();
    await screen.findByText("TEST-COURSE-42");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    expect(screen.getByTestId("course-save")).toBeDisabled();
    fireEvent.click(screen.getByTestId("course-detail-42"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
        "두 번째 상세",
      ),
    );
    await act(async () => {
      resolveOld(response(detail));
      await pending;
    });
    expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
      "두 번째 상세",
    );
  });

  it("shows server field errors without claiming success", async () => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      init?.method === "POST"
        ? failure(400, "VALIDATION_ERROR", [
            {
              field: "performanceDetails",
              message: "실적내역 길이를 확인하세요",
            },
          ])
        : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("실적내역 길이를 확인하세요");
    expect(screen.queryByText("저장되었습니다.")).not.toBeInTheDocument();
    expect(screen.getByTestId("course-performanceDetails")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("reports a saved occurred-date warning", async () => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      init?.method === "POST"
        ? response({
            achievement: { ...row, ...JSON.parse(String(init.body)) },
            occurredDateWarning: true,
            warningMessage: "평가대상 기간 밖 발생일입니다",
          })
        : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    await screen.findByText("평가대상 기간 밖 발생일입니다");
  });

  it("invalidates a pending detail when starting a new entry", async () => {
    let resolveDetail!: (value: Response) => void;
    const pending = new Promise<Response>((resolve) => {
      resolveDetail = resolve;
    });
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      String(input) === `${base}/41` ? pending : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    fireEvent.click(screen.getByTestId("course-new"));
    fill();
    await act(async () => {
      resolveDetail(response(detail));
      await pending;
    });
    expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
      "새 실적내역",
    );
    expect(screen.getByTestId("course-attachment-refs")).toHaveTextContent(
      "없음",
    );
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    expect(writes()[0][1]?.method).toBe("POST");
  });

  it("restores fetched detail on reset without losing the selected update target", async () => {
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
        detail.performanceDetails,
      ),
    );
    fill();
    fireEvent.click(screen.getByTestId("course-form-reset"));
    expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
      detail.performanceDetails,
    );
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    expect(writes()[0][0]).toBe(`${base}/41`);
    expect(writes()[0][1]?.method).toBe("PUT");
  });

  it("locks an apparently editable detail when the server reports confirmed data", async () => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      init?.method === "PUT"
        ? failure(409, "CONFIRMED_DATA_LOCKED")
        : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performanceDetails")).toHaveValue(
        detail.performanceDetails,
      ),
    );
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("서버 거부");
    expect(screen.getByTestId("course-save")).toBeDisabled();
    fireEvent.click(screen.getByTestId("course-form-reset"));
    expect(screen.getByTestId("course-save")).toBeDisabled();
    expect(screen.queryByText("저장되었습니다.")).not.toBeInTheDocument();
  });

  it("denies roles outside the screen's access policy without fetching business data", async () => {
    roles = ["R07"];
    mount();
    await screen.findByText("강좌 개설·운영 실적 권한이 없습니다");
    expect(
      transport.mock.calls.some(([input]) => String(input).startsWith(base)),
    ).toBe(false);
  });

  it("renders empty results rather than production fixtures", async () => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      String(input).startsWith(`${base}?`) ? list([]) : original(input, init),
    );
    mount();
    await screen.findByText("조회 조건에 맞는 결과가 없습니다.");
    expect(screen.queryByText("TEST-COURSE-41")).not.toBeInTheDocument();
  });

  it("keeps failed detail read-only instead of editing a list summary", async () => {
    const original = transport.getMockImplementation()!;
    transport.mockImplementation(async (input, init) =>
      String(input) === `${base}/41`
        ? failure(500, "INTERNAL_ERROR")
        : original(input, init),
    );
    mount();
    await screen.findByText("TEST-COURSE-41");
    fireEvent.click(screen.getByTestId("course-detail-41"));
    await screen.findByText("서버 거부");
    expect(screen.getByTestId("course-save")).toBeDisabled();
  });
});
