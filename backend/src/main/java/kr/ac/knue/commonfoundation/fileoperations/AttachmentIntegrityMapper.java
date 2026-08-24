package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttachmentIntegrityMapper {
    void insertCheck(AttachmentIntegrityCheckRow check);

    void completeCheck(@Param("checkId") Long checkId, @Param("status") String status);

    void insertFinding(@Param("checkId") Long checkId, @Param("draft") AttachmentIntegrityFindingDraft draft);

    List<AttachmentIntegrityFindingRow> listFindings(
            @Param("checkId") Long checkId,
            @Param("anomalyType") String anomalyType,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long countFindings(@Param("checkId") Long checkId, @Param("anomalyType") String anomalyType);

    List<AttachmentIntegrityFindingRow> listFindingsForExcel(
            @Param("checkId") Long checkId,
            @Param("anomalyType") String anomalyType);
}
