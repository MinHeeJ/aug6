package kr.ac.knue.commonfoundation.basic43;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ObjectionOpinionMapper {
    List<ObjectionOpinionRow> listObjectionOpinions(@Param("criteria") ObjectionOpinionSearchCriteria criteria);
    long countObjectionOpinions(@Param("criteria") ObjectionOpinionSearchCriteria criteria);
    ObjectionOpinionRow findLatestByObjectionId(@Param("objectionId") Long objectionId);
    int objectionScopeExists(@Param("objectionId") Long objectionId, @Param("handlerUserId") Long handlerUserId);
    int rejectionReasonExists(@Param("reasonCode") String reasonCode);
    ObjectionOpinionRow insertTransition(@Param("objectionId") Long objectionId,
                                          @Param("evaluationYear") String evaluationYear,
                                          @Param("applicantUserId") Long applicantUserId,
                                          @Param("applicantOpinionSnapshot") String applicantOpinionSnapshot,
                                          @Param("objectionContentSnapshot") String objectionContentSnapshot,
                                          @Param("reviewerOpinion") String reviewerOpinion,
                                          @Param("decisionResult") String decisionResult,
                                          @Param("reasonCode") String reasonCode,
                                          @Param("processedBy") Long processedBy,
                                          @Param("changeReason") String changeReason);
}
