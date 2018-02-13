package kurento.wisonic.test.websocket;

import com.google.gson.JsonObject;
import kurento.wisonic.test.base.BaseWebSocket;
import kurento.wisonic.test.model.Kurento;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author 木数难数
 */

@ServerEndpoint(value = "/broadcast-websocket")
public class BroadcastWebSocket extends BaseWebSocket {

    private MediaPipeline pipeline = null;
    private WebRtcEndpoint webRtcEndpoint = null;
    private RecorderEndpoint recorder = null;

    private static final String RECORDER_FILE_PATH = "file:///home/ubuntu/video/mp4.webm";

    @Override
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);
        log.debug("Incoming message from session '{}': {}", session.getId(), jsonMessage);

        switch (jsonMessage.get("id").getAsString()) {
            case "presenter":
                try {
                    presenter(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "presenterResponse");
                }
                break;
            case "viewer":
                try {
                    viewer(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "viewerResponse");
                }
                break;
            case "onIceCandidate": {
                if(webRtcEndpoint != null) {
                    JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                    IceCandidate iceCandidate = new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    webRtcEndpoint.addIceCandidate(iceCandidate);
                }
                break;
            }
            case "stop":
                stop(session);
                break;
            default:
                break;
        }
    }

    private synchronized void presenter(final Session session, JsonObject jsonMessage) throws IOException {

        pipeline = kurento.createMediaPipeline();

        webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

        String sessionId = jsonMessage.get("sessionId").getAsString();
        Kurento kurentoModel = Kurento.DAO.findById(sessionId);
        if (kurentoModel != null) {
            kurentoModel.set("pipeline_id", pipeline.getId()).set("webrtc_endpoint_id", webRtcEndpoint.getId()).update();
        } else {
            kurentoModel = new Kurento().set("session_id", sessionId).set("pipeline_id", pipeline.getId()).set("webrtc_endpoint_id", webRtcEndpoint.getId());
            kurentoModel.save();
        }

        recorder = new RecorderEndpoint.Builder(pipeline, RECORDER_FILE_PATH)
                .withMediaProfile(MediaProfileSpecType.WEBM).build();
        recorder.addRecordingListener(event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "recording");
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(response.toString());
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });

        recorder.addStoppedListener(event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "stopped");
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(response.toString());
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });

        webRtcEndpoint.connect(recorder);

        webRtcEndpoint.addIceCandidateFoundListener(getListener(session));

        String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

        JsonObject response = new JsonObject();
        response.addProperty("id", "presenterResponse");
        response.addProperty("response", "accepted");
        response.addProperty("sdpAnswer", sdpAnswer);

        synchronized (session) {
            session.getBasicRemote().sendText(response.toString());
        }
        webRtcEndpoint.gatherCandidates();

        recorder.record();

    }

    private synchronized void viewer(final Session session, JsonObject jsonMessage)
            throws IOException {

        String sessionId = jsonMessage.get("sessionId").getAsString();
        Kurento kurentoModel = Kurento.DAO.findById(sessionId);

        if (kurentoModel == null) {
            sendError(session, sessionId + " is not broadcasting");
            return;
        }

        ServerManager serverManager = kurento.getServerManager();
        List<MediaPipeline> list = serverManager.getPipelines();
        for (MediaPipeline mp : list) {
            if (mp.getId().equals(kurentoModel.getPipelineId())) {
                pipeline = mp;
                break;
            }
        }

        if (pipeline == null) {
            sendError(session, "pipeline is null!");
            return;
        }

        WebRtcEndpoint preWebRtcEp = null;

        List<MediaObject> mediaObjects = pipeline.getChildren();
        for (MediaObject mediaObject : mediaObjects) {
            if (mediaObject.getId().equals(kurentoModel.getWebRtcEndpointId())) {
                preWebRtcEp = (WebRtcEndpoint) mediaObject;
                break;
            }
        }

        webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

        webRtcEndpoint.addIceCandidateFoundListener(getListener(session));

        if (preWebRtcEp == null) {
            sendError(session, " No one is broadcasting! ");
            return;
        }

        preWebRtcEp.connect(webRtcEndpoint);

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

    private void sendError(Session session, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message", message);
        try {
            session.getBasicRemote().sendText(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private EventListener<IceCandidateFoundEvent> getListener(final Session session) {
        return event -> {
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
        };
    }

    @Override
    protected synchronized void stop(Session session) throws IOException {
        if(webRtcEndpoint != null) {
            webRtcEndpoint.release();
        }
        if (recorder != null) {
            final CountDownLatch stoppedCountDown = new CountDownLatch(1);
            ListenerSubscription subscriptionId = recorder
                    .addStoppedListener(event -> stoppedCountDown.countDown());
            recorder.stop();
            try {
                if (!stoppedCountDown.await(5, TimeUnit.SECONDS)) {
                    log.error("Error waiting for recorder to stop");
                }
            } catch (InterruptedException e) {
                log.error("Exception while waiting for state change", e);
            }
            recorder.removeStoppedListener(subscriptionId);
        }
        pipeline = null;
        webRtcEndpoint = null;
    }

    @OnClose
    public void afterConnectionClosed(Session session) throws IOException {
        stop(session);
    }
}
