package DB;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity(name = "Credentials")
public class Credential implements IDBEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "UserId")
	private User user;
    @Column(name = "Credential")
	private String credntial;
	
    
	public Credential() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Credential(User user, String credntial) {
		super();
		this.user = user;
		this.credntial = credntial;
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


	public String getCredntial() {
		return credntial;
	}


	public void setCredntial(String credntial) {
		this.credntial = credntial;
	}


	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.user = ((Credential)other).user;
			this.credntial = ((Credential)other).credntial;
		}
	}

}
