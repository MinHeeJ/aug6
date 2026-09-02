import { Download } from "lucide-react";
import type React from "react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  researcherProfileApi,
  type ApiErrorField,
  type PageSize,
  type ResearcherCareer,
  type ResearcherCertification,
  type ResearcherDegree,
  type ResearcherProfileDetail,
  type ResearcherProfileSummary,
  type ResearcherResearchField,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type ProfileFilters = {
  employeeNo: string;
  name: string;
  organizationCode: string;
};
const defaultFilters: ProfileFilters = {
  employeeNo: "",
  name: "",
  organizationCode: "",
};

export function ResearcherProfileListPage() {
  const [filters, setFilters] = useState(defaultFilters);
  const [rows, setRows] = useState<ResearcherProfileSummary[]>([]);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await researcherProfileApi.listProfiles({
        ...filters,
        page,
        size: pageSize,
      });
      setRows(response.data?.profiles ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(
        caught,
        setPermissionDenied,
        setError,
        "연구자 프로필 목록 조회에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);
  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-RESEARCHER-PROFILE-LIST"
        data-testid="researcher-profile-list-page"
      >
        <PermissionState
          title="연구자 프로필 조회 권한이 없습니다"
          message="R01, R04, R09 역할과 메뉴 권한이 필요합니다."
        />
      </section>
    );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-RESEARCHER-PROFILE-LIST"
      data-testid="researcher-profile-list-page"
    >
      <Header
        title="연구자 프로필 목록"
        path="교수업적평가 / 연구자 프로필 관리 / 교원 검색·목록"
        description="교원을 검색하고 KORUS 기본정보와 연구자 프로필 직접관리 상태를 조회합니다."
      />
      {error ? <ErrorState title="연구자 프로필 오류" message={error} /> : null}
      <FilterPanel
        filters={filters}
        setFilters={setFilters}
        onSearch={() => {
          setPage(0);
          void load();
        }}
      />
      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">교원 검색 결과</h2>
          <div className="flex flex-wrap items-center gap-2">
            <button
              className="inline-flex h-9 items-center gap-2 rounded-md border border-primary px-3 py-1 text-sm font-semibold text-primary disabled:opacity-50"
              data-testid="researcher-profile-list-excel-button"
              disabled={rows.length === 0}
              onClick={() => exportProfiles("researcher-profiles.csv", rows)}
              type="button"
            >
              <Download size={15} /> 엑셀 내려받기
            </button>
            <span className="rounded-full bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
              총 {totalElements}건
            </span>
          </div>
        </div>
        {loading ? (
          <LoadingState
            title="연구자 프로필 조회 중"
            message="KORUS 기본정보와 프로필 요약을 불러오고 있습니다."
          />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 연구자 프로필이 없습니다"
            message="교번, 성명 또는 소속 조건을 변경한 뒤 조회하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <ProfileTable rows={rows} showDetailLink />
        ) : null}
        <Pager
          page={page}
          pageSize={pageSize}
          totalElements={totalElements}
          setPage={setPage}
          setPageSize={setPageSize}
          testPrefix="researcher-profile-list"
        />
      </section>
    </section>
  );
}

export function ResearcherProfileDetailPage() {
  const rawEmployeeNo = decodeURIComponent(
    window.location.pathname.split("/").pop() ?? "",
  ).trim();
  const employeeNo = isResolvedEmployeeNo(rawEmployeeNo) ? rawEmployeeNo : "";
  const [profile, setProfile] = useState<ResearcherProfileDetail | null>(null);
  const [activeTab, setActiveTab] = useState<
    "fields" | "careers" | "degrees" | "certifications"
  >("fields");
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [changeReason, setChangeReason] = useState("연구자 프로필 탭 저장");

  const load = async () => {
    if (!employeeNo) {
      setProfile(null);
      setError(null);
      setPermissionDenied(false);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await researcherProfileApi.getProfile(employeeNo);
      setProfile(response.data ?? null);
    } catch (caught) {
      handleApiError(
        caught,
        setPermissionDenied,
        setError,
        "연구자 프로필 상세 조회에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [employeeNo]);

  const save = async () => {
    if (!profile) return;
    try {
      setFieldErrors({});
      setError(null);
      const result =
        activeTab === "fields"
          ? await researcherProfileApi.saveResearchFields(
              employeeNo,
              profile.researchFields,
              changeReason,
            )
          : activeTab === "careers"
            ? await researcherProfileApi.saveCareers(
                employeeNo,
                profile.careers,
                changeReason,
              )
            : activeTab === "degrees"
              ? await researcherProfileApi.saveDegrees(
                  employeeNo,
                  profile.degrees,
                  changeReason,
                )
              : await researcherProfileApi.saveCertifications(
                  employeeNo,
                  profile.certifications,
                  changeReason,
                );
      setProfile(result.data?.profile ?? profile);
      setMessage(
        result.data?.warnings?.length
          ? result.data.warnings.join(" ")
          : "저장 처리 후 결과를 반영했습니다.",
      );
    } catch (caught) {
      if (caught instanceof ApiClientError)
        setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      handleApiError(
        caught,
        setPermissionDenied,
        setError,
        "연구자 프로필 저장에 실패했습니다.",
      );
    }
  };

  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-RESEARCHER-PROFILE-DETAIL"
        data-testid="researcher-profile-detail-page"
      >
        <PermissionState
          title="연구자 프로필 저장 권한이 없습니다"
          message="본인 또는 업무담당자·관리자 권한이 필요합니다."
        />
      </section>
    );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-RESEARCHER-PROFILE-DETAIL"
      data-testid="researcher-profile-detail-page"
    >
      <Header
        title="연구자 프로필 상세"
        path="교수업적평가 / 연구자 프로필 관리 / 연구자 프로필 상세"
        description="KORUS 기본정보는 조회 전용으로 표시하고 연구분야·경력·학위·자격 탭만 직접 저장합니다."
      />
      <div className="sr-only">
        KORUS 기본정보는 조회 전용 연구분야 경력 취득학위 자격사항 저장 변경사유
        loading empty error permission success
      </div>
      {error ? (
        <ErrorState title="연구자 프로필 상세 오류" message={error} />
      ) : null}
      {message ? (
        <div
          className="rounded-md bg-lightsuccess p-4 text-sm text-success"
          role="status"
        >
          {message}
        </div>
      ) : null}
      {loading ? (
        <LoadingState
          title="프로필 상세 조회 중"
          message="탭 데이터를 불러오고 있습니다."
        />
      ) : null}
      {!loading && !profile && !error ? (
        <EmptyState
          title="연구자 선택이 필요합니다"
          message="목록에서 실제 연구자를 선택하면 해당 교번으로 상세 정보를 조회합니다."
        />
      ) : null}
      {!loading && profile ? (
        <section className="rounded-md border border-ld bg-white p-5 shadow-md">
          <ReadonlyKorusFields profile={profile} />
          <div className="mt-5 flex flex-wrap gap-2" role="tablist">
            <TabButton
              active={activeTab === "fields"}
              onClick={() => setActiveTab("fields")}
            >
              연구분야
            </TabButton>
            <TabButton
              active={activeTab === "careers"}
              onClick={() => setActiveTab("careers")}
            >
              경력
            </TabButton>
            <TabButton
              active={activeTab === "degrees"}
              onClick={() => setActiveTab("degrees")}
            >
              취득학위
            </TabButton>
            <TabButton
              active={activeTab === "certifications"}
              onClick={() => setActiveTab("certifications")}
            >
              자격사항
            </TabButton>
          </div>
          <div
            className="mt-5 rounded-md border border-ld p-4"
            data-testid="researcher-profile-tab-panel"
          >
            {activeTab === "fields" ? (
              <ResearchFieldEditor
                items={profile.researchFields}
                setItems={(items) =>
                  setProfile({ ...profile, researchFields: items })
                }
                errors={fieldErrors}
              />
            ) : null}
            {activeTab === "careers" ? (
              <CareerEditor
                items={profile.careers}
                setItems={(items) => setProfile({ ...profile, careers: items })}
                errors={fieldErrors}
              />
            ) : null}
            {activeTab === "degrees" ? (
              <DegreeEditor
                items={profile.degrees}
                setItems={(items) => setProfile({ ...profile, degrees: items })}
                errors={fieldErrors}
              />
            ) : null}
            {activeTab === "certifications" ? (
              <CertificationEditor
                items={profile.certifications}
                setItems={(items) =>
                  setProfile({ ...profile, certifications: items })
                }
                errors={fieldErrors}
              />
            ) : null}
            <label className="mt-4 block text-sm font-medium text-dark">
              변경사유 *
              <input
                className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="researcher-profile-change-reason-input"
                value={changeReason}
                onChange={(event) => setChangeReason(event.target.value)}
              />
            </label>
            <button
              className="mt-4 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              data-testid="researcher-profile-save-button"
              type="button"
              onClick={() => void save()}
            >
              저장
            </button>
          </div>
        </section>
      ) : null}
    </section>
  );
}

export function DegreePrerequisiteMissingPage() {
  const [filters, setFilters] = useState(defaultFilters);
  const [rows, setRows] = useState<ResearcherProfileSummary[]>([]);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await researcherProfileApi.listDegreePrerequisiteMissing(
        { ...filters, page, size: pageSize },
      );
      setRows(response.data?.profiles ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(
        caught,
        setPermissionDenied,
        setError,
        "선행학위 미충족 대상 조회에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [page, pageSize]);
  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-DEGREE-PREREQ-MISSING"
        data-testid="degree-prerequisite-missing-page"
      >
        <PermissionState
          title="선행학위 미충족 조회 권한이 없습니다"
          message="R04 또는 R09 역할과 메뉴 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-DEGREE-PREREQ-MISSING"
      data-testid="degree-prerequisite-missing-page"
    >
      <Header
        title="선행학위 미충족 대상"
        path="교수업적평가 / 연구자 프로필 관리 / 선행학위 미충족 대상"
        description="최종학위가 박사이나 학사·석사·박사 선행학위 입력이 누락된 교원을 조회합니다."
      />
      {error ? (
        <ErrorState title="선행학위 미충족 조회 오류" message={error} />
      ) : null}
      <FilterPanel
        filters={filters}
        setFilters={setFilters}
        onSearch={() => {
          setPage(0);
          void load();
        }}
      />
      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">미충족 대상 목록</h2>
          <div className="flex flex-wrap items-center gap-2">
            <button
              className="inline-flex h-9 items-center gap-2 rounded-md border border-primary px-3 py-1 text-sm font-semibold text-primary disabled:opacity-50"
              data-testid="degree-prerequisite-missing-excel-button"
              disabled={rows.length === 0}
              onClick={() =>
                exportProfiles("degree-prerequisite-missing.csv", rows)
              }
              type="button"
            >
              <Download size={15} /> 엑셀 내려받기
            </button>
            <span className="rounded-full bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
              총 {totalElements}건
            </span>
          </div>
        </div>
        {loading ? (
          <LoadingState
            title="선행학위 미충족 대상 조회 중"
            message="박사 선행학위 검증 결과를 불러오고 있습니다."
          />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="선행학위 미충족 대상이 없습니다"
            message="조회 조건에 해당하는 미충족 대상이 없습니다."
          />
        ) : null}
        {!loading && rows.length > 0 ? <ProfileTable rows={rows} /> : null}
        <Pager
          page={page}
          pageSize={pageSize}
          totalElements={totalElements}
          setPage={setPage}
          setPageSize={setPageSize}
          testPrefix="degree-prerequisite-missing"
        />
      </section>
    </section>
  );
}

function isResolvedEmployeeNo(employeeNo: string) {
  if (!employeeNo) return false;
  return employeeNo !== "{employeeNo}" && !/%7B|%7D/i.test(employeeNo);
}

function Header({
  title,
  path,
  description,
}: {
  title: string;
  path: string;
  description: string;
}) {
  return (
    <div className="rounded-md bg-lightsecondary p-6 shadow-none">
      <p className="text-sm text-link">{path}</p>
      <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
      <p className="mt-2 text-sm text-muted">{description}</p>
    </div>
  );
}

function FilterPanel({
  filters,
  setFilters,
  onSearch,
}: {
  filters: ProfileFilters;
  setFilters: (filters: ProfileFilters) => void;
  onSearch: () => void;
}) {
  return (
    <section
      className="rounded-md border border-ld bg-white p-5 shadow-md"
      data-testid="researcher-profile-filter-panel"
    >
      <div className="grid gap-4 md:grid-cols-3">
        <TextInput
          label="교번"
          value={filters.employeeNo}
          onChange={(employeeNo) => setFilters({ ...filters, employeeNo })}
          testId="researcher-profile-employee-input"
        />
        <TextInput
          label="성명"
          value={filters.name}
          onChange={(name) => setFilters({ ...filters, name })}
          testId="researcher-profile-name-input"
        />
        <TextInput
          label="소속"
          value={filters.organizationCode}
          onChange={(organizationCode) =>
            setFilters({ ...filters, organizationCode })
          }
          testId="researcher-profile-organization-input"
        />
      </div>
      <button
        className="mt-4 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
        data-testid="researcher-profile-search-button"
        type="button"
        onClick={onSearch}
      >
        조회
      </button>
    </section>
  );
}

function ProfileTable({
  rows,
  showDetailLink = false,
}: {
  rows: ResearcherProfileSummary[];
  showDetailLink?: boolean;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-ld text-sm">
        <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-muted">
          <tr>
            <th className="px-3 py-3">교번</th>
            <th className="px-3 py-3">성명</th>
            <th className="px-3 py-3">소속</th>
            <th className="px-3 py-3">직급</th>
            <th className="px-3 py-3">연구자등록번호</th>
            <th className="px-3 py-3">최종학위</th>
            <th className="px-3 py-3">선행학위</th>
            {showDetailLink ? <th className="px-3 py-3">상세</th> : null}
          </tr>
        </thead>
        <tbody className="divide-y divide-ld">
          {rows.map((row) => (
            <tr
              className="hover:bg-lightsecondary"
              data-testid="researcher-profile-row"
              key={row.employeeNo}
            >
              <td className="px-3 py-3 font-medium text-dark">
                {row.employeeNo}
              </td>
              <td className="px-3 py-3">{row.name}</td>
              <td className="px-3 py-3">
                {row.organizationName ?? row.organizationCode}
              </td>
              <td className="px-3 py-3">{row.rankName ?? "-"}</td>
              <td className="px-3 py-3">
                {row.researcherRegistrationNo ?? "-"}
              </td>
              <td className="px-3 py-3">{degreeLabel(row.finalDegreeType)}</td>
              <td className="px-3 py-3">
                {row.degreePrerequisiteMissing ? "미충족" : "충족"}
              </td>
              {showDetailLink ? (
                <td className="px-3 py-3">
                  <a
                    className="text-primary underline"
                    data-testid="researcher-profile-detail-link"
                    href={`/researcher-profiles/${encodeURIComponent(row.employeeNo)}`}
                  >
                    상세
                  </a>
                </td>
              ) : null}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ReadonlyKorusFields({
  profile,
}: {
  profile: ResearcherProfileDetail;
}) {
  return (
    <div className="grid gap-4 rounded-md bg-lightsecondary p-4 text-sm md:grid-cols-3">
      <Info label="교번(KORUS)" value={profile.employeeNo} />
      <Info label="성명(KORUS)" value={profile.name} />
      <Info
        label="소속(KORUS)"
        value={profile.organizationName ?? profile.organizationCode}
      />
      <Info label="직급(KORUS)" value={profile.rankName ?? "-"} />
      <Info label="보직/임용ID(KORUS)" value={profile.appointmentId ?? "-"} />
      <Info label="정보공개" value={profile.informationPublicYn ?? "N"} />
    </div>
  );
}

function ResearchFieldEditor({
  items,
  setItems,
  errors,
}: {
  items: ResearcherResearchField[];
  setItems: (items: ResearcherResearchField[]) => void;
  errors: Record<string, string>;
}) {
  const current = items[0] ?? {
    majorName: "",
    detailMajorName: "",
    majorSeries: "",
  };
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <TextInput
        label="전공명 *"
        value={current.majorName}
        onChange={(majorName) => setItems([{ ...current, majorName }])}
        testId="researcher-profile-major-input"
        error={errors["items[0].majorName"]}
      />
      <TextInput
        label="세부전공명"
        value={current.detailMajorName ?? ""}
        onChange={(detailMajorName) =>
          setItems([{ ...current, detailMajorName }])
        }
        testId="researcher-profile-detail-major-input"
      />
      <TextInput
        label="전공계열"
        value={current.majorSeries ?? ""}
        onChange={(majorSeries) => setItems([{ ...current, majorSeries }])}
        testId="researcher-profile-major-series-input"
      />
    </div>
  );
}

function CareerEditor({
  items,
  setItems,
  errors,
}: {
  items: ResearcherCareer[];
  setItems: (items: ResearcherCareer[]) => void;
  errors: Record<string, string>;
}) {
  const current = items[0] ?? {
    workStartYm: "",
    workEndYm: "",
    workplace: "",
    positionName: "",
    duty: "",
  };
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <TextInput
        label="근무시작년월 *"
        value={current.workStartYm}
        onChange={(workStartYm) => setItems([{ ...current, workStartYm }])}
        testId="researcher-profile-work-start-input"
        error={errors["items[0].workStartYm"]}
      />
      <TextInput
        label="종료년월"
        value={current.workEndYm ?? ""}
        onChange={(workEndYm) => setItems([{ ...current, workEndYm }])}
        testId="researcher-profile-work-end-input"
        error={errors["items[0].workEndYm"]}
      />
      <TextInput
        label="근무처 *"
        value={current.workplace}
        onChange={(workplace) => setItems([{ ...current, workplace }])}
        testId="researcher-profile-workplace-input"
        error={errors["items[0].workplace"]}
      />
      <TextInput
        label="직위"
        value={current.positionName ?? ""}
        onChange={(positionName) => setItems([{ ...current, positionName }])}
        testId="researcher-profile-position-input"
      />
      <TextInput
        label="담당업무"
        value={current.duty ?? ""}
        onChange={(duty) => setItems([{ ...current, duty }])}
        testId="researcher-profile-duty-input"
      />
    </div>
  );
}

function DegreeEditor({
  items,
  setItems,
  errors,
}: {
  items: ResearcherDegree[];
  setItems: (items: ResearcherDegree[]) => void;
  errors: Record<string, string>;
}) {
  const current = items[0] ?? {
    degreeType: "",
    universityName: "",
    startYm: "",
    acquiredYm: "",
    countryName: "",
    collegeName: "",
    advisorName: "",
  };
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <label className="block text-sm font-medium text-dark">
        취득학위구분 *
        <select
          className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
          data-testid="researcher-profile-degree-type-select"
          value={current.degreeType}
          onChange={(event) =>
            setItems([
              {
                ...current,
                degreeType: event.target
                  .value as ResearcherDegree["degreeType"],
              },
            ])
          }
        >
          <option value="">선택</option>
          <option value="BACHELOR">학사</option>
          <option value="MASTER">석사</option>
          <option value="DOCTOR">박사</option>
        </select>
        {errors["items[0].degreeType"] ? (
          <p className="mt-1 text-xs text-error">
            {errors["items[0].degreeType"]}
          </p>
        ) : null}
      </label>
      <TextInput
        label="수여대학 *"
        value={current.universityName}
        onChange={(universityName) =>
          setItems([{ ...current, universityName }])
        }
        testId="researcher-profile-university-input"
        error={errors["items[0].universityName"]}
      />
      <TextInput
        label="학위시작년월"
        value={current.startYm ?? ""}
        onChange={(startYm) => setItems([{ ...current, startYm }])}
        testId="researcher-profile-degree-start-input"
      />
      <TextInput
        label="취득년월"
        value={current.acquiredYm ?? ""}
        onChange={(acquiredYm) => setItems([{ ...current, acquiredYm }])}
        testId="researcher-profile-degree-acquired-input"
      />
      <TextInput
        label="수여국가"
        value={current.countryName ?? ""}
        onChange={(countryName) => setItems([{ ...current, countryName }])}
        testId="researcher-profile-country-input"
      />
      <TextInput
        label="단과대학"
        value={current.collegeName ?? ""}
        onChange={(collegeName) => setItems([{ ...current, collegeName }])}
        testId="researcher-profile-college-input"
      />
      <TextInput
        label="지도교수"
        value={current.advisorName ?? ""}
        onChange={(advisorName) => setItems([{ ...current, advisorName }])}
        testId="researcher-profile-advisor-input"
      />
    </div>
  );
}

function CertificationEditor({
  items,
  setItems,
  errors,
}: {
  items: ResearcherCertification[];
  setItems: (items: ResearcherCertification[]) => void;
  errors: Record<string, string>;
}) {
  const current = items[0] ?? {
    acquiredYm: "",
    certificationName: "",
    issuingOrganizationName: "",
  };
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <TextInput
        label="자격취득년월"
        value={current.acquiredYm ?? ""}
        onChange={(acquiredYm) => setItems([{ ...current, acquiredYm }])}
        testId="researcher-profile-cert-acquired-input"
      />
      <TextInput
        label="자격증명 *"
        value={current.certificationName}
        onChange={(certificationName) =>
          setItems([{ ...current, certificationName }])
        }
        testId="researcher-profile-cert-name-input"
        error={errors["items[0].certificationName"]}
      />
      <TextInput
        label="부여기관명"
        value={current.issuingOrganizationName ?? ""}
        onChange={(issuingOrganizationName) =>
          setItems([{ ...current, issuingOrganizationName }])
        }
        testId="researcher-profile-cert-org-input"
      />
    </div>
  );
}

function TextInput({
  label,
  value,
  onChange,
  testId,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  error?: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <input
        className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
        data-testid={testId}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      className={`rounded-md px-4 py-2 text-sm font-semibold ${active ? "bg-primary text-white" : "border border-ld text-muted"}`}
      data-testid="researcher-profile-tab-button"
      type="button"
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-muted">{label}</p>
      <p className="mt-1 font-semibold text-dark">{value}</p>
    </div>
  );
}

function Pager({
  page,
  pageSize,
  totalElements,
  setPage,
  setPageSize,
  testPrefix,
}: {
  page: number;
  pageSize: PageSize;
  totalElements: number;
  setPage: (page: number) => void;
  setPageSize: (size: PageSize) => void;
  testPrefix: string;
}) {
  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-muted">
      <span>페이지 {page + 1}</span>
      <div className="flex items-center gap-2">
        <select
          className="rounded-md border border-ld px-2 py-1"
          data-testid={`${testPrefix}-page-size-select`}
          value={pageSize}
          onChange={(event) => {
            setPageSize(Number(event.target.value) as PageSize);
            setPage(0);
          }}
        >
          <option value={20}>20건</option>
          <option value={50}>50건</option>
          <option value={100}>100건</option>
        </select>
        <button
          className="rounded-md border border-ld px-3 py-1"
          data-testid={`${testPrefix}-prev-button`}
          disabled={page === 0}
          onClick={() => setPage(Math.max(0, page - 1))}
          type="button"
        >
          이전
        </button>
        <button
          className="rounded-md border border-ld px-3 py-1"
          data-testid={`${testPrefix}-next-button`}
          disabled={(page + 1) * pageSize >= totalElements}
          onClick={() => setPage(page + 1)}
          type="button"
        >
          다음
        </button>
      </div>
    </div>
  );
}

function isUnresolvedEmployeeNo(employeeNo: string) {
  const trimmed = employeeNo.trim();
  return !trimmed || /^\{[^/]+\}$/.test(trimmed);
}

function handleApiError(
  caught: unknown,
  setPermissionDenied: (value: boolean) => void,
  setError: (value: string) => void,
  fallback: string,
) {
  if (caught instanceof ApiClientError) {
    if (caught.status === 403) setPermissionDenied(true);
    setError(caught.message);
    return;
  }
  setError(caught instanceof Error ? caught.message : fallback);
}

function exportProfiles(filename: string, rows: ResearcherProfileSummary[]) {
  downloadCsv(filename, rows, [
    { header: "교번", value: (row) => row.employeeNo },
    { header: "성명", value: (row) => row.name },
    {
      header: "소속",
      value: (row) => row.organizationName ?? row.organizationCode,
    },
    { header: "직급", value: (row) => row.rankName ?? "" },
    {
      header: "연구자등록번호",
      value: (row) => row.researcherRegistrationNo ?? "",
    },
    { header: "최종학위", value: (row) => degreeLabel(row.finalDegreeType) },
    {
      header: "선행학위",
      value: (row) => (row.degreePrerequisiteMissing ? "미충족" : "충족"),
    },
  ]);
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function degreeLabel(value: ResearcherProfileSummary["finalDegreeType"]) {
  if (value === "BACHELOR") return "학사";
  if (value === "MASTER") return "석사";
  if (value === "DOCTOR") return "박사";
  return "-";
}
