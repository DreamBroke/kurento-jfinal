/*
 * (C) Copyright 2015-2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kurento.wisonic.test.model;

import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User session.
 *
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class UserSessionRecorder {

    private final Logger log = LoggerFactory.getLogger(UserSessionRecorder.class);

    private String id;
    private WebRtcEndpoint webRtcEndpoint;
    private RecorderEndpoint recorderEndpoint;
    private MediaPipeline mediaPipeline;
    private Date stopTimestamp;

    public UserSessionRecorder(Session session) {
        this.id = session.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;
    }

    public void setRecorderEndpoint(RecorderEndpoint recorderEndpoint) {
        this.recorderEndpoint = recorderEndpoint;
    }

    public MediaPipeline getMediaPipeline() {
        return mediaPipeline;
    }

    public void setMediaPipeline(MediaPipeline mediaPipeline) {
        this.mediaPipeline = mediaPipeline;
    }

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
    }

    public Date getStopTimestamp() {
        return stopTimestamp;
    }

    public void stop() {
        if (recorderEndpoint != null) {
            final CountDownLatch stoppedCountDown = new CountDownLatch(1);
            ListenerSubscription subscriptionId = recorderEndpoint
                    .addStoppedListener(event -> stoppedCountDown.countDown());
            recorderEndpoint.stop();
            try {
                if (!stoppedCountDown.await(5, TimeUnit.SECONDS)) {
                    log.error("Error waiting for recorder to stop");
                }
            } catch (InterruptedException e) {
                log.error("Exception while waiting for state change", e);
            }
            recorderEndpoint.removeStoppedListener(subscriptionId);
        }
    }

    public void release() {
        this.mediaPipeline.release();
        this.webRtcEndpoint = null;
        this.mediaPipeline = null;
        if (this.stopTimestamp == null) {
            this.stopTimestamp = new Date();
        }
    }
}
