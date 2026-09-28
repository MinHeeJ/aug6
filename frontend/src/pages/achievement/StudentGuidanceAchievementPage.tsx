import { EducationAchievementListPage } from "./EducationAchievementListPage";

/**
 * FR-027 route composition for the shared education-achievement workspace.
 */
export function StudentGuidanceAchievementPage() {
  return (
    <EducationAchievementListPage
      screenId="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"
      testId="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"
      title="학생지도 실적 관리"
      collectionPath="/api/business/student-guidance-achievements"
    />
  );
}
