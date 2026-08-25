package kr.ac.knue.commonfoundation.manuals;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ManualManagementMapper {
    List<ManualRow> listManuals(@Param("manualType") String manualType,
            @Param("targetUser") String targetUser,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("limit") int limit,
            @Param("offset") int offset);
    long countManuals(@Param("manualType") String manualType,
            @Param("targetUser") String targetUser,
            @Param("effectiveDate") LocalDate effectiveDate);
    ManualRow findManual(@Param("manualId") Long manualId, @Param("effectiveDate") LocalDate effectiveDate);
    ManualRow findDuplicate(@Param("manualType") String manualType,
            @Param("targetUser") String targetUser,
            @Param("version") String version);
    void insertManual(@Param("manualType") String manualType,
            @Param("version") String version,
            @Param("targetUser") String targetUser,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("userId") Long userId,
            @Param("changeReason") String changeReason);
    Long lastManualId();
    void insertManualFile(@Param("manualId") Long manualId,
            @Param("originalFileName") String originalFileName,
            @Param("internalStorageName") String internalStorageName,
            @Param("fileContent") byte[] fileContent,
            @Param("userId") Long userId);
    ManualDownload findDownload(@Param("manualId") Long manualId);
}
