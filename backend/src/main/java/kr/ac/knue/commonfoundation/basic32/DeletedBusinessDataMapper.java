package kr.ac.knue.commonfoundation.basic32;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeletedBusinessDataMapper {
    List<DeletedBusinessDataRow> listDeletedData(@Param("criteria") DeletedBusinessDataSearchCriteria criteria);

    long countDeletedData(@Param("criteria") DeletedBusinessDataSearchCriteria criteria);
}
