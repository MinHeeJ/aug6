package kr.ac.knue.commonfoundation.basic36;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FullTimeFacultyStatusService {
    private final FullTimeFacultyStatusMapper mapper;

    public FullTimeFacultyStatusService(FullTimeFacultyStatusMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public FullTimeFacultyStatusSearchResponse list(FullTimeFacultyStatusSearchCriteria criteria) {
        return new FullTimeFacultyStatusSearchResponse(mapper.listStatuses(criteria), Math.max(criteria.page(), 0),
                criteria.safeSize(), mapper.countStatuses(criteria), criteria.baseYear());
    }
}
