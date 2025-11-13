package me.test.oauth.dto;

import lombok.*;
import me.test.oauth.entity.Department;
import me.test.oauth.entity.User;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoOrganization {
    Department dept;
    Department parent;
    User user;
}

