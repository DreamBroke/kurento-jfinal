package kurento.wisonic.test.websocket;

import com.google.gson.JsonObject;
import kurento.wisonic.test.base.BaseWebSocket;
import kurento.wisonic.test.model.Room;
import kurento.wisonic.test.model.RoomManager;
import kurento.wisonic.test.model.UserRegistryGroup;
import kurento.wisonic.test.model.UserSessionGroup;
import org.kurento.client.IceCandidate;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * @author 木数难数
 */
@ServerEndpoint(value = "/group_websocket")
public class GroupWebSocket extends BaseWebSocket {

    private static RoomManager roomManager = new RoomManager();
    private static UserRegistryGroup registry = new UserRegistryGroup();

    @Override
    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        final JsonObject jsonMessage = GSON.fromJson(message, JsonObject.class);

        final UserSessionGroup user = registry.getBySession(session);

        if (user != null) {
            log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
        } else {
            log.debug("Incoming message from new user: {}", jsonMessage);
        }

        switch (jsonMessage.get("id").getAsString()) {
            case "joinRoom":
                joinRoom(jsonMessage, session);
                break;
            case "receiveVideoFrom":
                final String senderName = jsonMessage.get("sender").getAsString();
                final UserSessionGroup sender = registry.getByName(senderName);
                final String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
                user.receiveVideoFrom(sender, sdpOffer);
                break;
            case "leaveRoom":
                leaveRoom(user);
                break;
            case "onIceCandidate":
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

                if (user != null) {
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand, jsonMessage.get("name").getAsString());
                }
                break;
            default:
                break;
        }
    }

    private void joinRoom(JsonObject params, Session session) throws IOException {
        final String roomName = params.get("room").getAsString();
        final String name = params.get("name").getAsString();
        log.info("PARTICIPANT {}: trying to join room {}", name, roomName);

        Room room = roomManager.getRoom(roomName);
        final UserSessionGroup user = room.join(name, session);
        registry.register(user);
    }

    private void leaveRoom(UserSessionGroup user) throws IOException {
        final Room room = roomManager.getRoom(user.getRoomName());
        room.leave(user);
        if (room.getParticipants().isEmpty()) {
            roomManager.removeRoom(room);
        }
    }

    @OnClose
    public void afterConnectionClosed(Session session) throws IOException {
        UserSessionGroup user = registry.removeBySession(session);
        roomManager.getRoom(user.getRoomName()).leave(user);
    }

    @Override
    protected void stop(Session session) throws IOException {

    }
}
