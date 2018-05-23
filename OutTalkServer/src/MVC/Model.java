package MVC;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;

import DB.Contact;
import DB.Credential;
import DB.DBManager;
import DB.Event;
import DB.ProfilePicture;
import DB.User;
import DB.UserEvent;
import Enums.DBEntityType;
import Enums.ErrorType;
import Requests.AddFriendRequestData;
import Requests.ChangePasswordRequestData;
import Requests.CloseEventRequestData;
import Requests.ConfirmEventRequestData;
import Requests.ContactsListRequestData;
import Requests.CreateEventRequestData;
import Requests.CreateUserRequestData;
import Requests.DeclineEventRequestData;
import Requests.EditContactsListRequestData;
import Requests.EditUserRequestData;
import Requests.EventProtocolRequestData;
import Requests.EventsListRequestData;
import Requests.IsUserExistRequestData;
import Requests.LeaveEventRequestData;
import Requests.LoginRequestData;
import Requests.ProfilePictureRequestData;
import Requests.RequestData;
import Requests.UpdateProfilePictureRequestData;
import Responses.AddFriendResponseData;
import Responses.BooleanResponseData;
import Responses.ContactsListResponseData;
import Responses.ErrorResponseData;
import Responses.EventProtocolResponseData;
import Responses.EventsListResponseData;
import Responses.IsUserExistResponseData;
import Responses.ProfilePictureResponseData;
import Responses.ResponseData;
import ResponsesEntitys.EventData;
import ResponsesEntitys.ProtocolLine;
import ResponsesEntitys.UserData;
import Tools.BytesHandler;

public class Model extends Observable {
	private static Model instance;
	private String pathToResources;
	private DBManager dbManager;
	private SocketHandler socketHandler;
	
	
	
	public SocketHandler getSocketHandler() {
		return socketHandler;
	}

	public void setSocketHandler(SocketHandler socketHandler) {
		this.socketHandler = socketHandler;
	}

	public EventData getEventData(Event e,List<UserData> udList)
	{
		return dbManager.getEventDataByEvent(e, udList);
	}
	
	public User getUser(String email)
	{
		return dbManager.getUser(email);
	}
	
	public String getPathToResources() {
		return pathToResources;
	}
	public void setPathToResources(String pathToResources) {
		this.pathToResources = pathToResources;
	}
	public static Model getInstance()
	{
		if(instance == null)
		{
			instance = new Model(DBManager.getInstance());
			instance.init();
		}
		return instance;
	}
	private void init()
	{
		dbManager = DBManager.getInstance();
	}

	public DBManager getDbManager() {
		return dbManager;
	}
	public Model(DBManager dbm) {
		super();
		// TODO Auto-generated constructor stub
		this.dbManager = dbm;
	}
	


	public ResponseData EventProtocol(EventProtocolRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send EventProtocolRequest");
		String protocolName = dbManager.getRelatedEventProtocol(reqData.getEventID());
		if (protocolName == null || protocolName.equals(""))
			return new ErrorResponseData(ErrorType.ProtocolIsNotExist);
		ArrayList<ProtocolLine> protocol = BytesHandler
				.fromTextFileToProtocol(pathToResources + "\\Protocols\\" + protocolName);
		return protocol != null ? new EventProtocolResponseData(reqData.getEventID(), protocol)
				: new ErrorResponseData(ErrorType.TechnicalError);
	}

	public ResponseData CreateEvent(CreateEventRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send CreateEventRequest");
		ArrayList<String> participantsEmail = (ArrayList<String>) (reqData.getUsersEmails());
		LinkedList<User> participants = new LinkedList<>();
		participantsEmail.forEach(pe -> {
			User u = dbManager.getUser(pe);
			if (u != null)
				participants.add(u);
		});
		participants.add(user);
		// create Event
		Event e = new Event(user, reqData.getTitle(), new Date(Calendar.getInstance().getTime().getTime()).toString(),
				0, 0, reqData.getDescription());
		if (!(dbManager.addToDataBase(e) > 0))
			return new ErrorResponseData(ErrorType.TechnicalError);
		// create UserEvent
		LinkedList<UserData> participantsUserData = getParticipantsUserData(e);
		participants.forEach(p -> {
			int answer = isEmailsEquals(p.getEmail(), user.getEmail()) ? 1 : 0;
			dbManager.addToDataBase(new UserEvent(p, e, answer));
		});
		// send invites
		socketHandler.sendEventInventationToUsers(getEventData(e, participantsUserData), participantsUserData);
		return new BooleanResponseData(true);
	}

	private LinkedList<UserData> getParticipantsUserData(Event e)
	{
		LinkedList<UserData> list = new LinkedList<>();
		ArrayList<UserEvent> ueList = dbManager.getUserEventByEventId(e.getId());
		ueList.forEach(uel -> {
			list.add(dbManager.getUserDataFromDBUserEntity(uel.getUser()));
		});
		return list;
	}
	
	public ResponseData IsUserExist(IsUserExistRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send IsUserExistRequest");
		User otherUser = dbManager.getUser(reqData.getEmail());
		if (otherUser == null)
			return new ErrorResponseData(ErrorType.FriendIsNotExist);
		return new IsUserExistResponseData(dbManager.getUserDataFromDBUserEntity(otherUser));
	}
	



	public ResponseData ProfilePicture(ProfilePictureRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send ProfilePictureRequest");
		ProfilePicture pp = dbManager.getUserProfilePicture(user.getId());
		if (pp == null)
			return new ErrorResponseData(ErrorType.UserHasNoProfilePicture);
		else
		{
			System.out.println(pathToResources + "\\ProfilePictures\\" + user.getId() + ".jpg");
			byte[] arr = BytesHandler.FromImageToByteArray(pathToResources + "ProfilePictures\\" + user.getId() + ".jpg", "jpg");
			ProfilePictureResponseData rd = new ProfilePictureResponseData(arr);
			return rd;
		}
	}
	
	public ResponseData CreateUser(CreateUserRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send CreateUserRequest");
		User u = new User(reqData.getUserEmail(), reqData.getFirstName(), reqData.getLastName(),reqData.getPhoneNumber(), reqData.getCountry());
		u.setId(dbManager.addToDataBase(u));
		if (u.getId() < 0)
			return new ErrorResponseData(ErrorType.TechnicalError);
		addProfilePicture(reqData);// TODO
		dbManager.addToDataBase(new Credential(u, reqData.getCredential()));
		return new BooleanResponseData(true);
	}

	public ResponseData ContactList(ContactsListRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send ContactListRequest");
		ArrayList<Contact> contactsList = dbManager.getContactsList(user.getId());
		LinkedList<UserData> list = new LinkedList<>();
		contactsList.forEach(c -> {
			User u = (User) dbManager.get(c.getFriend().getId(), DBEntityType.User);
			if (u != null) {
				list.add(dbManager.getUserDataFromDBUserEntity(u));
			}

		});
		return new ContactsListResponseData(list);
	}

	public ResponseData EditUser(EditUserRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send EditUserRequest");
		user.setCountry(reqData.getCountry());
		user.setPhoneNumber(reqData.getPhoneNumber());
		user.setFirstName(reqData.getFirstName());
		user.setLastName(reqData.getLastName());
		return new BooleanResponseData(dbManager.editInDataBase(user.getId(), DBEntityType.User, user));
	}

	public ResponseData EditContactsList(EditContactsListRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send EditContactsListRequest");
		ArrayList<Contact> currentContactsList = dbManager.getContactsList(user.getId());
		if (currentContactsList == null || currentContactsList.size() == 0)
			return new BooleanResponseData(false);
		if (reqData.getUpdatedFriendsList().size() == 0)
			return new BooleanResponseData(false);
		currentContactsList.forEach(contact -> {
			if (!reqData.getUpdatedFriendsList().contains("" + contact.getFriend().getEmail())) {
				dbManager.deleteFromDataBase(contact.getId(), DBEntityType.Contact);
			}
		});

		return new BooleanResponseData(true);
	}

	public ResponseData ChangePassword(ChangePasswordRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send ChangePasswordRequest");
		Credential credential = dbManager.getCredential(user.getId());
		if (credential == null)
			return new ErrorResponseData(ErrorType.TechnicalError);

		if (!credential.getCredntial().equals(reqData.getOldPassword()))
			return new ErrorResponseData(ErrorType.WrongPreviousPassword);

		if (reqData.getNewPassword().equals(reqData.getOldPassword()))
			return new ErrorResponseData(ErrorType.BothPasswordsEquals);
		credential.setCredntial(reqData.getNewPassword());
		Boolean res = dbManager.editInDataBase(credential.getId(), DBEntityType.Credential, credential);
		return res ? new BooleanResponseData(true) : new ErrorResponseData(ErrorType.TechnicalError);
	}

	public ResponseData AddFriend(AddFriendRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send AddFriendRequest");
		User friend = dbManager.getUser(reqData.getFriendMail());
		if (friend == null)
			return new ErrorResponseData(ErrorType.FriendIsNotExist);
		else if (user.getId() == friend.getId())
			return new ErrorResponseData(ErrorType.BothUsersEquals);
		else {
			if (dbManager.getContact(user.getId(), friend.getId()) == null) {
				dbManager.addToDataBase(new Contact(user, friend));
				return new AddFriendResponseData(dbManager.getUserDataFromDBUserEntity(friend));
			} else
				return new ErrorResponseData(ErrorType.AlreadyFriends);
		}

	}

	public ResponseData EventsList(EventsListRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send EventListRequest");
		LinkedList<EventData> eventsList = dbManager.getEventsList(user.getId());
		if (eventsList == null)
			return new ErrorResponseData(ErrorType.UserHasNoEvents);
		return new EventsListResponseData(eventsList);
	}
	
	public ResponseData DeclineEvent(DeclineEventRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send DeclineEventRequest");
		UserEvent ue = dbManager.getRelatedUserEvent(user.getId(), reqData.getEventId());
		if (ue == null)
			return new ErrorResponseData(ErrorType.NoPendingEvents);
		if (ue.getAnswer() == 0) {
			ue.setAnswer(2);
			dbManager.editInDataBase(ue.getId(), DBEntityType.UserEvent, ue);
		}
		return new BooleanResponseData(true);
	}

	public ResponseData LeaveEvent(LeaveEventRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send LeaveEventRequest");
		Event e = (Event) dbManager.get(reqData.getEventId(), DBEntityType.Event);
		if (e == null)
			return new ErrorResponseData(ErrorType.TechnicalError);
		if (e.getIsFinished() == 0) {
			UserEvent ue = dbManager.getRelatedUserEvent(user.getId(), reqData.getEventId());
			if (ue == null)
				return new ErrorResponseData(ErrorType.NoPendingEvents);
			else if (ue.getAnswer() == 1)
			{
				LinkedList<UserData> list = getParticipantsUserData(e);
				UserData ud = dbManager.getUserDataFromDBUserEntity(user);
				if(list.contains(ud))
					list.remove(ud);
				socketHandler.sendUserEventNotification(e,list,user,false);
			}
		}
		return new BooleanResponseData(true);
	}

	public ResponseData JoinEvent(ConfirmEventRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send ConfirmEventRequest");
		UserEvent ue = dbManager.getRelatedUserEvent(user.getId(), reqData.getEventId());
		if (ue == null)
			return new ErrorResponseData(ErrorType.NoPendingEvents);
		if (ue.getAnswer() == 0 || ue.getAnswer() == 2) {
			ue.setAnswer(1);
			dbManager.editInDataBase(ue.getId(), DBEntityType.UserEvent, ue);
		}
		LinkedList<UserData> list = getParticipantsUserData(ue.getEvent());
		UserData ud = dbManager.getUserDataFromDBUserEntity(user);
		if(list.contains(ud))
			list.remove(ud);
		socketHandler.sendUserEventNotification(ue.getEvent(),list,user,true);
		return new BooleanResponseData(true);
	}
	
	public ResponseData CloseEvent(CloseEventRequestData reqData,User user) {
		notifyObservers(reqData.getUserEmail() + " Send CloseEventRequest");
		Event event = (Event) dbManager.get(reqData.getEventId(), DBEntityType.Event);
		if (event == null)
			return new ErrorResponseData(ErrorType.EventIsNotExist);
		if (event.getAdmin().getId() == user.getId()) {
			event.setIsFinished(1);
			ArrayList<UserEvent> usersEvent = dbManager.getUserEventByEventId(event.getId());
			usersEvent.forEach(ue -> {
				if (ue.getAnswer() == 0)// didn't answer yet
				{
					ue.setAnswer(2);
					dbManager.editInDataBase(ue.getId(), DBEntityType.UserEvent, ue);
				}
			});
			if (dbManager.editInDataBase(event.getId(), DBEntityType.Event, event))
				return new ErrorResponseData(ErrorType.TechnicalError);
			else {
				// Get Byte array from req and send to GoogleService-TODO
				ArrayList<User> l = dbManager.getPariticpants(event.getId());
				LinkedList<UserData> list = getParticipantsUserData(event);
				UserData ud = dbManager.getUserDataFromDBUserEntity(user);
				if(list.contains(ud))
					list.remove(ud);
				socketHandler.sendEventCloseNotificationToUsers(event,list);
				return new BooleanResponseData(true);
			}
		} else
			return new ErrorResponseData(ErrorType.UserIsNotAdmin);
	}

	public static boolean isEmailsEquals(String mail1, String mail2) {
		return mail1.toLowerCase().equals(mail2.toLowerCase());
	}

	//TODO
	
	
	private void addProfilePicture(RequestData reqData)// TODO
	{

	}

	public ResponseData UpdateProfilePicture(UpdateProfilePictureRequestData reqData,User user) {
		return null;
		/*view.printToConsole(reqData.getUserEmail() + " Send UpdateProfilePictureRequest");
		User user = getUserFromDB(reqData);
		if (user == null)
			return new ErrorResponseData(ErrorType.UserIsNotExist);
		ProfilePicture pp = model.getDbManager().getUserProfilePicture(user.getId());
		String url = "" + user.getId() + ".jpg";
		if (pp == null)// Need to add to DB
		{
			pp = new ProfilePicture(user, url);
			model.getDbManager().addToDataBase(pp);
		}
		byte[] byteArr = Base64.getDecoder().decode(reqData.getProfilePictureBytes());

		return BytesHandler.SaveByteArrayInDestinationAsImage(byteArr, "jpg",
				pathToResources + "\\ProfilePictures\\" + url) ? new BooleanResponseData(true)
						: new ErrorResponseData(ErrorType.TechnicalError);*/
	}

}
