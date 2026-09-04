import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  PersonalAchievementScorePage,
  ResearchClassificationCriterionPage,
} from "./Basic50BusinessPages";

function mockFetch(data: unknown) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async () => ({
      ok: true,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => ({ success: true, data, meta: {} }),
    })),
  );
}

describe("BASIC-50 업무 화면", () => {
  it("개인 업적점수 화면은 총점, 세부규정, 산출근거 링크를 표시한다", async () => {
    mockFetch({
      teacherUserId: 1,
      teacherName: "교원1",
      evaluationYear: "2027",
      totalScore: 40,
      summaries: [
        { areaCode: "RESEARCH", areaName: "연구", subtotalScore: 40 },
      ],
      items: [
        {
          scoreId: 2,
          areaCode: "RESEARCH",
          areaName: "연구",
          itemCode: "RES-JOURNAL",
          itemName: "논문실적",
          score: 40,
          calculationDetail: "KCI 논문",
          ruleName: "논문실적 세부규정",
          evidenceUrl: "/admin/score-calculation-histories?scoreId=2",
        },
      ],
    });
    render(<PersonalAchievementScorePage />);
    await waitFor(() =>
      expect(screen.getByText(/교원1 총점 40/)).toBeInTheDocument(),
    );
    expect(screen.getByText("논문실적 세부규정")).toBeInTheDocument();
    expect(screen.getByTestId("score-evidence-link")).toHaveAttribute(
      "href",
      "/admin/score-calculation-histories?scoreId=2",
    );
  });

  it("연구실적 분류기준 화면은 기본 표시 건수와 Excel 버튼을 제공한다", async () => {
    mockFetch({
      criteria: [
        {
          criterionId: 1,
          areaCode: "RESEARCH",
          areaName: "연구",
          managementCriterionCode: "JOURNAL",
          managementCriterionName: "학술지",
          activeYn: "Y",
          classifiedAchievementCount: 3,
        },
      ],
      page: 0,
      pageSize: 20,
      totalElements: 1,
    });
    render(<ResearchClassificationCriterionPage />);
    await waitFor(() =>
      expect(screen.getByText("JOURNAL")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("page-size-select")).toHaveValue("20");
    expect(screen.getByTestId("criterion-csv-button")).toBeInTheDocument();
  });
});
