import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type LectureEvaluationAchievementRow,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

export function LectureEvaluationAchievementManagementPage() {
  const [filters, setFilters] = useState({
    managementNo: "",
    teacherName: "",
    managementItemCode: "",
    occurredDateFrom: "",
    occurredDateTo: "",
    certificationStatus: "",
  });
  const [rows, setRows] = useState<LectureEvaluationAchievementRow[]>([]);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [selected, setSelected] =
    useState<LectureEvaluationAchievementRow | null>(null);
  const [form, setForm] = useState({
    managementItemCode: "",
    occurredDate: "",
    detail: "{}",
    attachmentRef: "",
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await lectureEvaluationAchievementApi.list({
        ...filters,
        page: 0,
        pageSize,
      });
      setRows(response.data?.rows ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const selectRow = (row: LectureEvaluationAchievementRow) => {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      occurredDate: row.occurredDate,
      detail: row.achievementDetailJson || "{}",
      attachmentRef: row.attachmentRef ?? "",
    });
    setSuccess(null);
  };

  const save = async () => {
    if (!form.managementItemCode.trim() || !form.occurredDate) {
      setError("관리항목과 발생일은 필수 입력 항목입니다.");
      return;
    }
    let achievementDetail: object;
    try {
      achievementDetail = JSON.parse(form.detail || "{}");
    } catch {
      setError("상세 입력값은 JSON 형식이어야 합니다.");
      return;
    }
    if (
      !window.confirm(`${selected ? "수정" : "등록"} 내용을 저장하시겠습니까?`)
    )
      return;
    try {
      setSaving(true);
      setError(null);
      const response = await lectureEvaluationAchievementApi.save({
        achievementId: selected?.achievementId,
        managementItemCode: form.managementItemCode.trim(),
        occurredDate: form.occurredDate,
        achievementDetail,
        attachmentRef: form.attachmentRef.trim() || undefined,
      });
      setSuccess(
        response.data?.warning
          ? "저장되었습니다. 발생일이 평가대상 기간 밖입니다."
          : "저장되었습니다.",
      );
      setSelected(null);
      setForm({
        managementItemCode: "",
        occurredDate: "",
        detail: "{}",
        attachmentRef: "",
      });
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의평가 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied)
    return (
      <section data-testid="lecture-evaluation-achievement-page">
        <PermissionState
          title="강의평가 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04와 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"
      data-testid="lecture-evaluation-achievement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">교육영역 실적 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의평가 실적 관리
            </h1>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
              onClick={() => {
                downloadCsv("lecture-evaluation-achievements.csv", rows, [
                  { header: "관리번호", value: (row) => row.managementNo },
                  {
                    header: "관리항목",
                    value: (row) => row.managementItemCode,
                  },
                  { header: "발생일", value: (row) => row.occurredDate },
                  {
                    header: "인증상태",
                    value: (row) => row.certificationStatus,
                  },
                ]);
                setSuccess("엑셀 다운로드 파일을 생성했습니다.");
              }}
              data-testid="lecture-evaluation-excel-button"
            >
              <Download size={16} />
              엑셀 다운로드
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
              onClick={() => void load()}
              data-testid="lecture-evaluation-refresh-button"
            >
              <RefreshCw size={16} />
              새로고침
            </button>
          </div>
        </div>
      </div>
      {success ? <SuccessState title={success} /> : null}
      {error ? <ErrorState title="처리 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-4 md:grid-cols-7">
          <Input
            label="관리번호"
            value={filters.managementNo}
            onChange={(value) =>
              setFilters({ ...filters, managementNo: value })
            }
            testId="lecture-evaluation-management-no-input"
          />
          <Input
            label="교원명"
            value={filters.teacherName}
            onChange={(value) => setFilters({ ...filters, teacherName: value })}
            testId="lecture-evaluation-teacher-name-input"
          />
          <Input
            label="관리항목"
            value={filters.managementItemCode}
            onChange={(value) =>
              setFilters({ ...filters, managementItemCode: value })
            }
            testId="lecture-evaluation-management-item-input"
          />
          <Input
            label="발생일 시작"
            type="date"
            value={filters.occurredDateFrom}
            onChange={(value) =>
              setFilters({ ...filters, occurredDateFrom: value })
            }
            testId="lecture-evaluation-occurred-date-from-input"
          />
          <Input
            label="발생일 종료"
            type="date"
            value={filters.occurredDateTo}
            onChange={(value) =>
              setFilters({ ...filters, occurredDateTo: value })
            }
            testId="lecture-evaluation-occurred-date-to-input"
          />
          <Input
            label="인증상태"
            value={filters.certificationStatus}
            onChange={(value) =>
              setFilters({ ...filters, certificationStatus: value })
            }
            testId="lecture-evaluation-status-input"
          />
          <button
            type="button"
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="lecture-evaluation-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>
      <section className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-8">
          <div className="mb-4 flex justify-between">
            <h2 className="text-lg font-semibold text-dark">실적 목록</h2>
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                value={pageSize}
                onChange={(event) =>
                  setPageSize(Number(event.target.value) as PageSize)
                }
                data-testid="lecture-evaluation-size-select"
              >
                <option value={20}>20건</option>
                <option value={50}>50건</option>
                <option value={100}>100건</option>
              </select>
            </label>
          </div>
          {loading ? (
            <LoadingState
              title="조회 중"
              message="강의평가 실적을 불러오고 있습니다."
            />
          ) : rows.length === 0 ? (
            <EmptyState
              title="조회된 강의평가 실적이 없습니다"
              message="검색조건을 변경한 뒤 다시 조회하세요."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">관리번호</th>
                    <th className="px-3 py-2">교원</th>
                    <th className="px-3 py-2">관리항목</th>
                    <th className="px-3 py-2">발생일</th>
                    <th className="px-3 py-2">인증상태</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.achievementId}
                      className="cursor-pointer border-b border-ld hover:bg-lightsecondary"
                      onClick={() => selectRow(row)}
                      data-testid={`lecture-evaluation-row-${row.achievementId}`}
                    >
                      <td className="px-3 py-2">{row.managementNo}</td>
                      <td className="px-3 py-2">{row.teacherName ?? "-"}</td>
                      <td className="px-3 py-2">{row.managementItemCode}</td>
                      <td className="px-3 py-2">{row.occurredDate}</td>
                      <td className="px-3 py-2">{row.certificationStatus}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <aside
          className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4"
          data-testid="lecture-evaluation-detail-panel"
        >
          <h2 className="text-lg font-semibold text-dark">
            {selected ? "실적 상세·수정" : "실적 등록"}
          </h2>
          <div className="mt-4 space-y-4">
            <Input
              label="관리항목 *"
              value={form.managementItemCode}
              onChange={(value) =>
                setForm({ ...form, managementItemCode: value })
              }
              testId="lecture-evaluation-save-management-item-input"
            />
            <Input
              label="발생일 *"
              type="date"
              value={form.occurredDate}
              onChange={(value) => setForm({ ...form, occurredDate: value })}
              testId="lecture-evaluation-occurred-date-input"
            />
            <Input
              label="첨부 참조"
              value={form.attachmentRef}
              onChange={(value) => setForm({ ...form, attachmentRef: value })}
              testId="lecture-evaluation-attachment-ref-input"
            />
            <label className="block text-sm font-semibold text-ld">
              상세 입력(JSON)
              <textarea
                className="mt-2 w-full rounded-md border border-ld p-2 text-sm"
                value={form.detail}
                onChange={(event) =>
                  setForm({ ...form, detail: event.target.value })
                }
                data-testid="lecture-evaluation-detail-input"
              />
            </label>
            <button
              type="button"
              className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void save()}
              disabled={saving}
              data-testid="lecture-evaluation-save-button"
            >
              <Save size={16} />
              {saving ? "저장 중" : "저장"}
            </button>
          </div>
        </aside>
      </section>
    </section>
  );
}

function Input({
  label,
  value,
  onChange,
  testId,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  type?: string;
}) {
  return (
    <label className="block text-sm font-semibold text-ld">
      {label}
      <input
        type={type}
        className="mt-2 h-10 w-full rounded-md border border-ld px-3 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
    </label>
  );
}
