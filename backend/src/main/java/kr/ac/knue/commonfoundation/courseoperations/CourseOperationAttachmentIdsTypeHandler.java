package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Reads JSON attachment identifiers without registering a global List alias/handler. */
public class CourseOperationAttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType type)
            throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(value));
        } catch (Exception exception) {
            throw new SQLException("Invalid attachment identifiers", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet result, String column) throws SQLException {
        return parse(result.getString(column));
    }

    @Override
    public List<String> getNullableResult(ResultSet result, int column) throws SQLException {
        return parse(result.getString(column));
    }

    @Override
    public List<String> getNullableResult(CallableStatement result, int column) throws SQLException {
        return parse(result.getString(column));
    }

    private List<String> parse(String value) throws SQLException {
        try {
            return JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new SQLException("Invalid stored attachment identifiers", exception);
        }
    }
}
