package kr.ac.knue.commonfoundation.employmentrateachievements;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Domain checks and locking/scoped queries over the existing Excel foundation schema. */
@Mapper
public interface EmploymentRateExcelMapper {
    String findTemplateId();

    java.util.Map<String, Object> findUpload(@Param("uploadId") String uploadId, @Param("lock") boolean lock);

    java.util.List<java.util.Map<String, Object>> stagedRows(@Param("uploadId") String uploadId);

    void reject(@Param("uploadId") String uploadId);

    Long lockTeacher(@Param("teacherId") Long teacherId);

    java.util.List<kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow> histories(
            @Param("userId") Long userId, @Param("admin") boolean admin,
            @Param("uploadId") String uploadId, @Param("name") String name,
            @Param("limit") int limit, @Param("offset") int offset);

    long countHistories(@Param("userId") Long userId, @Param("admin") boolean admin,
            @Param("uploadId") String uploadId, @Param("name") String name);

    Long findActiveTeacher(@Param("employeeNo") String employeeNo);

    int countActiveItems(@Param("code") String code);

    int countDuplicates(
            @Param("teacherId") Long teacherId,
            @Param("year") String year,
            @Param("code") String code,
            @Param("date") String date,
            @Param("name") String name);
}
