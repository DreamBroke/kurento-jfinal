package kurento.wisonic.test.websocket;

import com.google.gson.JsonObject;
import kurento.wisonic.test.base.BaseWebSocket;
import kurento.wisonic.test.model.Kurento;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;

/**
 * @author 杨金鑫
 */
@ServerEndpoint(value = "/happy-new-year-viewer-websocket")
public class HappyNewYearViewerWebSocket extends BaseWebSocket {

    private MediaPipeline pipeline;
    private WebRtcEndpoint webRtcEndpoint;

    @Override
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);
        log.debug("Incoming message from session '{}': {}", session.getId(), jsonMessage);

        switch (jsonMessage.get("id").getAsString()) {
            case "viewer":
                try {
                    viewer(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "presenterResponse");
                }
                break;
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                if (webRtcEndpoint != null) {
                    IceCandidate ice = new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    webRtcEndpoint.addIceCandidate(ice);
                }
                break;
            }
            case "count-viewers":
                int count = pipeline.getChildren().size();
                JsonObject response = new JsonObject();
                response.addProperty("id", "count-viewers");
                response.addProperty("count", count - 1);
                session.getBasicRemote().sendText(response.toString());
                break;
            case "stop":
                stop(session);
                break;
            default:
                break;
        }
    }

    private synchronized void viewer(final Session session, JsonObject jsonMessage)
            throws IOException {
        Kurento presenter = Kurento.DAO.findById(1);
        List<MediaPipeline> mediaPipelines = kurento.getServerManager().getPipelines();
        for (MediaPipeline mp : mediaPipelines) {
            if (mp.getId().equals(presenter.getPipelineId())) {
                pipeline = mp;
                break;
            }
        }

        if (pipeline == null) {
            JsonObject response = new JsonObject();
            response.addProperty("id", "error");
            response.addProperty("response", "NoPipeline");
            session.getBasicRemote().sendText(response.toString());
            return;
        }

        webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

        WebRtcEndpoint presenterEp = null;

        List<MediaObject> mediaObjects = pipeline.getChildren();
        for (MediaObject mediaObject : mediaObjects) {
            if (mediaObject.getId().equals(presenter.getWebRtcEndpointId())) {
                presenterEp = (WebRtcEndpoint) mediaObject;
                break;
            }
        }

        if (presenterEp == null) {
            JsonObject response = new JsonObject();
            response.addProperty("id", "error");
            response.addProperty("response", "NoPresenter");
            session.getBasicRemote().sendText(response.toString());
            return;
        }

        presenterEp.connect(webRtcEndpoint);

        webRtcEndpoint.addIceCandidateFoundListener(event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "iceCandidate");
            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(response.toString());
                }
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        });

        String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "accepted");
        response.addProperty("sdpAnswer", sdpAnswer);

        synchronized (session) {
            session.getBasicRemote().sendText(response.toString());
        }
        webRtcEndpoint.gatherCandidates();
    }

    @Override
    protected void stop(Session session) throws IOException {
        if (webRtcEndpoint != null) {
            webRtcEndpoint.release();
            webRtcEndpoint = null;
        }
    }

    @OnClose
    public void afterConnectionClosed(Session session) throws IOException {
        stop(session);
    }
}
