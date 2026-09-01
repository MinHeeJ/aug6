package kr.ac.knue.commonfoundation.basic36;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AchievementDataHistoryMapper {
    List<AchievementDataHistoryRow> listHistories(@Param("criteria") AchievementDataHistorySearchCriteria criteria);

    long countHistories(@Param("criteria") AchievementDataHistorySearchCriteria criteria);

    List<AchievementDataAsOfRow> listAsOfSnapshots(@Param("criteria") AchievementDataAsOfSearchCriteria criteria);

    long countAsOfSnapshots(@Param("criteria") AchievementDataAsOfSearchCriteria criteria);
}
