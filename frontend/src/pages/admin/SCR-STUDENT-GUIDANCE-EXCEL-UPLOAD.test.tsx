import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { studentGuidanceUploadApi } from "../../api/apiClient";
import { StudentGuidanceExcelUploadPage } from "./SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    studentGuidanceUploadApi: {
      upload: vi.fn(),
      commit: vi.fn(),
      histories: vi.fn(),
      template: vi.fn(),
      errorFile: vi.fn(),
    },
  };
});

const uploadApi = vi.mocked(studentGuidanceUploadApi);

afterEach(() => {
  vi.clearAllMocks();
  vi.unstubAllGlobals();
});

describe("SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD", () => {
  it("파일을 선택하지 않으면 검증을 요청하지 않고 조치 안내를 표시한다", () => {
    render(<StudentGuidanceExcelUploadPage />);

    fireEvent.click(
      screen.getByTestId("student-guidance-upload-validate-button"),
    );

    expect(
      screen.getByText("업로드할 Excel 파일을 선택하세요."),
    ).toBeInTheDocument();
    expect(uploadApi.upload).not.toHaveBeenCalled();
  });

  it("오류 행이 있는 검증 결과에서는 오류파일 다운로드를 제공하고 전체 반영을 막는다", async () => {
    uploadApi.upload.mockResolvedValue({
      success: true,
      data: {
        uploadId: "SG-UP-ERR",
        originalFileName: "학생지도.csv",
        totalCount: 1,
        successCount: 0,
        errorCount: 1,
        errors: [
          {
            rowNumber: 2,
            columnName: "studentName",
            errorCode: "DUPLICATE",
            errorReason: "중복 학생지도 실적입니다.",
          },
        ],
      },
      meta: {},
    });
    uploadApi.errorFile.mockResolvedValue(new Blob(["errors"]));
    render(<StudentGuidanceExcelUploadPage />);

    const file = new File(["header\n"], "학생지도.csv", { type: "text/csv" });
    fireEvent.change(screen.getByTestId("student-guidance-upload-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(
      screen.getByTestId("student-guidance-upload-validate-button"),
    );

    expect(
      await screen.findByText(
        "오류행이 있어 전체 반영하지 않습니다. 오류파일을 내려받아 수정하세요.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("student-guidance-commit-button"),
    ).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByTestId("student-guidance-error-download-button"),
    );
    await waitFor(() =>
      expect(uploadApi.errorFile).toHaveBeenCalledWith("SG-UP-ERR"),
    );
    expect(uploadApi.commit).not.toHaveBeenCalled();
  });

  it("오류 없는 검증 결과는 확인 후 반영하고 이력을 새로 조회한다", async () => {
    uploadApi.upload.mockResolvedValue({
      success: true,
      data: {
        uploadId: "SG-UP-VALID",
        originalFileName: "학생지도.csv",
        totalCount: 1,
        successCount: 1,
        errorCount: 0,
        errors: [],
      },
      meta: {},
    });
    uploadApi.commit.mockResolvedValue({
      success: true,
      data: { uploadId: "SG-UP-VALID", savedCount: 1 },
      meta: {},
    });
    uploadApi.histories.mockResolvedValue({
      success: true,
      data: [
        {
          uploadId: "SG-UP-VALID",
          originalFileName: "학생지도.csv",
          uploaderUserId: 7,
          totalCount: 1,
          successCount: 1,
          errorCount: 0,
          savedCount: 1,
          processedAt: "2026-03-01T10:00:00",
        },
      ],
      meta: {},
    });
    vi.stubGlobal(
      "confirm",
      vi.fn(() => true),
    );
    render(<StudentGuidanceExcelUploadPage />);

    const file = new File(["header\n"], "학생지도.csv", { type: "text/csv" });
    fireEvent.change(screen.getByTestId("student-guidance-upload-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(
      screen.getByTestId("student-guidance-upload-validate-button"),
    );
    await screen.findByTestId("student-guidance-commit-button");
    fireEvent.click(screen.getByTestId("student-guidance-commit-button"));

    await waitFor(() =>
      expect(uploadApi.commit).toHaveBeenCalledWith("SG-UP-VALID"),
    );
    await waitFor(() => expect(uploadApi.histories).toHaveBeenCalled());
    expect(
      await screen.findByText("1건을 전체 반영했습니다."),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("student-guidance-history-row"),
    ).toHaveTextContent("학생지도.csv");
  });
});
