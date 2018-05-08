package DB;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "Users")
public class User implements IDBEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
	@Column(name = "Email")
	private String email;
	@Column(name = "FirstName")
	private String firstName;
	@Column(name = "LastName")
	private String lastName;
	@Column(name = "PhoneNumber")
	private String phoneNumber;
	@Column(name = "Country")
	private String country;
	
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}

	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public User(String email, String firstName, String lastName, String phoneNumber, String country) {
		super();

		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumber = phoneNumber;
		this.country = country;
	}
	@Override
	public String toString(){
		return "User [id="+id+",email="+email+",fullName="+firstName+",lastName="+lastName+",phoneNumber="+phoneNumber+",country="+country+"]";
	}
	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.email = ((User)other).email;
			this.firstName = ((User)other).firstName;
			this.lastName = ((User)other).lastName;
			this.phoneNumber = ((User)other).phoneNumber;
			this.country = ((User)other).country;
		}
		
	}
}
