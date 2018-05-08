package MVC;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class View {
	private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private Date date;
	
	public View() {
		super();
		// TODO Auto-generated constructor stub
		date = new Date();
	}


	public void printToConsole(String log)
	{
		date = new Date();
		System.out.println("["+sdf.format(date)+"]"+" "+log);
	}

}
