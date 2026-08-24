import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  AttachmentDeletePage,
  attachmentDeleteApi,
} from "./SCR-ATTACHMENT-DELETE";

describe("SCR-ATTACHMENT-DELETE", () => {
  it("renders target lookup, required delete reason, confirm modal copy, and result states", () => {
    const html = renderToStaticMarkup(<AttachmentDeletePage />);

    expect(html).toContain('data-screen-id="SCR-ATTACHMENT-DELETE"');
    expect(html).toContain('data-testid="attachment-delete-screen"');
    expect(html).toContain("첨부파일 삭제");
    expect(html).toContain("파일ID");
    expect(html).toContain("삭제대상 확인");
    expect(html).toContain("대상 파일");
    expect(html).toContain("연결 업무자료");
    expect(html).toContain("삭제사유");
    expect(html).toContain("논리삭제");
    expect(html).toContain("삭제 확인");
    expect(html).toContain("삭제 실행");
    expect(html).toContain("평가확정 자료는 삭제할 수 없습니다");
    expect(html).toContain("삭제 결과 안내");
  });

  it("uses relative delete target and logical delete API paths derived from selected file id", async () => {
    const calls: string[] = [];
    const originalFetch = globalThis.fetch;
    globalThis.fetch = (async (input: RequestInfo | URL) => {
      calls.push(String(input));
      return new Response(
        JSON.stringify({ success: true, data: {}, meta: {} }),
        {
          status: 200,
          headers: { "content-type": "application/json" },
        },
      );
    }) as typeof fetch;

    try {
      await attachmentDeleteApi.getAttachmentDeleteTarget(1001);
      await attachmentDeleteApi.logicallyDeleteAttachment(
        1001,
        "중복 제출 정리",
      );
    } finally {
      globalThis.fetch = originalFetch;
    }

    expect(calls).toEqual([
      "/api/admin/attachments/1001/delete-target",
      "/api/admin/attachments/1001/logical-delete",
    ]);
    expect(calls.join(" ")).not.toContain("localhost");
  });
});
