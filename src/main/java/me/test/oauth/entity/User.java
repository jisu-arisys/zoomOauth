package me.test.oauth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@ToString
@Entity
@Table(name="employee")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dept_code", referencedColumnName = "dept_code")
    private Department dept;

    @Column(name = "position", length = 100)
    private String position;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_type", referencedColumnName = "authority_name")
    private Authority userType;

    @Column(name = "activated")
    private Integer activated;

    @Column(name = "dept_index")
    private Integer index;

    public void setUserType(Authority userType) {
        this.userType = userType;
    }
}
