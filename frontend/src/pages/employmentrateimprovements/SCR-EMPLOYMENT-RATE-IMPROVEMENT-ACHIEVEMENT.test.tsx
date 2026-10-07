import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { EmploymentRateImprovementAchievementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
import { AppRouter } from "../../app/router";
import { useAuth } from "../../app/AuthProvider";

vi.mock("../../app/AuthProvider", () => ({ useAuth: vi.fn() }));

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const base = "/api/business/employment-rate-improvements";
const user: CurrentUser = {
  userId: 11,
  loginId: "teacher",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
const row = {
  achievementId: 83,
  managementNo: "EMP-083",
  teacherUserId: 11,
  teacherName: "교원",
  organizationCode: "EDU",
  evaluationYear: "2026",
  managementItemCode: "EMP-ITEM",
  achievementDate: "2026-04-10",
  achievementStatus: "DRAFT",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-03",
  mockExamQuestionPeriod: "2026년 1학기",
  attachmentIds: ["attachment-83"],
};
const envelope = <T,>(data: T) => ({ success: true, data, meta: {} });
let persisted = { ...row };
let total = 101;
let failList = false;
let failDetail = false;
let failSave: ApiClientError | null = null;

function mutations() {
  return vi
    .mocked(apiRequest)
    .mock.calls.filter(([, init]) =>
      ["POST", "PUT"].includes(init?.method ?? "GET"),
    );
}
async function ready() {
  await screen.findByText("EMP-083");
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "조회" })).toBeEnabled(),
  );
}
async function detail() {
  fireEvent.click(screen.getByTestId("employment-improvement-detail-83"));
  await waitFor(() =>
    expect(screen.getByLabelText("모의고사 출제기간")).toHaveValue(
      persisted.mockExamQuestionPeriod,
    ),
  );
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "조회" })).toBeEnabled(),
  );
}
function fillNew() {
  fireEvent.change(screen.getByLabelText("관리항목 *"), {
    target: { value: "EMP-NEW" },
  });
  fireEvent.change(screen.getByLabelText("업적발생일 *"), {
    target: { value: "2026-05-10" },
  });
  fireEvent.change(screen.getByLabelText("특강 시작일"), {
    target: { value: "2026-05-01" },
  });
  fireEvent.change(screen.getByLabelText("특강 종료일"), {
    target: { value: "2026-05-02" },
  });
  fireEvent.change(screen.getByLabelText("모의고사 출제기간"), {
    target: { value: "2026년 2학기" },
  });
}

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    persisted = { ...row };
    total = 101;
    failList = false;
    failDetail = false;
    failSave = null;
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(useAuth).mockReturnValue({
      status: "authenticated",
      user,
      error: null,
      login: vi.fn(),
      logout: vi.fn(),
      refresh: vi.fn(),
    });
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      const method = init?.method ?? "GET";
      if (method === "GET" && path.startsWith(`${base}?`)) {
        if (failList) throw new ApiClientError(500, "목록 조회 실패");
        const query = new URL(path, "https://test.invalid").searchParams;
        return envelope({
          achievements: total ? [persisted] : [],
          page: Number(query.get("page")),
          pageSize: Number(query.get("pageSize")),
          totalElements: total,
        });
      }
      if (method === "GET" && path === `${base}/83`) {
        if (failDetail) throw new ApiClientError(500, "상세 조회 실패");
        return envelope(persisted);
      }
      if (
        (method === "POST" && path === base) ||
        (method === "PUT" && path === `${base}/83`)
      ) {
        if (failSave) throw failSave;
        persisted = { ...persisted, ...JSON.parse(String(init?.body)) };
        return envelope({
          achievement: persisted,
          occurredDateWarning: false,
          warningMessage: null,
        });
      }
      throw new Error(`Unexpected API: ${method} ${path}`);
    });
  });
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/");
  });

  it("reaches the canonical screen through the authenticated application route", async () => {
    window.history.replaceState(
      {},
      "",
      "/faculty/education/employment-rate-improvements",
    );
    render(<AppRouter />);
    await ready();
    expect(screen.getByTestId("employment-improvement-page")).toHaveAttribute(
      "data-screen-id",
      "SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
    );
    expect(apiRequest).toHaveBeenCalledWith(`${base}?page=0&pageSize=20`);
    expect(screen.getByRole("button", { name: "신규 등록" })).toBeEnabled();
  });

  it("loads the list, submits all search filters, and requests real detail data", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    fireEvent.change(screen.getByLabelText("관리번호"), {
      target: { value: " EMP-083 " },
    });
    fireEvent.change(screen.getByLabelText("성명"), {
      target: { value: "교원" },
    });
    fireEvent.change(screen.getByLabelText("검색 관리항목"), {
      target: { value: "EMP-ITEM" },
    });
    fireEvent.change(screen.getByLabelText("검색 업적상태"), {
      target: { value: "DRAFT" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        expect.stringContaining("managementNo=EMP-083"),
      ),
    );
    await ready();
    const calls = vi.mocked(apiRequest).mock.calls;
    const path = calls[calls.length - 1][0];
    const query = new URL(path, "https://test.invalid").searchParams;
    expect(Object.fromEntries(query)).toMatchObject({
      teacherName: "교원",
      managementItemCode: "EMP-ITEM",
      achievementStatus: "DRAFT",
    });
    persisted = { ...row, mockExamQuestionPeriod: "서버 상세값" };
    await detail();
    expect(apiRequest).toHaveBeenCalledWith(`${base}/83`);
    expect(screen.getByLabelText("모의고사 출제기간")).toHaveValue(
      "서버 상세값",
    );
  });

  it("creates only the approved request fields and requeries saved detail and list", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    fireEvent.click(screen.getByRole("button", { name: "신규 등록" }));
    fillNew();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("저장되었습니다.");
    expect(mutations()).toHaveLength(1);
    expect(mutations()[0][0]).toBe(base);
    expect(mutations()[0][1]?.method).toBe("POST");
    expect(JSON.parse(String(mutations()[0][1]?.body))).toEqual({
      managementItemCode: "EMP-NEW",
      achievementDate: "2026-05-10",
      specialLectureStartDate: "2026-05-01",
      specialLectureEndDate: "2026-05-02",
      mockExamQuestionPeriod: "2026년 2학기",
      attachmentIds: [],
    });
    expect(apiRequest).toHaveBeenCalledWith(`${base}/83`);
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.filter(([path]) => path.startsWith(`${base}?`)),
    ).toHaveLength(2);
    expect(screen.getByLabelText("모의고사 출제기간")).toHaveValue(
      "2026년 2학기",
    );
  });

  it("updates an own draft with PUT and preserves existing attachment IDs", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    await detail();
    fireEvent.change(screen.getByLabelText("모의고사 출제기간"), {
      target: { value: "수정 기간" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("저장되었습니다.");
    expect(mutations()[0][0]).toBe(`${base}/83`);
    expect(mutations()[0][1]?.method).toBe("PUT");
    expect(JSON.parse(String(mutations()[0][1]?.body))).toMatchObject({
      mockExamQuestionPeriod: "수정 기간",
      attachmentIds: ["attachment-83"],
    });
    expect(screen.getByLabelText("모의고사 출제기간")).toHaveValue("수정 기간");
  });

  it("blocks missing required values, reversed dates, and cancelled confirmation", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(
      await screen.findByText("관리항목을 입력하세요."),
    ).toBeInTheDocument();
    expect(screen.getByText("업적발생일을 입력하세요.")).toBeInTheDocument();
    expect(window.confirm).not.toHaveBeenCalled();
    fillNew();
    fireEvent.change(screen.getByLabelText("특강 종료일"), {
      target: { value: "2026-04-30" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(
      await screen.findByText("특강 종료일은 시작일 이후여야 합니다."),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("특강 종료일"), {
      target: { value: "2026-05-01" },
    });
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(window.confirm).toHaveBeenCalled();
    expect(mutations()).toHaveLength(0);
  });

  it.each([
    "EVALUATION_CONFIRMED",
    "DEPARTMENT_CONFIRMED",
    "CERTIFIED",
    "SUBMITTED",
  ])("locks all editable fields for %s", async (status) => {
    persisted = { ...row, achievementStatus: status };
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    await detail();
    expect(
      screen.getByTestId("employment-improvement-lock-message"),
    ).toBeInTheDocument();
    for (const label of [
      "관리항목 *",
      "업적발생일 *",
      "특강 시작일",
      "특강 종료일",
      "모의고사 출제기간",
    ]) {
      expect(screen.getByLabelText(label)).toBeDisabled();
    }
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(mutations()).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "신규 등록" }));
    expect(screen.getByLabelText("관리항목 *")).toBeEnabled();
  });

  it.each(["DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"])(
    "permits own rejected status %s",
    async (status) => {
      persisted = { ...row, achievementStatus: status };
      render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
      await ready();
      await detail();
      expect(screen.getByRole("button", { name: "저장" })).toBeEnabled();
    },
  );

  it.each(["R02", "R04"])("allows %s reads but no writes", async (role) => {
    render(
      <EmploymentRateImprovementAchievementPage
        currentUser={{ ...user, roles: [role] }}
      />,
    );
    await ready();
    await detail();
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "신규 등록" }),
    ).not.toBeInTheDocument();
    expect(mutations()).toHaveLength(0);
  });

  it("locks another teacher's draft and denies unauthorized reads", async () => {
    persisted = { ...row, teacherUserId: 99 };
    const view = render(
      <EmploymentRateImprovementAchievementPage currentUser={user} />,
    );
    await ready();
    await detail();
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
    view.unmount();
    vi.mocked(apiRequest).mockClear();
    render(
      <EmploymentRateImprovementAchievementPage
        currentUser={{ ...user, roles: ["R07"] }}
      />,
    );
    expect(
      screen.getByText("취업률 제고 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("uses 20/50/100 page sizes and next/previous pages with reset", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    expect(apiRequest).toHaveBeenCalledWith(`${base}?page=0&pageSize=20`);
    fireEvent.click(screen.getByRole("button", { name: "다음" }));
    await screen.findByText("총 101건 / 2페이지");
    await ready();
    expect(apiRequest).toHaveBeenCalledWith(`${base}?page=1&pageSize=20`);
    fireEvent.click(screen.getByRole("button", { name: "이전" }));
    await screen.findByText("총 101건 / 1페이지");
    await ready();
    for (const size of [50, 100]) {
      fireEvent.change(screen.getByLabelText("표시 건수"), {
        target: { value: String(size) },
      });
      await waitFor(() =>
        expect(apiRequest).toHaveBeenCalledWith(
          `${base}?page=0&pageSize=${size}`,
        ),
      );
      await ready();
    }
  });

  it("shows API field arrays without losing input", async () => {
    failSave = new ApiClientError(400, "입력값 오류", {
      code: "VALIDATION_ERROR",
      message: "입력값 오류",
      fields: [
        { field: "managementItemCode", message: "허용되지 않은 관리항목" },
        { field: "managementItemCode", message: "관리항목 설정을 확인하세요" },
        { field: "specialLectureEndDate", message: "기간을 확인하세요" },
      ],
    });
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    fillNew();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(
      await screen.findByText("허용되지 않은 관리항목"),
    ).toBeInTheDocument();
    expect(screen.getByText("관리항목 설정을 확인하세요")).toBeInTheDocument();
    expect(screen.getByText("기간을 확인하세요")).toBeInTheDocument();
    expect(screen.getByLabelText("관리항목 *")).toHaveValue("EMP-NEW");
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "저장" })).toBeEnabled(),
    );
  });

  it("blocks stale edits after a detail failure until explicit new registration", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    await detail();
    failDetail = true;
    fireEvent.click(screen.getByTestId("employment-improvement-detail-83"));
    await screen.findByText("상세 조회 실패");
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "신규 등록" }));
    expect(screen.getByRole("button", { name: "저장" })).toBeEnabled();
  });

  it("does not retry an accepted mutation when saved detail requery fails", async () => {
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    fillNew();
    failDetail = true;
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText(
      "저장되었으나 상세 재조회에 실패했습니다. 상세를 다시 조회하세요.",
    );
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "조회" })).toBeEnabled(),
    );
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
    expect(screen.getByText("상세 조회 실패")).toBeInTheDocument();
    expect(mutations()).toHaveLength(1);
  });

  it("preserves occurrence-date warnings after saved detail and list refresh", async () => {
    const original = vi.mocked(apiRequest).getMockImplementation()!;
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      const response = await original(path, init);
      if (init?.method === "POST") {
        return envelope({
          achievement: persisted,
          occurredDateWarning: true,
          warningMessage: "평가대상 기간 밖 발생일입니다.",
        });
      }
      return response;
    });
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await ready();
    fillNew();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("평가대상 기간 밖 발생일입니다.");
    expect(screen.getByLabelText("업적발생일 *")).toHaveValue("2026-05-10");
    expect(apiRequest).toHaveBeenCalledWith(`${base}/83`);
  });

  it("shows server permission denial without offering mutations", async () => {
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(403, "데이터 범위 권한 없음"),
    );
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await screen.findByText("취업률 제고 실적 관리 권한이 없습니다");
    expect(
      screen.queryByRole("button", { name: "저장" }),
    ).not.toBeInTheDocument();
    expect(mutations()).toHaveLength(0);
  });

  it("renders empty and retryable list error states", async () => {
    total = 0;
    render(<EmploymentRateImprovementAchievementPage currentUser={user} />);
    await screen.findByText("조회된 취업률 제고 실적이 없습니다");
    failList = true;
    fireEvent.click(screen.getByRole("button", { name: "새로고침" }));
    await screen.findByText("목록 조회 실패");
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "새로고침" })).toBeEnabled(),
    );
    failList = false;
    total = 1;
    fireEvent.click(screen.getByRole("button", { name: "새로고침" }));
    await ready();
    expect(screen.queryByText("목록 조회 실패")).not.toBeInTheDocument();
  });
});
