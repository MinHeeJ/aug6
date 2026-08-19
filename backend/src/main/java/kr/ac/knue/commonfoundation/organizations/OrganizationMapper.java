package kr.ac.knue.commonfoundation.organizations;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationMapper {
    List<OrganizationRow> searchOrganizations(@Param("organizationCodeFilter") String organizationCodeFilter,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    List<OrganizationRow> findOrganizationsForTree();

    OrganizationRow findOrganization(@Param("organizationCode") String organizationCode);

    OrganizationRelationRow findCurrentRelation(@Param("organizationCode") String organizationCode);

    OrganizationRelationRow findOverlappingRelation(@Param("organizationCode") String organizationCode,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("excludeRelationId") Long excludeRelationId);

    void endRelation(@Param("relationId") Long relationId,
                     @Param("endDate") LocalDate endDate,
                     @Param("updatedBy") Long updatedBy,
                     @Param("changeReason") String changeReason);

    void insertRelation(@Param("organizationCode") String organizationCode,
                        @Param("parentOrganizationCode") String parentOrganizationCode,
                        @Param("effectiveStartDate") LocalDate effectiveStartDate,
                        @Param("effectiveEndDate") LocalDate effectiveEndDate,
                        @Param("createdBy") Long createdBy,
                        @Param("changeReason") String changeReason);

    OrganizationRelationRow findLatestRelation(@Param("organizationCode") String organizationCode);

    void insertHistory(@Param("before") OrganizationRelationRow before,
                       @Param("after") OrganizationRelationRow after,
                       @Param("organizationCode") String organizationCode,
                       @Param("changedBy") Long changedBy,
                       @Param("changeReason") String changeReason);

    List<OrganizationRelationHistoryRow> findRelationHistory(@Param("organizationCode") String organizationCode,
                                                             @Param("offset") int offset,
                                                             @Param("size") int size);
}
