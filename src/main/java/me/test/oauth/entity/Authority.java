package me.test.oauth.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "authority")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Authority implements GrantedAuthority {

    @Id
    @Column(name = "authority_name", length = 50)
    private String authorityName;

    @Override
    public String getAuthority() {
        return "ROLE_"+authorityName;
    }
}
