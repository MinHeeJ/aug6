package kr.ac.knue.commonfoundation.helpcontents;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HelpContentManagementMapper {
    List<HelpContentRow> listHelpContents(@Param("screenId") String screenId, @Param("limit") int limit, @Param("offset") int offset);
    long countHelpContents(@Param("screenId") String screenId);
    HelpContentRow findHelpContentRow(@Param("screenId") String screenId);
    HelpContentResponse findHelpContent(@Param("screenId") String screenId);
    void upsertHelpContent(@Param("screenId") String screenId,
            @Param("businessDescription") String businessDescription,
            @Param("inputCriteria") String inputCriteria,
            @Param("faq") String faq,
            @Param("contact") String contact,
            @Param("userId") Long userId,
            @Param("changeReason") String changeReason);
}
