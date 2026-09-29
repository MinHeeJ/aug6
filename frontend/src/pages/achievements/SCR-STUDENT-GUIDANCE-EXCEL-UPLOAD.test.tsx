import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { StudentGuidanceExcelUploadPage } from "./SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD";
describe("SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD", () => {
  it("renders the R07 template, validation, error-download, confirmation, and history flow", () => {
    const html = renderToStaticMarkup(<StudentGuidanceExcelUploadPage />);
    expect(html).toContain(
      'data-screen-id="SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD"',
    );
    expect(html).toContain("템플릿 다운로드");
    expect(html).toContain('data-testid="student-guidance-validate-button"');
    expect(html).toContain("업로드 이력");
  });
});
