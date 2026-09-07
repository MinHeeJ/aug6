import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SchoolInfoLookupPage } from "./SCR-SCHOOL-INFO-LOOKUP";
import { schoolInfoApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    schoolInfoApi: {
      search: vi.fn(),
      paths: {
        search: actual.schoolInfoApi.paths.search,
      },
    },
  };
});

describe("SCR-SCHOOL-INFO-LOOKUP", () => {
  beforeEach(() => {
    vi.mocked(schoolInfoApi.search).mockReset();
  });

  it("renders route contract, search controls, and relative API query path", () => {
    render(<SchoolInfoLookupPage />);

    expect(screen.getByTestId("school-info-lookup-page")).toHaveAttribute(
      "data-screen-id",
      "SCR-SCHOOL-INFO-LOOKUP",
    );
    expect(
      screen.getByText("시스템 관리 / 외부 연동 / 학교 정보 조회"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("학교명")).toBeInTheDocument();
    expect(screen.getByLabelText("시도교육청 코드")).toBeInTheDocument();
    expect(screen.getByTestId("school-info-search-button")).toBeEnabled();
    expect(
      schoolInfoApi.paths.search({
        schoolName: "가락",
        educationOfficeCode: "B10",
        page: 1,
        size: 100,
      }),
    ).toBe(
      "/api/admin/school-info?schoolName=%EA%B0%80%EB%9D%BD&educationOfficeCode=B10&page=1&size=100",
    );
  });

  it("renders successful result table and displayed row count from actual rows", async () => {
    vi.mocked(schoolInfoApi.search).mockResolvedValue({
      success: true,
      data: {
        page: 1,
        size: 100,
        displayedCount: 1,
        rows: [
          {
            educationOfficeName: "서울특별시교육청",
            schoolName: "가락고등학교",
            schoolKindName: "고등학교",
            locationName: "서울",
            foundationName: "공립",
            roadAddress: "서울 송파구 송이로 42",
            telephoneNumber: "02-0000-0000",
          },
        ],
      },
      meta: {},
    });

    render(<SchoolInfoLookupPage />);
    fireEvent.change(screen.getByLabelText("학교명"), {
      target: { value: "가락" },
    });
    fireEvent.click(screen.getByTestId("school-info-search-button"));

    await screen.findByText("가락고등학교");
    expect(screen.getByText("표시 1건")).toBeInTheDocument();
    expect(screen.getByText("교육청명")).toBeInTheDocument();
    expect(screen.getByText("학교종류")).toBeInTheDocument();
    expect(screen.getByText("도로명주소")).toBeInTheDocument();
  });

  it("shows empty guidance without error banner for INFO-200 empty result", async () => {
    vi.mocked(schoolInfoApi.search).mockResolvedValue({
      success: true,
      data: { page: 1, size: 100, displayedCount: 0, rows: [] },
      meta: {},
    });

    render(<SchoolInfoLookupPage />);
    fireEvent.click(screen.getByTestId("school-info-search-button"));

    await screen.findByText("조회 결과가 없습니다");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shows external API error reason without crashing the page", async () => {
    vi.mocked(schoolInfoApi.search).mockRejectedValue(
      new Error("NEIS 오류: 서비스 키가 유효하지 않습니다."),
    );

    render(<SchoolInfoLookupPage />);
    fireEvent.click(screen.getByTestId("school-info-search-button"));

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(
        "NEIS 오류: 서비스 키가 유효하지 않습니다.",
      ),
    );
    expect(screen.getByTestId("school-info-search-button")).toBeEnabled();
  });
});
