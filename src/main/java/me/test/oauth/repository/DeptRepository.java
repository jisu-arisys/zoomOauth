package me.test.oauth.repository;

import me.test.oauth.dto.DtoOrganization;
import me.test.oauth.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeptRepository extends JpaRepository<Department, String> {

    @Query("SELECT new me.test.oauth.dto.DtoOrganizationRead(d, p, null) FROM Department d LEFT JOIN Department p on (d.deptParentCode = p.deptCode) ORDER BY COALESCE(p.index,d.index), d.level, d.index")
    List<DtoOrganization> findAllByDeptParentCode();

    @Query("SELECT new me.test.oauth.dto.DtoOrganizationRead(d, p, u) FROM Department d " +
            "LEFT JOIN Department p on (d.deptParentCode = p.deptCode) " +
            "LEFT OUTER JOIN User u on (d.deptCode = u.dept.deptCode)" +
            "ORDER BY COALESCE(p.index,d.index), d.level, d.index, u.index")
    List<DtoOrganization> findAllByDeptParentCodeByUser();

}