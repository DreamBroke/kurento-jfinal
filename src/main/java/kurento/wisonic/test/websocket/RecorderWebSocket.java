package kurento.wisonic.test.websocket;

import com.google.gson.JsonObject;
import kurento.wisonic.test.base.BaseWebSocket;
import kurento.wisonic.test.model.UserRegistryRecorder;
import kurento.wisonic.test.model.UserSessionRecorder;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * @author Administrator
 */
@ServerEndpoint(value = "/recorder_websocket")
public class RecorderWebSocket extends BaseWebSocket {

    private static final String RECORDER_FILE_PATH = "file:///tmp/HelloWorldRecorded.webm";

    private static UserRegistryRecorder registry = new UserRegistryRecorder();

    @Override
    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);

        log.debug("Incoming message: {}", jsonMessage);

        UserSessionRecorder user = registry.getBySession(session);
        if (user != null) {
            log.debug("Incoming message from user '{}': {}", user.getId(), jsonMessage);
        } else {
            log.debug("Incoming message from new user: {}", jsonMessage);
        }

        switch (jsonMessage.get("id").getAsString()) {
            case "start":
                start(session, jsonMessage);
                break;
            case "stop":
                if (user != null) {
                    user.stop();
                }
            case "stopPlay":
                if (user != null) {
                    user.release();
                }
                break;
            case "play":
                play(user, session, jsonMessage);
                break;
            case "onIceCandidate": {
                JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

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

            MediaProfileSpecType profile = getMediaProfileFromMessage(jsonMessage);

            RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, RECORDER_FILE_PATH)
                    .withMediaProfile(profile).build();

            recorder.addRecordingListener(new EventListener<RecordingEvent>() {

                @Override
                public void onEvent(RecordingEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "recording");
                    try {
                        synchronized (session) {
                            session.getBasicRemote().sendText(response.toString());
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }

            });

            recorder.addStoppedListener(new EventListener<StoppedEvent>() {

                @Override
                public void onEvent(StoppedEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "stopped");
                    try {
                        synchronized (session) {
                            session.getBasicRemote().sendText(response.toString());
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }

            });

            recorder.addPausedListener(new EventListener<PausedEvent>() {

                @Override
                public void onEvent(PausedEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "paused");
                    try {
                        synchronized (session) {
                            session.getBasicRemote().sendText(response.toString());
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }

            });

            connectAccordingToProfile(webRtcEndpoint, recorder, profile);

            // 2. Store user session
            UserSessionRecorder user = new UserSessionRecorder(session);
            user.setMediaPipeline(pipeline);
            user.setWebRtcEndpoint(webRtcEndpoint);
            user.setRecorderEndpoint(recorder);
            registry.register(user);

            // 3. SDP negotiation
            String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

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

            JsonObject response = new JsonObject();
            response.addProperty("id", "startResponse");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (user) {
                session.getBasicRemote().sendText(response.toString());
            }

            webRtcEndpoint.gatherCandidates();

            recorder.record();
        } catch (Throwable t) {
            log.error("Start error", t);
            sendError(session, t.getMessage());
        }
    }

    private MediaProfileSpecType getMediaProfileFromMessage(JsonObject jsonMessage) {

        MediaProfileSpecType profile;
        switch (jsonMessage.get("mode").getAsString()) {
            case "audio-only":
                profile = MediaProfileSpecType.WEBM_AUDIO_ONLY;
                break;
            case "video-only":
                profile = MediaProfileSpecType.WEBM_VIDEO_ONLY;
                break;
            default:
                profile = MediaProfileSpecType.WEBM;
        }

        return profile;
    }

    private void connectAccordingToProfile(WebRtcEndpoint webRtcEndpoint, RecorderEndpoint recorder,
                                           MediaProfileSpecType profile) {
        switch (profile) {
            case WEBM:
                webRtcEndpoint.connect(recorder, MediaType.AUDIO);
                webRtcEndpoint.connect(recorder, MediaType.VIDEO);
                break;
            case WEBM_AUDIO_ONLY:
                webRtcEndpoint.connect(recorder, MediaType.AUDIO);
                break;
            case WEBM_VIDEO_ONLY:
                webRtcEndpoint.connect(recorder, MediaType.VIDEO);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported profile for this tutorial: " + profile);
        }
    }

    private void play(UserSessionRecorder user, final Session session, JsonObject jsonMessage) {
        try {

            // 1. Media logic
            final MediaPipeline pipeline = kurento.createMediaPipeline();
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, RECORDER_FILE_PATH).build();
            player.connect(webRtcEndpoint);

            // Player listeners
            player.addErrorListener(new EventListener<ErrorEvent>() {
                @Override
                public void onEvent(ErrorEvent event) {
                    log.info("ErrorEvent for session '{}': {}", session.getId(), event.getDescription());
                    sendPlayEnd(session, pipeline);
                }
            });
            player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
                @Override
                public void onEvent(EndOfStreamEvent event) {
                    log.info("EndOfStreamEvent for session '{}'", session.getId());
                    sendPlayEnd(session, pipeline);
                }
            });

            // 2. Store user session
            user.setMediaPipeline(pipeline);
            user.setWebRtcEndpoint(webRtcEndpoint);

            // 3. SDP negotiation
            String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "playResponse");
            response.addProperty("sdpAnswer", sdpAnswer);

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

            // 5. Play recorded stream
            player.play();

            synchronized (session) {
                session.getBasicRemote().sendText(response.toString());
            }

            webRtcEndpoint.gatherCandidates();
        } catch (Throwable t) {
            log.error("Play error", t);
            sendError(session, t.getMessage());
        }
    }

    private void sendPlayEnd(Session session, MediaPipeline pipeline) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("id", "playEnd");
            session.getBasicRemote().sendText(response.toString());
        } catch (IOException e) {
            log.error("Error sending playEndOfStream message", e);
        }
        // Release pipeline
        pipeline.release();
    }

    private void sendError(Session session, String message) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("id", "error");
            response.addProperty("message", message);
            session.getBasicRemote().sendText(response.toString());
        } catch (IOException e) {
            log.error("Exception sending message", e);
        }
    }

    @Override
    protected void stop(Session session) throws IOException {

    }

    @OnClose
    public void afterConnectionClosed(Session session) throws IOException {
        registry.removeBySession(session);
    }
}
