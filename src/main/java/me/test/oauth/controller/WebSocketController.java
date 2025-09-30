package me.test.oauth.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Controller
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();

    public WebSocketController(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;

        // 백그라운드 스레드에서 이벤트 처리
        Thread processor = new Thread(this::processEvents);
        processor.setDaemon(true); // 애플리케이션 종료 시 자동 종료
        processor.start();
    }

    /** 백그라운드 동작: 100ms마다 큐에서 webhook 이벤트를 꺼내고, 타입을 구분하여 클라이언트 ws에 보냄 **/
    @Scheduled(fixedDelay = 100)
    public void processEvents() {
        Map<String, Object> event;
        while ((event = queue.poll()) != null) {
            publishCtiEvent(event);
            break;
        }
    }

    /** 웹훅에서 호출해 큐목록에 이벤트 담기 **/
    public void enqueueEvent(Map<String, Object> event) {
        queue.offer(event);
        processEvents(); // 큐 넣자마자 처리
    }

    /** 단순 브로드캐스트 예시. **/
    public void publishCtiEvent(Map<String, Object> event) {
        messagingTemplate.convertAndSend("/topic/user-status", event);
    }

    /** 개인 전송 /queue/user-{id} 사용**/
//    public void queueCtiEvent(@RequestParam(required = true) String userId, Map<String, Object> event) {
//        messagingTemplate.convertAndSend("/queue/user-" + userId, event);
//    }

}
