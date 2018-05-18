package MVC;

import com.corundumstudio.socketio.SocketIOClient;

import Enums.RequestType;
import Requests.RequestData;
import Responses.ResponseData;

public interface IController {
	ResponseData execute(String data);

}
