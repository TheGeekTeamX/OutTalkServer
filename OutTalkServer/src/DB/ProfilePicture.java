package DB;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity(name = "ProfilePictures")
public class ProfilePicture implements IDBEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "UserId")
    private User user;
    @Column(name = "ProfilePictureURL")
	private String profilePictureUrl;
    
    
    
	public ProfilePicture() {
		super();
		// TODO Auto-generated constructor stub
	}



	public ProfilePicture(User user, String profilePictureUrl) {
		super();
		this.user = user;
		this.profilePictureUrl = profilePictureUrl;
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



	public String getProfilePictureUrl() {
		return profilePictureUrl;
	}



	public void setProfilePictureUrl(String profilePictureUrl) {
		this.profilePictureUrl = profilePictureUrl;
	}



	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.user = ((ProfilePicture)other).user;
			this.profilePictureUrl = ((ProfilePicture)other).profilePictureUrl;
		}
	}

}
