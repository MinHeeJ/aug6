package kr.ac.knue.commonfoundation.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;

class PersonnelInformationPortTest {
    @Test
    void korusMockSnapshotsAreReadableAndSourceMutationIsRejected() {
        PersonnelInformationPort port = new MockPersonnelInformationAdapter(null) {
            @Override public List<PersonnelSnapshot> search(PersonnelSearchCriteria criteria) {
                return List.of(new PersonnelSnapshot("E1001", "관리자", "ORG-ROOT", "직원", "ACTIVE", "시스템관리자", null, LocalDateTime.now()));
            }
        };
        assertThat(port.search(new PersonnelSearchCriteria(null, null, null, null, null))).hasSize(1);
        assertThatThrownBy(() -> port.rejectSourceMutation("name"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("KORUS");
    }
}
