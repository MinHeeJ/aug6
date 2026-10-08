import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { CourseOperationManagementPage } from "./SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const faculty: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
const row = {
  achievementId: 55,
  managementNo: "EDU-55",
  teacherUserId: 101,
  teacherName: "교원",
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "운영 내역",
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const listing = {
  achievements: [row],
  page: 0,
  pageSize: 20,
  totalElements: 1,
  managementItems: [
    {
      code: "COURSE_OPERATION",
      name: "강좌 운영",
      evaluationYear: "2026",
      teacherEditablePart: "SELF_REPORT",
    },
  ],
};

function respond(path: string) {
  return Promise.resolve({
    success: true,
    data: path.includes("?") ? listing : row,
    meta: {},
  });
}

async function select() {
  await screen.findByText("EDU-55");
  fireEvent.click(screen.getByTestId("course-detail-55"));
  await waitFor(() =>
    expect(screen.getByTestId("course-performance")).toHaveValue("운영 내역"),
  );
}

describe("course operations screen", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.mocked(apiRequest).mockImplementation((path) => respond(path));
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("loads detail through the selected id and updates through PUT with the approved fields", async () => {
    render(<CourseOperationManagementPage user={faculty} />);
    await select();
    fireEvent.change(screen.getByTestId("course-performance"), {
      target: { value: "수정 운영내역" },
    });
    vi.mocked(apiRequest).mockImplementation((path, init) => {
      if (init?.method === "PUT")
        return Promise.resolve({
          success: true,
          data: {
            achievement: { ...row, performanceDetails: "수정 운영내역" },
            occurredDateWarning: false,
          },
          meta: {},
        });
      return respond(path);
    });
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/course-operations/55");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "COURSE_OPERATION",
      achievementDate: "2026-04-10",
      performanceDetails: "수정 운영내역",
      attachmentIds: [],
    });
    expect(window.confirm).toHaveBeenCalled();
  });

  it("creates without a body achievementId and refreshes after success", async () => {
    render(<CourseOperationManagementPage user={faculty} />);
    await screen.findByText("EDU-55");
    fireEvent.change(screen.getByTestId("course-date"), {
      target: { value: "2026-04-11" },
    });
    fireEvent.change(screen.getByTestId("course-item"), {
      target: { value: "COURSE_OPERATION" },
    });
    fireEvent.change(screen.getByTestId("course-performance"), {
      target: { value: "신규 내역" },
    });
    vi.mocked(apiRequest).mockImplementation((path, init) =>
      init?.method === "POST"
        ? Promise.resolve({
            success: true,
            data: {
              achievement: { ...row, performanceDetails: "신규 내역" },
              occurredDateWarning: true,
              warningMessage: "발생일 경고",
            },
            meta: {},
          })
        : respond(path),
    );
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("발생일 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(call?.[0]).toBe("/api/business/course-operations");
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "achievementId",
    );
    expect(
      vi.mocked(apiRequest).mock.calls.filter(([path]) => path.includes("?"))
        .length,
    ).toBe(2);
  });

  it("confirmed detail disables editing and saving", async () => {
    vi.mocked(apiRequest).mockImplementation((path) =>
      Promise.resolve({
        success: true,
        data: path.includes("?")
          ? listing
          : { ...row, achievementStatus: "EVALUATION_CONFIRMED" },
        meta: {},
      }),
    );
    render(<CourseOperationManagementPage user={faculty} />);
    await select();
    expect(screen.getByTestId("course-save")).toBeDisabled();
    expect(screen.getByTestId("course-performance")).toBeDisabled();
    expect(screen.getByText(/평가확정 실적은 잠금/)).toBeInTheDocument();
  });

  it("read-only department role cannot see save or create controls", async () => {
    render(
      <CourseOperationManagementPage user={{ ...faculty, roles: ["R02"] }} />,
    );
    await select();
    expect(screen.queryByTestId("course-save")).not.toBeInTheDocument();
    expect(screen.queryByTestId("course-new-button")).not.toBeInTheDocument();
    expect(screen.getByTestId("course-performance")).toBeDisabled();
  });

  it("unrelated roles do not call the API", () => {
    render(
      <CourseOperationManagementPage user={{ ...faculty, roles: ["R07"] }} />,
    );
    expect(
      screen.getByText("강좌 운영 실적 접근 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("admin override can reach the feature", async () => {
    render(
      <CourseOperationManagementPage user={{ ...faculty, roles: ["R09"] }} />,
    );
    await select();
    expect(screen.getByTestId("course-save")).toBeEnabled();
  });

  it("empty results and unavailable settings disable saving", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        ...listing,
        achievements: [],
        managementItems: [],
        totalElements: 0,
      },
      meta: {},
    });
    render(<CourseOperationManagementPage user={faculty} />);
    await screen.findByText("조회된 실적이 없습니다");
    expect(screen.getByTestId("course-save")).toBeDisabled();
  });

  it("server object fields are displayed without breaking existing list field support", async () => {
    render(<CourseOperationManagementPage user={faculty} />);
    await select();
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(400, "입력값 오류", {
        code: "VALIDATION_ERROR",
        message: "입력값 오류",
        fields: { performanceDetails: "실적내역 오류" } as unknown as {
          field: string;
          message: string;
        }[],
      }),
    );
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("실적내역 오류");
    expect(screen.getByTestId("course-performance")).toHaveValue("운영 내역");
  });

  it("failed list displays error and not dummy rows", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("network"));
    render(<CourseOperationManagementPage user={faculty} />);
    await screen.findByText("강좌 운영 실적을 처리하지 못했습니다.");
    expect(screen.queryByText("EDU-55")).not.toBeInTheDocument();
  });

  it("requires confirmation and does not send on cancellation", async () => {
    render(<CourseOperationManagementPage user={faculty} />);
    await select();
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(screen.getByTestId("course-save"));
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "PUT"),
    ).toBe(false);
  });
});
