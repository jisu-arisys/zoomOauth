package me.test.oauth.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class CustomUserDetails implements UserDetails {
    private User user;

    public CustomUserDetails(User user) { this.user = user; }

    public User getUser() { return user; }
    public User getUserForJWT() {
        user.setPassword(null);
        return user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(user.getUserType());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return user.getUserType().getAuthorityName() != "LOCK"; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
