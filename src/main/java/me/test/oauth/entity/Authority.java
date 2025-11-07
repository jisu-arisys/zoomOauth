package me.test.oauth.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "authority")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Authority implements GrantedAuthority {

    @Id
    @Column(name = "authority_name", length = 50)
    private String authorityName;

    /** ✅ Security 에서 사용되는 권한 문자열 */
    @Override
    public String getAuthority() {
        return "ROLE_" + authorityName; // Security 용으로만 prefix 부여
    }

    /** ✅ 화면(JSON)에서 사용할 때는 prefix 없는 값 유지 */
    @JsonProperty("authorityName")
    public String getAuthorityName() {
        return authorityName;
    }
}
