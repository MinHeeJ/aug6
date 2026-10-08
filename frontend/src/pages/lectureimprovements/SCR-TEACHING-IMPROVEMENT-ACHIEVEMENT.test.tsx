import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { LectureImprovementPage } from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const user = (role: string): CurrentUser => ({
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: [role],
  menus: [],
});
const row = {
  achievementId: 9,
  managementNo: "EDU-test",
  teacherUserId: 101,
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  achievementStatus: "DRAFT",
  academicYear: "2025",
  semester: "1",
  achievementContent: "개선",
  attachmentIds: ["ref"],
};
const page = {
  achievements: [row],
  totalElements: 1,
  academicYears: ["2025", "2026"],
  semesters: ["1"],
  managementItems: [
    {
      code: "LECTURE_IMPROVEMENT",
      name: "강의개선",
      teacherEditablePart: "SELF_REPORT",
    },
  ],
};
const ok = (data: unknown) => ({ success: true, data, meta: {} });

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockImplementation(async (path) =>
    ok(path.includes("/9") ? row : page),
  );
  vi.spyOn(window, "confirm").mockReturnValue(true);
});

describe("강의개선 실적", () => {
  it("loads detail from selected identity and updates through PUT with no lifecycle fields", async () => {
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText("EDU-test");
    fireEvent.click(screen.getByRole("button", { name: "상세" }));
    await screen.findByText("실적 상세·수정");
    fireEvent.change(screen.getByLabelText("실적내용"), {
      target: { value: "개선 후" },
    });
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "PUT")
        return ok({
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "발생일 경고",
        });
      return ok(path.includes("/9") ? row : page);
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("발생일 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/lecture-improvements/9");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "LECTURE_IMPROVEMENT",
      achievementDate: "2026-04-10",
      academicYear: 2025,
      semester: 1,
      achievementContent: "개선 후",
      attachmentIds: ["ref"],
    });
  });

  it("creates only with POST and refreshes the list", async () => {
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText("EDU-test");
    fireEvent.change(screen.getByLabelText("관리항목 *"), {
      target: { value: "LECTURE_IMPROVEMENT" },
    });
    fireEvent.change(screen.getByLabelText("업적발생일 *"), {
      target: { value: "2026-04-10" },
    });
    fireEvent.change(screen.getByLabelText("학년도 *"), {
      target: { value: "2025" },
    });
    fireEvent.change(screen.getByLabelText("학기 *"), {
      target: { value: "1" },
    });
    vi.mocked(apiRequest).mockImplementation(async (_, init) =>
      init?.method === "POST"
        ? ok({ achievement: row, occurredDateWarning: false })
        : ok(page),
    );
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it.each(["R02", "R04"])("%s is read-only", async (role) => {
    render(<LectureImprovementPage user={user(role)} />);
    await screen.findByText("EDU-test");
    expect(
      screen.queryByRole("button", { name: "저장" }),
    ).not.toBeInTheDocument();
    expect(screen.getByText("조회 전용입니다.")).toBeInTheDocument();
  });

  it("hides the whole feature from R07 without requesting data", () => {
    render(<LectureImprovementPage user={user("R07")} />);
    expect(
      screen.getByText("강의개선 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("administrator can enter the feature", async () => {
    render(<LectureImprovementPage user={user("R09")} />);
    await screen.findByText("EDU-test");
    expect(
      screen.getByRole("button", { name: "신규 입력" }),
    ).toBeInTheDocument();
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "%s is locked",
    async (status) => {
      vi.mocked(apiRequest).mockImplementation(async (path) =>
        ok(path.includes("/9") ? { ...row, achievementStatus: status } : page),
      );
      render(<LectureImprovementPage user={user("R01")} />);
      await screen.findByText("EDU-test");
      fireEvent.click(screen.getByRole("button", { name: "상세" }));
      await screen.findByText("실적 상세·수정");
      expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
      expect(screen.getByLabelText("실적내용")).toBeDisabled();
    },
  );

  it("prevents editing someone else's detail", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path) =>
      ok(path.includes("/9") ? { ...row, teacherUserId: 102 } : page),
    );
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText("EDU-test");
    fireEvent.click(screen.getByRole("button", { name: "상세" }));
    await screen.findByText("실적 상세·수정");
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
  });

  it("empty semester settings disable saving rather than inventing values", async () => {
    vi.mocked(apiRequest).mockResolvedValue(ok({ ...page, semesters: [] }));
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText(
      "선택 가능한 학년도·학기·교원 입력 관리항목 설정이 없어 저장할 수 없습니다.",
    );
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
  });

  it("renders empty list", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      ok({ ...page, achievements: [], totalElements: 0 }),
    );
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText("총 0건");
    expect(screen.queryByText("EDU-test")).not.toBeInTheDocument();
  });

  it("renders request failure", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "조회 실패"),
    );
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText("조회 실패");
  });

  it("renders permission denial", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiClientError(403, "거부"));
    render(<LectureImprovementPage user={user("R01")} />);
    await screen.findByText("강의개선 실적 관리 권한이 없습니다");
  });
});
