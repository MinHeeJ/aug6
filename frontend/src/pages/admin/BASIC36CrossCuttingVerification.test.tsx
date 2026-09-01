import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  AchievementDataAsOfPage,
  AchievementDataHistoryPage,
} from "./SCR-ACHIEVEMENT-DATA-HISTORY";
import { FullTimeFacultyStatusPage } from "./SCR-FULL-TIME-FACULTY-STATUS";
import { KorusFacultySyncPage } from "./SCR-KORUS-FACULTY-SYNC";
import {
  DegreePrerequisiteMissingPage,
  ResearcherProfileDetailPage,
  ResearcherProfileListPage,
} from "./SCR-RESEARCHER-PROFILES";

const listScreens = [
  {
    name: "KORUS 교원 기본정보 연계",
    html: () => renderToStaticMarkup(<KorusFacultySyncPage />),
    exportTestId: "korus-faculty-sync-excel-button",
    pageSizeTestId: "korus-faculty-sync-page-size-select",
  },
  {
    name: "전임교원 현황",
    html: () => renderToStaticMarkup(<FullTimeFacultyStatusPage />),
    exportTestId: "full-time-faculty-status-excel-button",
    pageSizeTestId: "full-time-faculty-status-page-size-select",
  },
  {
    name: "연구자 프로필 목록",
    html: () => renderToStaticMarkup(<ResearcherProfileListPage />),
    exportTestId: "researcher-profile-list-excel-button",
    pageSizeTestId: "researcher-profile-list-page-size-select",
  },
  {
    name: "선행학위 미충족 대상",
    html: () => renderToStaticMarkup(<DegreePrerequisiteMissingPage />),
    exportTestId: "degree-prerequisite-missing-excel-button",
    pageSizeTestId: "degree-prerequisite-missing-page-size-select",
  },
  {
    name: "업적데이터 변경이력",
    html: () => renderToStaticMarkup(<AchievementDataHistoryPage />),
    exportTestId: "achievement-data-history-excel-button",
    pageSizeTestId: "achievement-data-history-page-size-select",
  },
  {
    name: "업적데이터 기준시점",
    html: () => renderToStaticMarkup(<AchievementDataAsOfPage />),
    exportTestId: "achievement-data-as-of-excel-button",
    pageSizeTestId: "achievement-data-as-of-page-size-select",
  },
];

describe("BASIC-36 cross-cutting verification", () => {
  it.each(listScreens)(
    "$name screen keeps 20/50/100 pagination and permits list Excel export",
    ({ html, exportTestId, pageSizeTestId }) => {
      const markup = html();

      expect(markup).toContain(`data-testid=\"${pageSizeTestId}\"`);
      expect(markup).toContain("20건");
      expect(markup).toContain("50건");
      expect(markup).toContain("100건");
      expect(markup).toContain(`data-testid=\"${exportTestId}\"`);
      expect(markup).toContain("엑셀 내려받기");
    },
  );

  it("forbids Excel export on the researcher profile write/detail screen", () => {
    window.history.pushState({}, "", "/researcher-profiles/E1001");

    const markup = renderToStaticMarkup(<ResearcherProfileDetailPage />);

    expect(markup).not.toContain("엑셀 내려받기");
    expect(markup).not.toContain("researcher-profile-detail-excel-button");
  });

  it.each(listScreens)(
    "$name initial page markup stays below the 3MB page budget",
    ({ html }) => {
      const encodedBytes = new TextEncoder().encode(html()).byteLength;

      expect(encodedBytes).toBeLessThanOrEqual(3 * 1024 * 1024);
    },
  );
});
