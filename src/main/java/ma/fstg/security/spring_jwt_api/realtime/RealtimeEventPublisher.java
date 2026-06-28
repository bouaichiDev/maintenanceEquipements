package ma.fstg.security.spring_jwt_api.realtime;

import ma.fstg.security.spring_jwt_api.dto.RealtimeMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishTicketEvent(Long organizationId, String type, Object payload) {
        if (organizationId == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/org/" + organizationId + "/tickets",
                new RealtimeMessage(type, payload));
        messagingTemplate.convertAndSend("/topic/org/" + organizationId + "/dashboard",
                new RealtimeMessage("DASHBOARD_REFRESH", null));
    }

    public void publishActivity(Long organizationId, Object payload) {
        if (organizationId == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/org/" + organizationId + "/activity",
                new RealtimeMessage("ACTIVITY", payload));
    }
}
