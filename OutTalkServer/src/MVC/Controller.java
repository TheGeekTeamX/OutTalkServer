package MVC;


import java.sql.Date;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import Notifications.EventCloseNotificationData;
import Notifications.EventInvitationNotificationData;
import Notifications.UserJoinEventNotification;
import Notifications.UserLeaveEventNotification;
import Requests.*;
import Responses.*;
import ResponsesEntitys.*;
import Tools.BytesHandler;


public class Controller implements Observer {
	private String pathToResources = ".\\src\\resources";
	private String url;
	private int port;
	private Controller instance;
	private Model model;
	private View view;
	private HashMap<String, SocketIOClient> connections;
	private SocketIOServer serverSock;
	private PausableThreadPoolExecutor executionPool;
	

	
	public ResponseData execute(String data) {
		// TODO Auto-generated method stub
		RequestData reqData = ClientHandler.getObjectFromString(data, RequestData.class);
		switch (reqData.getType()) {
		case AddFriendRequest:
			return AddFriend(ClientHandler.getObjectFromString(data, AddFriendRequestData.class));//checked
		case ChangePasswordRequest:
			return ChangePassword(ClientHandler.getObjectFromString(data, ChangePasswordRequestData.class));//checked
		case CreateUserRequest:
			return CreateUser(ClientHandler.getObjectFromString(data, CreateUserRequestData.class));
		case EditContactsListRequest:
			return EditContactsList(ClientHandler.getObjectFromString(data, EditContactsListRequestData.class));
		case EditUserRequest:
			return EditUser(ClientHandler.getObjectFromString(data, EditUserRequestData.class));
		case EventsListRequest:
			return EventsList(ClientHandler.getObjectFromString(data, EventsListRequestData.class));
		case ContactsListRequest:
			return ContactList(ClientHandler.getObjectFromString(data, ContactsListRequestData.class));//checked
		case CloseEventRequest:
			return CloseEvent(ClientHandler.getObjectFromString(data, CloseEventRequestData.class));
		case CreateEventRequest:
			return CreateEvent(ClientHandler.getObjectFromString(data, CreateEventRequestData.class));
		case EventProtocolRequest:
			return EventProtocol(ClientHandler.getObjectFromString(data, EventProtocolRequestData.class));//TODO
		case ProfilePictureRequest:
			return ProfilePicture(ClientHandler.getObjectFromString(data, ProfilePictureRequestData.class));
		case UpdateProfilePictureRequest:
			return UpdateProfilePicture(ClientHandler.getObjectFromString(data, UpdateProfilePictureRequestData.class));
		case IsUserExistRequest:
			return IsUserExist(ClientHandler.getObjectFromString(data, IsUserExistRequestData.class));
		case JoinEvent:
			return JoinEvent(ClientHandler.getObjectFromString(data, JoinEventRequestData.class));
		case DeclineEvent:
			return DeclineEvent(ClientHandler.getObjectFromString(data, DeclineEventRequestData.class));
		case LeaveEvent:
			return LeaveEvent(ClientHandler.getObjectFromString(data, JoinEventRequestData.class));
		default:
			System.out.println("default");
			break;
		}
		return null;
	}
	
	private ResponseData DeclineEvent(DeclineEventRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send DeclineEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		UserEvent ue = model.getDbManager().getRelatedUserEvent(user.getId(), reqData.getEventId());
		if(ue == null)
			return new ErrorResponseData(ErrorType.NoPendingEvents);
		if(ue.getAnswer() == 0)
		{
			ue.setAnswer(2);
			model.getDbManager().editInDataBase(ue.getId(), DBEntityType.UserEvent,ue);
		}
		return new BooleanResponseData(true);
	}
	
	private ResponseData LeaveEvent(JoinEventRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send LeaveEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		Event e = (Event) model.getDbManager().get(reqData.getEventId(), DBEntityType.Event);
		if(e == null)
			return new ErrorResponseData(ErrorType.TechnicalError);
		if(e.getIsFinished() == 0)
		{
			UserEvent ue = model.getDbManager().getRelatedUserEvent(user.getId(), reqData.getEventId());			
			if(ue == null)
				return new ErrorResponseData(ErrorType.NoPendingEvents);
			if(ue.getAnswer() == 1)
				sendUserEventNotification(ue.getEvent(), user,false);
		}
		return new BooleanResponseData(true);
	}
	
	private ResponseData JoinEvent(JoinEventRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send JoinEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		UserEvent ue = model.getDbManager().getRelatedUserEvent(user.getId(), reqData.getEventId());
		if(ue == null)
			return new ErrorResponseData(ErrorType.NoPendingEvents);
		if(ue.getAnswer() == 0 || ue.getAnswer() == 2)
		{
			ue.setAnswer(1);
			model.getDbManager().editInDataBase(ue.getId(), DBEntityType.UserEvent,ue);
		}
		sendUserEventNotification(ue.getEvent(), user,true);
		return new BooleanResponseData(true);
	}
	
	
	private void checkIfUserHasInvites(User u)
	{
		ArrayList<UserEvent> unAnsweredInvites = model.getDbManager().getUnAnsweredInvites(u.getId());
		if(unAnsweredInvites != null && unAnsweredInvites.size()!=0)
		{			
			unAnsweredInvites.forEach(i -> {
				ArrayList<UserData> l = new ArrayList<>();
				l.add(model.getDbManager().getUserDataFromDBUserEntity(i.getUser()));
				sendEventInventationToUsers(i.getEvent(), l);
			});
		}
	}
	
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
									ClientHandler.sendToClient(client, "Response", 
											new LoginResponseData(user.getId(), user.getFirstName(), user.getLastName(), user.getPhoneNumber(), model.getDbManager().getProfilePictureUrlByUserId(user.getId())));
									view.printToConsole(credential.getUser().getEmail()+" Is Connected");
									connections.put(user.getEmail(), client);
									checkIfUserHasInvites(user);
								}
								else
									ClientHandler.sendToClient(client, "Response", new ErrorResponseData(ErrorType.IncorrectCredentials));
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
	
	
	private ResponseData CloseEvent(CloseEventRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send CloseEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		Event event = (Event) model.getDbManager().get(reqData.getEventId(), DBEntityType.Event);
		if(event == null)
			return new ErrorResponseData(ErrorType.EventIsNotExist);
		if(event.getAdmin().getId() == user.getId())
		{			
			event.setIsFinished(1);
			LinkedList<UserEvent> usersEvent = model.getDbManager().getUserEventByEventId(event.getId());
			usersEvent.forEach(ue->{
				if(ue.getAnswer() == 0)//didn't answer yet
				{
					ue.setAnswer(2);
					model.getDbManager().editInDataBase(ue.getId(), DBEntityType.UserEvent, ue);
				}
			});
			if(model.getDbManager().editInDataBase(event.getId(), DBEntityType.Event, event))
				return new ErrorResponseData(ErrorType.TechnicalError);
			else
			{			
				//Get Byte array from req and send to GoogleService-TODO
				ArrayList<User> l = model.getDbManager().getPariticpants(event.getId());
				sendEventCloseNotificationToUsers(event,l);
				return new BooleanResponseData(true);
			}
		}
		else
			return new ErrorResponseData(ErrorType.UserIsNotAdmin);
	}
	
	private ResponseData EventProtocol(EventProtocolRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send EventProtocolRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		String protocolName = model.getDbManager().getRelatedEventProtocol(reqData.getEventID());
		if(protocolName == null || protocolName.equals(""))
			return new ErrorResponseData(ErrorType.ProtocolIsNotExist);
		ArrayList<ProtocolLine> protocol = BytesHandler.fromTextFileToProtocol(pathToResources+"\\Protocols\\"+protocolName);
		return protocol != null ? new EventProtocolResponseData(reqData.getEventID(), protocol) : new ErrorResponseData(ErrorType.TechnicalError);
	}
	

	
	private ResponseData CreateEvent(CreateEventRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send CreateEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList<String> participantsEmail = (ArrayList<String>) (reqData.getUsersEmails());
		LinkedList<User> participants = new LinkedList<>();
		participantsEmail.forEach(pe -> {
			User u = model.getDbManager().getUser(pe);
			if(u!=null)
				participants.add(u);		
		});
		
		//create Event
		Event e = new Event(user,reqData.getTitle(),new Date(Calendar.getInstance().getTime().getTime()),0,0,reqData.getDescription());
		if (!(model.getDbManager().addToDataBase(e) > 0))
			return new ErrorResponseData(ErrorType.TechnicalError);	
		//create UserEvent
		LinkedList<UserData> l = new LinkedList<>();
		participants.forEach(p -> {
			int answer = p.getEmail().equals(user.getEmail()) ? 1 :0 ;
			model.getDbManager().addToDataBase(new UserEvent(p,e,answer));
			l.add(model.getDbManager().getUserDataFromDBUserEntity(p));
		});
		//send invites
		sendEventInventationToUsers(e, l);
		return new BooleanResponseData(true);
	}
	

	private ResponseData IsUserExist(IsUserExistRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send IsUserExistRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		User otherUser=model.getDbManager().getUser(reqData.getEmail());
		if (otherUser == null)
			return new ErrorResponseData(ErrorType.FriendIsNotExist);
		return new IsUserExistResponseData(model.getDbManager().getUserDataFromDBUserEntity(otherUser));
	}
	
	private ResponseData UpdateProfilePicture(UpdateProfilePictureRequestData reqData)
	{

		view.printToConsole(reqData.getUserEmail()+" Send UpdateProfilePictureRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
		String url = ""+user.getId()+".jpg";
		if (pp == null)//Need to add to DB
		{
			pp = new ProfilePicture(user, url);
			model.getDbManager().addToDataBase(pp);
		}
		byte[] byteArr = Base64.getDecoder().decode(reqData.getProfilePictureBytes());
		
		return BytesHandler.SaveByteArrayInDestinationAsImage(byteArr, "jpg", pathToResources+"\\ProfilePictures\\"+url) ? 
				new BooleanResponseData(true) : 
					new ErrorResponseData(ErrorType.TechnicalError);
	}
	
	private ResponseData ProfilePicture(ProfilePictureRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send ProfilePictureRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
		if (pp == null)
			return new ErrorResponseData(ErrorType.UserHasNoProfilePicture);
		else
			return new ProfilePictureResponseData(BytesHandler.FromImageToByteArray(pathToResources+"\\ProfilePictures\\"+user.getId()+".jpg", "jpg"));		
	}
	
	
	public Model getModel() {
		return model;
	}


	
	private ResponseData CreateUser(CreateUserRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send CreateUserRequest");
		User user = getUserFromDB(reqData);
		if(user != null)
			return new ErrorResponseData(ErrorType.EmailAlreadyRegistered);
		User u = new User(reqData.getUserEmail(), reqData.getFirstName(),reqData.getLastName(), reqData.getPhoneNumber(),reqData.getCountry());
		u.setId(model.getDbManager().addToDataBase(u) );
		if(u.getId() < 0)
			return new ErrorResponseData(ErrorType.TechnicalError);
		addProfilePicture(reqData);//TODO
		model.getDbManager().addToDataBase(new Credential(u,reqData.getCredential()));			
		return new BooleanResponseData(true);
	}

	
	private ResponseData ContactList(ContactsListRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send ContactListRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList <Contact> contactsList = model.getDbManager().getContactsList(user.getId());
		LinkedList<UserData> list = new LinkedList<>();
		contactsList.forEach(c -> {
			User u = (User) model.getDbManager().get(c.getFriend().getId(), DBEntityType.User);
			if(u!=null)
			{
				list.add(model.getDbManager().getUserDataFromDBUserEntity(u));
			}
			
		});
		return new ContactsListResponseData(list);
	}
	
	private ResponseData EditUser(EditUserRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send EditUserRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		user.setCountry(reqData.getCountry());
		user.setPhoneNumber(reqData.getPhoneNumber());
		user.setFirstName(reqData.getFirstName());
		user.setLastName(reqData.getLastName());
		return new BooleanResponseData(model.getDbManager().editInDataBase(user.getId(), DBEntityType.User, user));
	}
	
	private ResponseData EditContactsList(EditContactsListRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send EditContactsListRequest");
		User user = model.getDbManager().getUser(reqData.getUserEmail());
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList <Contact> currentContactsList = model.getDbManager().getContactsList(user.getId());
		if (currentContactsList == null || currentContactsList.size() == 0)
			return new BooleanResponseData(false);
		if (reqData.getUpdatedFriendsList().size() == 0)
			return new BooleanResponseData(false);
		currentContactsList.forEach(contact -> {
			if(!reqData.getUpdatedFriendsList().contains(""+contact.getFriend().getEmail()))
			{				
				model.getDbManager().deleteFromDataBase(contact.getId(), DBEntityType.Contact);
			}
		});
		
		return new BooleanResponseData(true);
	}
	
	private ResponseData ChangePassword(ChangePasswordRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send ChangePasswordRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		Credential credential = model.getDbManager().getCredential(user.getId());
		if(credential == null)
			return new ErrorResponseData(ErrorType.TechnicalError);

		if (!credential.getCredntial().equals(reqData.getOldPassword()))
			return new ErrorResponseData(ErrorType.WrongPreviousPassword);

		if (reqData.getNewPassword().equals(reqData.getOldPassword()))
			return new ErrorResponseData(ErrorType.BothPasswordsEquals);
		credential.setCredntial(reqData.getNewPassword());
		Boolean res =model.getDbManager().editInDataBase(credential.getId(), DBEntityType.Credential, credential);
		return res ? new BooleanResponseData(true) : new ErrorResponseData(ErrorType.TechnicalError);
	}
	
	private ResponseData AddFriend(AddFriendRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send AddFriendRequest");
		User user = getUserFromDB(reqData);
		User friend = model.getDbManager().getUser(reqData.getFriendMail());
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
				return new AddFriendResponseData(model.getDbManager().getUserDataFromDBUserEntity(friend));
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
	private ResponseData EventsList(EventsListRequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send EventListRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		LinkedList<EventData> eventsList = model.getDbManager().getEventsList(user.getId());
		if(eventsList == null)
			return new ErrorResponseData(ErrorType.UserHasNoEvents);
		return new EventsListResponseData(eventsList);
	}

	@Override	
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	

	
	private void sendEventInventationToUsers(Event e,List<UserData> participants)
	{
		participants.forEach(p->{			
			SocketIOClient sock = connections.get(p.getEmail());
			if(sock != null)
			{
				ClientHandler.sendToClient(sock, "Notification", new EventInvitationNotificationData(model.getDbManager().getEventDataByEvent(e, participants)));
			}
		});
	}
	private void sendEventCloseNotificationToUsers(Event e,List<User> participants)
	{
		participants.forEach(p->{			
			SocketIOClient sock = connections.get(p.getEmail());
			if(sock != null)
			{
				ClientHandler.sendToClient(sock, "Notification", new EventCloseNotificationData(e.getId()));
			}
		});
	}
	

	private void sendUserEventNotification(Event event,User user,Boolean isJoin)
	{
		ArrayList<User> list = model.getDbManager().getPariticpants(event.getId());
		list.forEach(u -> {
			SocketIOClient sock = connections.get(u.getEmail());
			if(sock != null)
			{
				ClientHandler.sendToClient(sock, "Notification",isJoin ? new UserJoinEventNotification(event.getId(), user.getId()) : new UserLeaveEventNotification(event.getId(),user.getId()));
			}
		});
	}
	
}
