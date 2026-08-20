package kr.ac.knue.commonfoundation.evaluationyears;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationYearMapper {
    EvaluationYearSettingsRow getEvaluationYearSettings();

    List<EvaluationYearPreparationRow> listEvaluationYearPreparations();

    void upsertEvaluationYearSettings(@Param("currentEvaluationYear") Integer currentEvaluationYear,
                                      @Param("defaultSearchYear") Integer defaultSearchYear,
                                      @Param("updatedBy") Long updatedBy,
                                      @Param("changeReason") String changeReason);

    void upsertEvaluationYearPreparation(@Param("targetYear") Integer targetYear,
                                         @Param("copyRequestedYn") String copyRequestedYn,
                                         @Param("resetRequestedYn") String resetRequestedYn,
                                         @Param("updatedBy") Long updatedBy,
                                         @Param("changeReason") String changeReason);

    default void mutateExistingEvaluationResults(Object first, Object second, Object third) {
        throw new UnsupportedOperationException("기준연도 관리는 기존 평가자료를 삭제하거나 변경하지 않습니다.");
    }

    default void editReferenceInformationValues(Object first, Object second, Object third) {
        throw new UnsupportedOperationException("기준연도 관리는 개별 기준정보 값을 직접 편집하지 않습니다.");
    }
}
