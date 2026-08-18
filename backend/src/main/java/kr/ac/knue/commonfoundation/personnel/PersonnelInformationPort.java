package kr.ac.knue.commonfoundation.personnel;

import java.util.List;

public interface PersonnelInformationPort {
    List<PersonnelSnapshot> search(PersonnelSearchCriteria criteria);
    void rejectSourceMutation(String fieldName);
}
