import { useEffect, useState } from "react";
import {
  ApiClientError,
  degreeCompletionAchievementApi,
  type DegreeCompletionAchievement,
  type DegreeCompletionStudent,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const newStudent = (): DegreeCompletionStudent => ({
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});

/** Manages degree-completion headers and their required guided-student detail table for permitted faculty roles. */
export function MastersDoctoralGraduationAchievementManagementPage() {
  const [items, setItems] = useState<DegreeCompletionAchievement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [managementNo, setManagementNo] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [selectedId, setSelectedId] = useState<number | undefined>();
  const [selectedConfirmed, setSelectedConfirmed] = useState(false);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurredDate, setOccurredDate] = useState("");
  const [detail, setDetail] = useState("{}");
  const [students, setStudents] = useState<DegreeCompletionStudent[]>([
    newStudent(),
  ]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await degreeCompletionAchievementApi.list({
        managementNo,
        pageSize,
      });
      setItems(response.data?.items ?? []);
    } catch (requestError) {
      if (requestError instanceof ApiClientError && requestError.status === 403)
        setDenied(true);
      else
        setError(
          "목록을 불러오지 못했습니다. 검색조건을 확인한 뒤 다시 시도하세요.",
        );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const selectItem = (item: DegreeCompletionAchievement) => {
    setSelectedId(item.achievementId);
    setSelectedConfirmed(item.certificationStatus === "EVALUATION_CONFIRMED");
    setManagementItemCode(item.managementItemCode);
    setOccurredDate(item.occurredDate);
    setDetail(JSON.stringify(item.achievementDetail, null, 2));
    setStudents(item.students.length > 0 ? item.students : [newStudent()]);
    setSuccess(null);
  };

  const updateStudent = (
    index: number,
    patch: Partial<DegreeCompletionStudent>,
  ) => {
    setStudents((current) =>
      current.map((student, currentIndex) =>
        currentIndex === index ? { ...student, ...patch } : student,
      ),
    );
  };

  const save = async () => {
    if (selectedConfirmed) {
      setError("평가확정된 실적은 수정하거나 첨부를 변경할 수 없습니다.");
      return;
    }
    if (
      !managementItemCode.trim() ||
      students.some(
        (student) =>
          !student.studentName.trim() ||
          !student.thesisTitle.trim() ||
          !student.degreeAwardedDate,
      )
    ) {
      setError(
        "관리항목과 지도학생의 학위구분·성명·논문 제목·학위수여일을 모두 입력하세요.",
      );
      return;
    }
    let achievementDetail: Record<string, unknown>;
    try {
      achievementDetail = JSON.parse(detail) as Record<string, unknown>;
    } catch {
      setError("상세 입력은 올바른 JSON 형식으로 입력하세요.");
      return;
    }
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      await degreeCompletionAchievementApi.save({
        achievementId: selectedId,
        managementItemCode: managementItemCode.trim(),
        occurredDate: occurredDate || undefined,
        achievementDetail,
        students,
      });
      setSuccess("석·박사 배출 실적을 저장했습니다.");
      setSelectedId(undefined);
      await load();
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "저장에 실패했습니다. 입력값을 확인하세요.",
      );
    }
  };

  if (denied) return <PermissionState />;

  return (
    <main
      className="page-content space-y-6"
      data-testid="masters-doctoral-graduation-achievement-page"
    >
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">
          석·박사 배출 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          업적 입력 관리 / 교육영역 / 석·박사 배출
        </p>
      </section>
      {success && <SuccessState message={success} />}
      {error && <ErrorState message={error} />}

      <section
        className="card space-y-4"
        data-testid="degree-completion-search-panel"
      >
        <h2>검색조건</h2>
        <div className="flex flex-wrap items-end gap-3">
          <label>
            관리번호
            <input
              data-testid="degree-completion-management-no-search"
              value={managementNo}
              onChange={(event) => setManagementNo(event.target.value)}
            />
          </label>
          <label>
            표시 건수
            <select
              data-testid="degree-completion-page-size"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
          <button
            data-testid="degree-completion-search-button"
            onClick={() => void load()}
          >
            조회
          </button>
        </div>
      </section>

      <section
        className="card space-y-4"
        data-testid="degree-completion-detail-panel"
      >
        <h2>
          {selectedId ? "석·박사 배출 실적 수정" : "석·박사 배출 실적 입력"}
        </h2>
        {selectedConfirmed && (
          <p
            data-testid="degree-completion-confirmed-lock-message"
            role="alert"
          >
            평가확정된 실적은 수정·삭제·첨부 변경이 불가합니다.
          </p>
        )}
        <label>
          관리항목 <span aria-label="필수">*</span>
          <input
            data-testid="degree-completion-management-item"
            value={managementItemCode}
            onChange={(event) => setManagementItemCode(event.target.value)}
          />
        </label>
        <label>
          업적발생일
          <input
            data-testid="degree-completion-occurred-date"
            type="date"
            value={occurredDate}
            onChange={(event) => setOccurredDate(event.target.value)}
          />
        </label>
        <label>
          상세 입력(JSON)
          <textarea
            data-testid="degree-completion-detail"
            value={detail}
            onChange={(event) => setDetail(event.target.value)}
          />
        </label>
        <div data-testid="degree-completion-student-sub-table">
          <h3>지도학생 세부내역</h3>
          <table>
            <thead>
              <tr>
                <th>학위구분</th>
                <th>학생명</th>
                <th>논문 제목</th>
                <th>학위수여일</th>
                <th>삭제</th>
              </tr>
            </thead>
            <tbody>
              {students.map((student, index) => (
                <tr
                  data-testid={`degree-completion-student-row-${index}`}
                  key={`${student.degreeCompletionStudentId ?? "new"}-${index}`}
                >
                  <td>
                    <select
                      data-testid={`degree-completion-degree-type-${index}`}
                      value={student.degreeType}
                      onChange={(event) =>
                        updateStudent(index, {
                          degreeType: event.target
                            .value as DegreeCompletionStudent["degreeType"],
                        })
                      }
                    >
                      <option value="MASTER">석사</option>
                      <option value="DOCTOR">박사</option>
                    </select>
                  </td>
                  <td>
                    <input
                      data-testid={`degree-completion-student-name-${index}`}
                      value={student.studentName}
                      onChange={(event) =>
                        updateStudent(index, {
                          studentName: event.target.value,
                        })
                      }
                    />
                  </td>
                  <td>
                    <input
                      data-testid={`degree-completion-thesis-title-${index}`}
                      value={student.thesisTitle}
                      onChange={(event) =>
                        updateStudent(index, {
                          thesisTitle: event.target.value,
                        })
                      }
                    />
                  </td>
                  <td>
                    <input
                      data-testid={`degree-completion-awarded-date-${index}`}
                      type="date"
                      value={student.degreeAwardedDate}
                      onChange={(event) =>
                        updateStudent(index, {
                          degreeAwardedDate: event.target.value,
                        })
                      }
                    />
                  </td>
                  <td>
                    <button
                      data-testid={`degree-completion-student-remove-${index}`}
                      disabled={students.length === 1}
                      onClick={() =>
                        setStudents((current) =>
                          current.filter(
                            (_, currentIndex) => currentIndex !== index,
                          ),
                        )
                      }
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button
            data-testid="degree-completion-student-add-button"
            onClick={() => setStudents((current) => [...current, newStudent()])}
          >
            지도학생 추가
          </button>
        </div>
        <button
          data-testid="degree-completion-save-button"
          disabled={selectedConfirmed}
          onClick={() => void save()}
        >
          저장
        </button>
      </section>

      <section className="card" data-testid="degree-completion-list-panel">
        <h2>목록</h2>
        {loading ? (
          <LoadingState />
        ) : items.length === 0 ? (
          <EmptyState />
        ) : (
          <table>
            <thead>
              <tr>
                <th>관리번호</th>
                <th>관리항목</th>
                <th>업적발생일</th>
                <th>인증상태</th>
                <th>지도학생 수</th>
                <th>선택</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr
                  data-testid={`degree-completion-row-${item.achievementId}`}
                  key={item.achievementId}
                >
                  <td>{item.managementNo}</td>
                  <td>{item.managementItemCode}</td>
                  <td>{item.occurredDate}</td>
                  <td>{item.certificationStatus}</td>
                  <td>{item.students.length}</td>
                  <td>
                    <button
                      data-testid={`degree-completion-select-${item.achievementId}`}
                      onClick={() => selectItem(item)}
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}
