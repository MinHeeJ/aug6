import { useCallback, useEffect, useRef, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Row = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  specialLectureStartDate: string | null;
  specialLectureEndDate: string | null;
  mockExamQuestionPeriod: string | null;
  attachmentIds: string[];
};
type Item = {
  managementItemId: number;
  code: string;
  name: string;
  evaluationYear: string;
  requiredYn: string;
  dataType: string;
  teacherEditableYn: string;
};
type Result = {
  achievements: Row[];
  totalElements: number;
  managementItems: Item[];
};
type SaveResult = {
  achievement: Row;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
};
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
};
const emptyFilters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  certificationStatus: "",
};
const base = "/api/business/employment-rate-improvements";

/** FR-029 inside the existing authenticated shell; canonical route is registered by the integration owner. */
export function EmploymentRateImprovementManagementPage() {
  const { user } = useAuth();
  const [filters, setFilters] = useState(emptyFilters);
  const [applied, setApplied] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [rows, setRows] = useState<Row[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const listVersion = useRef(0);
  const detailVersion = useRef(0);
  const canCreate = user?.roles.includes("R01") ?? false;
  const locked =
    selected !== null &&
    (selected.achievementStatus !== "DRAFT" ||
      selected.teacherUserId !== user?.userId);
  const disabled = !canCreate || locked || saving || detailLoading;
  const year = selected?.evaluationYear ?? form.achievementDate.slice(0, 4);
  const choices = items.filter((item) => item.evaluationYear === year);
  const item = choices.find(
    (choice) => choice.code === form.managementItemCode,
  );

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403 || caught.status === 401);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      setError(caught.message);
    } else {
      setError("실적을 처리하지 못했습니다. 다시 시도하세요.");
    }
  };

  const load = useCallback(async () => {
    const version = ++listVersion.current;
    setLoading(true);
    setError("");
    setDenied(false);
    const query = new URLSearchParams({
      page: String(page),
      pageSize: String(pageSize),
    });
    Object.entries(applied).forEach(([key, value]) => {
      if (value.trim()) query.set(key, value.trim());
    });
    try {
      const response = await apiRequest<Result>(`${base}?${query}`);
      if (version !== listVersion.current) return;
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
      setItems(response.data?.managementItems ?? []);
    } catch (caught) {
      if (version !== listVersion.current) return;
      setRows([]);
      setTotal(0);
      handleError(caught);
    } finally {
      if (version === listVersion.current) setLoading(false);
    }
  }, [page, pageSize, applied]);

  useEffect(() => {
    void load();
    return () => {
      listVersion.current++;
    };
  }, [load]);

  const fill = (row: Row) => {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      specialLectureStartDate: row.specialLectureStartDate ?? "",
      specialLectureEndDate: row.specialLectureEndDate ?? "",
      mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
    });
  };

  const detail = async (row: Row) => {
    const version = ++detailVersion.current;
    setDetailLoading(true);
    setFields({});
    setSuccess("");
    setError("");
    try {
      const response = await apiRequest<Row>(`${base}/${row.achievementId}`);
      if (version === detailVersion.current && response.data)
        fill(response.data);
    } catch (caught) {
      if (version === detailVersion.current) handleError(caught);
    } finally {
      if (version === detailVersion.current) setDetailLoading(false);
    }
  };

  const save = async () => {
    if (disabled) return;
    const validation: Record<string, string> = {};
    if (!form.achievementDate)
      validation.achievementDate = "업적발생일을 입력하세요.";
    if (!form.managementItemCode)
      validation.managementItemCode = "관리항목을 선택하세요.";
    if (
      !!form.specialLectureStartDate !== !!form.specialLectureEndDate ||
      (form.specialLectureStartDate &&
        form.specialLectureEndDate < form.specialLectureStartDate)
    ) {
      validation.specialLectureEndDate =
        "특강 시작일과 종료일을 함께 입력하고 순서를 확인하세요.";
    }
    const periodValue =
      (item?.dataType === "TEXT" || item?.dataType === "DATE") &&
      !!form.specialLectureStartDate;
    if (
      item?.requiredYn === "Y" &&
      !form.mockExamQuestionPeriod.trim() &&
      !periodValue
    ) {
      validation.mockExamQuestionPeriod =
        "관리항목의 필수 실적값을 입력하세요.";
    }
    setFields(validation);
    if (
      Object.keys(validation).length ||
      !window.confirm("취업률 제고 실적을 저장하시겠습니까?")
    )
      return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const response = await apiRequest<SaveResult>(
        selected ? `${base}/${selected.achievementId}` : base,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode,
            achievementDate: form.achievementDate,
            specialLectureStartDate: form.specialLectureStartDate || null,
            specialLectureEndDate: form.specialLectureEndDate || null,
            mockExamQuestionPeriod: form.mockExamQuestionPeriod.trim() || null,
            attachmentIds: selected?.attachmentIds ?? [],
          }),
        },
      );
      if (response.data) {
        fill(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "평가대상 기간 밖 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
      data-testid="employment-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
      </div>
      {denied ? (
        <PermissionState message="해당 실적의 조회 또는 처리 권한이 없습니다." />
      ) : null}
      {error ? <ErrorState message={error} /> : null}
      {success ? <SuccessState message={success} /> : null}
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="grid gap-4 md:grid-cols-4">
          {Object.entries(filters).map(([key, value], index) => (
            <label key={key} className="text-sm text-muted">
              {["관리번호", "성명", "관리항목", "인증상태"][index]}
              <input
                data-testid={`employment-improvement-filter-${key}`.replace(
                  /[A-Z]/g,
                  (c) => `-${c.toLowerCase()}`,
                )}
                className="mt-1 w-full p-2"
                value={value}
                onChange={(event) =>
                  setFilters({ ...filters, [key]: event.target.value })
                }
              />
            </label>
          ))}
        </div>
        <button
          data-testid="employment-improvement-search"
          className="mt-4 rounded-md border border-primary px-4 py-2 text-primary"
          onClick={() => {
            setApplied({ ...filters });
            setPage(0);
          }}
          disabled={saving}
          type="button"
        >
          조회
        </button>
      </section>
      <section className="rounded-md border border-ld bg-white p-5">
        <h2 className="text-lg font-semibold">실적 목록</h2>
        <label className="text-sm">
          표시 건수
          <select
            data-testid="employment-improvement-page-size"
            className="m-2 p-2"
            value={pageSize}
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setPage(0);
            }}
          >
            {[20, 50, 100].map((size) => (
              <option key={size} value={size}>
                {size}건
              </option>
            ))}
          </select>
        </label>
        {loading ? <LoadingState /> : null}
        {!loading && !error && !rows.length ? (
          <EmptyState title="조회된 취업률 제고 실적이 없습니다" />
        ) : null}
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-lightsecondary text-left">
              <tr>
                {[
                  "관리번호",
                  "성명",
                  "관리항목",
                  "업적발생일",
                  "인증상태",
                  "첨부",
                  "상세",
                ].map((label) => (
                  <th key={label} className="px-3 py-2">
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`employment-improvement-row-${row.achievementId}`}
                >
                  <td className="p-3">{row.managementNo}</td>
                  <td className="p-3">{row.teacherName}</td>
                  <td className="p-3">{row.managementItemCode}</td>
                  <td className="p-3">{row.achievementDate}</td>
                  <td className="p-3">{row.achievementStatus}</td>
                  <td className="p-3">
                    {row.attachmentIds?.length ? "있음" : "없음"}
                  </td>
                  <td className="p-3">
                    <button
                      data-testid={`employment-improvement-detail-${row.achievementId}`}
                      className="rounded border border-primary px-3 py-1 text-primary"
                      disabled={saving || detailLoading}
                      onClick={() => void detail(row)}
                      type="button"
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center gap-4">
          <button
            data-testid="employment-improvement-previous"
            disabled={page === 0 || loading || saving}
            onClick={() => setPage(page - 1)}
            type="button"
          >
            이전
          </button>
          <span>
            총 {total}건 / {page + 1}페이지
          </span>
          <button
            data-testid="employment-improvement-next"
            disabled={(page + 1) * pageSize >= total || loading || saving}
            onClick={() => setPage(page + 1)}
            type="button"
          >
            다음
          </button>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-improvement-detail-panel"
      >
        <h2 className="text-lg font-semibold">실적 상세</h2>
        {detailLoading ? <LoadingState title="상세 조회 중" /> : null}
        {locked ? (
          <p className="mt-2 text-error">
            본인의 작성중 실적만 수정할 수 있습니다. 확정 실적은 잠겨 있습니다.
          </p>
        ) : null}
        <p className="mt-2 text-sm text-muted">
          업적발생일이 평가대상 기간 밖이면 경고와 함께 저장됩니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="text-sm">
            관리번호
            <input
              data-testid="employment-improvement-management-no"
              disabled
              value={selected?.managementNo ?? "자동 발급"}
            />
          </label>
          <label className="text-sm">
            성명
            <input
              data-testid="employment-improvement-teacher"
              disabled
              value={selected?.teacherName ?? user?.name ?? ""}
            />
          </label>
          <label className="text-sm">
            업적발생일 *
            <input
              data-testid="employment-improvement-achievement-date"
              type="date"
              className="mt-1 w-full p-2"
              disabled={disabled}
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
            <span className="text-error">{fields.achievementDate}</span>
          </label>
          <label className="text-sm">
            관리항목 *
            <select
              data-testid="employment-improvement-management-item"
              className="mt-1 w-full p-2"
              disabled={disabled || !choices.length}
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">
                {choices.length
                  ? "관리항목 선택"
                  : "평가연도의 관리항목 설정이 없습니다"}
              </option>
              {choices.map((choice) => (
                <option
                  key={choice.managementItemId}
                  value={choice.code}
                  disabled={
                    choice.teacherEditableYn !== "Y" ||
                    choices.filter((x) => x.code === choice.code).length !== 1
                  }
                >
                  {choice.name} ({choice.code})
                </option>
              ))}
            </select>
            <span className="text-error">{fields.managementItemCode}</span>
          </label>
          {(
            [
              "specialLectureStartDate",
              "specialLectureEndDate",
              "mockExamQuestionPeriod",
            ] as const
          ).map((key, i) => (
            <label key={key} className="text-sm">
              {["특강 시작일", "특강 종료일", "모의평가 출제기간"][i]}
              <input
                data-testid={`employment-improvement-${key}`.replace(
                  /[A-Z]/g,
                  (c) => `-${c.toLowerCase()}`,
                )}
                className="mt-1 w-full p-2"
                type={key === "mockExamQuestionPeriod" ? "text" : "date"}
                disabled={disabled}
                value={form[key]}
                onChange={(event) =>
                  setForm({ ...form, [key]: event.target.value })
                }
              />
              <span className="text-error">{fields[key]}</span>
            </label>
          ))}
          <label className="text-sm">
            인증상태
            <input
              data-testid="employment-improvement-status"
              disabled
              value={selected?.achievementStatus ?? "작성중"}
            />
          </label>
        </div>
        {item ? (
          <p className="mt-3 text-sm">
            입력 형식: {item.dataType} /{" "}
            {item.requiredYn === "Y" ? "실적값 필수" : "선택"}
          </p>
        ) : null}
        <div className="mt-4" role="status">
          첨부: {selected?.attachmentIds?.length ?? 0}건. 공통 파일 저장·소유권
          계약 연결 전에는 첨부 변경이 제한됩니다.
        </div>
        <div className="mt-5 flex justify-end gap-3">
          <button
            data-testid="employment-improvement-new"
            className="rounded-md border border-primary px-4 py-2 text-primary"
            disabled={!canCreate || saving || detailLoading}
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setFields({});
              setSuccess("");
              setError("");
            }}
            type="button"
          >
            신규
          </button>
          <button
            data-testid="employment-improvement-save"
            className="rounded-md bg-primary px-4 py-2 text-white disabled:opacity-60"
            disabled={disabled || denied}
            onClick={() => void save()}
            type="button"
          >
            {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}
