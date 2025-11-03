package me.test.oauth.service;

import me.test.oauth.entity.CustomUserDetails;
import me.test.oauth.entity.User;
import me.test.oauth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);


    /** 불변성 유지를 위해 생성자로 직접 주입함. **/
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Deprecated
    public UserDetails loadUserByUsername1(String username) throws UsernameNotFoundException {
        Optional<User> foundUser = userRepository.findByEmail(username);

        if(foundUser.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        User user = foundUser.get();
        logger.info("CustomUserDetailsService : {}", user.getUsername());
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getUserType().getAuthority())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

}
