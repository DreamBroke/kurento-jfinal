package kurento.wisonic.test.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;

/**
 * @author 木数难数
 */
public abstract class BaseWebSocket {

    protected KurentoClient kurento = KurentoClient.create("ws://192.168.1.180:8888/kurento");
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected static final Gson GSON = new GsonBuilder().create();

    /**
     * 用于处理WebSocket连接请求
     * @param message message
     * @param session session
     * @throws Exception exception
     */
    protected abstract void onMessage(String message, Session session) throws Exception;

    protected void handleErrorResponse(Throwable throwable, Session session, String responseId)
            throws IOException {
        stop(session);
        log.error(throwable.getMessage(), throwable);
        JsonObject response = new JsonObject();
        response.addProperty("id", responseId);
        response.addProperty("response", "rejected");
        response.addProperty("message", throwable.getMessage());
        session.getBasicRemote().sendText(response.toString());
    }


    /**
     * 用于停止会话
     * @param session session
     * @throws IOException IOException
     */
    protected abstract void stop(Session session) throws IOException;

}
