import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  apiRequest,
  ApiClientError,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
import { employmentRateApi } from "./employmentRateApi";
import { AppRouter } from "../../app/router";

let actor: CurrentUser;
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: actor,
    logout: vi.fn(),
  }),
}));
vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const row = {
  achievementId: 301,
  managementNo: "ERA-selected",
  teacherUserId: 101,
  teacherName: "교원",
  managementItemCode: "EMPLOYMENT_RATE_ACHIEVEMENT",
  achievementDate: "2026-04-10",
  evaluationYear: "2026",
  title: "실적",
  achievementStatus: "DRAFT",
};
const response = (data: unknown) => ({ success: true, data, meta: {} });

beforeEach(() => {
  vi.restoreAllMocks();
  vi.mocked(apiRequest).mockReset();
  actor = {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles: ["R01"],
    menus: [],
  };
  vi.spyOn(window, "confirm").mockReturnValue(true);
  vi.mocked(apiRequest).mockImplementation(async (path) => {
    if (path.endsWith("/301")) return response(row) as never;
    if (path.endsWith("/histories")) return response([]) as never;
    return response({
      achievements: [row],
      totalElements: 1,
      page: 0,
      pageSize: 20,
      managementItems: [
        {
          code: row.managementItemCode,
          name: "취업률",
          evaluationYear: "2026",
        },
      ],
    }) as never;
  });
});

describe("취업률 실적 화면", () => {
  it("retrieves selected detail rather than copying list state and PUT uses path identity", async () => {
    const save = vi.spyOn(employmentRateApi, "save").mockResolvedValue(
      response({
        achievement: row,
        occurredDateWarning: false,
        warningMessage: "",
      }),
    );
    render(<EmploymentRateAchievementPage user={actor} />);
    fireEvent.click(await screen.findByTestId("detail-301"));
    await waitFor(() =>
      expect(screen.getByTestId("achievement-title")).toHaveValue("실적"),
    );
    fireEvent.change(screen.getByTestId("achievement-title"), {
      target: { value: "변경" },
    });
    fireEvent.click(screen.getByTestId("save-button"));
    await waitFor(() =>
      expect(save).toHaveBeenCalledWith(
        expect.objectContaining({ title: "변경" }),
        301,
      ),
    );
    expect(save.mock.calls[0][0]).not.toHaveProperty("evaluationYear");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/301",
    );
  });

  it("locks confirmed detail and hides operator actions from faculty", async () => {
    vi.spyOn(employmentRateApi, "detail").mockResolvedValue(
      response({ ...row, achievementStatus: "EVALUATION_CONFIRMED" }),
    );
    render(<EmploymentRateAchievementPage user={actor} />);
    fireEvent.click(await screen.findByTestId("detail-301"));
    await screen.findByText("확정 또는 제출된 실적은 수정할 수 없습니다.");
    expect(screen.getByTestId("save-button")).toBeDisabled();
    expect(screen.getByTestId("attachment-ref")).toBeDisabled();
    expect(screen.queryByTestId("excel-tab")).not.toBeInTheDocument();
  });

  it("read-only roles cannot save or edit", async () => {
    actor.roles = ["R02"];
    render(<EmploymentRateAchievementPage user={actor} />);
    await screen.findByTestId("detail-301");
    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();
    expect(screen.getByTestId("achievement-title")).toBeDisabled();
  });

  it("does not issue save until required fields are provided", async () => {
    const save = vi.spyOn(employmentRateApi, "save");
    render(<EmploymentRateAchievementPage user={actor} />);
    await screen.findByTestId("detail-301");
    fireEvent.click(screen.getByTestId("save-button"));
    expect(save).not.toHaveBeenCalled();
    expect(screen.getAllByText("필수 항목을 입력하세요.")).toHaveLength(3);
  });

  it("renders empty list", async () => {
    vi.spyOn(employmentRateApi, "list").mockResolvedValue(
      response({
        achievements: [],
        managementItems: [],
        totalElements: 0,
        page: 0,
        pageSize: 20,
      }),
    );
    render(<EmploymentRateAchievementPage user={actor} />);
    await screen.findByText("조회된 실적이 없습니다.");
  });

  it("renders failed requests without invented rows", async () => {
    vi.spyOn(employmentRateApi, "list").mockRejectedValue(
      new ApiClientError(403, "forbidden"),
    );
    render(<EmploymentRateAchievementPage user={actor} />);
    await screen.findByText(
      "권한이 없습니다. 역할과 데이터 범위를 확인하세요.",
    );
    expect(screen.queryByTestId("detail-301")).not.toBeInTheDocument();
  });

  it("operator sees upload wizard but not individual write", async () => {
    actor.roles = ["R07"];
    render(<EmploymentRateAchievementPage user={actor} />);
    await screen.findByTestId("excel-panel");
    expect(screen.queryByTestId("individual-tab")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("bulk-tab"));
    expect(screen.getByTestId("bulk-execute")).toBeDisabled();
  });

  it("invalid upload shows counts and forbids commit", async () => {
    actor.roles = ["R07"];
    vi.spyOn(employmentRateApi, "upload").mockResolvedValue({
      success: false,
      meta: {},
      data: {
        uploadId: "selected-upload",
        validationStatus: "REJECTED",
        totalCount: 3,
        successCount: 2,
        errorCount: 1,
        savedCount: 0,
      },
    });
    render(<EmploymentRateAchievementPage user={actor} />);
    fireEvent.change(screen.getByTestId("excel-file"), {
      target: { files: [new File(["data"], "valid.xlsx")] },
    });
    fireEvent.click(screen.getByTestId("validate-button"));
    await screen.findByTestId("upload-result");
    expect(screen.getByTestId("commit-button")).toBeDisabled();
    expect(screen.getByTestId("error-download")).toBeInTheDocument();
  });

  it("valid upload needs explicit confirmation before commit", async () => {
    actor.roles = ["R07"];
    vi.spyOn(employmentRateApi, "upload").mockResolvedValue(
      response({
        uploadId: "selected-upload",
        validationStatus: "VALIDATED",
        totalCount: 2,
        successCount: 2,
        errorCount: 0,
        savedCount: 0,
      }),
    );
    const commit = vi.spyOn(employmentRateApi, "commit").mockResolvedValue(
      response({
        uploadId: "selected-upload",
        validationStatus: "COMMITTED",
        totalCount: 2,
        successCount: 2,
        errorCount: 0,
        savedCount: 2,
      }),
    );
    render(<EmploymentRateAchievementPage user={actor} />);
    fireEvent.change(screen.getByTestId("excel-file"), {
      target: { files: [new File(["data"], "valid.xlsx")] },
    });
    fireEvent.click(screen.getByTestId("validate-button"));
    await screen.findByTestId("commit-button");
    expect(commit).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("commit-button"));
    await screen.findByText("2건 반영했습니다.");
    expect(commit).toHaveBeenCalledWith("selected-upload");
  });

  it("authorized existing menu entry reaches the registered screen", async () => {
    actor.menus = [
      {
        menuId: 701,
        menuName: "취업률 실적 관리",
        screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENT",
        url: "/faculty/education/employment-rate-achievements",
        displayOrder: 1,
        children: [],
      },
    ];
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    const links = screen.getAllByRole("link", {
      name: "취업률 실적 관리",
      hidden: true,
    });
    fireEvent.click(links[0]);
    await screen.findByTestId("employment-rate-page");
    expect(window.location.pathname).toBe(
      "/faculty/education/employment-rate-achievements",
    );
  });
});
