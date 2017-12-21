package kurento.wisonic.test.base;

import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;

/**
 * @author 木数难数
 */
public abstract class BaseWebSocket {

    protected KurentoClient kurento = KurentoClient.create("ws://192.168.1.180:8888/kurento");
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     *  用于处理WebSocket连接请求
     *
     *  @param message message
     *  @param session session
     */

    public abstract void onMessage(String message, Session session) throws Exception;
}
