package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Materializes the PostgreSQL JSON array as opaque file IDs, never as filesystem paths. */
public class AttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> value, JdbcType type)
            throws SQLException {
        try {
            ps.setObject(i, JSON.writeValueAsString(value), java.sql.Types.OTHER);
        } catch (java.io.IOException exception) {
            throw new SQLException("Cannot serialize attachment IDs", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String name) throws SQLException {
        return parse(rs.getString(name));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int index) throws SQLException {
        return parse(rs.getString(index));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int index) throws SQLException {
        return parse(statement.getString(index));
    }

    private List<String> parse(String json) throws SQLException {
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (java.io.IOException exception) {
            throw new SQLException("Invalid attachment array", exception);
        }
    }
}
