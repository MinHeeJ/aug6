package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Decodes the serialized opaque attachment collection without globally aliasing List. */
public class CourseOperationAttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType jdbcType)
            throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(value));
        } catch (Exception exception) {
            throw new SQLException("첨부 참조를 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rows, String column) throws SQLException {
        return decode(rows.getString(column));
    }

    @Override
    public List<String> getNullableResult(ResultSet rows, int column) throws SQLException {
        return decode(rows.getString(column));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int column) throws SQLException {
        return decode(statement.getString(column));
    }

    private List<String> decode(String value) throws SQLException {
        if (value == null || value.isBlank()) return List.of();
        if (!value.startsWith("[")) return List.of(value);
        try {
            return JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new SQLException("첨부 참조 형식이 올바르지 않습니다.", exception);
        }
    }
}
