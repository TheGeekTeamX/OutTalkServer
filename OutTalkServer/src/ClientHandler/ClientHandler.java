package ClientHandler;

import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.Gson;

import MVC.Controller;

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
	
	public static void handleClient(SocketIOClient client, Controller controller, String data)
	{
		sendToClient(client, "Response", controller.execute(data));
	}

	
	public static void sendToClient(SocketIOClient client, String event, Object obj) {
		// TODO Auto-generated method stub

		String jsonString = getStringFromObject(obj);
		client.sendEvent(event, jsonString);
	}


	
	
}
