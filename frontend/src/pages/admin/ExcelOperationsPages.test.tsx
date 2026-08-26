import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  EXCEL_PAGE_SIZE_OPTIONS,
  ExcelDownloadManagementPage,
  ExcelUploadErrorManagementPage,
  ExcelUploadHistoryManagementPage,
  ExcelUploadManagementPage,
  UploadTemplateManagementPage,
  excelApi,
} from "./ExcelOperationsPages";

const compatibilitySmokeMatrix = [
  "Edge desktop",
  "Chrome desktop",
  "Safari desktop",
  "Opera desktop",
  "Whale desktop",
  "iPadOS tablet",
  "Android tablet",
] as const;

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("BASIC-26 Excel 운영 화면 계약", () => {
  it("목록 표시 건수는 20, 50, 100만 제공한다", () => {
    expect(EXCEL_PAGE_SIZE_OPTIONS).toEqual([20, 50, 100]);
  });

  it("업로드 양식 API는 상대 /api 경로와 실제 선택 templateId를 사용한다", async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => ({ success: true, data: {}, meta: {} }),
    }));
    vi.stubGlobal("fetch", fetchMock);
    await excelApi.listTemplates({
      businessType: "PROFESSOR_ACHIEVEMENT",
      effectiveDate: "2026-01-01",
    });
    const listCall = fetchMock.mock.calls[0] as unknown as [
      string,
      RequestInit | undefined,
    ];
    expect(listCall[0]).toContain("/api/admin/excel-upload-templates?");
    expect(listCall[0]).not.toContain("localhost");
    void excelApi.downloadTemplate("TPL-2026");
    const downloadCall = fetchMock.mock.calls[1] as unknown as [
      string,
      RequestInit | undefined,
    ];
    expect(downloadCall[0]).toBe(
      "/api/admin/excel-upload-templates/TPL-2026/file",
    );
  });

  it("엑셀 다운로드 생성은 현재 조회조건을 요청 본문에 포함한다", async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => ({
        success: true,
        data: { downloadId: "DL-1" },
        meta: {},
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);
    await excelApi.createDownload("ERROR", { uploadId: "UP-2026-ERROR" });
    const createCall = fetchMock.mock.calls[0] as unknown as [
      string,
      RequestInit,
    ];
    expect(createCall[0]).toBe("/api/admin/excel-downloads");
    expect(JSON.parse(createCall[1].body as string)).toEqual({
      outputType: "ERROR",
      queryCondition: { uploadId: "UP-2026-ERROR" },
    });
  });

  it("신규 Excel route는 표준 상태 영역과 접근 가능한 조작 요소를 렌더링한다", async () => {
    const fetchMock = vi.fn(async (url: string) => ({
      ok: true,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => {
        if (url.includes("excel-upload-histories"))
          return {
            success: true,
            data: { histories: [], page: 0, size: 20, totalElements: 0 },
          };
        if (url.includes("excel-upload-errors"))
          return {
            success: true,
            data: { errors: [], page: 0, size: 20, totalElements: 0 },
          };
        return {
          success: true,
          data: { templates: [], page: 0, size: 20, totalElements: 0 },
        };
      },
    }));
    vi.stubGlobal("fetch", fetchMock);

    const { rerender } = render(<UploadTemplateManagementPage />);
    expect(
      await screen.findByTestId("excel-upload-templates-screen"),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /조회/ })).toBeEnabled();
    expect(screen.getByRole("button", { name: /저장/ })).toBeEnabled();
    expect(screen.getByLabelText("업무구분 필수")).toBeInTheDocument();
    expect(screen.getByLabelText("시행일 필수")).toBeInTheDocument();

    rerender(<ExcelUploadManagementPage />);
    expect(screen.getByTestId("excel-uploads-screen")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /업로드·검증/ })).toBeEnabled();
    expect(screen.getByLabelText("엑셀 파일 필수")).toHaveAttribute(
      "accept",
      ".csv,.xls,.xlsx",
    );
    expect(
      screen.getByText(
        /템플릿 다운로드 → 파일 업로드 → 검증 → 검증결과 확인 → 반영/,
      ),
    ).toBeInTheDocument();

    rerender(<ExcelUploadHistoryManagementPage />);
    await waitFor(() =>
      expect(
        screen.getByTestId("excel-upload-histories-screen"),
      ).toBeInTheDocument(),
    );
    expect(screen.getByRole("button", { name: /조회/ })).toBeEnabled();
    expect(screen.getByText("이력 없음")).toBeInTheDocument();

    rerender(<ExcelUploadErrorManagementPage />);
    await waitFor(() =>
      expect(
        screen.getByTestId("excel-upload-errors-screen"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByRole("button", { name: /오류목록 다운로드/ }),
    ).toBeDisabled();
    expect(screen.getByLabelText("업로드ID 필수")).toBeInTheDocument();

    rerender(<ExcelDownloadManagementPage />);
    expect(screen.getByTestId("excel-downloads-screen")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /생성/ })).toBeEnabled();
    expect(screen.getByLabelText("현재 조회조건")).toBeInTheDocument();
  });

  it("호환성 smoke matrix는 데스크톱 주요 브라우저와 태블릿 환경을 명시한다", () => {
    expect(compatibilitySmokeMatrix).toEqual([
      "Edge desktop",
      "Chrome desktop",
      "Safari desktop",
      "Opera desktop",
      "Whale desktop",
      "iPadOS tablet",
      "Android tablet",
    ]);
  });

  it("frontend Excel 화면은 사용자가 선택하지 않은 샘플 ID를 기본 요청값으로 하드코딩하지 않는다", () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        headers: new Headers({ "content-type": "application/json" }),
        json: async () => ({
          success: true,
          data: { errors: [], page: 0, size: 20, totalElements: 0 },
        }),
      })),
    );

    const { rerender } = render(<ExcelUploadManagementPage />);
    expect(screen.getByTestId("excel-upload-template-id-input")).toHaveValue(
      "",
    );

    rerender(<ExcelUploadErrorManagementPage />);
    expect(screen.getByTestId("excel-error-upload-id-input")).toHaveValue("");

    rerender(<ExcelDownloadManagementPage />);
    expect(screen.getByTestId("excel-download-query-textarea")).toHaveValue(
      "{}",
    );
  });
});
