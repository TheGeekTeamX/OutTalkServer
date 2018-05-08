package MVC;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import ClientHandler.*;
import DB.*;
import Enums.*;
import Notifications.EventInvitationNotificationData;
import Requests.*;
import Responses.*;
import ResponsesEntitys.*;
public class Controller implements Observer,IController {
	private String url;
	private int port;
	private Controller instance;
	private Model model;
	private View view;
	private HashMap<String, SocketIOClient> connections;
	private SocketIOServer serverSock;
	private PausableThreadPoolExecutor executionPool;
	

	private String getClientEmailBySocket(SocketIOClient client)
	{
		for (String email : connections.keySet())
		{
			if(connections.get(email).equals(client))
				return email;
		}
		return null;
	}
	
	public void start()
	{		

		startServer();
		
	}
	
	//C'tor
	public Controller(Model model , View view,String ip, int port) {
		super();
		this.url = ip;
		this.port = port;
		connections = new HashMap<>();
		instance = this;
		this.model = model;
		this.view = view;
		executionPool = new PausableThreadPoolExecutor(10, 20, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<>(5));
	}
	//Server Methods
	public void initServerFunctionality(SocketIOServer serverSock)
	{
		serverSock.addDisconnectListener(new DisconnectListener() {
			
			@Override
			public void onDisconnect(SocketIOClient client) {
				// TODO Auto-generated method stub
				String email = getClientEmailBySocket(client);
				if(email != null)
				{
					view.printToConsole(email+" Is Disconnected");
					connections.remove(email);
				}
			}
		});
		//Login-First Connection
		serverSock.addEventListener("Login", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
				// TODO Auto-generated method stub
				executionPool.execute(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						LoginRequestData lrd = ClientHandler.getObjectFromString(data, LoginRequestData.class);
						if(getClientEmailBySocket(client) != null && getClientEmailBySocket(client).equals(lrd.getUserEmail()))
						{
							ClientHandler.sendToClient(client, "Response", new ErrorResponseData(ErrorType.ConnectionIsAlreadyEstablished));
							return;
						}
						else 
						{
							if(getClientEmailBySocket(client) != null && !getClientEmailBySocket(client).equals(lrd.getUserEmail()))
								connections.remove(getClientEmailBySocket(client));
							User user = model.getDbManager().getUser(lrd.getUserEmail());
							if(user == null)
							{
								ClientHandler.sendToClient(client, "Response", new ErrorResponseData(ErrorType.UserIsNotExist));
								return;
							}
							if (connections.get(user.getEmail() )==null)
							{								
								Credential credential = model.getDbManager().getCredential(user.getId());
								if(credential == null)
								{
									ClientHandler.sendToClient(client, "Response", new ErrorResponseData(ErrorType.TechnicalError));
									return;
								}
								if(lrd.getUserEmail().equals(credential.getUser().getEmail()) && 
										lrd.getPassword().equals(credential.getCredntial()))
								{
									ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
									ClientHandler.sendToClient(client, "Response", 
											new LoginResponseData(user.getId(), user.getFirstName(), user.getLastName(), user.getPhoneNumber(), pp != null ? pp.getProfilePictureUrl() : ""));
									view.printToConsole(credential.getUser().getEmail()+" Is Connected");
									connections.put(user.getEmail(), client);
								}
								else
									ClientHandler.sendToClient(client, "Response", new BooleanResponseData(false));
								return;
							}
						}				
					}
				});
				
			}
		});
		serverSock.addEventListener("Request", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
				// TODO Auto-generated method stub
				RequestData rd = ClientHandler.getObjectFromString(data, RequestData.class);
				if(getClientEmailBySocket(client).equals(rd.getUserEmail()))
				{
					executionPool.execute(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							ClientHandler.handleClient(client, instance, data);
						}
					});
				}
				else
					ClientHandler.sendToClient(client, "Response", new ErrorResponseData(ErrorType.UserMustToLogin));
				
			}
		});
	}
	
	
	
	private void startServer()
	{
		Configuration config = new Configuration();
		config.setHostname(url);
		config.setPort(port);
		view.printToConsole("Server Is Now Listening On " + url + ":" + port);
		serverSock = new SocketIOServer(config);
		initServerFunctionality(serverSock);
		serverSock.start();		
	}
	

	
	private void notifyParticipants(Event event)
	{
		ArrayList<User> participants = model.getDbManager().getPariticpants(event.getId());
		participants.forEach(p -> {
			SocketIOClient client = connections.get(p.getEmail());
			if(client != null)
			{
				
			}
		});
	}
	
	private ResponseData CloseEvent(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		Event event = (Event) model.getDbManager().get(((CloseEventRequestData)reqData).getEventId(), DBEntityType.Event);
		if(event == null)
			return new ErrorResponseData(ErrorType.EventIsNotExist);
		event.setIsFinished(1);
		if(model.getDbManager().editInDataBase(event.getId(), DBEntityType.Event, event))
			return new ErrorResponseData(ErrorType.TechnicalError);
		else
		{			
			//notify to all pariticpants
			//Get Byte array from req and send to GoogleService
			notifyParticipants(event);
			return new BooleanResponseData(true);
		}
	}
	
	private ResponseData EventProtocol(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		int eventId = ((EventProtocolRequestData)reqData).getEventID();
		
		return null;
	}
	
	private ResponseData PendingEvents(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		LinkedList<EventData> events = model.getDbManager().getRelatedPendingEvents(user.getId());
		if(events == null)
			return new ErrorResponseData(ErrorType.NoPendingEvents);
		return new PendingEventsResponseData(events);
	}
	
	private ResponseData CreateEvent(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		String[] participantsEmail = ((CreateEventRequestData)reqData).getUsersEmails().split(",");
		int i=0;
		LinkedList<User> participants = new LinkedList<>();
		do {
			User u = model.getDbManager().getUser(participantsEmail[i]);
			if(u!=null)
				participants.add(u);
			i++;
		}while(i < participantsEmail.length);
		
		//create Event
		Event e = new Event(user,((CreateEventRequestData)reqData).getTitle(), new Date(Calendar.getInstance().getTime().getTime()), 0, 0);
		if (!(model.getDbManager().addToDataBase(e) > 0))
			return new ErrorResponseData(ErrorType.TechnicalError);	
		//create UserEvent
		ArrayList<Integer> ids = new ArrayList<>();
		participants.forEach(p -> {
			ids.add(p.getId());
			model.getDbManager().addToDataBase(new UserEvent(p, e, 0));
		});
		
		sendInvitesToUsers(e, ids);
		
		return new BooleanResponseData(true);
	}
	
	@Override
	public ResponseData execute(RequestData reqData) {
		// TODO Auto-generated method stub
		switch (reqData.getType()) {
		case AddFriendRequest:
			return AddFriend(reqData);//checked
		case ChangePasswordRequest:
			return ChangePassword(reqData);//checked
		case CreateUserRequest:
			return CreateUser(reqData);
		case EditContactsListRequest:
			return EditContactsList(reqData);
		case EditUserRequest:
			return EditUser(reqData);
		case EventsListRequest:
			return EventsList(reqData);
		case ContactsListRequest:
			return ContactList(reqData);
		case CloseEventRequest:
			return CloseEvent(reqData);
		case CreateEventRequest:
			return CreateEvent(reqData);
		case EventProtocolRequest:
			return EventProtocol(reqData);
		case PendingEventsRequest:
			return PendingEvents(reqData);
		case ProfilePictureRequest:
			return ProfilePicture(reqData);
		case UpdateProfilePictureRequest:
			return UpdateProfilePicture(reqData);
		default:
			System.out.println("default");
			break;
		}
		return null;
	}
	
	private ResponseData UpdateProfilePicture(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
		if (pp == null)
			return new ErrorResponseData(ErrorType.UserHasNoProfilePicture);
		pp.setProfilePictureUrl(((UpdateProfilePictureRequestData)reqData).getNewProfilePictureUrl());
		return new BooleanResponseData(model.getDbManager().editInDataBase(pp.getId(), DBEntityType.ProfilePicture, pp));
	}
	
	private ResponseData ProfilePicture(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
		if (pp == null)
			return new ErrorResponseData(ErrorType.UserHasNoProfilePicture);
		else
			/*Attach Image To Response*/
		return null;
	}
	
	
	public Model getModel() {
		return model;
	}


	
	private ResponseData CreateUser(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user != null)
			return new ErrorResponseData(ErrorType.EmailAlreadyRegistered);
		User u = new User(reqData.getUserEmail(), ((CreateUserRequestData)reqData).getFirstName(),((CreateUserRequestData)reqData).getLastName(), ((CreateUserRequestData)reqData).getPhoneNumber(),((CreateUserRequestData)reqData).getCountry());
		if(model.getDbManager().addToDataBase(u) < 0)
			return new ErrorResponseData(ErrorType.TechnicalError);
		addProfilePicture(reqData);//TODO
		model.getDbManager().addToDataBase(new Credential(u,((CreateUserRequestData)reqData).getCredential()));			
		return new BooleanResponseData(true);
	}

	
	private ResponseData ContactList(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList <Contact> contactsList = model.getDbManager().getContactsList(user.getId());
		LinkedList<UserData> list = new LinkedList<>();
		contactsList.forEach(c -> {
			//list.add(new UserData(c.getFriend().getFullName(), c.getFriend().getEmail(), "TODO"));
		});
		return new ContactsListResponseData(list);
	}
	
	private ResponseData EditUser(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		addProfilePicture(reqData);//TODO
		user.setCountry(((EditUserRequestData)reqData).getCountry());
		user.setPhoneNumber(((EditUserRequestData)reqData).getPhoneNumber());
		user.setFirstName(((EditUserRequestData)reqData).getFirstName());
		user.setLastName(((EditUserRequestData)reqData).getLastName());
		return new BooleanResponseData(model.getDbManager().editInDataBase(user.getId(), DBEntityType.User, user));
	}
	
	private ResponseData EditContactsList(RequestData reqData)
	{
		User user = model.getDbManager().getUser(reqData.getUserEmail());
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList <Contact> currentContactsList = model.getDbManager().getContactsList(user.getId());
		if (currentContactsList.size() == 0)
			return new BooleanResponseData(false);
		LinkedList <String> newContactsList = ((EditContactsListRequestData)reqData).getUpdatedFriendsList();
		if (newContactsList.size() == 0)
			return new BooleanResponseData(false);
		currentContactsList.forEach(contact -> {
			if(!newContactsList.contains(""+contact.getFriend().getId()))
			{				
				System.out.println(contact.getId());
				model.getDbManager().deleteFromDataBase(contact.getId(), DBEntityType.Contact);
			}
		});
		
		return new BooleanResponseData(true);
	}
	
	private ResponseData ChangePassword(RequestData reqData)
	{
		String oldPass = ((ChangePasswordRequestData)reqData).getOldPassword();
		String newPass = ((ChangePasswordRequestData)reqData).getNewPassword();
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		Credential credential = model.getDbManager().getCredential(user.getId());
		if(credential == null)
			return new ErrorResponseData(ErrorType.TechnicalError);

		if (!credential.getCredntial().equals(oldPass))
			return new ErrorResponseData(ErrorType.WrongPreviousPassword);

		if (newPass.equals(oldPass))
			return new ErrorResponseData(ErrorType.BothPasswordsEquals);
		credential.setCredntial(newPass);
		Boolean res =model.getDbManager().editInDataBase(credential.getId(), DBEntityType.Credential, credential);
		return new BooleanResponseData(true);
	}
	
	private ResponseData AddFriend(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send AddFriendRequesr");
		User user = getUserFromDB(reqData);
		User friend = model.getDbManager().getUser(((AddFriendRequestData)reqData).getFriendMail());
		
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		else if(friend == null)
			return new ErrorResponseData(ErrorType.FriendIsNotExist);
		else if(user.getId() == friend.getId())
			return new ErrorResponseData(ErrorType.BothUsersEquals);
		else
		{
			if(model.getDbManager().getContact(user.getId(), friend.getId()) == null)
			{
				model.getDbManager().addToDataBase(new Contact(user, friend));
				return new AddFriendResponseData(new UserData(friend.getFirstName(),friend.getLastName(), friend.getEmail(), "Image URL"));
			}
			else
				return new ErrorResponseData(ErrorType.AlreadyFriends);
		}
			
	}
	
	private User getUserFromDB(RequestData reqData)
	{
		return model.getDbManager().getUser(reqData.getUserEmail());
	}
	private void addProfilePicture(RequestData reqData)//TODO
	{
		
	}
	private ResponseData EventsList(RequestData reqData)
	{
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		LinkedList<EventData> eventsList = model.getDbManager().getEventsList(user.getId());
		if(eventsList == null)
			return new ErrorResponseData(ErrorType.UserHasNoEvents);
		return new EventsListResponseData(eventsList);
	}


	
	
	public void sendInvitesToUsers(Event event, ArrayList<Integer> users)
	{
		ArrayList<User> participants = new ArrayList<>();
		ArrayList<String> participantsNames = new ArrayList<>();
		users.forEach(u -> {
			User user = (User) model.getDbManager().get(u, DBEntityType.User);
			if(user != null)
			{
				participants.add(user);
				participantsNames.add(user.getEmail());
			}
		});
		participants.forEach(p -> {
			SocketIOClient sock = connections.get(p.getEmail());
			if(sock != null)
			{
				ClientHandler.sendToClient(sock, "Notification", new EventInvitationNotificationData(event.getId(),participantsNames,event.getTitle()));
			}
		});
	}
	
	@Override	
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	
	
}
