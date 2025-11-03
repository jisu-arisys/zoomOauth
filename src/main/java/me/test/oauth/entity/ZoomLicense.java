package me.test.oauth.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="license_info_list")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ZoomLicense {

    @Id
    @Column(name = "type")
    private Integer type;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "display_name", length = 128)
    private String displayName;
}
