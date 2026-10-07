import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { EmploymentRateAchievementManagementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";

const base = "/api/business/employment-rate-achievements";
const row = {
  achievementId: 7,
  managementNo: "ER-007",
  teacherName: "김교원",
  managementItemCode: "EMP",
  achievementDate: "2025-03-01",
  achievementName: "취업률 실적",
  certificationStatus: "DRAFT",
  attachmentIds: [],
};
const reply = (data: unknown, status = 200, error?: unknown) =>
  ({
    ok: status < 400,
    status,
    json: async () => ({
      success: status < 400,
      data,
      error,
      meta: {},
    }),
  }) as Response;
const fetchMock = vi.fn<typeof fetch>();
beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
  vi.spyOn(window, "confirm").mockReturnValue(true);
});
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
function listAndDetail(status = "DRAFT") {
  fetchMock.mockImplementation(async (url) =>
    String(url).includes("/7")
      ? reply({
          ...row,
          certificationStatus: status,
        })
      : reply({
          achievements: [row],
          totalElements: 1,
        }),
  );
}
describe("취업률 실적 관리", () => {
  it("loads real detail and locks confirmed records without an editable status", async () => {
    listAndDetail("EVALUATION_CONFIRMED");

    render(<EmploymentRateAchievementManagementPage roles={["R01"]} />);

    fireEvent.click(await screen.findByRole("button", { name: "ER-007 상세" }));

    await screen.findByText("평가확정 실적은 수정할 수 없습니다.");

    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();

    expect(screen.getByLabelText("실적명")).toBeDisabled();

    expect(fetchMock).toHaveBeenCalledWith(
      `${base}/7`,
      expect.objectContaining({ credentials: "include" }),
    );
  });

  it("blocks missing required fields before confirmation or network mutation", async () => {
    fetchMock.mockResolvedValue(
      reply({
        achievements: [],
        totalElements: 0,
      }),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R01"]} />);

    await screen.findByText("조회된 실적이 없습니다");

    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(
      await screen.findByText("관리항목코드를 입력하세요."),
    ).toBeInTheDocument();

    expect(window.confirm).not.toHaveBeenCalled();

    expect(
      fetchMock.mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });

  it("uses PUT for selected records and refreshes persisted detail after confirmation", async () => {
    listAndDetail();

    render(<EmploymentRateAchievementManagementPage roles={["R01"]} />);

    fireEvent.click(await screen.findByRole("button", { name: "ER-007 상세" }));

    await waitFor(() =>
      expect(screen.getByLabelText("실적명")).toHaveValue("취업률 실적"),
    );

    fireEvent.change(screen.getByLabelText("실적명"), {
      target: { value: "수정 실적" },
    });

    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        `${base}/7`,
        expect.objectContaining({
          method: "PUT",
          body: JSON.stringify({
            managementItemCode: "EMP",
            achievementDate: "2025-03-01",
            achievementName: "수정 실적",
            attachmentRef: null,
          }),
        }),
      ),
    );

    expect(window.confirm).toHaveBeenCalled();
  });

  it("retains 400 upload diagnostics and prohibits partial commit", async () => {
    fetchMock.mockImplementation(async (url) =>
      String(url).endsWith("/excel-uploads")
        ? reply(
            {
              uploadId: "u-1",
              totalCount: 2,
              successCount: 1,
              errorCount: 1,
              errors: [
                {
                  rowNumber: 3,
                  columnName: "교번",
                  errorReason: "중복 데이터",
                },
              ],
            },
            400,
            { message: "오류 행이 있습니다" },
          )
        : reply([]),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R07"]} />);

    fireEvent.click(screen.getByRole("tab", { name: "Excel 등록" }));

    fireEvent.change(screen.getByLabelText("Excel 파일"), {
      target: { files: [new File(["content"], "rates.xlsx")] },
    });

    fireEvent.click(screen.getByRole("button", { name: "업로드·검증" }));

    await screen.findByText("중복 데이터");

    expect(screen.getByRole("button", { name: "전체 반영" })).toBeDisabled();

    expect(
      screen.getByRole("link", { name: "오류 파일 다운로드" }),
    ).toHaveAttribute("href", `${base}/excel-uploads/u-1/errors/download`);

    expect(
      fetchMock.mock.calls.some(
        ([, init]) => init?.headers && "Content-Type" in init.headers,
      ),
    ).toBe(false);
  });

  it("requires confirmation and commit only after clean upload validation", async () => {
    fetchMock.mockImplementation(async (url) =>
      String(url).endsWith("/commit")
        ? reply({ savedCount: 2 })
        : String(url).endsWith("/excel-uploads")
          ? reply({
              uploadId: "clean",
              totalCount: 2,
              successCount: 2,
              errorCount: 0,
              errors: [],
            })
          : reply([]),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R07"]} />);

    fireEvent.click(screen.getByRole("tab", { name: "Excel 등록" }));

    fireEvent.change(screen.getByLabelText("Excel 파일"), {
      target: { files: [new File(["x"], "rates.xlsx")] },
    });

    fireEvent.click(screen.getByRole("button", { name: "업로드·검증" }));

    await waitFor(() =>
      expect(screen.getByRole("button", { name: "전체 반영" })).toBeEnabled(),
    );

    expect(
      fetchMock.mock.calls.some(([url]) => String(url).endsWith("/commit")),
    ).toBe(false);

    fireEvent.click(screen.getByRole("button", { name: "전체 반영" }));

    await screen.findByText("2건을 전체 반영했습니다.");

    expect(screen.getByRole("button", { name: "전체 반영" })).toBeDisabled();

    expect(fetchMock).toHaveBeenCalledWith(
      `${base}/excel-uploads/clean/commit`,
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ confirmed: true }),
      }),
    );
  });

  it("keeps D4 bulk execution disabled even after preview and never creates a job", async () => {
    fetchMock.mockResolvedValue(
      reply({
        candidates: [],
        policyApproved: false,
      }),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R07"]} />);

    fireEvent.click(screen.getByRole("tab", { name: "일괄 처리" }));

    fireEvent.change(screen.getByLabelText("평가년도"), {
      target: { value: "2025" },
    });

    fireEvent.click(screen.getByRole("button", { name: "대상 미리보기" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining("/bulk-jobs/preview?"),
        expect.anything(),
      ),
    );

    expect(screen.getByRole("button", { name: "일괄 실행" })).toBeDisabled();

    expect(
      fetchMock.mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });

  it("applies page size to the API and server-side Excel download", async () => {
    fetchMock.mockResolvedValue(
      reply({
        achievements: [],
        totalElements: 0,
      }),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R01"]} />);

    await screen.findByText("조회된 실적이 없습니다");

    fireEvent.change(screen.getByLabelText("표시 건수"), {
      target: { value: "50" },
    });

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        `${base}?page=0&pageSize=50`,
        expect.anything(),
      ),
    );

    expect(
      screen.getByRole("link", { name: "Excel 다운로드" }),
    ).toHaveAttribute("href", `${base}/download?page=0&pageSize=50`);
  });

  it("does not mutate when save confirmation is cancelled", async () => {
    listAndDetail();

    vi.mocked(window.confirm).mockReturnValue(false);

    render(<EmploymentRateAchievementManagementPage roles={["R01"]} />);

    fireEvent.click(await screen.findByRole("button", { name: "ER-007 상세" }));

    await waitFor(() =>
      expect(screen.getByLabelText("실적명")).toHaveValue("취업률 실적"),
    );

    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(window.confirm).toHaveBeenCalled();

    expect(
      fetchMock.mock.calls.some(([, init]) => init?.method === "PUT"),
    ).toBe(false);
  });

  it("renders server permission errors without showing success", async () => {
    fetchMock.mockResolvedValue(
      reply(undefined, 403, {
        message: "데이터 범위 밖입니다",
        fields: [],
      }),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R01"]} />);

    expect(await screen.findByText("데이터 범위 밖입니다")).toBeInTheDocument();

    expect(
      screen.getByText("해당 작업의 역할 또는 데이터 범위 권한이 없습니다."),
    ).toBeInTheDocument();

    expect(screen.queryByText("저장되었습니다.")).not.toBeInTheDocument();
  });

  it("does not expose R07 operations to a read-only faculty role", async () => {
    fetchMock.mockResolvedValue(
      reply({
        achievements: [],
        totalElements: 0,
      }),
    );

    render(<EmploymentRateAchievementManagementPage roles={["R02"]} />);

    expect(screen.getByRole("tab", { name: "Excel 등록" })).toBeDisabled();

    expect(screen.getByRole("tab", { name: "일괄 처리" })).toBeDisabled();

    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
  });
});
