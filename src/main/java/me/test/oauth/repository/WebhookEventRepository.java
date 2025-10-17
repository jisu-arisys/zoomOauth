package me.test.oauth.repository;

import me.test.oauth.entity.webhook.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

}
