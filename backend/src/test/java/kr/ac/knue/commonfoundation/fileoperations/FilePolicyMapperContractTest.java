package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FilePolicyMapperContractTest {
    private final String mapperXml;

    FilePolicyMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/fileoperations/FilePolicyMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void upsertUsesBusinessTypeUniqueKeyAndSupportsRequeryForT004AndT005() {
        assertThat(mapperXml).contains("select id=\"findbybusinesstype\"");
        assertThat(mapperXml).contains("insert id=\"upsertfilepolicy\"");
        assertThat(mapperXml).contains("on conflict (business_type)");
        assertThat(mapperXml).contains("allowed_extensions = excluded.allowed_extensions");
        assertThat(mapperXml).contains("malware_scan_enabled = excluded.malware_scan_enabled");
    }

    @Test
    void optionalBusinessTypeFilterIsDynamicAndAvoidsNullBoundPredicates() {
        assertThat(mapperXml).contains("<if test=\"businesstype != null and businesstype != ''\">");
        assertThat(mapperXml).doesNotContain(" is null or ");
        assertThat(mapperXml).doesNotContain("coalesce(");
    }
}
