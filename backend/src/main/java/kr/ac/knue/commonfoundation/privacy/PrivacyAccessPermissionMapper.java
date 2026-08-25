package kr.ac.knue.commonfoundation.privacy;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrivacyAccessPermissionMapper {
    List<PrivacyAccessPermissionRow> listPrivacyAccessPermissions(@Param("criteria") PrivacyAccessPermissionSearchCriteria criteria);

    long countPrivacyAccessPermissions(@Param("criteria") PrivacyAccessPermissionSearchCriteria criteria);

    PrivacyAccessPermissionRow findByRoleCodeAndFieldKey(@Param("roleCode") String roleCode, @Param("fieldKey") String fieldKey);

    void upsertPrivacyAccessPermission(@Param("roleCode") String roleCode,
                                       @Param("fieldKey") String fieldKey,
                                       @Param("rawViewAllowedYn") String rawViewAllowedYn,
                                       @Param("maskedViewAllowedYn") String maskedViewAllowedYn,
                                       @Param("exportAllowedYn") String exportAllowedYn,
                                       @Param("accountViewAllowedYn") String accountViewAllowedYn,
                                       @Param("changeReason") String changeReason,
                                       @Param("updatedBy") Long updatedBy);
}
