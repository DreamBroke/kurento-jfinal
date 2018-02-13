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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 杨金鑫
 */
@ServerEndpoint(value = "/happy-new-year-websocket")
public class HappyNewYearWebSocket extends BaseWebSocket {

    private MediaPipeline pipeline;
    private WebRtcEndpoint webRtcEndpoint;

    private static final String USERNAME = "wisonic";
    private static final String PASSWORD = "w1s0n!c2018+1s";

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
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                if (webRtcEndpoint != null) {
                    IceCandidate ice = new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    webRtcEndpoint.addIceCandidate(ice);
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

    private synchronized void presenter(final Session session, JsonObject jsonMessage)
            throws IOException {
        pipeline = kurento.createMediaPipeline();
        webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

        Kurento presenter = Kurento.DAO.findById(1);
        presenter.set("pipeline_id", pipeline.getId()).set("webrtc_endpoint_id", webRtcEndpoint.getId()).update();
        Date now = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy.MM.dd-hh:mm:ss");
        String recorderFilePath = "file:///home/ubuntu/video/year-end-party-" + ft.format(now) + ".webm";
        RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, recorderFilePath)
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
        recorder.addPausedListener(event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "paused");
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(response.toString());
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
        webRtcEndpoint.connect(recorder, MediaType.AUDIO);
        webRtcEndpoint.connect(recorder, MediaType.VIDEO);

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
        response.addProperty("id", "presenterResponse");
        response.addProperty("response", "accepted");
        response.addProperty("sdpAnswer", sdpAnswer);

        synchronized (session) {
            session.getBasicRemote().sendText(response.toString());
        }
        webRtcEndpoint.gatherCandidates();

        recorder.record();
    }

    @Override
    protected void stop(Session session) throws IOException {

        if (webRtcEndpoint != null) {
            webRtcEndpoint.release();
            webRtcEndpoint = null;
        }
        if (pipeline != null) {
            pipeline.release();
            pipeline = null;
        }
    }

    @OnClose
    public void afterConnectionClosed(Session session) throws IOException {
        stop(session);
    }
}
