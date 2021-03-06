package kurento.wisonic.test.websocket;

import com.google.gson.JsonObject;
import kurento.wisonic.test.base.BaseWebSocket;
import kurento.wisonic.test.model.UserSessionHelloWorld;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 木数难数
 */

@ServerEndpoint(value = "/websocket")
public class HelloWorldWebSocket extends BaseWebSocket {

    private final ConcurrentHashMap<String, UserSessionHelloWorld> users = new ConcurrentHashMap<>();

    @Override
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(message);
        JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);

        log.debug("Incoming message: {}", jsonMessage);
        switch (jsonMessage.get("id").getAsString()) {
            case "start":
                start(session, jsonMessage);
                break;
            case "stop": {
                UserSessionHelloWorld user = users.remove(session.getId());
                if (user != null) {
                    user.release();
                }
                break;
            }
            case "onIceCandidate": {
                JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

                UserSessionHelloWorld user = users.get(session.getId());
                if (user != null) {
                    IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
                            jsonCandidate.get("sdpMid").getAsString(),
                            jsonCandidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(candidate);
                }
                break;
            }
            default:
                sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
                break;
        }
    }

    @Override
    protected void stop(Session session) throws IOException {

    }

    private void start(final Session session, JsonObject jsonMessage) {
        try {
            // 1. Media logic (webRtcEndpoint in loopback)
            MediaPipeline pipeline = kurento.createMediaPipeline();
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoint.connect(webRtcEndpoint);
            // 2. Store user session
            UserSessionHelloWorld user = new UserSessionHelloWorld();
            user.setMediaPipeline(pipeline);
            user.setWebRtcEndpoint(webRtcEndpoint);
            users.put(session.getId(), user);

            // 3. SDP negotiation
            String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "startResponse");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (session) {
                session.getBasicRemote().sendText(response.toString());
            }

            // 4. Gather ICE candidates
            webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.getBasicRemote().sendText(response.toString());
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            });

            webRtcEndpoint.gatherCandidates();

        } catch (Throwable t) {
            sendError(session, t.getMessage());
        }
    }

    private void sendError(Session session, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "error");
        response.addProperty("message", message);
        try {
            session.getBasicRemote().sendText(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
