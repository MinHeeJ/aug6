package kr.ac.knue.commonfoundation.schoolinfo;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchoolInfoService {
    private final SchoolInfoPort schoolInfoPort;

    public SchoolInfoService(SchoolInfoPort schoolInfoPort) {
        this.schoolInfoPort = schoolInfoPort;
    }

    public SchoolInfoSearchResponse search(SchoolInfoQuery query) {
        try {
            return schoolInfoPort.search(query);
        } catch (ExternalIntegrationException exception) {
            if (!query.schoolName().isBlank() || !query.educationOfficeCode().isBlank()) {
                throw exception;
            }
            return new SchoolInfoSearchResponse(query.page(), query.size(), 0, List.of());
        }
    }
}
