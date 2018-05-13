package MVC;

import com.corundumstudio.socketio.SocketIOClient;

import ClientHandler.ClientHandler;
import Notifications.NotificationData;

public class NotificationHandler {
	
	public static void sendNitification(SocketIOClient client, NotificationData notificationData)
	{
		client.sendEvent("Notification", ClientHandler.getStringFromObject(notificationData));
	}

}
