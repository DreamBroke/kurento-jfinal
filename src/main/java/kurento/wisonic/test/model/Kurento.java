package kurento.wisonic.test.model;

import com.jfinal.plugin.activerecord.Model;

/**
 * @author Chinhsin Yang
 */
public class Kurento extends Model<Kurento> {

    public static final Kurento DAO = new Kurento().dao();

    public String getWebRtcEndpointId() {
        return getStr("webrtc_endpoint_id");
    }

    public String getPipelineId() {
        return getStr("pipeline_id");
    }

}
