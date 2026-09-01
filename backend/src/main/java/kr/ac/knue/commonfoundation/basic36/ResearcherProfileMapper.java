package kr.ac.knue.commonfoundation.basic36;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResearcherProfileMapper {
    List<ResearcherProfileSummary> listProfiles(@Param("criteria") ResearcherProfileSearchCriteria criteria);
    long countProfiles(@Param("criteria") ResearcherProfileSearchCriteria criteria);
    ResearcherProfileSummary findProfile(@Param("employeeNo") String employeeNo);
    void ensureProfile(@Param("employeeNo") String employeeNo, @Param("changedBy") Long changedBy);
    List<ResearcherResearchFieldRow> listResearchFields(@Param("employeeNo") String employeeNo);
    List<ResearcherCareerRow> listCareers(@Param("employeeNo") String employeeNo);
    List<ResearcherDegreeRow> listDegrees(@Param("employeeNo") String employeeNo);
    List<ResearcherCertificationRow> listCertifications(@Param("employeeNo") String employeeNo);
    void deleteResearchFields(@Param("employeeNo") String employeeNo);
    void deleteCareers(@Param("employeeNo") String employeeNo);
    void deleteDegrees(@Param("employeeNo") String employeeNo);
    void deleteCertifications(@Param("employeeNo") String employeeNo);
    void insertResearchField(@Param("employeeNo") String employeeNo, @Param("item") ResearcherProfileTabItem item, @Param("changedBy") Long changedBy);
    void insertCareer(@Param("employeeNo") String employeeNo, @Param("item") ResearcherProfileTabItem item, @Param("changedBy") Long changedBy);
    void insertDegree(@Param("employeeNo") String employeeNo, @Param("item") ResearcherProfileTabItem item, @Param("changedBy") Long changedBy);
    void insertCertification(@Param("employeeNo") String employeeNo, @Param("item") ResearcherProfileTabItem item, @Param("changedBy") Long changedBy);
    void updateProfileDegreeStatus(@Param("employeeNo") String employeeNo, @Param("finalDegreeType") String finalDegreeType, @Param("missing") boolean missing, @Param("changedBy") Long changedBy);
    void touchProfile(@Param("employeeNo") String employeeNo, @Param("changedBy") Long changedBy);
    List<ResearcherProfileSummary> listDegreePrerequisiteMissing(@Param("criteria") ResearcherProfileSearchCriteria criteria);
    long countDegreePrerequisiteMissing(@Param("criteria") ResearcherProfileSearchCriteria criteria);
    void insertChangeHistory(@Param("employeeNo") String employeeNo, @Param("tab") String tab, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);
    void insertAchievementHistory(@Param("employeeNo") String employeeNo, @Param("tab") String tab, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);
}
