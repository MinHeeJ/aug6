import { useEffect, useState } from "react";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";
import {
  lectureImprovementsApi,
  type LectureImprovement,
  type LectureImprovementInput,
  type LectureImprovementList,
} from "./lectureImprovementsApi";

const emptyForm: LectureImprovementInput = {
  managementItemCode: "",
  achievementDate: "",
  performanceContent: "",
  academicYear: "",
  semester: "",
};
const control = "mt-1 w-full rounded-md border border-ld bg-white p-2 text-sm";
const button =
  "rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50";
const editable = ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"];

export function LectureImprovementPage({ user }: { user: CurrentUser | null }) {
  const [data, setData] = useState<LectureImprovementList | null>(null);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<LectureImprovementInput>(emptyForm);
  const [managementNo, setManagementNo] = useState("");
  const [teacherName, setTeacherName] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [permission, setPermission] = useState(false);
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const admin = user?.roles.includes("R09");
  const canRead = user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const canWrite = Boolean(admin || user?.roles.includes("R01"));
  const locked =
    selected !== null &&
    (!editable.includes(selected.achievementStatus) ||
      (!admin && selected.teacherUserId !== user?.userId));
  const disabled = !canWrite || locked || loading || saving || permission;

  function failure(caught: unknown) {
    const denied = caught instanceof ApiClientError && caught.status === 403;
    setPermission(denied);
    setError(
      caught instanceof Error ? caught.message : "요청을 처리하지 못했습니다.",
    );
    if (caught instanceof ApiClientError) {
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    }
  }

  async function load() {
    setLoading(true);
    setError("");
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (managementNo.trim()) query.set("managementNo", managementNo.trim());
      if (teacherName.trim()) query.set("teacherName", teacherName.trim());
      const response = await lectureImprovementsApi.list(query);
      setData(response.data ?? null);
      setPermission(false);
    } catch (caught) {
      setData(null);
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (canRead) void load();
  }, [page, pageSize, canRead]);

  async function detail(id: number) {
    setLoading(true);
    setSelected(null);
    setForm(emptyForm);
    setError("");
    setSuccess("");
    setFields({});
    try {
      const response = await lectureImprovementsApi.detail(id);
      if (response.data) {
        const row = response.data;
        setSelected(row);
        setForm({
          managementItemCode: row.managementItemCode,
          achievementDate: row.achievementDate,
          performanceContent: row.performanceContent,
          academicYear: row.academicYear,
          semester: row.semester,
          attachmentRef: row.attachmentRef ?? "",
        });
      }
    } catch (caught) {
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  async function save(submit = false) {
    if (disabled) return;
    const missing: Record<string, string> = {};
    for (const key of [
      "managementItemCode",
      "achievementDate",
      "performanceContent",
      "academicYear",
      "semester",
    ] as const) {
      if (!form[key]?.trim()) missing[key] = "필수 항목을 입력하세요.";
    }
    if (!/^\d{4}$/.test(form.academicYear))
      missing.academicYear = "학년도는 YYYY 형식으로 입력하세요.";
    setFields(missing);
    if (Object.keys(missing).length > 0) return;
    if (
      !window.confirm(
        submit
          ? "강의개선 실적을 제출하시겠습니까?"
          : "강의개선 실적을 저장하시겠습니까?",
      )
    )
      return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const payload = {
        ...form,
        attachmentRef: form.attachmentRef?.trim() || undefined,
      };
      if (selected) delete payload.evaluationYear;
      if (submit && selected) payload.achievementStatus = "SUBMITTED";
      const response = await lectureImprovementsApi.save(
        payload,
        selected?.achievementId,
      );
      const saved = response.data?.achievement;
      await load();
      if (saved) await detail(saved.achievementId);
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "평가기간 밖 발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
    } catch (caught) {
      failure(caught);
    } finally {
      setSaving(false);
    }
  }

  if (!canRead || permission)
    return <PermissionState message="강의개선 실적 접근 권한이 없습니다." />;

  return (
    <div
      data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
      data-testid="lecture-improvements-screen"
    >
      <section className="mb-6 rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">강의개선 실적 관리</h1>
        <p className="mt-2 text-sm text-muted">
          업적 입력 관리 &gt; 교육영역 &gt; 강의개선 실적 관리
        </p>
      </section>
      {loading && <LoadingState />}
      {error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      <section className="my-4 rounded-md bg-white p-6 shadow-md">
        <div className="grid gap-4 md:grid-cols-3">
          <label>
            관리번호
            <input
              className={control}
              value={managementNo}
              data-testid="lecture-filter-number"
              onChange={(event) => setManagementNo(event.target.value)}
            />
          </label>
          <label>
            성명
            <input
              className={control}
              value={teacherName}
              data-testid="lecture-filter-name"
              onChange={(event) => setTeacherName(event.target.value)}
            />
          </label>
          <button
            className={button}
            data-testid="lecture-search"
            disabled={loading}
            onClick={() => {
              if (page === 0) void load();
              else setPage(0);
            }}
          >
            조회
          </button>
        </div>
      </section>
      <section className="my-4 rounded-md bg-white p-6 shadow-md">
        <div className="mb-4 flex flex-wrap items-center gap-3">
          <h2 className="font-semibold">
            실적 목록 ({data?.totalElements ?? 0}건)
          </h2>
          <label>
            표시 건수
            <select
              className={control}
              value={pageSize}
              data-testid="lecture-page-size"
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
          {canWrite && (
            <button
              className={button}
              data-testid="lecture-new"
              disabled={loading || saving}
              onClick={() => {
                setSelected(null);
                setForm(emptyForm);
                setFields({});
                setSuccess("");
              }}
            >
              신규 등록
            </button>
          )}
          <button
            className={button}
            data-testid="lecture-download"
            disabled={!data?.achievements.length}
            onClick={() =>
              downloadCsv("강의개선_목록.csv", data?.achievements ?? [], [
                { header: "관리번호", value: (row) => row.managementNo },
                { header: "성명", value: (row) => row.teacherName },
                { header: "학년도", value: (row) => row.academicYear },
                { header: "학기", value: (row) => row.semester },
                { header: "실적내용", value: (row) => row.performanceContent },
              ])
            }
          >
            현재 목록 다운로드 (CSV)
          </button>
        </div>
        {!loading && data?.achievements.length === 0 && <EmptyState />}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr>
                {[
                  "관리번호",
                  "성명",
                  "관리항목",
                  "업적발생일",
                  "학년도",
                  "학기",
                  "상태",
                  "상세",
                ].map((name) => (
                  <th className="p-2" key={name}>
                    {name}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data?.achievements.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`lecture-row-${row.achievementId}`}
                  className="border-t border-ld"
                >
                  <td className="p-2">{row.managementNo}</td>
                  <td className="p-2">{row.teacherName}</td>
                  <td className="p-2">{row.managementItemCode}</td>
                  <td className="p-2">{row.achievementDate}</td>
                  <td className="p-2">{row.academicYear}</td>
                  <td className="p-2">{row.semester}</td>
                  <td className="p-2">{row.achievementStatus}</td>
                  <td className="p-2">
                    <button
                      className={button}
                      data-testid={`lecture-detail-${row.achievementId}`}
                      disabled={loading}
                      onClick={() => void detail(row.achievementId)}
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <button
            data-testid="lecture-previous"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            data-testid="lecture-next"
            disabled={
              loading || (page + 1) * pageSize >= (data?.totalElements ?? 0)
            }
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="lecture-detail-panel"
      >
        <h2 className="mb-4 font-semibold">
          {selected ? "실적 상세" : "신규 실적"}
        </h2>
        {locked && (
          <p className="mb-4 text-warning">
            확정·제출 상태 또는 타인의 실적은 수정할 수 없습니다.
          </p>
        )}
        {!canWrite && <p className="mb-4 text-muted">조회 전용 화면입니다.</p>}
        <div className="grid gap-4 md:grid-cols-2">
          <label>
            평가연도 (등록 시 선택)
            <input
              className={control}
              data-testid="lecture-evaluation-year"
              disabled={disabled || Boolean(selected)}
              value={selected?.evaluationYear ?? form.evaluationYear ?? ""}
              placeholder="미입력 시 발생일 연도"
              onChange={(event) =>
                setForm({
                  ...form,
                  evaluationYear: event.target.value || undefined,
                })
              }
            />
          </label>
          <label>
            관리항목 *
            <select
              className={control}
              data-testid="lecture-management-item"
              value={form.managementItemCode}
              disabled={disabled}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">선택하세요</option>
              {(data?.managementItems ?? [])
                .filter(
                  (option, index, options) =>
                    options.findIndex((entry) => entry.code === option.code) ===
                    index,
                )
                .map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.name}
                  </option>
                ))}
            </select>
            {fields.managementItemCode && (
              <span className="text-error">{fields.managementItemCode}</span>
            )}
          </label>
          <label>
            업적발생일 *
            <input
              className={control}
              type="date"
              data-testid="lecture-date"
              value={form.achievementDate}
              disabled={disabled}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
            {fields.achievementDate && (
              <span className="text-error">{fields.achievementDate}</span>
            )}
          </label>
          <label>
            학년도 *
            <input
              className={control}
              data-testid="lecture-academic-year"
              value={form.academicYear}
              disabled={disabled}
              onChange={(event) =>
                setForm({
                  ...form,
                  academicYear: event.target.value,
                  semester: "",
                })
              }
            />
            {fields.academicYear && (
              <span className="text-error">{fields.academicYear}</span>
            )}
          </label>
          <label>
            학기 *
            <select
              className={control}
              data-testid="lecture-semester"
              value={form.semester}
              disabled={disabled}
              onChange={(event) =>
                setForm({ ...form, semester: event.target.value })
              }
            >
              <option value="">선택하세요</option>
              {(data?.semesters ?? [])
                .filter(
                  (option) =>
                    !option.academicYear ||
                    option.academicYear === form.academicYear,
                )
                .map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.name}
                  </option>
                ))}
            </select>
            {fields.semester && (
              <span className="text-error">{fields.semester}</span>
            )}
          </label>
          <label>
            첨부 참조
            <input
              className={control}
              data-testid="lecture-attachment"
              value={form.attachmentRef ?? ""}
              disabled={disabled}
              onChange={(event) =>
                setForm({ ...form, attachmentRef: event.target.value })
              }
            />
            {fields.attachmentRef && (
              <span className="text-error">{fields.attachmentRef}</span>
            )}
          </label>
          <label className="md:col-span-2">
            실적내용 *
            <textarea
              className={control}
              data-testid="lecture-content"
              rows={5}
              value={form.performanceContent}
              disabled={disabled}
              onChange={(event) =>
                setForm({ ...form, performanceContent: event.target.value })
              }
            />
            {fields.performanceContent && (
              <span className="text-error">{fields.performanceContent}</span>
            )}
          </label>
        </div>
        {canWrite && (
          <div className="mt-4 flex gap-3">
            <button
              className={button}
              data-testid="lecture-save"
              disabled={disabled}
              onClick={() => void save()}
            >
              저장
            </button>
            {selected && (
              <button
                className={button}
                data-testid="lecture-submit"
                disabled={disabled}
                onClick={() => void save(true)}
              >
                제출 / 재제출
              </button>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
