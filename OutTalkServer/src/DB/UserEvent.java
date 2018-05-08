package DB;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;


@Entity(name = "UserEvents")

public class UserEvent implements IDBEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "UserId")
    private User user;
    @OneToOne(targetEntity = Event.class)
    @JoinColumn(name = "EventId")
    private Event event;
	@Column(name="IsAccepted")
	private int isAccepted;
    
    
	public int getIsAccepted() {
		return isAccepted;
	}
	public void setIsAccepted(int isAccepted) {
		this.isAccepted = isAccepted;
	}
	public UserEvent() {
		super();
		// TODO Auto-generated constructor stub
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public UserEvent(User user, Event event, int isAccepted) {
		super();
		this.user = user;
		this.event = event;
		this.isAccepted = isAccepted;
	}
	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.user = ((UserEvent)other).user;
			this.event = ((UserEvent)other).event;
			this.isAccepted = ((UserEvent)other).isAccepted;
		}
	}
    
	
}


