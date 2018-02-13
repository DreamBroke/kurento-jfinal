package kurento.wisonic.test.controller;

import com.google.gson.JsonObject;
import com.jfinal.core.Controller;
import kurento.wisonic.test.model.Kurento;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
public class BroadcastJs extends Controller {

    public void index() {
        render("index.html");
    }

    public void saveMassage() throws IOException {
        String sessionId = getSession().getId();
        Kurento kurento = Kurento.DAO.findById(sessionId);
        if (kurento != null) {
            kurento.set("pipeline_id", getPara("pipeline")).set("webrtc_endpoint_id", getPara("webRtc")).update();
        } else {
            new Kurento().set("session_id", sessionId).set("pipeline_id", getPara("pipeline")).set("webrtc_endpoint_id", getPara("webRtc")).save();
        }
        JsonObject result = new JsonObject();
        result.addProperty("sessionId", sessionId);
        renderJson(result.toString());
    }

    public void getWebRtc() {
        String sessionId = getPara("sessionId");
        Kurento kurento = Kurento.DAO.findById(sessionId);
        JsonObject result = new JsonObject();
        result.addProperty("webRtcEndpoint", kurento.getWebRtcEndpointId());
        result.addProperty("pipeline", kurento.getPipelineId());
        renderJson(result.toString());
    }

}
