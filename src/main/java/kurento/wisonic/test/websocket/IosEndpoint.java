package kurento.wisonic.test.websocket;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

/**
 * @author 杨金鑫
 */

@ServerEndpoint("/ios-websocket")
public class IosEndpoint {
    @OnMessage
    public String hello(String message) {
        System.out.println("Received : "+ message);
        return message;
    }

    @OnOpen
    public void myOnOpen(Session session) {
        System.out.println("WebSocket opened: " + session.getId());
    }

    @OnClose
    public void myOnClose(CloseReason reason) {
        System.out.println("Closing a WebSocket due to " + reason.getReasonPhrase());
    }

}
