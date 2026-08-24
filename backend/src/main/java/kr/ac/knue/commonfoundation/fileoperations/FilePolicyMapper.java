package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FilePolicyMapper {
    List<FilePolicyRow> listFilePolicies(@Param("businessType") String businessType);

    List<FilePolicyRow> searchFilePolicies(FilePolicySearchCriteria criteria);

    long countFilePolicies(FilePolicySearchCriteria criteria);

    FilePolicyRow findByBusinessType(@Param("businessType") String businessType);

    void upsertFilePolicy(@Param("businessType") String businessType,
                          @Param("allowedExtensions") String allowedExtensions,
                          @Param("maxFileSizeMb") Integer maxFileSizeMb,
                          @Param("maxFilesPerItem") Integer maxFilesPerItem,
                          @Param("maxTotalSizeMb") Integer maxTotalSizeMb,
                          @Param("maxFilenameLength") Integer maxFilenameLength,
                          @Param("malwareScanEnabled") String malwareScanEnabled,
                          @Param("updatedBy") Long updatedBy);
}
