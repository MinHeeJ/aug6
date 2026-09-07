package kr.ac.knue.commonfoundation.schoolinfo;

import org.springframework.stereotype.Service;

@Service
public class SchoolInfoService {
    private final SchoolInfoPort schoolInfoPort;

    public SchoolInfoService(SchoolInfoPort schoolInfoPort) {
        this.schoolInfoPort = schoolInfoPort;
    }

    public SchoolInfoSearchResponse search(SchoolInfoQuery query) {
        return schoolInfoPort.search(query);
    }
}
