package kr.ac.knue.commonfoundation.personnel;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PersonnelMapper {
    @Select("""
            <script>
            select employee_no as "employeeNo", name, organization_code as "organizationCode", rank_name as "rankName",
                   employment_status as "employmentStatus", position_name as "positionName", retirement_date as "retirementDate",
                   last_synced_at as "lastSyncedAt"
            from korus_personnel_snapshots
            where status = 'ACTIVE'
            <if test='employeeNo != null and employeeNo != ""'>and employee_no = #{employeeNo}</if>
            <if test='name != null and name != ""'>and name like concat('%', #{name}, '%')</if>
            <if test='organizationCode != null and organizationCode != ""'>and organization_code = #{organizationCode}</if>
            <if test='rankName != null and rankName != ""'>and rank_name = #{rankName}</if>
            <if test='employmentStatus != null and employmentStatus != ""'>and employment_status = #{employmentStatus}</if>
            order by employee_no
            </script>
            """)
    List<PersonnelSnapshot> search(@Param("employeeNo") String employeeNo, @Param("name") String name,
                                   @Param("organizationCode") String organizationCode, @Param("rankName") String rankName,
                                   @Param("employmentStatus") String employmentStatus);
}
