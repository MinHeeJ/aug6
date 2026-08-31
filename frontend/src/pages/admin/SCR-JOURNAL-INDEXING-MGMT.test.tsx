import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { JournalIndexingInfoManagementPage } from "./SCR-JOURNAL-INDEXING-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    journalIndexingInfoApi: {
      listJournalIndexingInfos: vi.fn(async () => ({
        success: true,
        data: {
          journalIndexingInfos: [
            {
              journalIndexingInfoId: 920,
              ruleVersionId: 10,
              versionCode: "B34-DRAFT-2026",
              versionStatus: "DRAFT",
              issn: "1225-6463",
              journalName: "한국교육학술지",
              indexingType: "KCI",
              indexingTypeName: "등재지",
              publicationCountry: "KR",
              validStartDate: "2026-01-01",
              validEndDate: "2026-12-31",
              sourceName: "파일럿 시드",
              sourceUpdatedAt: "2026-08-31T09:00:00",
              activeYn: "Y",
              changeReason: "학술지 등재정보 정비",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveJournalIndexingInfo: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-JOURNAL-INDEXING-MGMT", () => {
  it("renders journal indexing route contract and required save states", () => {
    const html = renderToStaticMarkup(<JournalIndexingInfoManagementPage />);

    expect(html).toContain('data-screen-id="SCR-JOURNAL-INDEXING-MGMT"');
    expect(html).toContain('data-testid="journal-indexing-info-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 학술지·후보지 등재정보 관리",
    );
    expect(html).toContain("학술지·후보지 등재정보 관리");
    expect(html).toContain("ISSN");
    expect(html).toContain("학술지명");
    expect(html).toContain("등재구분");
    expect(html).toContain("발행국가");
    expect(html).toContain("출처");
    expect(html).toContain("갱신일시");
    expect(html).toContain(
      "같은 ISSN과 유효기간이 중복되는 등재정보는 등록할 수 없습니다",
    );
    const componentSource = JournalIndexingInfoManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 학술지 등재정보가 없습니다");
    expect(componentSource).toContain(
      "학술지·후보지 등재정보 관리 권한이 없습니다",
    );
  });

  it("uses relative journal indexing API and exposes default page size options", async () => {
    const { journalIndexingInfoApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<JournalIndexingInfoManagementPage />);

    expect(
      journalIndexingInfoApi.listJournalIndexingInfos,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<JournalIndexingInfoManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});
