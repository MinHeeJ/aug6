package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.List;
import org.apache.ibatis.type.Alias;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Converts the foundation JSON array explicitly rather than relying on JDBC array coercion. */
@Alias("CourseOperationAttachmentIdsTypeHandler")
public class AttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> ids, JdbcType type)
            throws SQLException {
        try {
            statement.setObject(index, JSON.writeValueAsString(ids), Types.OTHER);
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
    public List<String> getNullableResult(CallableStatement statement, int column) throws SQLException {
        return parse(statement.getString(column));
    }

    private List<String> parse(String value) throws SQLException {
        try {
            return value == null ? List.of() : JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new SQLException("Invalid stored attachment identifiers", exception);
        }
    }
}
