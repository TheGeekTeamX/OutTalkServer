package MVC;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

import javax.imageio.ImageIO;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import ClientHandler.*;
import DB.*;
import Enums.*;
import Notifications.EventCloseNotificationData;
import Notifications.EventInvitationNotificationData;
import Requests.*;
import Responses.*;
import ResponsesEntitys.*;
import Tools.BytesHandler;
public class Controller implements Observer,IController {
	private String url;
	private int port;
	private Controller instance;
	private Model model;
	private View view;
	private HashMap<String, SocketIOClient> connections;
	private SocketIOServer serverSock;
	private PausableThreadPoolExecutor executionPool;
	

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
			return ContactList(reqData);//checked
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
		case IsUserExistRequest:
			return IsUserExist(reqData);
		default:
			System.out.println("default");
			break;
		}
		return null;
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
	
	
	private ResponseData CloseEvent(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send CloseEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		Event event = (Event) model.getDbManager().get(((CloseEventRequestData)reqData).getEventId(), DBEntityType.Event);
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
	
	private ResponseData EventProtocol(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send EventProtocolRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		int eventId = ((EventProtocolRequestData)reqData).getEventID();
		
		return null;
	}
	
	private ResponseData PendingEvents(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send PendingEventsRequest");
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
		view.printToConsole(reqData.getUserEmail()+" Send CreateEventRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList<String> participantsEmail = (ArrayList<String>) ((CreateEventRequestData)reqData).getUsersEmails();
		int i=0;
		LinkedList<User> participants = new LinkedList<>();
		participantsEmail.forEach(pe -> {
			User u = model.getDbManager().getUser(pe);
			if(u!=null)
				participants.add(u);		
		});
		
		//create Event
		Event e = new Event(user,
				((CreateEventRequestData)reqData).getTitle(),
				new Date(Calendar.getInstance().getTime().getTime()),
				0,
				0,
				((CreateEventRequestData)reqData).getDescription());
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
	

	private ResponseData IsUserExist(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send IsUserExistRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		User otherUser=model.getDbManager().getUser(((IsUserExistRequestData)reqData).getEmail());
		if (otherUser == null)
			return new ErrorResponseData(ErrorType.FriendIsNotExist);
		return new IsUserExistResponseData(model.getDbManager().getUserDataFromDBUserEntity(otherUser));
	}
	
	private ResponseData UpdateProfilePicture(RequestData reqData)
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
		String pictureBytes = ((UpdateProfilePictureRequestData)reqData).getProfilePictureBytes();
		byte[] byteArr = Base64.getDecoder().decode(pictureBytes);
		
		return BytesHandler.SaveByteArrayInDestinationAsImage(byteArr, "jpg", ".\\src\\resources\\ProfilePictures\\"+url) ? 
				new BooleanResponseData(true) : 
					new ErrorResponseData(ErrorType.TechnicalError);
	}
	
	private ResponseData ProfilePicture(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send ProfilePictureRequest");
		User user = getUserFromDB(reqData);
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
		if (pp == null)
			return new ErrorResponseData(ErrorType.UserHasNoProfilePicture);
		else
			return new ProfilePictureResponseData(BytesHandler.FromImageToByteArray(new File(".\\src\\resources\\ProfilePictures\\"+user.getId()+".jpg"), "jpg"));		
	}
	
	
	public Model getModel() {
		return model;
	}


	
	private ResponseData CreateUser(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send CreateUserRequest");
		User user = getUserFromDB(reqData);
		if(user != null)
			return new ErrorResponseData(ErrorType.EmailAlreadyRegistered);
		User u = new User(reqData.getUserEmail(), ((CreateUserRequestData)reqData).getFirstName(),((CreateUserRequestData)reqData).getLastName(), ((CreateUserRequestData)reqData).getPhoneNumber(),((CreateUserRequestData)reqData).getCountry());
		u.setId(model.getDbManager().addToDataBase(u) );
		if(u.getId() < 0)
			return new ErrorResponseData(ErrorType.TechnicalError);
		addProfilePicture(reqData);//TODO
		model.getDbManager().addToDataBase(new Credential(u,((CreateUserRequestData)reqData).getCredential()));			
		return new BooleanResponseData(true);
	}

	
	private ResponseData ContactList(RequestData reqData)
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
	
	private ResponseData EditUser(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send EditUserRequest");
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
		view.printToConsole(reqData.getUserEmail()+" Send EditContactsListRequest");
		User user = model.getDbManager().getUser(reqData.getUserEmail());
		if(user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ArrayList <Contact> currentContactsList = model.getDbManager().getContactsList(user.getId());
		if (currentContactsList == null || currentContactsList.size() == 0)
			return new BooleanResponseData(false);
		LinkedList <String> newContactsList = ((EditContactsListRequestData)reqData).getUpdatedFriendsList();
		if (newContactsList.size() == 0)
			return new BooleanResponseData(false);
		currentContactsList.forEach(contact -> {
			if(!newContactsList.contains(""+contact.getFriend().getEmail()))
			{				
				model.getDbManager().deleteFromDataBase(contact.getId(), DBEntityType.Contact);
			}
		});
		
		return new BooleanResponseData(true);
	}
	
	private ResponseData ChangePassword(RequestData reqData)
	{
		view.printToConsole(reqData.getUserEmail()+" Send ChangePasswordRequest");
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
		view.printToConsole(reqData.getUserEmail()+" Send AddFriendRequest");
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
	private ResponseData EventsList(RequestData reqData)
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
	
	private byte[] getBytesOfFile(File f)
	{
		if(f != null)
		{
			long length = f.length();
			try {
				return Files.readAllBytes(f.toPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new byte[0];
			}
		}
		return new byte[0];
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
	
	
}
