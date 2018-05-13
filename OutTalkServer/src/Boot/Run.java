package Boot;

import MVC.*;

public class Run {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		View view = new View();
		view.printToConsole("Server Wakes Up...Wait For Acknowledge");
		String ip = "localhost";
		int port = 8080;
		if(args.length == 2)
		{
			ip = args[0];
			port = Integer.parseInt(args[1]);
		}
		Model model = Model.getInstance();
		Controller controller = new Controller(model,view,ip,port);
		controller.start();
		
	}

}
