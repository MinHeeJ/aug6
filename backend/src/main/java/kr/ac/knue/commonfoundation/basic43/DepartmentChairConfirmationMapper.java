package kr.ac.knue.commonfoundation.basic43;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepartmentChairConfirmationMapper {
    List<DepartmentChairConfirmationRow> listDepartmentChairConfirmTargets(@Param("criteria") DepartmentChairConfirmationSearchCriteria criteria);
    long countDepartmentChairConfirmTargets(@Param("criteria") DepartmentChairConfirmationSearchCriteria criteria);
    DepartmentChairConfirmationRow findLatestByAchievementId(@Param("achievementId") Long achievementId);
    int activeDepartmentChairConfirmPeriodExists(@Param("evaluationYear") String evaluationYear,
                                                 @Param("departmentOrganizationCode") String departmentOrganizationCode,
                                                 @Param("areaCode") String areaCode);
    int transitionAllowed(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
    int rejectionReasonExists(@Param("reasonCode") String reasonCode);
    DepartmentChairConfirmationRow insertTransition(@Param("achievementId") Long achievementId,
                                                    @Param("evaluationYear") String evaluationYear,
                                                    @Param("departmentOrganizationCode") String departmentOrganizationCode,
                                                    @Param("areaCode") String areaCode,
                                                    @Param("confirmStatus") String confirmStatus,
                                                    @Param("previousStatus") String previousStatus,
                                                    @Param("nextStatus") String nextStatus,
                                                    @Param("opinion") String opinion,
                                                    @Param("reasonCode") String reasonCode,
                                                    @Param("processedBy") Long processedBy,
                                                    @Param("changeReason") String changeReason);
}
