package me.test.oauth.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

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
}
