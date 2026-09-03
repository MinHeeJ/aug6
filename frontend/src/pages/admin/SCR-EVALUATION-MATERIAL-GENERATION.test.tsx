import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EvaluationMaterialGenerationPage } from "./SCR-EVALUATION-MATERIAL-GENERATION";
import { evaluationMaterialGenerationApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationMaterialGenerationApi: {
      preview: vi.fn(),
      create: vi.fn(),
    },
  };
});

describe("EvaluationMaterialGenerationPage", () => {
  it("renders filters, preview rows, create confirmation result, and result link", async () => {
    vi.mocked(evaluationMaterialGenerationApi.preview).mockResolvedValue({
      success: true,
      meta: {},
      data: {
        targets: [
          {
            sourceAchievementId: 9101,
            evaluationYear: "2026",
            areaCode: "RESEARCH_CREATION",
            organizationCode: "COLL-01",
            targetUserId: 3,
            sourceStatus: "인증",
            achievementType: "FACULTY_ACHIEVEMENT",
            achievementTitle: "인증 원천 실적",
            generationStatus: "미생성",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        targetCount: 1,
      },
    });
    vi.mocked(evaluationMaterialGenerationApi.create).mockResolvedValue({
      success: true,
      meta: { requestId: "REQ-B45-TEST" },
      data: {
        batchId: "B45-GENERATION-20260903-000002",
        requestId: "REQ-B45-TEST",
        targetCount: 1,
        createdCount: 1,
        excludedCount: 0,
      },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<EvaluationMaterialGenerationPage />);

    expect(await screen.findByText("인증 원천 실적")).toBeInTheDocument();
    fireEvent.change(
      screen.getByTestId("evaluation-material-generation-organization-input"),
      { target: { value: "COLL-01" } },
    );
    fireEvent.click(
      screen.getByTestId("evaluation-material-generation-create-button"),
    );

    await waitFor(() =>
      expect(evaluationMaterialGenerationApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          evaluationYear: "2026",
          areaCode: "RESEARCH_CREATION",
          organizationCode: "COLL-01",
        }),
      ),
    );
    expect(
      await screen.findByText("평가자료 생성 요청 완료"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("evaluation-material-generation-result-link"),
    ).toHaveAttribute("href", "/admin/evaluation-batch-results");
  });
});
