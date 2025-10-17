//package me.test.oauth.entity.webhook;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.databind.PropertyNamingStrategies;
//import com.fasterxml.jackson.databind.annotation.JsonNaming;
//import jakarta.persistence.*;
//import lombok.Data;
//import org.springframework.format.annotation.DateTimeFormat;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "webhook_payload")
//@Data
//@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
//public class Payload {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "account_id")
//    private String accountId;
//
//    private String event_ts;
//
//    @OneToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "object_id", referencedColumnName = "id")
//    private WebhookObject object;
//}
//
