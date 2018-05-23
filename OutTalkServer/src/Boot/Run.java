package Boot;


import MVC.*;

public class Run {

	public static void main(String[] args) throws InterruptedException {
		
		Test t = new Test();
		t.test(".\\src\\resources\\ProfilePictures\\1.jpg");
//		Thread.sleep(2000);
//		t.test(".\\1.jpg");
//		Thread.sleep(2000);
//		t.test("./1.jpg");
//		Thread.sleep(2000);
//		t.test(".\\resources\\ProfilePictures\\1.jpg");
//		Thread.sleep(2000);
//		t.test(".\\ProfilePictures\\1.jpg");
//		Thread.sleep(2000);
//		t.test("./src/resources/ProfilePictures/1.jpg");
//		Thread.sleep(2000);
//		t.test("./resources/ProfilePictures/1.jpg");
//		Thread.sleep(2000);
//		t.test("./ProfilePictures/1.jpg");
//		Thread.sleep(2000);
//		t.test(".//src//resources//ProfilePictures//1.jpg");
//		Thread.sleep(2000);
//		t.test(".//resources//ProfilePictures//1.jpg");
//		Thread.sleep(2000);
//		t.test(".//ProfilePictures//1.jpg");
		// TODO Auto-generated method stub
		/*View view = new View();
		view.printToConsole("Server Wakes Up...Wait For Acknowledge");
		String ip = "localhost";
		int port = 8080;
		String pathToResources = ".\\src\\resources\\";
		if(args.length == 2)
		{
			ip = args[0];
			port = Integer.parseInt(args[1]);
			pathToResources = "";
			//pathToResources = typeOfSource == 1 ? "C:\\Users\\Sahar Mizrahi\\Desktop\\Study\\FinalProject\\Run\\resources":"C:\\Users\\project06\\Desktop\\OutTalk\\resources";
		}
		Model model = Model.getInstance();
		Controller controller = new Controller(model,view,ip,port,pathToResources);
		controller.start();*/
		
	}

}
