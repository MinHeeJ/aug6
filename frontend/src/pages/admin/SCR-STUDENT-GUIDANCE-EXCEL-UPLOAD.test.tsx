import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { StudentGuidanceExcelUploadPage } from "./SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD";
vi.mock("../../api/apiClient", async () => ({
  ApiClientError: class extends Error {},
  studentGuidanceUploadApi: {
    upload: vi.fn(),
    commit: vi.fn(),
    histories: vi.fn(),
  },
}));
describe("StudentGuidanceExcelUploadPage", () => {
  it("renders the R07 validation and confirmation entry path", () => {
    render(<StudentGuidanceExcelUploadPage />);
    expect(
      screen.getByTestId("student-guidance-file-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("student-guidance-upload-button"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("student-guidance-history-button"),
    ).toBeInTheDocument();
  });
});
