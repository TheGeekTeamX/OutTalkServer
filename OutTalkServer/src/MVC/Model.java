package MVC;

import java.util.Observable;

public class Model extends Observable {
	private static Model instance;
	private DBManager dbManager;
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
	public void testDB()
	{
		dbManager.test();
	}
	public DBManager getDbManager() {
		return dbManager;
	}
	public Model(DBManager dbm) {
		super();
		// TODO Auto-generated constructor stub
		this.dbManager = dbm;
		
	}
	
	

}
