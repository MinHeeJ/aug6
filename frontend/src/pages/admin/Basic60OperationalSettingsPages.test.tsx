import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  CourseAreaGroupGradeQueryPage,
  EvaluationElementManagementItemSettingsPage,
} from "./Basic60OperationalSettingsPages";

function mockFetch(body: unknown) {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: body,
        meta: { requestId: "TEST" },
      }),
    }),
  );
}

describe("BASIC-60 operational settings pages", () => {
  it("renders evaluation element management item settings route with API-backed rows", async () => {
    mockFetch({
      evaluationElementManagementItemSettings: [
        {
          settingId: 1,
          ruleVersionId: 10,
          ruleVersionStatus: "DRAFT",
          targetScope: "COLLEGE_EDU",
          areaCode: "EDUCATION",
          itemCode: "LECTURE",
          evaluationYear: "2026",
          elementCode: "COURSE_GROUP",
          managementItemCode: "ATTENDANCE",
          managementItemName: "출석관리",
          sortOrder: 1,
          activeYn: "Y",
          teacherEditableYn: "Y",
          effectiveStartDate: "2026-01-01",
          effectiveEndDate: "2026-12-31",
          evaluationConfirmedYn: "N",
        },
      ],
      page: 0,
      pageSize: 20,
      totalElements: 1,
    });

    render(<EvaluationElementManagementItemSettingsPage />);

    expect(
      screen.getByTestId("evaluation-element-management-item-settings-page"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByText("ATTENDANCE / 출석관리")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("element-save-button")).toBeInTheDocument();
  });

  it("renders read-only course area group grade query without mutation CTA", async () => {
    mockFetch({
      courseAreaGroupGrades: [
        {
          resultId: 1,
          facultyUserId: 2,
          employeeNo: "E0002",
          facultyName: "교원사용자",
          completionType: "MAJOR",
          semester: "2026-1",
          courseArea: "LECTURE",
          groupGrade: 95.5,
          detailSummary: "전공 강의 그룹평가",
          publishedYn: "Y",
          evaluatedAt: "2026-09-10T09:00:00",
        },
      ],
      page: 0,
      pageSize: 20,
      totalElements: 1,
    });

    render(<CourseAreaGroupGradeQueryPage />);

    expect(
      screen.getByTestId("course-area-group-grade-query-page"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getAllByText("교원사용자 (E0002)").length).toBeGreaterThan(
        0,
      ),
    );
    expect(screen.queryByText("저장")).not.toBeInTheDocument();
  });
});
