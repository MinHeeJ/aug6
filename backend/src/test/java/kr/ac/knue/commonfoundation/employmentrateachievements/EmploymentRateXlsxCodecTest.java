package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/** Verifies adversarial workbook rejection and literal output with actual OOXML ZIP contents. */
class EmploymentRateXlsxCodecTest {
    private static final List<List<String>> ROWS = List.of(List.of("열", "빈 열", "한글"), List.of("=1+1", "", "&<>"));

    @Test
    void literalFormulaLookingValuesAndBlankCellsRoundTrip() {
        assertThat(EmploymentRateXlsxCodec.read(EmploymentRateXlsxCodec.write(ROWS))).isEqualTo(ROWS);
    }

    @Test
    void actualFormulasAreForbidden() throws Exception {
        byte[] bytes = mutate("xl/worksheets/sheet1.xml", text -> text.replace("<is>", "<f>SUM(A1)</f><is>"));
        assertThatThrownBy(() -> EmploymentRateXlsxCodec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void externalRelationshipsAreForbidden() throws Exception {
        byte[] bytes = mutate("xl/_rels/workbook.xml.rels", text -> text.replace("Target=", "TargetMode=\"External\" Target="));
        assertThatThrownBy(() -> EmploymentRateXlsxCodec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dtdAndExternalEntitiesAreForbidden() throws Exception {
        byte[] bytes = mutate("xl/worksheets/sheet1.xml", text ->
                "<!DOCTYPE worksheet [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>" + text);
        assertThatThrownBy(() -> EmploymentRateXlsxCodec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void overExpandedZipIsBounded() throws Exception {
        byte[] bytes = mutate("xl/worksheets/sheet1.xml", text -> "a".repeat(21 * 1024 * 1024));
        assertThatThrownBy(() -> EmploymentRateXlsxCodec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void corruptArchiveCannotBeAcceptedAsSpreadsheet() {
        assertThatThrownBy(() -> EmploymentRateXlsxCodec.read(new byte[]{'P', 'K', 1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] mutate(String path, java.util.function.UnaryOperator<String> edit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(EmploymentRateXlsxCodec.write(ROWS)));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                byte[] content = in.readAllBytes();
                if (entry.getName().equals(path)) {
                    content = edit.apply(new String(content, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                }
                zip.putNextEntry(new ZipEntry(entry.getName()));
                zip.write(content);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
