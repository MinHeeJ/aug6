import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest, type CurrentUser } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
import type { Achievement } from "./employmentRateApi";
vi.mock("../../app/AuthProvider", () => ({
  useAuth: vi.fn(),
}));
vi.mock("../../api/apiClient", async () => ({
  ...(await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  )),
  apiRequest: vi.fn(),
}));
const base = "/api/business/employment-rate-achievements";
const row: Achievement = {
  achievementId: 83,
  teacherUserId: 1,
  managementNo: "EMP-83",
  teacherName: "교원",
  managementItemCode: "EMP",
  achievementDate: "2026-03-01",
  achievementName: "취업 지도",
  achievementStatus: "DRAFT",
  certificationStatus: "DRAFT",
  attachmentIds: [],
};
function session(roles = ["R01"]) {
  vi.mocked(useAuth).mockReturnValue({
    status: "authenticated",
    user: {
      userId: 1,
      roles,
      name: "교원",
      loginId: "teacher",
      menus: [],
    } as CurrentUser,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  });
}
function envelope(data: unknown) {
  return {
    success: true,
    data,
    meta: {},
  };
}
function requests(record = row) {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (path.startsWith(`${base}?`))
      return envelope({
        achievements: [record],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      });
    if (path === `${base}/83` && (!init || init.method !== "PUT"))
      return envelope(record);
    if (
      (path === base && init?.method === "POST") ||
      (path === `${base}/83` && init?.method === "PUT")
    ) {
      return envelope({
        achievement: record,
        occurredDateWarning: false,
        warningMessage: null,
      });
    }
    if (path.startsWith("/api/admin/excel-upload-templates?"))
      return envelope({
        templates: [
          {
            templateId: "t83",
            templateVersion: "1",
            businessType: "EMPLOYMENT_RATE_ACHIEVEMENT",
            effectiveDate: "2026-01-01",
            rules: [],
          },
        ],
      });
    if (path.startsWith("/api/admin/excel-upload-histories?"))
      return envelope({
        histories: [],
      });
    if (path.startsWith("/api/admin/excel-upload-errors?"))
      return envelope({
        errors: [],
      });
    if (path === "/api/admin/excel-uploads/u83/commit")
      return envelope({
        uploadId: "u83",
        savedCount: 2,
      });
    if (path === `${base}/bulk-jobs/job83`)
      return envelope({
        jobId: "job83",
        status: "COMPLETED",
        processedCount: 1,
        unprocessedCount: 1,
        items: [
          {
            targetUserId: 9,
            processedYn: "N",
            unprocessedReason: "기간 밖",
          },
        ],
      });
    throw new Error(`Unexpected request: ${path} ${init?.method ?? "GET"}`);
  });
}
describe("취업률 실적 관리", () => {
  beforeEach(() => {
    session();
    requests();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    vi.mocked(apiRequest).mockReset();
  });
  it("loads detail, saves PUT and requeries persisted values from the nested save result", async () => {
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.click(
      screen.getByRole("button", {
        name: "상세 EMP-83",
      }),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("실적명")).toHaveValue("취업 지도"),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 저장된 상세를 다시 조회했습니다.");
    expect(apiRequest).toHaveBeenCalledWith(`${base}/83`, {
      method: "PUT",
      body: JSON.stringify({
        managementItemCode: "EMP",
        achievementDate: "2026-03-01",
        achievementName: "취업 지도",
        attachmentIds: [],
      }),
    });
  });
  it("preserves existing attachments as read-only detail", async () => {
    requests({ ...row, attachmentIds: ["existing-file"] });
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.click(screen.getByTestId("employment-detail-83"));
    await screen.findByText("existing-file");
    expect(
      screen.getByText(
        "첨부파일은 읽기 전용으로 유지합니다. 이 화면에서는 신규 파일을 첨부하지 않습니다.",
      ),
    ).toBeInTheDocument();
  });
  it("gives every interactive control a unique test ID across tabs and repeated rows", async () => {
    session(["R09"]);
    const normal = vi.mocked(apiRequest).getMockImplementation()!;
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (path.startsWith(`${base}?`))
        return envelope({
          achievements: [
            row,
            { ...row, achievementId: 84, managementNo: "EMP-84" },
          ],
          totalElements: 2,
          page: 0,
          pageSize: 20,
        });
      if (path.startsWith("/api/admin/excel-upload-histories?"))
        return envelope({
          histories: ["u83", "u84"].map((uploadId) => ({
            uploadId,
            originalFileName: `${uploadId}.xlsx`,
            totalCount: 1,
            successCount: 1,
            errorCount: 0,
            savedCount: 1,
            processedAt: "2026-03-01",
          })),
        });
      return normal(path, init);
    });
    const { container } = render(<EmploymentRateAchievementPage />);
    function assertControlIds() {
      const controls = Array.from(
        container.querySelectorAll("button, select, input, textarea"),
      );
      const ids = controls.map((control) =>
        control.getAttribute("data-testid"),
      );
      expect(
        ids.every((id) => !!id && /^employment-[a-z0-9-]+$/.test(id)),
      ).toBe(true);
      expect(new Set(ids).size).toBe(ids.length);
    }
    await screen.findByTestId("employment-detail-84");
    expect(screen.getByTestId("employment-detail-83")).toBeInTheDocument();
    assertControlIds();
    fireEvent.click(screen.getByTestId("employment-upload-tab"));
    await screen.findByTestId("employment-history-errors-u84");
    expect(
      screen.getByTestId("employment-history-errors-u83"),
    ).toBeInTheDocument();
    assertControlIds();
    fireEvent.click(screen.getByTestId("employment-bulk-tab"));
    expect(
      screen.getByTestId("employment-target-condition"),
    ).toBeInTheDocument();
    assertControlIds();
  });
  it("blocks missing fields, confirms registration, and respects cancellation", async () => {
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(
      screen.getByText("관리항목과 실적일자를 입력하세요."),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("관리항목 코드"), {
      target: {
        value: "EMP",
      },
    });
    fireEvent.change(screen.getByLabelText("실적일자"), {
      target: {
        value: "2026-03-01",
      },
    });
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(apiRequest).not.toHaveBeenCalledWith(
      base,
      expect.objectContaining({
        method: "POST",
      }),
    );
    vi.mocked(window.confirm).mockReturnValue(true);
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 저장된 상세를 다시 조회했습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      base,
      expect.objectContaining({
        method: "POST",
      }),
    );
  });
  it.each(["EVALUATION_CONFIRMED", "CERTIFIED", "SUBMITTED"])(
    "locks non-draft %s",
    async (status) => {
      requests({
        ...row,
        achievementStatus: status,
        certificationStatus: status,
      });
      render(<EmploymentRateAchievementPage />);
      await screen.findByText("EMP-83");
      fireEvent.click(
        screen.getByRole("button", {
          name: "상세 EMP-83",
        }),
      );
      await screen.findByText("선택한 실적은 읽기 전용입니다.");
      expect(screen.getByLabelText("실적명")).toBeDisabled();
      expect(
        screen.getByRole("button", {
          name: "저장",
        }),
      ).toBeDisabled();
    },
  );
  it.each(["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"])(
    "allows the owner to edit %s",
    async (status) => {
      requests({
        ...row,
        achievementStatus: status,
        certificationStatus: status,
      });
      render(<EmploymentRateAchievementPage />);
      await screen.findByText("EMP-83");
      fireEvent.click(
        screen.getByRole("button", {
          name: "상세 EMP-83",
        }),
      );
      await waitFor(() =>
        expect(screen.getByLabelText("실적명")).toHaveValue("취업 지도"),
      );
      await waitFor(() =>
        expect(
          screen.getByRole("button", {
            name: "저장",
          }),
        ).toBeEnabled(),
      );
      fireEvent.click(
        screen.getByRole("button", {
          name: "저장",
        }),
      );
      await screen.findByText(
        "저장되었습니다. 저장된 상세를 다시 조회했습니다.",
      );
      expect(apiRequest).toHaveBeenCalledWith(
        `${base}/83`,
        expect.objectContaining({
          method: "PUT",
        }),
      );
    },
  );
  it("locks another owner's draft for R01", async () => {
    requests({
      ...row,
      teacherUserId: 2,
    });
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.click(
      screen.getByRole("button", {
        name: "상세 EMP-83",
      }),
    );
    await screen.findByText("선택한 실적은 읽기 전용입니다.");
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
  });
  it.each(["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"])(
    "R09 can read and update another owner's %s and access upload",
    async (status) => {
      session(["R09"]);
      requests({
        ...row,
        teacherUserId: 2,
        achievementStatus: status,
        certificationStatus: status,
      });
      render(<EmploymentRateAchievementPage />);
      await screen.findByText("EMP-83");
      fireEvent.click(
        screen.getByRole("button", {
          name: "상세 EMP-83",
        }),
      );
      await waitFor(() =>
        expect(
          screen.getByRole("button", {
            name: "저장",
          }),
        ).toBeEnabled(),
      );
      fireEvent.click(
        screen.getByRole("button", {
          name: "저장",
        }),
      );
      await screen.findByText(
        "저장되었습니다. 저장된 상세를 다시 조회했습니다.",
      );
      await waitFor(() =>
        expect(
          screen.getByRole("button", {
            name: "신규 등록",
          }),
        ).toBeEnabled(),
      );
      expect(apiRequest).toHaveBeenCalledWith(
        `${base}/83`,
        expect.objectContaining({
          method: "PUT",
        }),
      );
      fireEvent.click(
        screen.getByRole("tab", {
          name: "Excel 업로드",
        }),
      );
      await waitFor(() =>
        expect(screen.getByLabelText("표준 템플릿")).toHaveTextContent(
          "1 / 2026-01-01",
        ),
      );
      expect(screen.getByLabelText("Excel 파일")).toBeEnabled();
    },
  );
  it.each([
    "EVALUATION_CONFIRMED",
    "CERTIFIED",
    "SUBMITTED",
    "DEPARTMENT_CONFIRMED",
    "UNKNOWN",
  ])("R09 does not bypass the %s workflow lock", async (status) => {
    session(["R09"]);
    requests({
      ...row,
      teacherUserId: 2,
      achievementStatus: status,
      certificationStatus: status,
    });
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.click(
      screen.getByRole("button", {
        name: "상세 EMP-83",
      }),
    );
    await screen.findByText("선택한 실적은 읽기 전용입니다.");
    expect(screen.getByLabelText("실적명")).toBeDisabled();
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "PUT"),
    ).toBe(false);
  });
  it.each(["POST", "PUT"])(
    "displays the nested occurred-date warning after %s",
    async (method) => {
      const normal = vi.mocked(apiRequest).getMockImplementation()!;
      vi.mocked(apiRequest).mockImplementation(async (path, init) => {
        if (init?.method === method)
          return envelope({
            achievement: row,
            occurredDateWarning: true,
            warningMessage: "업적발생일이 평가기간 밖에 있습니다.",
          });
        return normal(path, init);
      });
      render(<EmploymentRateAchievementPage />);
      await screen.findByText("EMP-83");
      if (method === "PUT") {
        fireEvent.click(
          screen.getByRole("button", {
            name: "상세 EMP-83",
          }),
        );
        await waitFor(() =>
          expect(screen.getByLabelText("실적명")).toHaveValue("취업 지도"),
        );
        await waitFor(() =>
          expect(
            screen.getByRole("button", {
              name: "저장",
            }),
          ).toBeEnabled(),
        );
      } else {
        fireEvent.change(screen.getByLabelText("관리항목 코드"), {
          target: {
            value: "EMP",
          },
        });
        fireEvent.change(screen.getByLabelText("실적일자"), {
          target: {
            value: "2026-03-01",
          },
        });
      }
      fireEvent.click(
        screen.getByRole("button", {
          name: "저장",
        }),
      );
      await screen.findByText(
        "저장되었습니다. 저장된 상세를 다시 조회했습니다.",
      );
      expect(
        screen.getByText("업적발생일이 평가기간 밖에 있습니다."),
      ).toBeInTheDocument();
      expect(screen.getByLabelText("실적명")).toHaveValue("취업 지도");
    },
  );
  it("R09 can create a new record", async () => {
    session(["R09"]);
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.change(screen.getByLabelText("관리항목 코드"), {
      target: {
        value: "EMP",
      },
    });
    fireEvent.change(screen.getByLabelText("실적일자"), {
      target: {
        value: "2026-03-01",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다. 저장된 상세를 다시 조회했습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      base,
      expect.objectContaining({
        method: "POST",
      }),
    );
  });
  it("does not allow R02 to mutate records", async () => {
    session(["R02"]);
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    expect(
      screen.queryByRole("button", {
        name: "저장",
      }),
    ).not.toBeInTheDocument();
  });
  it("R07 does not issue forbidden list/detail reads and can query old bulk results", async () => {
    session(["R07"]);
    render(<EmploymentRateAchievementPage />);
    fireEvent.click(
      screen.getByRole("tab", {
        name: "일괄 처리결과",
      }),
    );
    expect(
      screen.getByRole("button", {
        name: "일괄 실행 (정책 승인 대기)",
      }),
    ).toBeDisabled();
    fireEvent.change(screen.getByLabelText("작업 ID"), {
      target: {
        value: "job83",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "결과 조회",
      }),
    );
    await screen.findByText("기간 밖");
    expect(screen.getByText("처리 1건 / 미처리 1건")).toBeInTheDocument();
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([path]) => path.startsWith(`${base}?`)),
    ).toBe(false);
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });
  it("retains 400 upload diagnostics, blocks commit and exposes error export", async () => {
    session(["R07"]);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({
          success: false,
          data: {
            uploadId: "u83",
            validationStatus: "ERROR",
            totalCount: 2,
            successCount: 1,
            errorCount: 1,
            savedCount: 0,
            excludedCount: 0,
            errors: [
              {
                errorId: "e1",
                rowNumber: 2,
                columnName: "managementItemCode",
                errorReason: "중복 실적",
                errorCode: "DUPLICATE",
              },
            ],
          },
          error: {
            message: "중복 파일입니다.",
          },
        }),
      }),
    );
    render(<EmploymentRateAchievementPage />);
    fireEvent.change(screen.getByLabelText("Excel 파일"), {
      target: {
        files: [new File(["xlsx"], "invalid.xlsx")],
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "업로드 및 검증",
      }),
    );
    await screen.findByText("중복 실적");
    expect(
      screen.getByRole("button", {
        name: "검증 자료 반영",
      }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", {
        name: "오류 XLSX 다운로드",
      }),
    ).toBeEnabled();
  });
  it("commits clean staging only once and invalidates validation on file change", async () => {
    session(["R07"]);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () =>
          envelope({
            uploadId: "u83",
            validationStatus: "VALIDATED",
            totalCount: 2,
            successCount: 2,
            errorCount: 0,
            savedCount: 0,
            excludedCount: 0,
            errors: [],
          }),
      }),
    );
    render(<EmploymentRateAchievementPage />);
    fireEvent.change(screen.getByLabelText("Excel 파일"), {
      target: {
        files: [new File(["xlsx"], "valid.xlsx")],
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "업로드 및 검증",
      }),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "검증 자료 반영",
        }),
      ).toBeEnabled(),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "검증 자료 반영",
      }),
    );
    await screen.findByText("2건 반영되었습니다.");
    expect(
      screen.getByRole("button", {
        name: "검증 자료 반영",
      }),
    ).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Excel 파일"), {
      target: {
        files: [new File(["new"], "new.xlsx")],
      },
    });
    expect(
      screen.getByRole("button", {
        name: "검증 자료 반영",
      }),
    ).toBeDisabled();
  });
  it("downloads a real binary XLSX using the active page size", async () => {
    const createUrl = vi.fn().mockReturnValue("blob:employment");
    const revokeUrl = vi.fn();
    class DownloadURL extends URL {
      static createObjectURL = createUrl;
      static revokeObjectURL = revokeUrl;
    }
    vi.stubGlobal("URL", DownloadURL);
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Blob(["xlsx"]), {
        headers: {
          "content-type":
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "content-disposition": "attachment; filename=employment.xlsx",
        },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.change(screen.getByLabelText("페이지 크기"), {
      target: {
        value: "50",
      },
    });
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "조회",
        }),
      ).toBeEnabled(),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "목록 XLSX 다운로드",
      }),
    );
    await screen.findByText("XLSX 파일을 다운로드했습니다.");
    expect(fetchMock).toHaveBeenCalledWith(
      `${base}/download?page=0&pageSize=50`,
      {
        credentials: "include",
      },
    );
    expect(createUrl).toHaveBeenCalledWith(
      expect.objectContaining({
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        size: 13,
      }),
    );
    expect(revokeUrl).toHaveBeenCalledWith("blob:employment");
  });
  it("preserves failed-save input and repeated field errors", async () => {
    const { ApiClientError } = await import("../../api/apiClient");
    requests();
    const normal = vi.mocked(apiRequest).getMockImplementation()!;
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "POST")
        throw new ApiClientError(409, "평가확정 실적입니다.", {
          code: "CONFIRMED_DATA_LOCKED",
          message: "평가확정 실적입니다.",
          fields: [
            {
              field: "managementItemCode",
              message: "기간 확인 필요",
            },
            {
              field: "managementItemCode",
              message: "코드 확인 필요",
            },
          ],
        });
      return normal(path, init);
    });
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.change(screen.getByLabelText("관리항목 코드"), {
      target: {
        value: "EMP",
      },
    });
    fireEvent.change(screen.getByLabelText("실적일자"), {
      target: {
        value: "2026-03-01",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("평가확정 실적입니다.");
    expect(
      screen.getByText("managementItemCode: 기간 확인 필요"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("managementItemCode: 코드 확인 필요"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("관리항목 코드")).toHaveValue("EMP");
  });
  it("blocks duplicate save after an accepted mutation whose detail requery fails", async () => {
    requests();
    const normal = vi.mocked(apiRequest).getMockImplementation()!;
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (path === `${base}/83` && !init) throw new Error("상세 연결 실패");
      return normal(path, init);
    });
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.change(screen.getByLabelText("관리항목 코드"), {
      target: {
        value: "EMP",
      },
    });
    fireEvent.change(screen.getByLabelText("실적일자"), {
      target: {
        value: "2026-03-01",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText(
      "저장은 완료되었으나 상세 재조회에 실패했습니다. 상세를 다시 조회하세요.",
    );
    expect(
      screen.getByRole("button", {
        name: "저장",
      }),
    ).toBeDisabled();
    expect(screen.getByText("상세 연결 실패")).toBeInTheDocument();
  });
  it("renders empty and request failure states", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      envelope({
        achievements: [],
        totalElements: 0,
      }),
    );
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("조회된 취업률 실적이 없습니다.");
    vi.mocked(apiRequest).mockRejectedValue(new Error("서버 연결 실패"));
    fireEvent.click(
      screen.getByRole("button", {
        name: "조회",
      }),
    );
    await screen.findByText("서버 연결 실패");
  });
});
