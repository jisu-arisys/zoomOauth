package me.test.oauth.entity.webhook;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "webhook_object")
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WebhookObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long object_id;

    @Column(name = "id")
    private String id; // 원래 JSON의 "id" 필드

    @Column(name = "date_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;

    @Column(name = "email")
    String email;

    // webhook마다 달라지는 추가 데이터 (예: presence_status, call_id 등)
    @JsonAnySetter
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> extraFields;
}
