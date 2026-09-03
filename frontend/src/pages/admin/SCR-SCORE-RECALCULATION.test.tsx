import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ScoreRecalculationPage } from "./SCR-SCORE-RECALCULATION";
import { scoreRecalculationApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    scoreRecalculationApi: {
      preview: vi.fn(),
      recalculate: vi.fn(),
    },
  };
});

describe("ScoreRecalculationPage", () => {
  it("renders before after preview, validates formula version, confirms recalculation, and shows result", async () => {
    vi.mocked(scoreRecalculationApi.preview).mockResolvedValue({
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
            formulaVersionId: 7,
            ruleVersionId: 3,
            formulaCode: "FIXED-2026",
            beforeScore: 8,
            afterScore: 10,
            nextGenerationNo: 2,
            materialStatus: "인증",
            achievementTitle: "교육 평가자료",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        targetCount: 1,
      },
    });
    vi.mocked(scoreRecalculationApi.recalculate).mockResolvedValue({
      success: true,
      meta: { requestId: "REQ-B45-RECALC-TEST" },
      data: {
        batchId: "B45-RECALCULATION-20260903-000002",
        requestId: "REQ-B45-RECALC-TEST",
        targetCount: 1,
        recalculatedCount: 1,
        excludedCount: 0,
      },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<ScoreRecalculationPage />);

    expect(await screen.findByText("교육 평가자료")).toBeInTheDocument();
    expect(screen.getByText("8.00")).toBeInTheDocument();
    expect(screen.getByText("10.00")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("score-recalculation-run-button"));
    expect(
      await screen.findByText("산식버전을 선택하세요."),
    ).toBeInTheDocument();

    fireEvent.change(
      screen.getByTestId("score-recalculation-formula-version-input"),
      {
        target: { value: "7" },
      },
    );
    fireEvent.change(screen.getByTestId("score-recalculation-area-input"), {
      target: { value: "EDUCATION" },
    });
    fireEvent.click(screen.getByTestId("score-recalculation-run-button"));

    await waitFor(() =>
      expect(scoreRecalculationApi.recalculate).toHaveBeenCalledWith(
        expect.objectContaining({
          evaluationYear: "2026",
          areaCode: "EDUCATION",
          formulaVersionId: "7",
        }),
      ),
    );
    expect(
      await screen.findByText("점수 재계산 요청 완료"),
    ).toBeInTheDocument();
    expect(screen.getByText(/재계산 1건/)).toBeInTheDocument();
  });
});
