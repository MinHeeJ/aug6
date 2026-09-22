import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  CourseAreaGroupGradeQueryPage,
  EvaluationElementManagementItemSettingsPage,
  ManagementItemEvaluationScoreSettingsPage,
  ParticipationAllocationRateSettingsPage,
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

  it("renders the management-item evaluation-score route with API-backed score rows", async () => {
    mockFetch({
      managementItemEvaluationScoreSettings: [
        {
          settingId: 3003,
          ruleVersionId: 10,
          ruleVersionStatus: "DRAFT",
          targetScope: "COLLEGE_EDU",
          areaCode: "EDUCATION",
          itemCode: "LECTURE",
          evaluationYear: "2026",
          elementCode: "COURSE_GROUP",
          managementItemCode: "ATTENDANCE",
          organizationCode: "COL-EDU",
          organizationName: "사범대학",
          evaluationScore: 10,
          maxScore: 20,
          sortOrder: 1,
          activeYn: "Y",
          effectiveStartDate: "2026-01-01",
          effectiveEndDate: "2026-12-31",
          evaluationConfirmedYn: "N",
        },
      ],
      page: 0,
      pageSize: 20,
      totalElements: 1,
    });

    render(<ManagementItemEvaluationScoreSettingsPage />);

    expect(
      screen.getByTestId("management-item-evaluation-score-settings-page"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByTestId("score-settings-row")).toHaveTextContent(
        "COL-EDU / 10 (상한 20)",
      ),
    );
    expect(screen.getByTestId("score-save-button")).toBeInTheDocument();
  });

  it("renders read-only course area group grade query without mutation CTA", async () => {
    mockFetch({
      items: [
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

  it("exports the evaluation-element list using its currently selected conditions", async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (url.includes("/download")) {
        return {
          ok: true,
          headers: new Headers({
            "content-type":
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          }),
          blob: async () => new Blob(["xlsx"]),
        };
      }
      return {
        ok: true,
        headers: new Headers({ "content-type": "application/json" }),
        json: async () => ({
          success: true,
          data: {
            evaluationElementManagementItemSettings: [],
            page: 0,
            pageSize: 20,
            totalElements: 0,
          },
          meta: { requestId: "TEST" },
        }),
      };
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<EvaluationElementManagementItemSettingsPage />);
    fireEvent.change(screen.getByLabelText("평가영역"), {
      target: { value: "EDUCATION" },
    });
    fireEvent.click(screen.getByTestId("element-search-button"));
    await waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) =>
          String(url).includes("areaCode=EDUCATION"),
        ),
      ).toBe(true),
    );

    fireEvent.click(screen.getByTestId("element-download-button"));
    await waitFor(() => {
      const downloadCall = fetchMock.mock.calls.find(([url]) =>
        String(url).includes(
          "/api/admin/evaluation-element-management-item-settings/download",
        ),
      );
      expect(downloadCall?.[0]).toContain("areaCode=EDUCATION");
      expect(downloadCall?.[0]).toContain("page=0");
      expect(downloadCall?.[0]).toContain("pageSize=20");
    });
  });

  it("provides the standard Excel toolbar action on each BASIC-70 settings screen", () => {
    const { rerender } = render(
      <EvaluationElementManagementItemSettingsPage />,
    );
    expect(screen.getByTestId("element-download-button")).toBeEnabled();

    rerender(<ParticipationAllocationRateSettingsPage />);
    expect(screen.getByTestId("participation-download-button")).toBeEnabled();

    rerender(<ManagementItemEvaluationScoreSettingsPage />);
    expect(screen.getByTestId("score-download-button")).toBeEnabled();
    expect(screen.getByLabelText("표시 건수")).toHaveValue("20");
    expect(screen.getByRole("option", { name: "50건" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "100건" })).toBeInTheDocument();
  });

  it("confirms a BASIC-70 settings save, shows a toast, and refreshes the current list", async () => {
    const row = {
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
      teacherEditablePart: "SELF_REPORT",
      effectiveStartDate: "2026-01-01",
      effectiveEndDate: "2026-12-31",
      evaluationConfirmedYn: "N",
    };
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      const isSave = init?.method === "POST";
      return {
        ok: true,
        headers: new Headers({ "content-type": "application/json" }),
        json: async () => ({
          success: true,
          data: isSave
            ? row
            : {
                evaluationElementManagementItemSettings: [row],
                page: 0,
                pageSize: 20,
                totalElements: 1,
              },
          meta: { requestId: "B70-UI-SAVE" },
        }),
      };
    });
    vi.stubGlobal("fetch", fetchMock);
    const confirmation = vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<EvaluationElementManagementItemSettingsPage />);
    await waitFor(() =>
      expect(screen.getByTestId("element-settings-row")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("element-settings-row"));
    fireEvent.change(screen.getByLabelText("변경 사유 *"), {
      target: { value: "BASIC-70 저장 검증" },
    });
    fireEvent.click(screen.getByTestId("element-save-button"));

    expect(confirmation).toHaveBeenCalledWith(
      "평가요소별 관리항목 설정 값을 저장하시겠습니까?",
    );
    await waitFor(() =>
      expect(screen.getByText("저장되었습니다")).toBeInTheDocument(),
    );
    expect(
      fetchMock.mock.calls.filter(
        ([url, init]) =>
          String(url).includes(
            "/api/admin/evaluation-element-management-item-settings",
          ) && (init as RequestInit | undefined)?.method === "POST",
      ),
    ).toHaveLength(1);
    expect(
      fetchMock.mock.calls.filter(([url]) =>
        String(url).includes(
          "/api/admin/evaluation-element-management-item-settings?",
        ),
      ).length,
    ).toBeGreaterThanOrEqual(2);
  });
});
