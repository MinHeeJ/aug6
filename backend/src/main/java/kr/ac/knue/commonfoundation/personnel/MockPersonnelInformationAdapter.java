package kr.ac.knue.commonfoundation.personnel;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;

@Service
public class MockPersonnelInformationAdapter implements PersonnelInformationPort {
    private final PersonnelMapper personnelMapper;

    public MockPersonnelInformationAdapter(PersonnelMapper personnelMapper) {
        this.personnelMapper = personnelMapper;
    }

    @Override
    public List<PersonnelSnapshot> search(PersonnelSearchCriteria criteria) {
        return personnelMapper.search(criteria.employeeNo(), criteria.name(), criteria.organizationCode(), criteria.rankName(), criteria.employmentStatus());
    }

    @Override
    public void rejectSourceMutation(String fieldName) {
        throw new BusinessValidationException("KORUS 원천 인사정보는 직접 수정할 수 없습니다.",
                List.of(new ValidationError(fieldName, "조회 전용 KORUS snapshot 필드입니다.")));
    }
}
