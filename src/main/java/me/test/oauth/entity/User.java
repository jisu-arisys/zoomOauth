package me.test.oauth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name="users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @JsonIgnore
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY) //자동증가되는 PK
    private Long userId;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "email", length = 50)
    private String email;

    @JsonIgnore
    @Column(name = "activated")
    private String activated;


    //권한에 대한 관계설정 : user:authorities 다대다 관계를 위해서 -> user:user_authority:authorities 별도의 테이블을 만들어 일대다, 다대일로 정의함
    @ManyToMany
    @JoinTable(name = "user_authority", // 조인 테이블명
                joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "user_id")}, // 외래키
                inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "authority_name")}) // 반대 엔티티의 외래키
    private Set<Authority> authorities;


}
