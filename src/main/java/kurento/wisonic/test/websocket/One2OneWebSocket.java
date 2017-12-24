package kurento.wisonic.test.websocket;

import com.google.gson.JsonObject;
import kurento.wisonic.test.base.BaseWebSocket;
import kurento.wisonic.test.model.CallMediaPipelineOne2One;
import kurento.wisonic.test.model.UserRegistryOne2One;
import kurento.wisonic.test.model.UserSessionOne2One;
import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.JsonUtils;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 木数难数
 */
@ServerEndpoint(value = "/one2one_websocket")
public class One2OneWebSocket extends BaseWebSocket {

    private final ConcurrentHashMap<String, CallMediaPipelineOne2One> pipelines = new ConcurrentHashMap<>();

    private static UserRegistryOne2One registry = new UserRegistryOne2One();

    @Override
    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);
        UserSessionOne2One user = registry.getBySession(session);

        if (user != null) {
            log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
        } else {
            log.debug("Incoming message from new user: {}", jsonMessage);
        }

        switch (jsonMessage.get("id").getAsString()) {
            case "register":
                try {
                    register(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "resgisterResponse");
                }
                break;
            case "call":
                try {
                    call(user, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "callResponse");
                }
                break;
            case "incomingCallResponse":
                incomingCallResponse(user, jsonMessage);
                break;
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                if (user != null) {
                    IceCandidate cand =
                            new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid")
                                    .getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand);
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

    private void register(Session session, JsonObject jsonMessage) throws IOException {
        String name = jsonMessage.getAsJsonPrimitive("name").getAsString();

        UserSessionOne2One caller = new UserSessionOne2One(session, name);
        String responseMsg = "accepted";
        if (name.isEmpty()) {
            responseMsg = "rejected: empty user name";
        } else if (registry.exists(name)) {
            responseMsg = "rejected: user '" + name + "' already registered";
        } else {
            registry.register(caller);
        }

        JsonObject response = new JsonObject();
        response.addProperty("id", "resgisterResponse");
        response.addProperty("response", responseMsg);
        caller.sendMessage(response);
    }

    private void call(UserSessionOne2One caller, JsonObject jsonMessage) throws IOException {
        String to = jsonMessage.get("to").getAsString();
        String from = jsonMessage.get("from").getAsString();
        JsonObject response = new JsonObject();

        if (registry.exists(to)) {
            caller.setSdpOffer(jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString());
            caller.setCallingTo(to);

            response.addProperty("id", "incomingCall");
            response.addProperty("from", from);

            UserSessionOne2One callee = registry.getByName(to);
            callee.sendMessage(response);
            callee.setCallingFrom(from);
        } else {
            response.addProperty("id", "callResponse");
            response.addProperty("response", "rejected: user '" + to + "' is not registered");

            caller.sendMessage(response);
        }
    }

    private void incomingCallResponse(final UserSessionOne2One callee, JsonObject jsonMessage)
            throws IOException {
        String callResponse = jsonMessage.get("callResponse").getAsString();
        String from = jsonMessage.get("from").getAsString();
        final UserSessionOne2One calleer = registry.getByName(from);
        String to = calleer.getCallingTo();

        if ("accept".equals(callResponse)) {
            log.debug("Accepted call from '{}' to '{}'", from, to);

            CallMediaPipelineOne2One pipeline = null;
            try {
                pipeline = new CallMediaPipelineOne2One(kurento);
                pipelines.put(calleer.getSessionId(), pipeline);
                pipelines.put(callee.getSessionId(), pipeline);

                callee.setWebRtcEndpoint(pipeline.getCalleeWebRtcEp());
                pipeline.getCalleeWebRtcEp().addIceCandidateFoundListener(
                        event -> {
                            JsonObject response = new JsonObject();
                            response.addProperty("id", "iceCandidate");
                            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                            try {
                                synchronized (callee.getSession()) {
                                    callee.getSession().getBasicRemote().sendText(response.toString());
                                }
                            } catch (IOException e) {
                                log.debug(e.getMessage());
                            }
                        });

                calleer.setWebRtcEndpoint(pipeline.getCallerWebRtcEp());
                pipeline.getCallerWebRtcEp().addIceCandidateFoundListener(
                        event -> {
                            JsonObject response = new JsonObject();
                            response.addProperty("id", "iceCandidate");
                            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                            try {
                                synchronized (calleer.getSession()) {
                                    calleer.getSession().getBasicRemote().sendText(response.toString());
                                }
                            } catch (IOException e) {
                                log.debug(e.getMessage());
                            }
                        });

                String calleeSdpOffer = jsonMessage.get("sdpOffer").getAsString();
                String calleeSdpAnswer = pipeline.generateSdpAnswerForCallee(calleeSdpOffer);
                JsonObject startCommunication = new JsonObject();
                startCommunication.addProperty("id", "startCommunication");
                startCommunication.addProperty("sdpAnswer", calleeSdpAnswer);

                synchronized (callee) {
                    callee.sendMessage(startCommunication);
                }

                pipeline.getCalleeWebRtcEp().gatherCandidates();

                String callerSdpOffer = registry.getByName(from).getSdpOffer();
                String callerSdpAnswer = pipeline.generateSdpAnswerForCaller(callerSdpOffer);
                JsonObject response = new JsonObject();
                response.addProperty("id", "callResponse");
                response.addProperty("response", "accepted");
                response.addProperty("sdpAnswer", callerSdpAnswer);

                synchronized (calleer) {
                    calleer.sendMessage(response);
                }

                pipeline.getCallerWebRtcEp().gatherCandidates();

            } catch (Throwable t) {
                log.error(t.getMessage(), t);

                if (pipeline != null) {
                    pipeline.release();
                }

                pipelines.remove(calleer.getSessionId());
                pipelines.remove(callee.getSessionId());

                JsonObject response = new JsonObject();
                response.addProperty("id", "callResponse");
                response.addProperty("response", "rejected");
                calleer.sendMessage(response);

                response = new JsonObject();
                response.addProperty("id", "stopCommunication");
                callee.sendMessage(response);
            }

        } else {
            JsonObject response = new JsonObject();
            response.addProperty("id", "callResponse");
            response.addProperty("response", "rejected");
            calleer.sendMessage(response);
        }
    }

    @Override
    public void stop(Session session) throws IOException {
        String sessionId = session.getId();
        if (pipelines.containsKey(sessionId)) {
            pipelines.get(sessionId).release();
            CallMediaPipelineOne2One pipeline = pipelines.remove(sessionId);
            pipeline.release();

            // Both users can stop the communication. A 'stopCommunication'
            // message will be sent to the other peer.
            UserSessionOne2One stopperUser = registry.getBySession(session);
            if (stopperUser != null) {
                UserSessionOne2One stoppedUser = (stopperUser.getCallingFrom() != null) ? registry.getByName(stopperUser.getCallingFrom()) : stopperUser.getCallingTo() != null ? registry.getByName(stopperUser.getCallingTo()) : null;

                if (stoppedUser != null) {
                    JsonObject message = new JsonObject();
                    message.addProperty("id", "stopCommunication");
                    stoppedUser.sendMessage(message);
                    stoppedUser.clear();
                }
                stopperUser.clear();
            }

        }
    }

    @OnClose
    public void afterConnectionClosed(Session session) throws IOException {
        stop(session);
    }

}
