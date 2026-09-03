import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { FinalEvaluationConfirmationPage } from "./SCR-FINAL-EVALUATION-CONFIRMATION";
import { finalEvaluationConfirmationApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    finalEvaluationConfirmationApi: {
      list: vi.fn(),
      confirm: vi.fn(),
      cancel: vi.fn(),
    },
  };
});

describe("FinalEvaluationConfirmationPage", () => {
  it("renders confirmation list and lets college staff confirm certified targets", async () => {
    vi.mocked(finalEvaluationConfirmationApi.list).mockResolvedValue({
      success: true,
      meta: {},
      data: {
        confirmations: [
          {
            targetId: 1,
            evaluationYear: "2026",
            areaCode: "EDUCATION",
            organizationName: "교육학과",
            targetName: "홍길동",
            confirmationStatus: "인증",
            confirmedBy: null,
            confirmedByName: null,
            confirmedAt: null,
            canceledBy: null,
            canceledByName: null,
            canceledAt: null,
            cancelReason: null,
            materialCount: 2,
            totalScore: 20,
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
      },
    });
    vi.mocked(finalEvaluationConfirmationApi.confirm).mockResolvedValue({
      success: true,
      meta: { requestId: "REQ-B45-CONFIRM-TEST" },
      data: {
        batchId: "B45-CONFIRMATION-20260903-000002",
        requestId: "REQ-B45-CONFIRM-TEST",
        targetId: 1,
        previousStatus: "인증",
        nextStatus: "평가확정",
        changedMaterialCount: 2,
      },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<FinalEvaluationConfirmationPage currentRoles={["R04"]} />);

    expect(await screen.findByText(/홍길동/)).toBeInTheDocument();
    expect(screen.getAllByText("인증").length).toBeGreaterThanOrEqual(1);
    fireEvent.click(screen.getByTestId("final-evaluation-confirm-button"));

    await waitFor(() =>
      expect(finalEvaluationConfirmationApi.confirm).toHaveBeenCalledWith(1, {
        evaluationYear: "2026",
      }),
    );
    expect(await screen.findByText("최종평가 처리 완료")).toBeInTheDocument();
    expect(screen.getByText(/인증 → 평가확정/)).toBeInTheDocument();
  });

  it("shows cancel modal only for cancel role and requires cancel reason before request", async () => {
    vi.mocked(finalEvaluationConfirmationApi.list).mockResolvedValue({
      success: true,
      meta: {},
      data: {
        confirmations: [
          {
            targetId: 1,
            evaluationYear: "2026",
            areaCode: "EDUCATION",
            organizationName: "교육학과",
            targetName: "홍길동",
            confirmationStatus: "평가확정",
            confirmedBy: 4,
            confirmedByName: "단과대학담당자",
            confirmedAt: "2026-09-03T09:00:00",
            canceledBy: null,
            canceledByName: null,
            canceledAt: null,
            cancelReason: null,
            materialCount: 2,
            totalScore: 20,
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
      },
    });
    vi.mocked(finalEvaluationConfirmationApi.cancel).mockResolvedValue({
      success: true,
      meta: { requestId: "REQ-B45-CANCEL-TEST" },
      data: {
        batchId: "B45-CONFIRMATION-20260903-000003",
        requestId: "REQ-B45-CANCEL-TEST",
        targetId: 1,
        previousStatus: "평가확정",
        nextStatus: "인증",
        changedMaterialCount: 2,
      },
    });

    render(<FinalEvaluationConfirmationPage currentRoles={["R08"]} />);

    expect(await screen.findByText("단과대학담당자")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("final-evaluation-cancel-button"));
    expect(
      screen.getByTestId("final-evaluation-cancel-modal"),
    ).toBeInTheDocument();
    fireEvent.click(
      screen.getByTestId("final-evaluation-cancel-submit-button"),
    );
    expect(
      await screen.findByText("확정취소 사유를 입력하세요."),
    ).toBeInTheDocument();

    fireEvent.change(
      screen.getByTestId("final-evaluation-cancel-reason-input"),
      {
        target: { value: "이의신청 인용" },
      },
    );
    fireEvent.click(
      screen.getByTestId("final-evaluation-cancel-submit-button"),
    );

    await waitFor(() =>
      expect(finalEvaluationConfirmationApi.cancel).toHaveBeenCalledWith(1, {
        evaluationYear: "2026",
        cancelReason: "이의신청 인용",
      }),
    );
    expect(await screen.findByText(/평가확정 → 인증/)).toBeInTheDocument();
  });
});
