import { EducationAchievementPage } from "./EducationAchievementPage";

/** Renders the faculty master and doctoral completion achievement management screen. */
export function DegreeCompletionAchievementPage() {
  return (
    <EducationAchievementPage
      achievementType="DEGREE_COMPLETION"
      screenId="SCR-DEGREE-COMPLETION-ACHIEVEMENT"
      title="석·박사 배출 실적 관리"
      testPrefix="degree-completion"
      degreeCompletion
    />
  );
}
