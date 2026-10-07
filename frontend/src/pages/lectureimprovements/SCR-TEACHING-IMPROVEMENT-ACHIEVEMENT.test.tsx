import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { TeachingImprovementAchievementPage } from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
const row = {
  achievementId: 41,
  managementNo: "LI-fixture",
  teacherUserId: 101,
  evaluationYear: "2026",
  managementItemCode: "EDU-ITEM",
  achievementDate: "2025-12-31",
  achievementContent: "수업 개선",
  academicYear: 2025,
  semester: 2,
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const list = {
  success: true,
  data: { achievements: [row], totalElements: 1 },
  meta: {},
};

describe("강의개선 실적", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("reads actual detail and updates selected path without identity fields in body", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list)
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: { ...row, academicYear: 2026 },
          occurredDateWarning: true,
          warningMessage: "발생일 경고",
        },
        meta: {},
      })
      .mockResolvedValue(list);
    render(<TeachingImprovementAchievementPage user={user} />);
    await screen.findByText("LI-fixture");
    fireEvent.click(screen.getByTestId("lecture-detail-41"));
    await waitFor(() =>
      expect(screen.getByLabelText("학년도")).toHaveValue(2025),
    );
    fireEvent.change(screen.getByLabelText("학년도"), {
      target: { value: "2026" },
    });
    fireEvent.click(screen.getByTestId("lecture-save"));
    await screen.findByText("발생일 경고");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements/41",
      {
        method: "PUT",
        body: JSON.stringify({
          managementItemCode: "EDU-ITEM",
          achievementDate: "2025-12-31",
          achievementContent: "수업 개선",
          academicYear: 2026,
          semester: 2,
          attachmentIds: [],
        }),
      },
    );
  });

  it("creates through POST, and blocks missing required fields before any write", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], totalElements: 0 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievement: row, occurredDateWarning: false },
        meta: {},
      })
      .mockResolvedValue(list);
    render(<TeachingImprovementAchievementPage user={user} />);
    await screen.findByText("등록된 실적이 없습니다.");
    fireEvent.click(screen.getByTestId("lecture-save"));
    expect(apiRequest).toHaveBeenCalledTimes(1);
    fireEvent.change(screen.getByLabelText("관리항목코드"), {
      target: { value: "EDU-ITEM" },
    });
    fireEvent.change(screen.getByLabelText("업적발생일"), {
      target: { value: "2025-12-31" },
    });
    fireEvent.change(screen.getByLabelText("학년도"), {
      target: { value: "2025" },
    });
    fireEvent.change(screen.getByLabelText("학기"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("실적내용"), {
      target: { value: "수업 개선" },
    });
    fireEvent.click(screen.getByTestId("lecture-save"));
    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements",
      expect.objectContaining({
        method: "POST",
      }),
    );
  });

  it("confirmed details disable edits; reader roles cannot create", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list)
      .mockResolvedValueOnce({
        success: true,
        data: { ...row, achievementStatus: "EVALUATION_CONFIRMED" },
        meta: {},
      });
    render(<TeachingImprovementAchievementPage user={user} />);
    await screen.findByText("LI-fixture");
    fireEvent.click(screen.getByTestId("lecture-detail-41"));
    await screen.findByText("타인 또는 현재 상태의 실적은 수정할 수 없습니다.");
    expect(screen.getByTestId("lecture-save")).toBeDisabled();
    expect(screen.getByLabelText("학년도")).toBeDisabled();
  });

  it("handles permission-denied states", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "접근 거부"),
    );
    render(
      <TeachingImprovementAchievementPage user={{ ...user, roles: ["R04"] }} />,
    );
    await screen.findByText("강의개선 실적 권한이 없습니다");
  });

  it("reader role sees real results with disabled create controls", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list);
    render(
      <TeachingImprovementAchievementPage user={{ ...user, roles: ["R02"] }} />,
    );
    await screen.findByText("LI-fixture");
    expect(screen.getByTestId("lecture-save")).toBeDisabled();
    expect(screen.getByTestId("lecture-new")).toBeDisabled();
  });

  it("displays server field errors without reporting success", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list)
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockRejectedValueOnce(
        new ApiClientError(400, "입력값을 확인하세요.", {
          code: "VALIDATION_ERROR",
          message: "입력값을 확인하세요.",
          fields: [{ field: "academicYear", message: "학년도 오류" }],
        }),
      );
    render(<TeachingImprovementAchievementPage user={user} />);
    await screen.findByText("LI-fixture");
    fireEvent.click(screen.getByTestId("lecture-detail-41"));
    await waitFor(() =>
      expect(screen.getByLabelText("학년도")).toHaveValue(2025),
    );
    fireEvent.click(screen.getByTestId("lecture-save"));
    await screen.findByText("학년도 오류");
    expect(screen.queryByText("저장되었습니다.")).not.toBeInTheDocument();
  });

  it("role gate rejects unauthorized direct route without making API calls", async () => {
    render(
      <TeachingImprovementAchievementPage user={{ ...user, roles: ["R07"] }} />,
    );
    await screen.findByText("강의개선 실적 권한이 없습니다");
    expect(apiRequest).not.toHaveBeenCalled();
  });
});
