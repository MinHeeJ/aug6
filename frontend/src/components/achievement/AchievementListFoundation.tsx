import { useMemo, useState, type ReactNode } from "react";

type ApiError = {
  message?: string;
  fields?: Array<{ field: string; message: string }>;
};

type AchievementListFoundationProps<T> = {
  rows: T[];
  totalElements: number;
  requiredFields: Record<string, string>;
  renderRow: (row: T) => ReactNode;
  onSave: () => Promise<void>;
  onReload: () => Promise<void>;
  onExcelDownload: () => void;
};

export function AchievementListFoundation<T>({
  rows,
  totalElements,
  requiredFields,
  renderRow,
  onSave,
  onReload,
  onExcelDownload,
}: AchievementListFoundationProps<T>) {
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [page, setPage] = useState(0);
  const [message, setMessage] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});

  const pageRows = useMemo(
    () => rows.slice(page * size, page * size + size),
    [page, rows, size],
  );
  const missingRequiredFields = Object.entries(requiredFields)
    .filter(([, value]) => !value.trim())
    .map(([field]) => field);

  const save = async () => {
    if (missingRequiredFields.length > 0) {
      setMessage("필수 입력 항목을 확인해 주세요");
      setFields(
        Object.fromEntries(
          missingRequiredFields.map((field) => [
            field,
            "필수 입력 항목입니다.",
          ]),
        ),
      );
      return;
    }
    if (!window.confirm("저장하시겠습니까")) return;
    try {
      setMessage(null);
      setFields({});
      await onSave();
      await onReload();
    } catch (caught) {
      const error = caught as ApiError;
      setMessage(error.message ?? "저장하지 못했습니다.");
      setFields(
        Object.fromEntries(
          (error.fields ?? []).map(({ field, message: fieldMessage }) => [
            field,
            fieldMessage,
          ]),
        ),
      );
    }
  };

  return (
    <section data-testid="achievement-list-foundation">
      <div className="flex gap-2">
        <select
          aria-label="표시 건수"
          value={size}
          onChange={(event) => {
            setPage(0);
            setSize(Number(event.target.value) as 20 | 50 | 100);
          }}
        >
          {[20, 50, 100].map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
        <button type="button" onClick={onExcelDownload}>
          엑셀 다운로드
        </button>
        <button type="button" onClick={() => void save()}>
          저장
        </button>
      </div>
      {message && <p role="alert">{message}</p>}
      {Object.entries(fields).map(([field, fieldMessage]) => (
        <p key={field} role="alert">
          {field}: {fieldMessage}
        </p>
      ))}
      {pageRows.length === 0 ? (
        <p>조회된 실적이 없습니다</p>
      ) : (
        <table>
          <tbody>{pageRows.map(renderRow)}</tbody>
        </table>
      )}
      <div>
        <button
          type="button"
          disabled={page === 0}
          onClick={() => setPage((current) => Math.max(0, current - 1))}
        >
          이전
        </button>
        <span>{page + 1}</span>
        <button
          type="button"
          disabled={(page + 1) * size >= totalElements}
          onClick={() => setPage((current) => current + 1)}
        >
          다음
        </button>
        <button type="button" onClick={() => void onReload()}>
          다시 시도
        </button>
      </div>
    </section>
  );
}
