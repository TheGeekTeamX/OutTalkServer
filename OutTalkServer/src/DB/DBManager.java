package DB;


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

import Enums.*;
import ResponsesEntitys.EventData;
import ResponsesEntitys.UserData;


public class DBManager {
	private static ReentrantLock lock;
	private static SessionFactory factory;
	private Session session;
	private static DBManager instance = null;
	
	//Protocols
	@SuppressWarnings("unchecked")
	public String getRelatedEventProtocol(int eventId)
	{
		startSession();
		ArrayList<Protocol> p = (ArrayList<Protocol>) session.createQuery(String.format("from Protocols where EventId = %d",eventId)).list();
		closeSession();
		return p != null ? (p.size() != 0 ? p.get(0).getProtocolURL() : "") : "";
	}
	
	//Event
	
	@SuppressWarnings("unchecked")
	public ArrayList<User> getPariticpants(int eventId)
	{
		startSession();
		ArrayList<UserEvent> list = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where EventId = %d and Answer = 1", eventId)).list();
		closeSession();
		if(list == null)
			return null;
		ArrayList<User> users = new ArrayList<>();
		list.forEach(ue -> {
			users.add(ue.getUser());
		});
		return users;
	}
	
	@SuppressWarnings("unchecked")
	public LinkedList<EventData> getEventsList(int userId)
	{
		startSession();
		ArrayList<UserEvent> userEventList = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where UserId = %d and Answer = 1" , userId)).list();
		LinkedList<EventData> eventsList = new LinkedList<>();
		userEventList.forEach(ue->{
			ArrayList<User> usersList = getPariticpants(ue.getEvent().getId());
			ArrayList<UserData> usersDataList = new ArrayList<>();
			usersList.forEach(u->{
				usersDataList.add(getUserDataFromDBUserEntity(u));
			});
			eventsList.add(getEventDataByEvent(ue.getEvent(), usersDataList));
		});
		
		closeSession();


		return eventsList;
	}
	
	public EventData getEventDataByEvent(Event e, List<UserData> udlist)
	{
		return new EventData(e.getId(),
				e.getTitle(),
				e.getDateCreated(),
				udlist,
				getRelatedEventProtocol(e.getId()),
				e.getAdmin().getEmail(),
				e.getDescription(),
				e.getIsFinished()== 1 ? false :true);
	}
	
	//User
	@SuppressWarnings("unchecked")
	public Contact getContact(int userId, int friendId)
	{
		startSession();
		ArrayList<Contact> list = (ArrayList<Contact>)session.createQuery(String.format("from Contacts where UserId = %d and FriendId = %d",userId,friendId)).list();
		closeSession();
		return list != null ? (list.size() != 0 ? list.get(0) : null) : null;

	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Contact> getContactsList(int userId)
	{
		startSession();
		ArrayList<Contact> list = (ArrayList<Contact>)session.createQuery(String.format("from Contacts where UserId = %d" , userId)).list();
		closeSession();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public User getUser(String email)
	{
		startSession();
		ArrayList<User> list = (ArrayList<User>)session.createQuery("from Users where Email like '%"+email+"%'").list();
		closeSession();
		return list != null ? (list.size() != 0 ? list.get(0) : null) : null;
	}
	
	@SuppressWarnings("unchecked")
	public Credential getCredential(int userId)
	{
		startSession();
		ArrayList<Credential> list = (ArrayList<Credential>)session.createQuery(String.format("from Credentials where UserId = %d", userId)).list();
		closeSession();
		return list != null ? list.get(0) : null;
	}
	
	public UserData getUserDataFromDBUserEntity(User user)
	{
		return new UserData(user.getFirstName(), user.getLastName(), user.getEmail(), getProfilePictureUrlByUserId(user.getId()), user.getPhoneNumber());
	}
	
	//UserEvent
	@SuppressWarnings("unchecked")
	public UserEvent getRelatedUserEvent(int userId,int eventId)
	{
		startSession();
		ArrayList<UserEvent> list = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where UserId = %d and EventId = %d",userId,eventId)).list();
		closeSession();
		return list == null ? null : (list.size() == 0 ? null : list.get(0));
	}

	@SuppressWarnings("unchecked")
	public ArrayList<UserEvent> getUnAnsweredInvites(int userId)
	{
		startSession();
		ArrayList<UserEvent> list = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where Answer = 0 and UserId = %d", userId)).list();
		closeSession();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<UserEvent> getUserEventByEventId(int eventId)
	{
		startSession();
		ArrayList<UserEvent> usersEvent = (ArrayList<UserEvent>)session.createQuery(String.format("from UserEvents where EventId = %d and Answer = 1", eventId)).list();
		closeSession();
		return usersEvent;
	}
	
	
	//Profile Pictures
	@SuppressWarnings("unchecked")
	public ProfilePicture getUserProfilePicture(int userId)
	{
		startSession();
		ArrayList<ProfilePicture> list = (ArrayList<ProfilePicture>)session.createQuery(String.format("from ProfilePictures where UserId = %d", userId)).list();
		closeSession();
		return list != null ? (list.size() != 0 ? list.get(0) : null) : null;
	}
	
	public String getProfilePictureUrlByUserId(int userId)
	{
		return (this.getUserProfilePicture(userId)).getProfilePictureUrl();
	}
	
	//Core
	private Transaction startSession()
	{
		lock.lock();
		session = factory.openSession();
		return session.beginTransaction();
	}
	
	private void closeSession()
	{
		session.close();
		lock.unlock();
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


	


	

}
