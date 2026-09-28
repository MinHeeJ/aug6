import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StudentGuidanceExcelPage } from "./EducationAchievementPages";
import { educationAchievementApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    educationAchievementApi: {
      ...actual.educationAchievementApi,
      uploadStudentGuidance: vi.fn(),
    },
  };
});

describe("SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD", () => {
  it("disables apply when the API reports an error row", async () => {
    vi.mocked(educationAchievementApi.uploadStudentGuidance).mockResolvedValue({
      success: true,
      data: {
        totalCount: 2,
        successCount: 1,
        failureCount: 1,
        errorFileRef: "opaque-ref",
      },
      meta: {},
    });
    render(<StudentGuidanceExcelPage />);
    fireEvent.change(screen.getByTestId("student-guidance-file-input"), {
      target: { files: [new File(["sheet"], "guidance.xlsx")] },
    });
    fireEvent.click(screen.getByTestId("student-guidance-upload-button"));
    expect(
      await screen.findByText("오류 행이 있어 전체 반영할 수 없습니다."),
    ).toBeInTheDocument();
    expect(screen.getByTestId("student-guidance-apply-button")).toBeDisabled();
  });
});
