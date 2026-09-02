package kr.ac.knue.commonfoundation.basic42;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.basic36.ResearcherProfileSummary;
import kr.ac.knue.commonfoundation.batch.BatchResultRow;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic42MapperRuntimeContractTest {
    @Test
    void exceptionPeriodMapperUsesKorusSnapshotNameInsteadOfMissingUsersNameColumn() throws Exception {
        String xml = mapper("mapper/exceptionperiod/ExceptionPeriodMapper.xml");

        assertThat(xml)
                .contains("left join korus_personnel_snapshots k on k.employee_no = u.employee_no")
                .contains("k.name as \"teacherName\"")
                .contains("or k.name ilike concat('%', #{criteria.normalizedKeyword}, '%')")
                .doesNotContain("u.name as \"teacherName\"")
                .doesNotContain("select name from users")
                .doesNotContain("or u.name ilike");
    }

    @Test
    void booleanConstructorArgumentsUsePrimitiveMyBatisAliasesForCompiledRecordConstructors() throws Exception {
        assertPrimitiveBooleanConstructor(ResearcherProfileSummary.class,
                String.class, String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, boolean.class, LocalDateTime.class);
        assertPrimitiveBooleanConstructor(BatchResultRow.class,
                String.class, String.class, String.class, String.class, LocalDateTime.class, LocalDateTime.class,
                Integer.class, Integer.class, Integer.class, Integer.class, Long.class, boolean.class);

        assertThat(mapper("mapper/basic36/ResearcherProfileMapper.xml"))
                .contains("<arg column=\"degreePrerequisiteMissing\" javaType=\"_boolean\"/>")
                .contains("coalesce(p.degree_prerequisite_missing_yn, 'N') = 'Y' as \"degreePrerequisiteMissing\"");
        assertThat(mapper("mapper/batch/BatchResultMapper.xml"))
                .contains("<arg column=\"hasLog\" javaType=\"_boolean\"/>")
                .contains("(bel.execution_id is not null) as \"hasLog\"")
                .contains("ber.elapsed_millis as \"elapsedMillis\"")
                .contains("<arg column=\"elapsedMillis\" javaType=\"java.lang.Long\"/>");
    }

    private static void assertPrimitiveBooleanConstructor(Class<?> type, Class<?>... parameterTypes) throws NoSuchMethodException {
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        assertThat(constructor.getParameterTypes()).contains(boolean.class).doesNotContain(Boolean.class);
    }

    private static String mapper(String path) throws Exception {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
