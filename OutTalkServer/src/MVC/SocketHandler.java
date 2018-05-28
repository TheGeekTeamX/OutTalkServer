package MVC;

import java.util.HashMap;
import java.util.List;
import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.Gson;
import Notifications.*;
import ResponsesEntitys.EventData;
import ResponsesEntitys.UserData;

public class SocketHandler {
	
	private HashMap<String, SocketIOClient> connections;
	private Gson gson;
	
	public  <T> T getObjectFromString(String data, Class<T> classOfT)
	{
		return gson.fromJson(data, classOfT);
	}
	
	public  String getStringFromObject(Object obj)
	{
		return gson.toJson(obj);
	}
	
	public void sendToClient(SocketIOClient client, String event, Object obj) {
		// TODO Auto-generated method stub
		String jsonString = getStringFromObject(obj);
		client.sendEvent(event, jsonString);
	}
	
	public SocketHandler(HashMap<String, SocketIOClient> connections) {
		super();
		this.connections = connections;
		this.gson = new Gson();
	}

	public void sendEventInventationToUsers(EventData ed, List<UserData> participants) {
		participants.forEach(p -> {
			SocketIOClient sock = connections.get(p.getEmail());
			if (sock != null) {
				sendToClient(sock, "Notification",
						new EventInvitationNotificationData(ed));
			}
		});
	}

	public void sendEventCloseNotificationToUsers(EventData eventData, List<UserData> participants) {
		participants.forEach(p -> {
			SocketIOClient sock = connections.get(p.getEmail());
			if (sock != null) {
				sendToClient(sock, "Notification", new EventCloseNotificationData(eventData));
			}
		});
	}

	public void sendUserEventNotification(EventData eventData,List<UserData> list, UserData userData, Boolean isJoin) {
		list.forEach(u -> {
			SocketIOClient sock = connections.get(u.getEmail());
			if (sock != null) {
				sendToClient(sock, "Notification",
						isJoin ? new UserJoinEventNotification(eventData, userData)
								: new UserLeaveEventNotification(eventData, userData));
			}
		});
	}




}
