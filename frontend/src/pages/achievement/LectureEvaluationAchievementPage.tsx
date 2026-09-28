import { EducationAchievementListPage } from "./EducationAchievementListPage";

/**
 * FR-025 route composition for the shared education-achievement workspace.
 */
export function LectureEvaluationAchievementPage() {
  return (
    <EducationAchievementListPage
      screenId="SCR-LECTURE-EVALUATION-ACHIEVEMENT"
      testId="SCR-LECTURE-EVALUATION-ACHIEVEMENT"
      title="강의평가 실적 관리"
      collectionPath="/api/business/lecture-evaluation-achievements"
    />
  );
}
