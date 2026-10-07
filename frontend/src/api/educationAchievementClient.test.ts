import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  courseOperationApi,
  employmentRateApi,
  employmentRateImprovementApi,
  lectureImprovementApi,
} from "./apiClient";

const fetchMock = vi.fn();
beforeEach(() => {
  fetchMock.mockReset().mockImplementation(
    async () =>
      new Response(
        JSON.stringify({
          success: true,
          data: {
            achievement: { achievementId: 917 },
            occurredDateWarning: true,
          },
          meta: {},
        }),
        { headers: { "Content-Type": "application/json" } },
      ),
  );
  vi.stubGlobal("fetch", fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

const common = {
  managementItemCode: "EDUCATION",
  achievementDate: "2026-03-01",
  attachmentIds: [],
};

describe("education API client registrations", () => {
  it("registers separate list/detail/create/update methods for all four resources", async () => {
    const cases = [
      {
        base: "/api/business/employment-rate-improvements",
        calls: async () => {
          const input = { ...common, specialLectureStartDate: "2026-03-01" };
          await employmentRateImprovementApi.list(2, 50);
          await employmentRateImprovementApi.detail(917);
          await employmentRateImprovementApi.create(input);
          await employmentRateImprovementApi.update(917, input);
          return input;
        },
      },
      {
        base: "/api/business/course-operations",
        calls: async () => {
          const input = { ...common, performanceDetails: "강좌 운영" };
          await courseOperationApi.list(2, 50);
          await courseOperationApi.detail(917);
          await courseOperationApi.create(input);
          await courseOperationApi.update(917, input);
          return input;
        },
      },
      {
        base: "/api/business/lecture-improvements",
        calls: async () => {
          const input = {
            ...common,
            achievementContent: "수업 개선",
            academicYear: 2026,
            semester: 1 as const,
          };
          await lectureImprovementApi.list(2, 50);
          await lectureImprovementApi.detail(917);
          await lectureImprovementApi.create(input);
          await lectureImprovementApi.update(917, input);
          return input;
        },
      },
      {
        base: "/api/business/employment-rate-achievements",
        calls: async () => {
          const input = { ...common, achievementName: "취업 실적" };
          await employmentRateApi.list(2, 50);
          await employmentRateApi.detail(917);
          await employmentRateApi.create(input);
          await employmentRateApi.update(917, input);
          return input;
        },
      },
    ];
    for (const entry of cases) {
      fetchMock.mockClear();
      const input = await entry.calls();
      expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
        `${entry.base}?page=2&pageSize=50`,
        `${entry.base}/917`,
        entry.base,
        `${entry.base}/917`,
      ]);
      expect(fetchMock.mock.calls[2][1]).toMatchObject({
        method: "POST",
        body: JSON.stringify(input),
      });
      expect(fetchMock.mock.calls[3][1]).toMatchObject({
        method: "PUT",
        body: JSON.stringify(input),
      });
      for (const [, init] of fetchMock.mock.calls) {
        expect(init.credentials).toBe("include");
        if (init.body)
          expect(JSON.parse(init.body)).not.toHaveProperty("achievementId");
      }
    }
  });

  it("preserves exact conflict codes, field errors and occurrence warnings", async () => {
    const response = await courseOperationApi.create({
      ...common,
      performanceDetails: "강좌",
    });
    expect(response.data?.occurredDateWarning).toBe(true);
    const error = {
      code: "CONFIRMED_DATA_LOCKED",
      message: "평가확정 실적입니다.",
      fields: [{ field: "achievementDate", message: "발생일을 확인하세요." }],
    };
    fetchMock.mockImplementationOnce(
      async () =>
        new Response(
          JSON.stringify({
            success: false,
            error,
            meta: { requestId: "request-test" },
          }),
          { status: 409, headers: { "Content-Type": "application/json" } },
        ),
    );
    await expect(
      courseOperationApi.update(917, { ...common, performanceDetails: "강좌" }),
    ).rejects.toMatchObject({ status: 409, apiError: error });
  });

  it("reuses the employment workflow with multipart file-only staging and canonical bulk paths", async () => {
    const file = new File(["test workbook request body"], "achievements.xlsx");
    await employmentRateApi.upload(file);
    const [uploadPath, uploadInit] = fetchMock.mock.calls[0];
    expect(uploadPath).toBe(
      "/api/business/employment-rate-achievements/excel-uploads",
    );
    expect(uploadInit).toMatchObject({
      method: "POST",
      credentials: "include",
    });
    expect(uploadInit.headers).toBeUndefined();
    expect([...uploadInit.body.keys()]).toEqual(["file"]);
    expect(uploadInit.body.get("file")).toBe(file);
    const input = {
      evaluationYear: "2026",
      actionType: "GENERATE" as const,
      targetCondition: {},
    };
    await employmentRateApi.createBulk(input);
    await employmentRateApi.bulkResult("job/selected");
    await employmentRateApi.download(0, 20);
    expect(fetchMock.mock.calls[1]).toEqual([
      "/api/business/employment-rate-achievements/bulk-jobs",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify(input),
        credentials: "include",
      }),
    ]);
    expect(fetchMock.mock.calls[2][0]).toBe(
      "/api/business/employment-rate-achievements/bulk-jobs/job%2Fselected",
    );
    expect(fetchMock.mock.calls[3]).toEqual([
      "/api/business/employment-rate-achievements/download?page=0&pageSize=20",
      { credentials: "include" },
    ]);
  });
});
