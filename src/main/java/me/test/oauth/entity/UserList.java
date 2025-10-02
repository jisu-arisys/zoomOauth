package me.test.oauth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="userlist")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserList {

    @Id
    private String id;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(length = 100, unique = true)
    private String email;

    private Integer type;

    @Column(length = 50)
    private String timezone;

    private Integer verified;

    @Column(length = 100)
    private String dept;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_client_version", length = 50)
    private String lastClientVersion;

    @Column(length = 10)
    private String language;

    @Column(length = 20)
    private String status;

    @Column(name = "role_id", length = 100)
    private String roleId;

    @Column(name = "user_created_at")
    private LocalDateTime userCreatedAt;
}