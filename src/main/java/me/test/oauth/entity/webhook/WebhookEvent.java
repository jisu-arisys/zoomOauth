package me.test.oauth.entity.webhook;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Map;

@Entity
@Table(name = "webhook_event")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
/** payload 별도 테이블 없이 플랫하게 저장 **/
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String event;  // 예: "user.signed_in"

    @CreationTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String created;

    @Column(name = "account_id")
    private String accountId;

    private String event_ts;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "object_id", referencedColumnName = "object_id")
    private WebhookObject object;

    // webhook마다 달라지는 추가 데이터 (예: presence_status, call_id 등)
    @JsonAnySetter
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> extraFields;
}
