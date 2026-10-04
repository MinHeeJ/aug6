import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "../../app/router";

const authenticatedUser = vi.hoisted(() => ({
  userId: 83,
  loginId: "achievement-reader",
  name: "교육영역 조회자",
  roles: ["R01"],
  menus: [],
}));

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: authenticatedUser,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

vi.mock("../../components/layout/AdminShell", () => ({
  AdminShell: ({ children }: { children: ReactNode }) => (
    <main>{children}</main>
  ),
}));

vi.mock(
  "../employmentrateimprovements/SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
  () => ({
    EmploymentRateImprovementsPage: () => <div>취업률 제고 화면</div>,
  }),
);

vi.mock("../courseoperations/SCR-COURSE-OPERATIONS", () => ({
  CourseOperationsPage: () => <div>강좌 개설·운영 화면</div>,
}));

vi.mock("../lectureimprovements/SCR-LECTURE-IMPROVEMENTS", () => ({
  LectureImprovementsPage: () => <div>강의개선 화면</div>,
}));

vi.mock(
  "../employmentrateachievements/SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
  () => ({
    EmploymentRateAchievementsPage: () => <div>취업률 실적 화면</div>,
  }),
);

describe("BASIC-83 교육영역 실적 route registration", () => {
  afterEach(() => {
    authenticatedUser.roles = ["R01"];
    window.history.replaceState({}, "", "/");
  });

  it.each([
    ["/faculty/employment-rate-improvement-achievements", "취업률 제고 화면"],
    ["/faculty/course-offering-operation-achievements", "강좌 개설·운영 화면"],
    ["/faculty/teaching-improvement-achievements", "강의개선 화면"],
    ["/faculty/employment-rate-achievements", "취업률 실적 화면"],
    ["/faculty/education/employment-rate-improvements", "취업률 제고 화면"],
    ["/faculty/education/course-operations", "강좌 개설·운영 화면"],
    ["/faculty/education/lecture-improvements", "강의개선 화면"],
    ["/faculty/education/employment-rate-achievements", "취업률 실적 화면"],
  ])("renders the authorized feature page at %s", (path, expectedScreen) => {
    window.history.replaceState({}, "", path);

    render(<AppRouter />);

    expect(screen.getByText(expectedScreen)).toBeInTheDocument();
  });

  it("registers the R07 upload and bulk-operation entry at the canonical route", () => {
    authenticatedUser.roles = ["R07"];
    window.history.replaceState(
      {},
      "",
      "/faculty/employment-rate-achievements",
    );

    render(<AppRouter />);

    expect(screen.getByText("취업률 실적 화면")).toBeInTheDocument();
  });

  it("blocks R07 from a reader-only education-achievement route", () => {
    authenticatedUser.roles = ["R07"];
    window.history.replaceState(
      {},
      "",
      "/faculty/education/lecture-improvements",
    );

    render(<AppRouter />);

    expect(
      screen.getByText("강의개선 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
  });

  it("blocks a role that is not allowed to read education achievements", () => {
    authenticatedUser.roles = ["R03"];
    window.history.replaceState(
      {},
      "",
      "/faculty/education/lecture-improvements",
    );

    render(<AppRouter />);

    expect(
      screen.getByText("강의개선 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
  });
});
