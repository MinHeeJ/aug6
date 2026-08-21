package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationsManagementMapper {
    java.util.List<PositionAssignmentRow> searchPositionAssignments(@Param("criteria") AssignmentSearchCriteria criteria);
    int countPositionAssignments(@Param("criteria") AssignmentSearchCriteria criteria);
    PositionAssignmentRow findPositionAssignmentById(@Param("positionAssignmentId") Long positionAssignmentId);
    int existsUser(@Param("userId") Long userId);
    int existsOrganization(@Param("organizationCode") String organizationCode);
    int insertPositionAssignment(@Param("positionCode") String positionCode, @Param("userId") Long userId, @Param("organizationCode") String organizationCode, @Param("effectiveStartDate") LocalDate effectiveStartDate, @Param("effectiveEndDate") LocalDate effectiveEndDate, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    int updatePositionAssignment(@Param("positionAssignmentId") Long positionAssignmentId, @Param("positionCode") String positionCode, @Param("userId") Long userId, @Param("organizationCode") String organizationCode, @Param("effectiveStartDate") LocalDate effectiveStartDate, @Param("effectiveEndDate") LocalDate effectiveEndDate, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);

    java.util.List<DutyAssignmentRow> searchDutyAssignments(@Param("criteria") AssignmentSearchCriteria criteria);
    int countDutyAssignments(@Param("criteria") AssignmentSearchCriteria criteria);
    DutyAssignmentRow findDutyAssignmentById(@Param("dutyAssignmentId") Long dutyAssignmentId);
    int insertDutyAssignment(@Param("dutyOrganization") String dutyOrganization, @Param("userId") Long userId, @Param("dutyArea") String dutyArea, @Param("validStartDate") LocalDate validStartDate, @Param("validEndDate") LocalDate validEndDate, @Param("dataScopeType") String dataScopeType, @Param("processingPermission") String processingPermission, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    int updateDutyAssignment(@Param("dutyAssignmentId") Long dutyAssignmentId, @Param("dutyOrganization") String dutyOrganization, @Param("userId") Long userId, @Param("dutyArea") String dutyArea, @Param("validStartDate") LocalDate validStartDate, @Param("validEndDate") LocalDate validEndDate, @Param("dataScopeType") String dataScopeType, @Param("processingPermission") String processingPermission, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    java.util.List<DutyAssignmentRow> findEffectiveDutyAssignments(@Param("userId") Long userId, @Param("dutyArea") String dutyArea, @Param("referenceDate") LocalDate referenceDate);

    java.util.List<DataScopeRuleRow> searchDataScopeRules(@Param("criteria") AssignmentSearchCriteria criteria);
    int countDataScopeRules(@Param("criteria") AssignmentSearchCriteria criteria);
    DataScopeRuleRow findDataScopeRule(@Param("roleCode") String roleCode, @Param("dataScopeType") String dataScopeType, @Param("organizationCode") String organizationCode, @Param("dutyArea") String dutyArea);
    int existsRole(@Param("roleCode") String roleCode);
    int insertDataScopeRule(@Param("roleCode") String roleCode, @Param("dataScopeType") String dataScopeType, @Param("organizationCode") String organizationCode, @Param("dutyArea") String dutyArea, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    int updateDataScopeRule(@Param("dataScopeRuleId") Long dataScopeRuleId, @Param("dataScopeType") String dataScopeType, @Param("organizationCode") String organizationCode, @Param("dutyArea") String dutyArea, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    java.util.List<DataScopeRuleRow> findRulesByRoles(@Param("roleCodes") java.util.List<String> roleCodes);
}
