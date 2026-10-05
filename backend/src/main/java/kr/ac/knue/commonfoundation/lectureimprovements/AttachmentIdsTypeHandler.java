package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Maps PostgreSQL JSON attachment arrays to the API's opaque attachment-id list. */
public class AttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            List<String> parameter,
            JdbcType jdbcType) throws SQLException {
        try {
            statement.setObject(index, OBJECT_MAPPER.writeValueAsString(parameter), Types.OTHER);
        } catch (Exception exception) {
            throw new SQLException("첨부 참조 배열을 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return read(resultSet.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return read(resultSet.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return read(statement.getString(columnIndex));
    }

    private List<String> read(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(value, LIST_TYPE);
        } catch (Exception exception) {
            throw new SQLException("첨부 참조 배열을 읽을 수 없습니다.", exception);
        }
    }
}
