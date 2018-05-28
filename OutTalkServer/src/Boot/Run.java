package Boot;


import MVC.*;

public class Run {

	public static void main(String[] args) throws InterruptedException {
		
		/*Test t = new Test();
		t.test("/Images/1.jpg");*/

		
		
		lunchServer(args);

		
	}
	
	public static void lunchServer(String[] args)
	{
		View view = new View();
		view.printToConsole("Server Wakes Up...Wait For Acknowledge");
		String ip = "localhost";
		int port = 8080;
		if(args.length == 2)
		{
			ip = args[0];
			port = Integer.parseInt(args[1]);
			//pathToResources = typeOfSource == 1 ? "C:\\Users\\Sahar Mizrahi\\Desktop\\Study\\FinalProject\\Run\\resources":"C:\\Users\\project06\\Desktop\\OutTalk\\resources";
		}
		Model model = Model.getInstance();
		Controller controller = new Controller(model,view,ip,port);
		controller.start();
	}

}
