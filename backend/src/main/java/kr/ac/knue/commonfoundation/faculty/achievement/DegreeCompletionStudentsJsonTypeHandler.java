package kr.ac.knue.commonfoundation.faculty.achievement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Converts PostgreSQL's aggregated student-detail JSON into the API projection without exposing
 * storage-only columns or requiring an additional query per achievement row.
 */
public class DegreeCompletionStudentsJsonTypeHandler extends BaseTypeHandler<List<DegreeCompletionStudentRow>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<List<DegreeCompletionStudentRow>> TYPE = new TypeReference<>() { };

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<DegreeCompletionStudentRow> parameter, JdbcType jdbcType) throws SQLException {
        try {
            statement.setString(index, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (Exception exception) {
            throw new SQLException("지도학생 상세 JSON을 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public List<DegreeCompletionStudentRow> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return read(resultSet.getString(columnName));
    }

    @Override
    public List<DegreeCompletionStudentRow> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return read(resultSet.getString(columnIndex));
    }

    @Override
    public List<DegreeCompletionStudentRow> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return read(statement.getString(columnIndex));
    }

    private List<DegreeCompletionStudentRow> read(String json) throws SQLException {
        try {
            return json == null ? List.of() : OBJECT_MAPPER.readValue(json, TYPE);
        } catch (Exception exception) {
            throw new SQLException("지도학생 상세 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
