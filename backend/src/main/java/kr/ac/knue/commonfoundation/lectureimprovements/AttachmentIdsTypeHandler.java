package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Materializes JSONB attachment identifiers without exposing storage paths in the read model. */
public class AttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType jdbcType)
            throws SQLException {
        statement.setObject(index, serialize(value), Types.OTHER);
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return deserialize(resultSet.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return deserialize(resultSet.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return deserialize(statement.getString(columnIndex));
    }

    private String serialize(List<String> value) throws SQLException {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new SQLException("첨부 식별자를 JSON으로 변환할 수 없습니다.", exception);
        }
    }

    private List<String> deserialize(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(value, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new SQLException("첨부 식별자 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
