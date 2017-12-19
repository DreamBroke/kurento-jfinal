package kurento.wisonic.test.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import kurento.wisonic.test.model.UserSession;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 木数难数
 */

@ServerEndpoint(value = "/websocket")
public class HelloWorldWebSocket {
    private static final Gson GSON = new GsonBuilder().create();
    private final Logger log = LoggerFactory.getLogger(HelloWorldWebSocket.class);
    private int i = 0, j = 0;

    private KurentoClient kurento = KurentoClient.create("ws://192.168.1.180:8888/kurento");

    private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(message);
        JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);

        log.debug("Incoming message: {}", jsonMessage);
        i++;
        System.out.println("handleTextMessage ===============================" + jsonMessage.get("id").getAsString() + "_+_+_+_" + i);
        switch (jsonMessage.get("id").getAsString()) {
            case "start":
                start(session, jsonMessage);
                break;
            case "stop": {
                UserSession user = users.remove(session.getId());
                if (user != null) {
                    user.release();
                }
                break;
            }
            case "onIceCandidate": {
                System.out.println("==============" + jsonMessage);

                JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

                UserSession user = users.get(session.getId());
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

    private void start(final Session session, JsonObject jsonMessage) {
        try {
            // 1. Media logic (webRtcEndpoint in loopback)
            MediaPipeline pipeline = kurento.createMediaPipeline();
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoint.connect(webRtcEndpoint);
            System.out.println("start+++++++++++++++++++++++++++++_-----------------------");
            // 2. Store user session
            UserSession user = new UserSession();
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
                            j++;
                            System.out.println(response.toString() + "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-" + "_________" + j);
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
