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

/** Explicitly materializes the header's JSON text array as the public attachmentIds collection. */
public class CourseOperationAttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> ids, JdbcType type)
            throws SQLException {
        try {
            ps.setString(i, JSON.writeValueAsString(ids));
        } catch (Exception e) {
            throw new SQLException("Invalid attachment references", e);
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
    public List<String> getNullableResult(CallableStatement cs, int index) throws SQLException {
        return parse(cs.getString(index));
    }

    private List<String> parse(String value) throws SQLException {
        try {
            return value == null ? List.of() : JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new SQLException("Invalid persisted attachment references", e);
        }
    }
}
