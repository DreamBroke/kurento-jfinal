package kurento.wisonic.test.model;

import com.google.gson.JsonObject;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import java.io.IOException;

/**
 * @author 木数难数
 */
public class UserSessionOne2Many {
    private static final Logger log = LoggerFactory.getLogger(UserSessionOne2Many.class);

    private Session session;
    private WebRtcEndpoint webRtcEndpoint;
    private HttpSession httpSession;

    public UserSessionOne2Many(Session session) {
        this.session = session;
    }

    public UserSessionOne2Many(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    public Session getSession() {
        return session;
    }

    public HttpSession getSession(Object obj) {
        return httpSession;
    }

    public void sendMessage(JsonObject message) throws IOException {
        session.getBasicRemote().sendText(message.toString());
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;
    }

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
    }
}
