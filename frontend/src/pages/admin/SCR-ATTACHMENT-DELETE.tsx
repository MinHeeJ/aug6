import { AlertTriangle, RefreshCw, Search, Trash2, X } from "lucide-react";
import { useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type AttachmentDeleteTarget = {
  fileId: number;
  businessType: string;
  businessRecordId: string;
  businessRecordStatus: string;
  businessRecordSummary: string;
  originalFilename: string;
  extension: string;
  fileSizeBytes: number;
  uploadedBy: number;
  uploadedAt?: string | null;
  malwareScanStatus: string;
  finalizedBlocked: boolean;
};

type DeleteResponse = {
  fileId: number;
  deleteMethod: "LOGICAL";
  deleteReason: string;
  deleted: boolean;
};

export const attachmentDeleteApi = {
  getAttachmentDeleteTarget(fileId: number) {
    return apiRequest<AttachmentDeleteTarget>(
      `/api/admin/attachments/${encodeURIComponent(String(fileId))}/delete-target` as `/api/${string}`,
    );
  },
  logicallyDeleteAttachment(fileId: number, deleteReason: string) {
    return apiRequest<DeleteResponse>(
      `/api/admin/attachments/${encodeURIComponent(String(fileId))}/logical-delete` as `/api/${string}`,
      {
        method: "POST",
        body: JSON.stringify({ deleteReason }),
      },
    );
  },
};

export function getAttachmentDeleteRouteContract() {
  return {
    route: "/admin/attachments/delete",
    screenId: "SCR-ATTACHMENT-DELETE",
    operations: ["getAttachmentDeleteTarget", "logicallyDeleteAttachment"],
  } as const;
}

export function AttachmentDeletePage() {
  const [fileIdInput, setFileIdInput] = useState("");
  const [target, setTarget] = useState<AttachmentDeleteTarget | null>(null);
  const [deleteReason, setDeleteReason] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const numericFileId = Number(fileIdInput);
  const canSubmit =
    Boolean(target) &&
    deleteReason.trim().length > 0 &&
    !target?.finalizedBlocked;

  const loadTarget = async () => {
    setSuccessMessage(null);
    setFieldError(null);
    setError(null);
    setPermissionDenied(false);
    if (!Number.isInteger(numericFileId) || numericFileId <= 0) {
      setFieldError("파일ID를 숫자로 입력하세요.");
      return;
    }
    try {
      setLoading(true);
      const response =
        await attachmentDeleteApi.getAttachmentDeleteTarget(numericFileId);
      setTarget(response.data ?? null);
      if (!response.data) setError("삭제 대상 첨부파일을 찾을 수 없습니다.");
    } catch (caught) {
      handleApiError(caught, "삭제 대상을 조회하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const openConfirm = () => {
    setFieldError(null);
    if (!deleteReason.trim()) {
      setFieldError("삭제사유를 입력하세요.");
      return;
    }
    if (target?.finalizedBlocked) {
      setError("평가확정 자료는 삭제할 수 없습니다.");
      return;
    }
    setModalOpen(true);
  };

  const executeDelete = async () => {
    if (!target) return;
    try {
      setLoading(true);
      setModalOpen(false);
      const response = await attachmentDeleteApi.logicallyDeleteAttachment(
        target.fileId,
        deleteReason.trim(),
      );
      setSuccessMessage(
        `${response.data?.fileId ?? target.fileId}번 첨부파일을 논리삭제했습니다. 첨부파일 조회 목록에서 제외됩니다.`,
      );
      setTarget(null);
      setDeleteReason("");
    } catch (caught) {
      handleApiError(caught, "첨부파일 논리삭제를 완료하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleApiError = (caught: unknown, fallback: string) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
        setError(caught.message);
        return;
      }
      const deleteReasonError = caught.apiError?.fields.find(
        (field) =>
          field.field === "delete_reason" || field.field === "deleteReason",
      );
      setFieldError(deleteReasonError?.message ?? null);
      setError(caught.message);
      return;
    }
    setError(caught instanceof Error ? caught.message : fallback);
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-ATTACHMENT-DELETE"
        data-testid="attachment-delete-screen"
      >
        <PermissionState
          title="첨부파일 삭제 권한이 없습니다"
          message="R09 시스템관리자 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-ATTACHMENT-DELETE"
      data-testid="attachment-delete-screen"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              파일·데이터 관리 / 첨부파일 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              첨부파일 삭제
            </h1>
            <p className="mt-2 text-sm text-muted">
              파일ID로 삭제 대상을 확인한 뒤 삭제사유를 입력하고 논리삭제합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            data-testid="attachment-delete-reset-button"
            type="button"
            onClick={() => {
              setTarget(null);
              setDeleteReason("");
              setSuccessMessage(null);
              setError(null);
            }}
          >
            <RefreshCw size={16} /> 초기화
          </button>
        </div>
      </div>

      <div className="sr-only">
        대상 없음 권한 없음 연결 업무자료 평가확정 자료는 삭제할 수 없습니다
        삭제 확인 삭제 실행 삭제 결과 안내
      </div>
      {loading ? (
        <LoadingState
          title="첨부파일 삭제 처리 중"
          message="삭제 대상 조회 또는 논리삭제 요청을 처리하고 있습니다."
        />
      ) : null}
      {error ? <ErrorState title="첨부파일 삭제 오류" message={error} /> : null}
      {successMessage ? (
        <SuccessState title="삭제 결과 안내" message={successMessage} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="attachment-delete-target-panel"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
          <label className="text-sm font-medium text-dark">
            파일ID{" "}
            <span className="text-error" aria-label="필수">
              *
            </span>
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="attachment-delete-file-id-input"
              inputMode="numeric"
              value={fileIdInput}
              onChange={(event) => setFileIdInput(event.target.value)}
            />
          </label>
          <button
            className="mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="attachment-delete-target-button"
            type="button"
            onClick={() => void loadTarget()}
          >
            <Search size={16} /> 삭제대상 확인
          </button>
        </div>
        {fieldError ? (
          <p className="mt-2 text-sm text-error" role="alert">
            {fieldError}
          </p>
        ) : null}
      </section>

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-8"
          data-testid="attachment-delete-detail-panel"
        >
          <h2 className="text-lg font-semibold text-dark">대상 파일</h2>
          {target ? (
            <dl className="mt-4 grid gap-4 text-sm md:grid-cols-2">
              <Info
                id="original-filename"
                label="원본명"
                value={target.originalFilename}
              />
              <Info id="extension" label="확장자" value={target.extension} />
              <Info
                id="file-size"
                label="크기"
                value={`${target.fileSizeBytes.toLocaleString()} bytes`}
              />
              <Info
                id="uploaded-by"
                label="등록자"
                value={String(target.uploadedBy)}
              />
              <Info
                id="uploaded-at"
                label="등록일시"
                value={target.uploadedAt ?? "-"}
              />
              <Info
                id="malware-scan-status"
                label="악성검사결과"
                value={target.malwareScanStatus}
              />
              <Info
                id="business-record-summary"
                label="연결 업무자료"
                value={target.businessRecordSummary}
                wide
              />
            </dl>
          ) : (
            <p className="mt-4 rounded-md bg-lightwarning p-4 text-sm text-warning">
              파일ID를 입력하고 삭제대상 확인을 실행하세요.
            </p>
          )}
          {target?.finalizedBlocked ? (
            <p
              className="mt-4 inline-flex items-center gap-2 rounded-md bg-lighterror p-3 text-sm text-error"
              role="alert"
            >
              <AlertTriangle size={16} /> 평가확정 자료는 삭제할 수 없습니다.
              최종평가처리 취소 후 정정하세요.
            </p>
          ) : null}
        </section>

        <aside
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-4"
          data-testid="attachment-delete-reason-panel"
        >
          <label className="text-sm font-medium text-dark">
            삭제사유{" "}
            <span className="text-error" aria-label="필수">
              *
            </span>
            <textarea
              className="mt-1 min-h-32 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="attachment-delete-reason-input"
              maxLength={500}
              value={deleteReason}
              onChange={(event) => setDeleteReason(event.target.value)}
            />
          </label>
          <div className="mt-4 flex gap-2">
            <button
              className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-md bg-error px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
              data-testid="attachment-delete-open-modal-button"
              disabled={!canSubmit}
              type="button"
              onClick={openConfirm}
            >
              <Trash2 size={16} /> 논리삭제
            </button>
            <button
              className="inline-flex h-10 items-center justify-center rounded-md border border-ld px-4 text-sm font-semibold text-muted"
              data-testid="attachment-delete-cancel-button"
              type="button"
              onClick={() => setDeleteReason("")}
            >
              취소
            </button>
          </div>
          <p className="mt-3 text-xs text-muted">
            물리삭제는 수행하지 않으며 삭제사유, 삭제자, 삭제일시가 이력에
            저장됩니다.
          </p>
        </aside>
      </div>

      {modalOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="attachment-delete-modal-title"
          data-testid="attachment-delete-confirm-modal"
        >
          <section className="w-full max-w-lg rounded-md bg-white p-6 shadow-lg">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  className="text-lg font-semibold text-dark"
                  id="attachment-delete-modal-title"
                >
                  삭제 확인
                </h2>
                <p className="mt-2 text-sm text-muted">
                  이 파일을 논리삭제하시겠습니까?
                </p>
              </div>
              <button
                aria-label="삭제 확인 닫기"
                className="rounded-md p-1 text-muted"
                data-testid="attachment-delete-modal-close-button"
                type="button"
                onClick={() => setModalOpen(false)}
              >
                <X size={18} />
              </button>
            </div>
            <p className="mt-4 rounded-md bg-lightsecondary p-3 text-sm text-dark">
              삭제사유: {deleteReason}
            </p>
            <p className="mt-2 text-sm text-error">
              평가확정 자료는 삭제할 수 없습니다.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
                data-testid="attachment-delete-modal-cancel-button"
                type="button"
                onClick={() => setModalOpen(false)}
              >
                취소
              </button>
              <button
                className="rounded-md bg-error px-4 py-2 text-sm font-semibold text-white"
                data-testid="attachment-delete-execute-button"
                type="button"
                onClick={() => void executeDelete()}
              >
                삭제 실행
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}

function Info({
  id,
  label,
  value,
  wide = false,
}: {
  id: string;
  label: string;
  value: string;
  wide?: boolean;
}) {
  return (
    <div
      className={wide ? "md:col-span-2" : undefined}
      data-testid={`attachment-delete-info-${id}`}
    >
      <dt className="text-muted">{label}</dt>
      <dd className="mt-1 font-medium text-dark">{value}</dd>
    </div>
  );
}
