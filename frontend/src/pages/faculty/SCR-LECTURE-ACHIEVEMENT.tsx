import { EducationAchievementPage } from "./EducationAchievementPage";

/** Renders the faculty lecture-achievement management screen. */
export function LectureAchievementPage() {
  return (
    <EducationAchievementPage
      achievementType="LECTURE_ACHIEVEMENT"
      screenId="SCR-LECTURE-ACHIEVEMENT"
      title="강의 실적 관리"
      testPrefix="lecture-achievement"
    />
  );
}
