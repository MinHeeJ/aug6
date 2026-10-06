import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
const auth = vi.hoisted(() => ({
  roles: ["R01"],
}));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      roles: auth.roles,
    },
  }),
}));
vi.mock("../../api/apiClient", async () => ({
  ...(await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  )),
  apiRequest: vi.fn(),
}));
const base = "/api/business/employment-rate-achievements";
const row = {
  achievementId: 83,
  managementNo: "EMP-83",
  teacherName: "홍교원",
  managementItemCode: "EMPLOYMENT",
  achievementDate: "2026-04-10",
  achievementName: "취업률 조사",
  attachmentIds: ["file-83", "file-84"],
  certificationStatus: "DRAFT",
};
const ok = (data: unknown) => ({
  success: true,
  data,
  meta: {},
});
const bulkRequest = (actionType = "GENERATE", targetCondition = {}) => ({
  evaluationYear: "2026",
  actionType,
  targetCondition,
});
beforeEach(() => {
  auth.roles = ["R01"];
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockImplementation(async (path) => {
    if (path.includes("histories")) return ok([]) as never;
    if (path === `${base}/83`) return ok(row) as never;
    return ok({
      achievements: [row],
      page: 0,
      pageSize: 20,
      totalElements: 101,
    }) as never;
  });
  vi.spyOn(window, "confirm").mockReturnValue(true);
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
async function openDetail() {
  fireEvent.click(
    await screen.findByRole("button", {
      name: "EMP-83 상세",
    }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("실적명")).toHaveValue(row.achievementName),
  );
}
function openBulk() {
  render(<EmploymentRateAchievementPage />);
  fireEvent.click(
    screen.getByRole("tab", {
      name: "일괄 처리",
    }),
  );
  fireEvent.change(screen.getByLabelText("평가연도 *"), {
    target: {
      value: "2026",
    },
  });
}
describe("취업률 실적 관리 계약", () => {
  it("loads canonical detail fields and persists both attachment IDs with PUT", async () => {
    render(<EmploymentRateAchievementPage />);
    await openDetail();
    expect(screen.getByLabelText("업적발생일 *")).toHaveValue("2026-04-10");
    expect(screen.getByLabelText("첨부 참조 ID (쉼표 구분)")).toHaveValue(
      "file-83, file-84",
    );
    fireEvent.change(screen.getByLabelText("실적명"), {
      target: {
        value: "수정된 실적",
      },
    });
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "PUT") {
        return ok({
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "평가대상 기간 밖 발생일입니다.",
        }) as never;
      }
      if (path === `${base}/83`)
        return ok({
          ...row,
          achievementName: "수정된 실적",
        }) as never;
      return ok({
        achievements: [row],
        totalElements: 1,
      }) as never;
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("평가대상 기간 밖 발생일입니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      `${base}/83`,
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({
          managementItemCode: "EMPLOYMENT",
          achievementDate: "2026-04-10",
          achievementName: "수정된 실적",
          attachmentIds: ["file-83", "file-84"],
        }),
      }),
    );
    expect(screen.getByLabelText("실적명")).toHaveValue("수정된 실적");
  });
  it("validates required values, respects cancellation, and creates with POST", async () => {
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(screen.getByText("관리항목을 입력하세요.")).toBeInTheDocument();
    expect(window.confirm).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("관리항목 *"), {
      target: {
        value: "EMPLOYMENT",
      },
    });
    fireEvent.change(screen.getByLabelText("업적발생일 *"), {
      target: {
        value: "2026-04-10",
      },
    });
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
    vi.mocked(window.confirm).mockReturnValue(true);
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "POST")
        return ok({
          achievement: row,
          occurredDateWarning: false,
        }) as never;
      return ok(
        path === `${base}/83`
          ? row
          : {
              achievements: [row],
              totalElements: 1,
            },
      ) as never;
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      base,
      expect.objectContaining({
        method: "POST",
      }),
    );
  });
  it.each([
    "SUBMITTED",
    "DEPARTMENT_CONFIRMED",
    "DEPARTMENT_REJECTED",
    "CERTIFIED",
    "CERTIFICATION_REJECTED",
    "EVALUATION_CONFIRMED",
    "DELETED",
  ])(
    "locks %s even for R01 and restores a new editable draft",
    async (certificationStatus) => {
      vi.mocked(apiRequest).mockImplementation(
        async (path) =>
          ok(
            path === `${base}/83`
              ? {
                  ...row,
                  certificationStatus,
                }
              : {
                  achievements: [row],
                  totalElements: 1,
                },
          ) as never,
      );
      render(<EmploymentRateAchievementPage />);
      await openDetail();
      for (const label of [
        "관리항목 *",
        "업적발생일 *",
        "실적명",
        "첨부 참조 ID (쉼표 구분)",
      ]) {
        expect(screen.getByLabelText(label)).toBeDisabled();
      }
      expect(
        screen.getByRole("button", {
          name: "저장",
        }),
      ).toBeDisabled();
      fireEvent.click(
        screen.getByRole("button", {
          name: "저장",
        }),
      );
      expect(window.confirm).not.toHaveBeenCalled();
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([, init]) => init?.method === "PUT"),
      ).toBe(false);
      fireEvent.click(
        screen.getByRole("button", {
          name: "신규 등록",
        }),
      );
      expect(
        screen.getByRole("button", {
          name: "저장",
        }),
      ).toBeEnabled();
      expect(screen.getByLabelText("실적명")).toHaveValue("");
    },
  );
  it("restricts R02 to reading and denies unrelated roles", async () => {
    auth.roles = ["R02"];
    const view = render(<EmploymentRateAchievementPage />);
    await openDetail();
    expect(screen.getByLabelText("관리항목 *")).toBeDisabled();
    expect(
      screen.queryByRole("button", {
        name: "저장",
      }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("tab", {
        name: "Excel 일괄등록",
      }),
    ).not.toBeInTheDocument();
    view.unmount();
    auth.roles = ["R09"];
    vi.mocked(apiRequest).mockClear();
    render(<EmploymentRateAchievementPage />);
    expect(
      screen.getByText("취업률 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });
  it("retries failed reads without retaining stale rows", async () => {
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(403, "데이터 범위 권한이 없습니다."),
    );
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("데이터 범위 권한이 없습니다.");
    expect(screen.queryByText("EMP-83")).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", {
        name: "조회",
      }),
    );
    await screen.findByText("EMP-83");
  });
  it("preserves pagination in reads and downloads", async () => {
    render(<EmploymentRateAchievementPage />);
    await screen.findByText("EMP-83");
    fireEvent.change(screen.getByLabelText("페이지 크기"), {
      target: {
        value: "50",
      },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(`${base}?page=0&pageSize=50`),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "다음",
      }),
    );
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(`${base}?page=1&pageSize=50`),
    );
    expect(
      screen.getByRole("link", {
        name: "Excel 다운로드",
      }),
    ).toHaveAttribute("href", `${base}/download?page=1&pageSize=50`);
    fireEvent.change(screen.getByLabelText("페이지 크기"), {
      target: {
        value: "100",
      },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(`${base}?page=0&pageSize=100`),
    );
  });
  it.each([0, 1])(
    "validates multipart and only commits clean uploads (errorCount=%s)",
    async (errorCount) => {
      auth.roles = ["R07"];
      vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue({
          ok: true,
          status: 200,
          json: async () =>
            ok({
              uploadId: "upload-83",
              totalCount: 2,
              successCount: 2 - errorCount,
              errorCount,
              errors: errorCount
                ? [
                    {
                      rowNumber: 3,
                      columnName: "교번",
                      errorReason: "중복 데이터",
                    },
                  ]
                : [],
            }),
        }),
      );
      vi.mocked(apiRequest).mockImplementation(
        async (path) =>
          ok(
            path.endsWith("/commit")
              ? {
                  savedCount: 2,
                }
              : [],
          ) as never,
      );
      render(<EmploymentRateAchievementPage />);
      fireEvent.change(screen.getByLabelText("업로드 파일"), {
        target: {
          files: [new File(["fixture"], "실적.xlsx")],
        },
      });
      fireEvent.click(
        screen.getByRole("button", {
          name: "업로드·검증",
        }),
      );
      await screen.findByText(
        `정상행 ${2 - errorCount}건 / 오류행 ${errorCount}건 / 전체 2건`,
      );
      if (errorCount) {
        expect(
          screen.getByRole("button", {
            name: "전체 반영",
          }),
        ).toBeDisabled();
        expect(
          screen.getByRole("link", {
            name: "오류파일 다운로드",
          }),
        ).toHaveAttribute(
          "href",
          `${base}/excel-uploads/upload-83/errors/download`,
        );
      } else {
        vi.mocked(window.confirm).mockReturnValue(false);
        fireEvent.click(
          screen.getByRole("button", {
            name: "전체 반영",
          }),
        );
        expect(
          vi
            .mocked(apiRequest)
            .mock.calls.some(([path]) => path.endsWith("/commit")),
        ).toBe(false);
        vi.mocked(window.confirm).mockReturnValue(true);
        fireEvent.click(
          screen.getByRole("button", {
            name: "전체 반영",
          }),
        );
        await screen.findByText("2건을 전체 반영했습니다.");
        expect(
          screen.getByRole("button", {
            name: "전체 반영",
          }),
        ).toBeDisabled();
      }
      expect(fetch).toHaveBeenCalledWith(
        `${base}/excel-uploads`,
        expect.objectContaining({
          method: "POST",
          credentials: "include",
          body: expect.any(FormData),
        }),
      );
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([path]) => path.startsWith(`${base}?`)),
      ).toBe(false);
      fireEvent.change(screen.getByLabelText("업로드 파일"), {
        target: {
          files: [new File(["other"], "새실적.xlsx")],
        },
      });
      expect(
        screen.queryByRole("button", {
          name: "전체 반영",
        }),
      ).not.toBeInTheDocument();
    },
  );
  it("shows nonblocking Excel warnings from validation and commit", async () => {
    auth.roles = ["R07"];
    const validationWarning = "행 2: 업적발생일이 평가대상기간 밖입니다.";
    const commitWarning = "행 2: 반영 시 평가대상기간을 다시 확인했습니다.";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () =>
          ok({
            uploadId: "upload-warning",
            originalFileName: "실적.xlsx",
            validationStatus: "VALIDATED",
            totalCount: 1,
            successCount: 1,
            errorCount: 0,
            excludedCount: 0,
            savedCount: 0,
            errors: [],
            warnings: [validationWarning],
            errorDownloadUrl: null,
          }),
      }),
    );
    vi.mocked(apiRequest).mockImplementation(
      async (path) =>
        ok(
          path.endsWith("/commit")
            ? {
                uploadId: "upload-warning",
                savedCount: 1,
                warnings: [commitWarning],
              }
            : [],
        ) as never,
    );
    render(<EmploymentRateAchievementPage />);
    fireEvent.change(screen.getByLabelText("업로드 파일"), {
      target: { files: [new File(["fixture"], "실적.xlsx")] },
    });
    fireEvent.click(screen.getByRole("button", { name: "업로드·검증" }));
    await screen.findByText(validationWarning);
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "전체 반영" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("button", { name: "전체 반영" }));
    await screen.findByText(commitWarning);
    await screen.findByText("1건을 전체 반영했습니다.");
  });

  it("shows server existing count, invalidates edits, confirms execution, and surfaces policy409", async () => {
    auth.roles = ["R07"];
    vi.mocked(apiRequest).mockImplementation(async (path) => {
      if (path === `${base}/bulk-jobs/preview`) {
        return ok({
          existingAchievementCount: 7,
          policyApproved: false,
        }) as never;
      }
      if (path === `${base}/bulk-jobs`)
        throw new ApiClientError(409, "OQ-83-01 실행 정책 미확정");
      if (path.endsWith("/seed-job"))
        return ok({
          jobId: "seed-job",
          evaluationYear: "2026",
          actionType: "GENERATE",
          jobStatus: "PARTIAL_FAILURE",
          totalCount: 2,
          processedCount: 1,
          unprocessedCount: 1,
          items: [
            {
              targetUserId: 31,
              processed: false,
              errorReason: "확정 대상",
            },
          ],
        }) as never;
      return ok([]) as never;
    });
    openBulk();
    expect(
      screen.getByRole("button", {
        name: "일괄 실행",
      }),
    ).toBeDisabled();
    fireEvent.click(
      screen.getByRole("button", {
        name: "대상·실행조건 미리보기",
      }),
    );
    await screen.findByText("기존 실적 건수: 7건");
    expect(apiRequest).toHaveBeenCalledWith(`${base}/bulk-jobs/preview`, {
      method: "POST",
      body: JSON.stringify(bulkRequest()),
    });
    expect(
      screen.getByText(/생성 대상 건수나 실행 승인이 아닙니다/),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("작업 유형"), {
      target: {
        value: "DELETE",
      },
    });
    expect(
      screen.getByRole("button", {
        name: "일괄 실행",
      }),
    ).toBeDisabled();
    fireEvent.click(
      screen.getByRole("button", {
        name: "대상·실행조건 미리보기",
      }),
    );
    await screen.findByText("기존 실적 건수: 7건");
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(
      screen.getByRole("button", {
        name: "일괄 실행",
      }),
    );
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([path]) => path === `${base}/bulk-jobs`),
    ).toBe(false);
    vi.mocked(window.confirm).mockReturnValue(true);
    fireEvent.click(
      screen.getByRole("button", {
        name: "일괄 실행",
      }),
    );
    await screen.findByText("OQ-83-01 실행 정책 미확정");
    expect(apiRequest).toHaveBeenCalledWith(`${base}/bulk-jobs`, {
      method: "POST",
      body: JSON.stringify(bulkRequest("DELETE")),
    });
    expect(
      screen.getByRole("button", {
        name: "일괄 실행",
      }),
    ).toBeDisabled();
    fireEvent.change(screen.getByLabelText("작업 ID"), {
      target: {
        value: "seed-job",
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "처리결과 조회",
      }),
    );
    await screen.findByText("전체 2건 / 처리 1건 / 미처리 1건");
    expect(screen.getByText(/확정 대상/)).toBeInTheDocument();
  });
  it("rejects unsupported bulk conditions before preview and handles preview failures", async () => {
    auth.roles = ["R07"];
    openBulk();
    fireEvent.change(screen.getByLabelText("대상·실행조건 (JSON)"), {
      target: {
        value: '{"teacherName":"홍교원"}',
      },
    });
    fireEvent.click(
      screen.getByRole("button", {
        name: "대상·실행조건 미리보기",
      }),
    );
    await screen.findByText(
      "대상 조건은 managementNo, managementItemCode, certificationStatus만 지원합니다.",
    );
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([path]) => path.endsWith("/preview")),
    ).toBe(false);
    fireEvent.change(screen.getByLabelText("대상·실행조건 (JSON)"), {
      target: {
        value:
          '{"managementNo":"EMP-83","managementItemCode":"EMPLOYMENT","certificationStatus":"DRAFT"}',
      },
    });
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(500, "미리보기 조회 실패"),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "대상·실행조건 미리보기",
      }),
    );
    await screen.findByText("미리보기 조회 실패");
    expect(
      screen.getByRole("button", {
        name: "일괄 실행",
      }),
    ).toBeDisabled();
  });
  it("preserves unsaved inputs and exposes server field errors on conflict", async () => {
    render(<EmploymentRateAchievementPage />);
    await openDetail();
    fireEvent.change(screen.getByLabelText("실적명"), {
      target: {
        value: "보존할 입력",
      },
    });
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(409, "입력기간이 아닙니다.", {
        code: "PERIOD_NOT_ACTIVE",
        message: "입력기간이 아닙니다.",
        fields: [
          {
            field: "achievementDate",
            message: "입력기간을 확인하세요.",
          },
        ],
      }),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "저장",
      }),
    );
    await screen.findByText("입력기간이 아닙니다.");
    expect(screen.getByText("입력기간을 확인하세요.")).toBeInTheDocument();
    expect(screen.getByLabelText("실적명")).toHaveValue("보존할 입력");
  });
});
