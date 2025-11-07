package me.test.oauth.dto;

import lombok.*;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomUser;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoUsers {
    User user;
    ZoomUser zoomUser;
}
