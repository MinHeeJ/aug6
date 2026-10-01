import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { StudentGuidanceExcelUploadPage } from "./SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD";

function apiResponse(data: unknown, status = 200) {
  return new Response(
    JSON.stringify({ success: status < 400, data, meta: {} }),
    {
      status,
      headers: { "content-type": "application/json" },
    },
  );
}

describe("SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("validates a selected CSV, confirms a zero-error result, and refreshes upload history", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(apiResponse([]))
      .mockResolvedValueOnce(
        apiResponse({
          uploadId: "SG-UP-001",
          originalFileName: "student-guidance.csv",
          totalCount: 1,
          successCount: 1,
          errorCount: 0,
          errors: [],
        }),
      )
      .mockResolvedValueOnce(apiResponse([]))
      .mockResolvedValueOnce(
        apiResponse({ uploadId: "SG-UP-001", savedCount: 1 }),
      )
      .mockResolvedValueOnce(
        apiResponse([
          {
            uploadId: "SG-UP-001",
            originalFileName: "student-guidance.csv",
            totalCount: 1,
            successCount: 1,
            errorCount: 0,
            savedCount: 1,
          },
        ]),
      );
    vi.stubGlobal("fetch", fetch);

    render(<StudentGuidanceExcelUploadPage />);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    const file = new File(
      [
        "templateVersion,managementItemCode,guidanceStartDate,guidanceEndDate,studentName\n",
      ],
      "student-guidance.csv",
      { type: "text/csv" },
    );
    fireEvent.change(screen.getByTestId("student-guidance-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("student-guidance-upload-button"));

    await screen.findByTestId("student-guidance-commit-button");
    expect(fetch).toHaveBeenNthCalledWith(
      2,
      "/api/business/student-guidance-achievements/excel-uploads",
      expect.objectContaining({ method: "POST" }),
    );
    fireEvent.click(screen.getByTestId("student-guidance-commit-button"));
    await screen.findByTestId("student-guidance-commit-modal");
    fireEvent.click(
      screen.getByTestId("student-guidance-commit-confirm-button"),
    );

    await screen.findByText("1건을 전체 반영했습니다.");
    expect(fetch).toHaveBeenCalledWith(
      "/api/business/student-guidance-achievements/excel-uploads/SG-UP-001/commit",
      expect.objectContaining({ method: "POST" }),
    );
    await screen.findByTestId("student-guidance-history-row");
  });

  it("shows an error file action and never shows commit when validation has errors", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(apiResponse([]))
      .mockResolvedValueOnce(
        apiResponse({
          uploadId: "SG-UP-ERR",
          originalFileName: "student-guidance.csv",
          totalCount: 1,
          successCount: 0,
          errorCount: 1,
          errors: [
            {
              rowNumber: 2,
              columnName: "studentName",
              errorReason: "학생명은 필수입니다.",
            },
          ],
        }),
      )
      .mockResolvedValueOnce(apiResponse([]));
    vi.stubGlobal("fetch", fetch);

    render(<StudentGuidanceExcelUploadPage />);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    const file = new File(
      [
        "templateVersion,managementItemCode,guidanceStartDate,guidanceEndDate,studentName\n",
      ],
      "student-guidance.csv",
      { type: "text/csv" },
    );
    fireEvent.change(screen.getByTestId("student-guidance-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("student-guidance-upload-button"));

    await screen.findByText("오류 행이 있어 전체 반영하지 않습니다.");
    expect(
      screen.getByTestId("student-guidance-error-download-link"),
    ).toHaveAttribute(
      "href",
      "/api/business/student-guidance-achievements/excel-uploads/SG-UP-ERR/errors/download",
    );
    expect(
      screen.queryByTestId("student-guidance-commit-button"),
    ).not.toBeInTheDocument();
  });
});
