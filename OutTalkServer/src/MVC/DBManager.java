package MVC;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import DB.*;
import Enums.*;
import ResponsesEntitys.*;


public class DBManager {
	private static ReentrantLock lock;
	private static SessionFactory factory;
	private Session session;
	private static DBManager instance = null;
	
	public UserEvent getRelatedUserEvent(int userId,int eventId)
	{
		startSession();
		ArrayList<UserEvent> list = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where and UserId = {0} and EventId = {1}",userId,eventId)).list();
		closeSession();
		return list == null ? null : (list.size() == 0 ? null : list.get(0));
	}
	
	public ArrayList<UserEvent> getUnAnsweredInvites(int userId)
	{
		startSession();
		ArrayList<UserEvent> list = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where Answer = 0 and UserId = {0}", userId)).list();
		closeSession();
		return list;
	}
	
	public ArrayList<User> getPariticpants(int eventId)
	{
		startSession();
		ArrayList<UserEvent> list = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where EventId = {0} and Answer = 1", eventId)).list();
		if(list == null)
			return null;
		ArrayList<User> users = new ArrayList<>();
		list.forEach(ue -> {
			users.add(ue.getUser());
		});
		closeSession();
		return users;
	}
	
	private Protocol getProtocol(int eventId)
	{
		return null;
	}
	
	private Transaction startSession()
	{
		lock.lock();
		session = factory.openSession();
		return session.beginTransaction();
	}
	
	public Contact getContact(int userId, int friendId)
	{
		startSession();
		ArrayList<Contact> list = (ArrayList<Contact>)session.createQuery(String.format("from Contacts where UserId = {0} and FriendId = {1}",userId,friendId)).list();
		closeSession();
		return list != null ? (list.size() != 0 ? list.get(0) : null) : null;

	}
	
	private void closeSession()
	{
		session.close();
		lock.unlock();
	}
	
	public void test()
	{
		User u = getUser("Test123@gmail.com");
		System.out.println(u.toString());
	}
	
	public ProfilePicture getUserProfilePicture(int userId)
	{
		startSession();
		ArrayList<ProfilePicture> list = (ArrayList<ProfilePicture>)session.createQuery(String.format("from ProfilePictures where UserId = {0}", userId)).list();
		closeSession();
		return list != null ? (list.size() != 0 ? list.get(0) : null) : null;
	}
	
	public LinkedList<EventData> getEventsList(int userId)
	{
		startSession();
		
		ArrayList<UserEvent> userEventList = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where UserId = {0}" , userId)).list();
		ArrayList<UserData> usersDataList = new ArrayList<>();
		userEventList.forEach(ue->{
			usersDataList.add(getUserDataFromDBUserEntity(ue.getUser()));
		});
		LinkedList<EventData> events = new LinkedList<>();
		userEventList.forEach(ue -> {
			events.add(getEventDataByEvent(ue.getEvent(),usersDataList));
		});

		closeSession();
		return events;
	}
	
	public ArrayList<Contact> getContactsList(int userId)
	{
		startSession();
		ArrayList<Contact> list = (ArrayList<Contact>)session.createQuery(String.format("from Contacts where UserId = {0}" , userId)).list();
		closeSession();
		return list;
	}
	public User getUser(String email)
	{
		startSession();
		ArrayList<User> list = (ArrayList<User>)session.createQuery(String.format("from Users where Email like '%{0}%'", email)).list();
		closeSession();
		return list != null ? (list.size() != 0 ? list.get(0) : null) : null;
	}
	public Credential getCredential(int userId)
	{
		startSession();
		ArrayList<Credential> list = (ArrayList<Credential>)session.createQuery(String.format("from Credentials where UserId = {0}", userId)).list();
		closeSession();
		return list != null ? list.get(0) : null;
	}
	
	public IDBEntity get(int id, DBEntityType entityType)
	{
		Transaction tx = null;
		IDBEntity entity = null;
		try {
			tx = startSession();
			switch (entityType)
			{
			case User:
				entity= session.get(User.class, id);
				break;
			case UserEvent:
				entity = session.get(UserEvent.class, id);
				break;
			case Event:
				entity = session.get(Event.class, id);
				break;
			case Contact:
				entity = session.get(Contact.class, id);
				break;
			case Credential:
				entity = session.get(Credential.class, id);
				break;
			case ProfilePicture:
				entity = session.get(ProfilePicture.class, id);
				break;
			case Protocol:
				entity = session.get(Protocol.class, id);
				break;
			default:
				break;
			}
		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			return null;
		} finally {
			closeSession();
		}
		return entity;
	}
	public int addToDataBase(Object obj)
	{
		Transaction tx = null;
		int id=0;
		try {
			tx = startSession();
			id = (int)session.save(obj);
			tx.commit();
		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			return -1;
		} finally {
			closeSession();
		}
		return id;
	}
	
	public Boolean deleteFromDataBase(int id,DBEntityType entityType)
	{
		IDBEntity entity = get(id, entityType);
		Transaction tx = null;
		try {
			tx = startSession();
			if(entity != null)
			{
				session.delete(entity);
				tx.commit();
			}
			else
				return false;
		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			return false;
		} finally {
			closeSession();
		}
		return true;
	}
	
	public Boolean editInDataBase(int id,DBEntityType entityType,IDBEntity updatedObj)
	{
		IDBEntity entity = get(id, entityType);
		Transaction tx = null;
		try {
			tx = startSession();
			if(entity != null)
			{
				entity.update(updatedObj);
				session.update(entity);
				tx.commit();
			}
			else
				return false;
		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			return false;
		} finally {
			closeSession();
		}
		return true;
	}

	public static DBManager getInstance()
	{
		if (instance == null)
		{
			instance = new DBManager();
			instance.connectToDataBase();
		}
		return instance;
	}
	private void connectToDataBase()
	{
		Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
		factory = new Configuration().configure().buildSessionFactory();
		lock = new ReentrantLock();
	}

	public LinkedList<EventData> getRelatedPendingEvents(int userId)
	{
		ArrayList<UserEvent> pendingEvents = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where Answer = 0 and UserId = {0}", userId)).list();
		if(pendingEvents == null)
			return null;
		LinkedList<EventData> eventsData = new LinkedList<>();
		pendingEvents.forEach(pe -> {
			Event e = (Event) get(pe.getEvent().getId(), DBEntityType.Event);
			if(e !=  null)
			{
				LinkedList<String> participantsNames = new LinkedList<>();
				ArrayList<UserEvent> participants = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where EventId = {0}", e.getId())).list();
				ArrayList<UserData> udList = new ArrayList<>();
				participants.forEach(p->{
					udList.add(getUserDataFromDBUserEntity(p.getUser()));
				});
				participants.forEach(p -> {
					participantsNames.add(p.getUser().getEmail());
				});
				eventsData.add(getEventDataByEvent(pe.getEvent(), udList));
				
			}
		});
		return eventsData;
	}
	
	public String getProfilePictureUrlByUserId(int userId)
	{
		return (this.getUserProfilePicture(userId)).getProfilePictureUrl();
	}
	
	public EventData getEventDataByEvent(Event e, List<UserData> udlist)
	{
		return new EventData(e.getId(),
				e.getTitle(),
				e.getDateCreated(),
				udlist,
				"URL",
				((User)get(e.getId(),DBEntityType.User)).getEmail(),
				e.getDescription(),
				e.getIsFinished()== 1 ? false :true) ;
	}
	public UserData getUserDataFromDBUserEntity(User user)
	{
		return new UserData(user.getFirstName(), user.getLastName(), user.getEmail(), getProfilePictureUrlByUserId(user.getId()), user.getPhoneNumber());
	}
	
	public LinkedList<UserEvent> getUserEventByEventId(int eventId)
	{
		startSession();
		LinkedList<UserEvent> usersEvent = (LinkedList<UserEvent>)session.createQuery(String.format("from UserEvents where EventId = {0}", eventId)).list();
		closeSession();
		return usersEvent;
	}
}
