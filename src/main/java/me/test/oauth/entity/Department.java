package me.test.oauth.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.*;

@ToString
@Entity
@Table(name="department")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Department {

    @Id
    @Column(name = "dept_code")
    private Integer deptCode;

    @Column(name = "dept_name", length = 128)
    private String deptName;

    @Column(name = "dept_email", length = 200)
    private String deptEmail;

    @Column(name = "dept_status")
    private Integer deptStatus;

    @Column(name = "dept_color")
    private String deptColor;

    /**조직관리를 위한 옵션필드 추가 **/
    @Column(name = "dept_parent_code")
    private Integer deptParentCode;
    private Integer index;
    private Integer level;
    private String icon;
}
