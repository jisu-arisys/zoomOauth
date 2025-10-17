package me.test.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.test.oauth.entity.webhook.WebhookEvent;
import me.test.oauth.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEvent saveWebhook(WebhookEvent event) throws JsonProcessingException {
        return webhookEventRepository.save(event);
    }
}