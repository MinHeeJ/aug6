import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EvaluationMaterialDeletionPage } from "./SCR-EVALUATION-MATERIAL-DELETION";
import { evaluationMaterialDeletionApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationMaterialDeletionApi: {
      preview: vi.fn(),
      delete: vi.fn(),
    },
  };
});

describe("EvaluationMaterialDeletionPage", () => {
  it("renders deletion preview, validates reason, confirms delete, and shows result", async () => {
    vi.mocked(evaluationMaterialDeletionApi.preview).mockResolvedValue({
      success: true,
      meta: {},
      data: {
        targets: [
          {
            evaluationMaterialId: 45001,
            evaluationYear: "2026",
            areaCode: "EDUCATION",
            targetUserId: 1,
            sourceAchievementId: 43001,
            generationBatchId: "B45-GENERATION-20260903-000001",
            materialStatus: "인증",
            materialOrigin: "BATCH_GENERATED",
            achievementTitle: "생성 평가자료",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        targetCount: 1,
      },
    });
    vi.mocked(evaluationMaterialDeletionApi.delete).mockResolvedValue({
      success: true,
      meta: { requestId: "REQ-B45-DELETION-TEST" },
      data: {
        batchId: "B45-DELETION-20260903-000001",
        requestId: "REQ-B45-DELETION-TEST",
        targetCount: 1,
        deletedCount: 1,
        excludedCount: 0,
      },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<EvaluationMaterialDeletionPage />);

    expect(await screen.findByText("생성 평가자료")).toBeInTheDocument();
    fireEvent.change(
      screen.getByTestId("evaluation-material-deletion-area-input"),
      {
        target: { value: "EDUCATION" },
      },
    );
    fireEvent.change(
      screen.getByTestId("evaluation-material-deletion-generation-batch-input"),
      {
        target: { value: "B45-GENERATION-20260903-000001" },
      },
    );
    fireEvent.click(
      screen.getByTestId("evaluation-material-deletion-delete-button"),
    );
    expect(
      await screen.findByText("삭제사유를 입력하세요."),
    ).toBeInTheDocument();

    fireEvent.change(
      screen.getByTestId("evaluation-material-deletion-reason-input"),
      {
        target: { value: "잘못 생성된 평가자료 재생성" },
      },
    );
    fireEvent.click(
      screen.getByTestId("evaluation-material-deletion-delete-button"),
    );

    await waitFor(() =>
      expect(evaluationMaterialDeletionApi.delete).toHaveBeenCalledWith(
        expect.objectContaining({
          evaluationYear: "2026",
          areaCode: "EDUCATION",
          generationBatchId: "B45-GENERATION-20260903-000001",
          deleteReason: "잘못 생성된 평가자료 재생성",
        }),
      ),
    );
    expect(
      await screen.findByText("평가자료 삭제 요청 완료"),
    ).toBeInTheDocument();
    expect(screen.getByText(/삭제 1건/)).toBeInTheDocument();
  });
});
