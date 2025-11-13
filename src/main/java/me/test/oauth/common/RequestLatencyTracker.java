package me.test.oauth.common;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RequestLatencyTracker {
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    public void start(String key) {
        startTimes.put(key, System.currentTimeMillis());
        log.debug("시작 : " + key);
    }

    public Long end(String key) {
        Long start = startTimes.get(key);
        if (start == null) return null;

        Long elapsed = System.currentTimeMillis() - start;
        if (elapsed != null) {
            log.debug("🔔 사용자 업데이트 완료 (key: " + key + ")");
            log.debug("⏱ 총 소요시간: " + elapsed + "ms");
        } else {
            log.debug("⚠ 시작 기록 없음 (이미 처리되었거나 REST요청과 연결되지 않은 webhook)");
        }

        return elapsed;
    }
}
