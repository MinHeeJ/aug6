package kr.ac.knue.commonfoundation.basic36;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FullTimeFacultyStatusMapper {
    List<FullTimeFacultyStatusRow> listStatuses(@Param("criteria") FullTimeFacultyStatusSearchCriteria criteria);
    long countStatuses(@Param("criteria") FullTimeFacultyStatusSearchCriteria criteria);
}
