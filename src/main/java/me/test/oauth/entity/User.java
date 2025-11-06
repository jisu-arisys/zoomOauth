package me.test.oauth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.function.BiConsumer;

@Entity
@Table(name="employee")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @JsonIgnore
    @Id
    @Column(name = "emp_no")
    @GeneratedValue(strategy = GenerationType.IDENTITY) //자동증가되는 PK
    private Long empNo;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "dept_code", length = 100)
    private String deptCode;

    @Column(name = "position", length = 100)
    private String position;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_type", referencedColumnName = "authority_name")
    private Authority userType;

    @JsonIgnore
    @Column(name = "activated")
    private Integer activated;


    // 사내 User 에 적용할 속성맵
    public static final Map<String, BiConsumer<User, String>> companyUserMapping = Map.of(
            "password", User::setPassword,
            "position", User::setPosition,
            "dept_code", User::setDeptCode,
            "userType", User::setUserType,
            "username", User::setUsername // 예: displayName → username 변환
    );

    private void setUserType(String s) {
        this.userType = Authority.builder().authorityName(s).build();
    }
}
