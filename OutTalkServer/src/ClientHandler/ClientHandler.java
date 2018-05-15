package ClientHandler;

import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.Gson;

import MVC.IController;
import Requests.*;

public class ClientHandler  {
	private static Gson gson = new Gson();

	public ClientHandler() {
	}

	public static <T> T getObjectFromString(String data, Class<T> classOfT)
	{
		return gson.fromJson(data, classOfT);
	}
	
	public static String getStringFromObject(Object obj)
	{
		return gson.toJson(obj);
	}
	
	public static void handleClient(SocketIOClient client, IController controller, String data) {

		RequestData rd = (RequestData) gson.fromJson(data, RequestData.class);
		switch (rd.getType()) {
		case AddFriendRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, AddFriendRequestData.class)));
			return;
		case ChangePasswordRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, ChangePasswordRequestData.class)));
			return;
		case CloseEventRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, CloseEventRequestData.class)));
			return;
		case CreateEventRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, CreateEventRequestData.class)));
			return;
		case CreateUserRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, CreateEventRequestData.class)));
			return;
		case EditContactsListRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, EditContactsListRequestData.class)));
			return;
		case EditUserRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, EditUserRequestData.class)));
			return;
		case EventProtocolRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, EventProtocolRequestData.class)));
			return;
		case EventsListRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, EventsListRequestData.class)));
			return;
		case ContactsListRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, ContactsListRequestData.class)));
			return;
		case ProfilePictureRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, ProfilePictureRequestData.class)));
			return;
		case UpdateProfilePictureRequest:
			sendToClient(client, "Response", controller.execute(getObjectFromString(data, UpdateProfilePictureRequestData.class)));
			return;
		default:
			return;
		}
	}

	
	public static void sendToClient(SocketIOClient client, String event, Object obj) {
		// TODO Auto-generated method stub

		String jsonString = getStringFromObject(obj);
		client.sendEvent(event, jsonString);
	}


	
	
}
