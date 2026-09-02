import { render, screen, waitFor } from "@testing-library/react";
import { renderToStaticMarkup } from "react-dom/server";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { researcherProfileApi } from "../../api/apiClient";
import {
  DegreePrerequisiteMissingPage,
  ResearcherProfileDetailPage,
  ResearcherProfileListPage,
} from "./SCR-RESEARCHER-PROFILES";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  const profile = {
    employeeNo: "E1001",
    name: "홍길동",
    organizationCode: "KNUE-DEPT-COMP",
    organizationName: "컴퓨터교육과",
    rankName: "교수",
    appointmentId: "E1001-APPT",
    researcherRegistrationNo: "RID-1001",
    externalProvisionYn: "N",
    informationPublicYn: "Y",
    finalDegreeType: "DOCTOR",
    degreePrerequisiteMissing: true,
    researchFields: [
      {
        majorName: "컴퓨터교육",
        detailMajorName: "AI교육",
        majorSeries: "공학",
      },
    ],
    careers: [
      {
        workStartYm: "202001",
        workEndYm: "202412",
        workplace: "한국교원대학교",
      },
    ],
    degrees: [{ degreeType: "DOCTOR", universityName: "한국교원대학교" }],
    certifications: [
      { certificationName: "교원자격", issuingOrganizationName: "교육부" },
    ],
  };
  return {
    ...actual,
    researcherProfileApi: {
      listProfiles: vi.fn(async () => ({
        success: true,
        data: { profiles: [profile], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })),
      getProfile: vi.fn(async () => ({
        success: true,
        data: profile,
        meta: {},
      })),
      saveResearchFields: vi.fn(),
      saveCareers: vi.fn(),
      saveDegrees: vi.fn(),
      saveCertifications: vi.fn(),
      listDegreePrerequisiteMissing: vi.fn(async () => ({
        success: true,
        data: { profiles: [profile], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })),
    },
  };
});

describe("SCR-RESEARCHER-PROFILES", () => {
  it("renders profile list search contract and Korean UI labels", () => {
    const html = renderToStaticMarkup(<ResearcherProfileListPage />);

    expect(html).toContain('data-screen-id="SCR-RESEARCHER-PROFILE-LIST"');
    expect(html).toContain('data-testid="researcher-profile-list-page"');
    expect(html).toContain("교원 검색·목록");
    expect(html).toContain("교번");
    expect(html).toContain("성명");
    expect(html).toContain("소속");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });

  it("renders detail tabs, readonly KORUS fields, and save CTA", () => {
    window.history.pushState({}, "", "/researcher-profiles/E1001");

    const html = renderToStaticMarkup(<ResearcherProfileDetailPage />);

    expect(html).toContain('data-screen-id="SCR-RESEARCHER-PROFILE-DETAIL"');
    expect(html).toContain("KORUS 기본정보는 조회 전용");
    expect(html).toContain("연구분야");
    expect(html).toContain("경력");
    expect(html).toContain("취득학위");
    expect(html).toContain("자격사항");
    expect(html).toContain("저장");
  });

  it("does not call placeholder detail API and shows researcher selection guidance", async () => {
    window.history.pushState({}, "", "/researcher-profiles/%7BemployeeNo%7D");
    vi.mocked(researcherProfileApi.getProfile).mockClear();

    render(<ResearcherProfileDetailPage />);

    await waitFor(() =>
      expect(researcherProfileApi.getProfile).not.toHaveBeenCalled(),
    );
    expect(screen.getByText("연구자 선택이 필요합니다")).toBeTruthy();
    expect(screen.getByText(/목록에서 실제 연구자를 선택/)).toBeTruthy();
  });

  it("renders degree prerequisite missing route without mutation CTA", () => {
    const html = renderToStaticMarkup(<DegreePrerequisiteMissingPage />);

    expect(html).toContain('data-screen-id="SCR-DEGREE-PREREQ-MISSING"');
    expect(html).toContain('data-testid="degree-prerequisite-missing-page"');
    expect(html).toContain("선행학위 미충족 대상");
    expect(html).toContain("학사·석사·박사");
    expect(html).toContain("조회");
    expect(html).not.toContain("삭제");
  });
});
