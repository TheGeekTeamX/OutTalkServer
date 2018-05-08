package DB;

import javax.persistence.*;
@Entity(name = "Contacts")
public class Contact implements IDBEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "UserId")
    private User user;
    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "FriendId")
    private User friend;
    
    
	public Contact() {
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
	public User getFriend() {
		return friend;
	}
	public void setFriendId(User friend) {
		this.friend = friend;
	}
	public Contact(User user, User friend) {
		super();
		this.user = user;
		this.friend = friend;
	}
	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.user = ((Contact)other).user;
			this.friend = ((Contact)other).friend;
		}
		
	}
	
    
}
