package kurento.wisonic.test.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jfinal.core.Controller;
import kurento.wisonic.test.model.UserSessionOne2Many;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Yang Jinxin   --email: yangjinxin@wisonic.cn
 * 这是一个失败的例子，暂时没有写如何主动发送ice候选给前端的方法
 */
public class BroadcastHttp extends Controller {

    protected KurentoClient kurento = KurentoClient.create("ws://192.168.1.180:8888/kurento");
    private static UserSessionOne2Many presenterUserSession;
    private static final String RECORDER_FILE_PATH = "file:///home/wisonic/kurento-record/kurento-record-mp4/mp4-test.mp4";
    protected static final Gson GSON = new GsonBuilder().create();
    private static MediaPipeline pipeline;
    private static Map<String, List<JsonObject>> iceCandidates = new ConcurrentHashMap<>();

    public void index() {
        render("index.html");
    }

    public void signaling() throws IOException, InterruptedException {
        String requestData = getRequest().getReader().lines().collect(Collectors.joining());
        JsonObject jsonMessage = GSON.fromJson(requestData, JsonObject.class);
        switch (jsonMessage.get("id").getAsString()) {
            case "presenter":

                try {
                    iceCandidates.computeIfAbsent(getSession().getId(), k -> new Vector<>());

                    presenter(jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, "presenterResponse");
                }
                break;
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                UserSessionOne2Many user = null;
                for (int i = 0; i < 10; i++) {
                    if(presenterUserSession != null) {
                        break;
                    }
                    Thread.sleep(100);
                }
                if (presenterUserSession != null) {
                    if (presenterUserSession.getSession(null) == getSession()) {
                        user = presenterUserSession;
                    }
                }
                if (user != null) {
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    if (user.getWebRtcEndpoint() == null) {
                        System.out.println("user is not null but webrtcEndpoint is null");
                        System.out.println(user == presenterUserSession);
                        break;
                    }
                    user.addCandidate(cand);
                } else {
                    System.out.println("user is null!");
                }
                renderNull();
                break;
            }
            case "stop":
                stop(getSession());
                break;
            default:
                break;
        }
    }

    public void signalingTest() throws IOException {
        String requestData = getRequest().getReader().lines().collect(Collectors.joining());
        JsonObject jsonMessage = GSON.fromJson(requestData, JsonObject.class);
        test(jsonMessage);
        System.out.println("Something");
    }

    private void test(JsonObject jsonMessage) {
        System.out.println(jsonMessage.toString());
        JsonObject response = new JsonObject();
        response.addProperty("id", "presenterResponse");
        response.addProperty("response", "accepted");
        response.addProperty("sdpAnswer", jsonMessage.get("sdpOffer").getAsString());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        renderJson(response.toString());
    }

    private void presenter(JsonObject jsonMessage)
            throws IOException {
        if (presenterUserSession == null) {
            pipeline = kurento.createMediaPipeline();
            WebRtcEndpoint webrtcEp = new WebRtcEndpoint.Builder(pipeline).build();

            presenterUserSession = new UserSessionOne2Many(getSession());
            presenterUserSession.setWebRtcEndpoint(webrtcEp);



            RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, RECORDER_FILE_PATH)
                    .withMediaProfile(MediaProfileSpecType.MP4).build();
            recorder.addRecordingListener(event -> {
                JsonObject response = new JsonObject();
                response.addProperty("id", "recording");
                renderJson(response.toString());
            });

            recorder.addStoppedListener(event -> {
                JsonObject response = new JsonObject();
                response.addProperty("id", "stopped");
                renderJson(response.toString());
            });

            presenterUserSession.getWebRtcEndpoint().connect(recorder, MediaType.AUDIO);
            presenterUserSession.getWebRtcEndpoint().connect(recorder, MediaType.VIDEO);

            WebRtcEndpoint presenterWebRtc = presenterUserSession.getWebRtcEndpoint();
            presenterWebRtc.addIceCandidateFoundListener(getListener());

            String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
            String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "presenterResponse");
            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (getSession()) {
                renderJson(response.toString());
            }


            presenterWebRtc.gatherCandidates();

            recorder.record();

        } else {
            JsonObject response = new JsonObject();
            response.addProperty("id", "presenterResponse");
            response.addProperty("response", "rejected");
            response.addProperty("message",
                    "Another user is currently acting as sender. Try again later ...");
            renderJson(response.toString());
        }
    }

    private EventListener<IceCandidateFoundEvent> getListener() {
        return event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "iceCandidate");
            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));

        };
    }

    private void handleErrorResponse(Throwable throwable, String responseId)
            throws IOException {

        JsonObject response = new JsonObject();
        response.addProperty("id", responseId);
        response.addProperty("response", "rejected");
        response.addProperty("message", throwable.getMessage());
        renderJson(response.toString());
    }

    protected synchronized void stop(HttpSession session) throws IOException {
        String sessionId = session.getId();
        if (presenterUserSession != null && presenterUserSession.getSession(null).getId().equals(sessionId)) {
            if (pipeline != null) {
                pipeline.release();
            }
            pipeline = null;
            presenterUserSession = null;
        }
        renderNull();
    }

}
