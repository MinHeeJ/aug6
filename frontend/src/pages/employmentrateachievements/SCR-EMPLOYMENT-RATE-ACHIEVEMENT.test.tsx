import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
import {
  employmentRateRequest,
  employmentRateDownload,
} from "./employmentRateApi";
import { AppRouter } from "../../app/router";

vi.mock("./employmentRateApi", () => ({
  employmentRateRequest: vi.fn(),
  employmentRateDownload: vi.fn(),
}));
const authState = vi.hoisted(() => ({ user: null as CurrentUser | null }));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: authState.user,
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));
vi.mock("../../components/layout/AdminShell", () => ({
  AdminShell: ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  ),
}));

function user(role: string): CurrentUser {
  return {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles: [role],
    menus: [],
  };
}
const row = {
  achievementId: 42,
  teacherUserId: 101,
  teacherName: "교원",
  managementNo: "selected",
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_ACHIEVEMENT",
  achievementName: "실적",
  achievementDate: "2026-04-10",
  achievementStatus: "DRAFT",
};
const listing = {
  achievements: [row],
  totalElements: 1,
  managementItems: [
    {
      managementItemCode: row.managementItemCode,
      managementItemName: "취업률",
      evaluationYear: "2026",
    },
  ],
};

describe("취업률 실적 화면", () => {
  beforeEach(() => {
    vi.mocked(employmentRateRequest).mockReset();
    vi.mocked(employmentRateDownload).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(employmentRateRequest).mockImplementation(async (suffix) => {
      if (suffix === "/42") return row;
      if (suffix === "/excel-uploads/histories") return [];
      return listing;
    });
  });

  it("authorized direct route renders the destination screen", async () => {
    authState.user = user("R01");
    window.history.replaceState(
      {},
      "",
      "/faculty/employment-rate-achievements",
    );
    render(<AppRouter />);
    expect(await screen.findByText("취업률 실적 관리")).toBeInTheDocument();
    expect(await screen.findByText("selected")).toBeInTheDocument();
  });

  it("reads selected detail and uses path-owned PUT identity with immutable year excluded", async () => {
    render(<EmploymentRateAchievementPage user={user("R01")} />);
    fireEvent.click(await screen.findByTestId("employment-rate-detail-42"));
    await screen.findByText("평가연도: 2026 (수정 불가)");
    fireEvent.change(screen.getByTestId("employment-rate-name"), {
      target: { value: "수정 실적" },
    });
    vi.mocked(employmentRateRequest).mockResolvedValueOnce({
      achievement: row,
      occurredDateWarning: false,
    });
    fireEvent.click(screen.getByTestId("employment-rate-save"));
    await waitFor(() =>
      expect(employmentRateRequest).toHaveBeenCalledWith("/42", {
        method: "PUT",
        body: JSON.stringify({
          managementItemCode: row.managementItemCode,
          achievementDate: row.achievementDate,
          achievementName: "수정 실적",
        }),
      }),
    );
    expect(await screen.findByText("저장되었습니다.")).toBeInTheDocument();
  });

  it("locks confirmed records", async () => {
    vi.mocked(employmentRateRequest).mockImplementation(async (suffix) =>
      suffix === "/42"
        ? { ...row, achievementStatus: "EVALUATION_CONFIRMED" }
        : listing,
    );
    render(<EmploymentRateAchievementPage user={user("R01")} />);
    fireEvent.click(await screen.findByTestId("employment-rate-detail-42"));
    await screen.findByText("확정 또는 제출된 실적은 변경할 수 없습니다.");
    expect(screen.getByTestId("employment-rate-save")).toBeDisabled();
    expect(screen.getByTestId("employment-rate-date")).toBeDisabled();
  });

  it("read-only role cannot save", async () => {
    render(<EmploymentRateAchievementPage user={user("R02")} />);
    await screen.findByText("selected");
    expect(
      screen.queryByTestId("employment-rate-save"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("employment-rate-name")).toBeDisabled();
  });

  it("R07 does not call individual list and cannot execute unapproved bulk policy", async () => {
    render(<EmploymentRateAchievementPage user={user("R07")} />);
    await screen.findByText("업로드 이력이 없습니다.");
    expect(
      screen.queryByTestId("employment-rate-individual-tab"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("employment-rate-bulk-execute")).toBeDisabled();
    expect(employmentRateRequest).not.toHaveBeenCalledWith(
      expect.stringContaining("?page="),
    );
  });

  it("validation errors disable commit and preserve download using real returned upload identity", async () => {
    vi.mocked(employmentRateRequest).mockImplementation(async (suffix) => {
      if (suffix === "/excel-uploads")
        return {
          uploadId: "selected-upload",
          totalCount: 2,
          successCount: 1,
          errorCount: 1,
          savedCount: 0,
        };
      if (suffix.endsWith("/errors"))
        return [{ rowNumber: 3, errorReason: "교번 오류" }];
      return [];
    });
    render(<EmploymentRateAchievementPage user={user("R07")} />);
    fireEvent.change(screen.getByTestId("employment-rate-file"), {
      target: { files: [new File(["xlsx"], "rates.xlsx")] },
    });
    fireEvent.click(screen.getByTestId("employment-rate-validate"));
    await screen.findByText("3행: 교번 오류");
    expect(screen.getByTestId("employment-rate-commit")).toBeDisabled();
    fireEvent.click(screen.getByTestId("employment-rate-errors-download"));
    await waitFor(() =>
      expect(employmentRateDownload).toHaveBeenCalledWith(
        "/excel-uploads/selected-upload/errors/download",
        "errors.xlsx",
      ),
    );
  });

  it("empty list and absent DB settings disable save", async () => {
    vi.mocked(employmentRateRequest).mockResolvedValue({
      achievements: [],
      managementItems: [],
      totalElements: 0,
    });
    render(<EmploymentRateAchievementPage user={user("R01")} />);
    await screen.findByText("조회 결과가 없습니다.");
    expect(screen.getByTestId("employment-rate-save")).toBeDisabled();
  });

  it("failed request and permission denial are visible", async () => {
    vi.mocked(employmentRateRequest).mockRejectedValue(
      new ApiClientError(403, "범위 밖입니다."),
    );
    render(<EmploymentRateAchievementPage user={user("R01")} />);
    await screen.findByText("권한 없음: 범위 밖입니다.");
  });

  it("disallowed user sees permission state", () => {
    render(<EmploymentRateAchievementPage user={user("R03")} />);
    expect(screen.getByRole("alert")).toHaveTextContent("권한이 없습니다");
    expect(employmentRateRequest).not.toHaveBeenCalled();
  });
});
