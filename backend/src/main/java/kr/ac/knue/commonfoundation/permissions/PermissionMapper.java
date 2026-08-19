package kr.ac.knue.commonfoundation.permissions;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper {
    @Select("""
            select m.menu_id as "menuId", m.parent_menu_id as "parentMenuId", m.menu_name as "menuName",
                   m.screen_id as "screenId", m.url as "url", m.icon as "icon", m.display_order as "displayOrder"
            from menus m
            left join menu_usage_settings mus on mus.menu_id = m.menu_id
            where m.status = 'ACTIVE'
              and m.system_use_yn = 'Y'
              and (m.url is null or (
                  coalesce(mus.system_use_yn, m.system_use_yn) = 'Y'
                  and mus.exposure_start_at is not null
                  and mus.exposure_end_at is not null
                  and CURRENT_TIMESTAMP between mus.exposure_start_at and mus.exposure_end_at
              ))
            order by coalesce(m.parent_menu_id, 0), m.display_order, m.menu_id
            """)
    List<MenuRow> findActiveMenus();

    @Select("""
            select case
                       when not exists (select 1 from menus where url = #{path} and status != 'DELETED') then 1
                       when exists (
                           select 1
                           from menus m
                           left join menu_usage_settings mus on mus.menu_id = m.menu_id
                           where m.url = #{path}
                             and m.status = 'ACTIVE'
                             and m.system_use_yn = 'Y'
                             and coalesce(mus.system_use_yn, m.system_use_yn) = 'Y'
                             and mus.exposure_start_at is not null
                             and mus.exposure_end_at is not null
                             and CURRENT_TIMESTAMP between mus.exposure_start_at and mus.exposure_end_at
                       ) then 1
                       else 0
                   end
            """)
    int isMenuRouteExposed(@Param("path") String path);

    @Select("""
            select mp.target_type as "targetType", mp.access_allowed as "accessAllowed"
            from menu_permissions mp
            join menus m on m.menu_id = mp.menu_id
            where m.url = #{path} and mp.status = 'ACTIVE'
              and (
                (mp.target_type = 'USER' and mp.target_id = cast(#{userId} as varchar))
                or (mp.target_type = 'ORGANIZATION' and exists (
                    select 1
                    from users u
                    join korus_personnel_snapshots k on k.employee_no = u.employee_no
                    where u.user_id = #{userId}
                      and k.organization_code = mp.target_id
                ))
                or (mp.target_type = 'ROLE' and mp.target_id in (${roleCodesCsv}))
              )
            """)
    List<PermissionRule> findRulesForPath(@Param("userId") Long userId, @Param("path") String path, @Param("roleCodesCsv") String roleCodesCsv);

    @Select("""
            select mp.target_type as "targetType", mp.access_allowed as "accessAllowed"
            from menu_permissions mp
            where mp.menu_id = #{menuId} and mp.status = 'ACTIVE'
              and (
                (mp.target_type = 'USER' and mp.target_id = cast(#{userId} as varchar))
                or (mp.target_type = 'ORGANIZATION' and exists (
                    select 1
                    from users u
                    join korus_personnel_snapshots k on k.employee_no = u.employee_no
                    where u.user_id = #{userId}
                      and k.organization_code = mp.target_id
                ))
                or (mp.target_type = 'ROLE' and mp.target_id in (${roleCodesCsv}))
              )
            """)
    List<PermissionRule> findRulesForMenu(@Param("userId") Long userId, @Param("menuId") Long menuId, @Param("roleCodesCsv") String roleCodesCsv);
}
