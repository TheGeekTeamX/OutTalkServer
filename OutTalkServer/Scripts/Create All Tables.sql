create table Users(
	Id integer identity(1,1) primary key,
	Email nvarchar(50) not null,
	FirstName nvarchar (50) not null,
	LastName nvarchar (50) not null,
	PhoneNumber nvarchar(50) not null,
	Country nvarchar(20) not null,
)

create table Events(
	Id integer identity(1,1) primary key,
	AdminId integer not null,
	Title nvarchar(50) not null,
	DateCreated date not null,
	IsFinished bit not null,
	IsConverted bit not null,
	Description nvarchar(199) not null,
	foreign key (AdminId) references Users(Id)	
)


create table Contacts(
	Id int identity(1,1) primary key,
	UserId int not null,
	FriendId int not null,
	foreign key (UserId) references Users(Id),
	foreign key (FriendId) references Users(Id)	
)


create table UserEvents(
	Id integer identity(1,1) primary key,
	UserId int not null,
	EventId int not null,
	Answer int not null,
	foreign key (UserId) references Users(Id),
	foreign key (EventId) references Events(Id)	
)

create table ProfilePictures(
	Id integer identity(1,1) primary key,
	UserId int not null,
	ProfilePictureURL nvarchar(50) not null,
	foreign key (UserId) references Users(Id),
)

create table Credentials(
	Id integer identity(1,1) primary key,
	UserId int not null,
	Credential nvarchar(50) not null,
	foreign key (UserId) references Users(Id),
)

create table Protocols(
	Id integer identity(1,1) primary key,
	EventId int not null,
	ProtocolURL nvarchar(50) not null,
	foreign key (EventId) references Events(EventId),
)
