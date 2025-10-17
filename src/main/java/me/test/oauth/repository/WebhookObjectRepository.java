package me.test.oauth.repository;

import me.test.oauth.entity.webhook.WebhookObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookObjectRepository extends JpaRepository<WebhookObject, String> {
    // 대소문자 무시 조회
    Optional<WebhookObject> findByIdIgnoreCase(String id);
}
