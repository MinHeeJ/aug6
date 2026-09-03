import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { EvaluationBatchResultPage } from "./SCR-EVALUATION-BATCH-RESULT";
import { evaluationBatchResultApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationBatchResultApi: {
      list: vi.fn(),
      errors: vi.fn(),
    },
  };
});

describe("EvaluationBatchResultPage", () => {
  it("renders batch result counts, filters by job type and target condition, and opens error detail", async () => {
    vi.mocked(evaluationBatchResultApi.list).mockResolvedValue({
      success: true,
      meta: {},
      data: {
        results: [
          {
            batchId: "B45-GENERATION-20260903-000001",
            jobType: "GENERATION",
            jobTypeName: "생성",
            requestStatus: "COMPLETED",
            targetCondition: '{"evaluationYear":"2026","areaCode":"EDUCATION"}',
            totalCount: 3,
            successCount: 1,
            failureCount: 1,
            excludedCount: 1,
            requestId: "REQ-B45-SEED-003",
            requestedAt: "2026-09-03T09:00:00",
            completedAt: "2026-09-03T09:01:00",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
      },
    });
    vi.mocked(evaluationBatchResultApi.errors).mockResolvedValue({
      success: true,
      meta: {},
      data: {
        batchId: "B45-GENERATION-20260903-000001",
        errors: [
          {
            batchId: "B45-GENERATION-20260903-000001",
            targetKey: "ACH-43002",
            targetName: "연구업적 미인증",
            errorCode: "SOURCE_STATUS_NOT_CERTIFIED",
            message: "인증 상태 원천만 평가자료로 생성할 수 있습니다.",
            detail: "sourceStatus=제출",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
      },
    });

    render(<EvaluationBatchResultPage />);

    expect(
      await screen.findByText("B45-GENERATION-20260903-000001"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("생성").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("COMPLETED")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getAllByText("1").length).toBeGreaterThanOrEqual(3);

    fireEvent.change(
      screen.getByTestId("evaluation-batch-result-job-type-select"),
      {
        target: { value: "GENERATION" },
      },
    );
    fireEvent.change(
      screen.getByTestId("evaluation-batch-result-target-condition-input"),
      {
        target: { value: "EDUCATION" },
      },
    );
    fireEvent.click(
      screen.getByTestId("evaluation-batch-result-search-button"),
    );

    await waitFor(() =>
      expect(evaluationBatchResultApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({
          jobType: "GENERATION",
          targetCondition: "EDUCATION",
          page: 0,
          size: 20,
        }),
      ),
    );

    fireEvent.click(
      screen.getByTestId("evaluation-batch-result-errors-button"),
    );
    expect(
      await screen.findByText("SOURCE_STATUS_NOT_CERTIFIED"),
    ).toBeInTheDocument();
    expect(screen.getByText("ACH-43002")).toBeInTheDocument();
    expect(
      screen.getByText("인증 상태 원천만 평가자료로 생성할 수 있습니다."),
    ).toBeInTheDocument();
  });
});
