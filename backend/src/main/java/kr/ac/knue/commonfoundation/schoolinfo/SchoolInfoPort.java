package kr.ac.knue.commonfoundation.schoolinfo;

public interface SchoolInfoPort {
    SchoolInfoSearchResponse search(SchoolInfoQuery query);
}
