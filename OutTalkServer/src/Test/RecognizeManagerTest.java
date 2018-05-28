package Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import Recognize.RecognizeManager;
import Recognize.WavSplitFixedTime;
import ResponsesEntitys.ProtocolLine;

public class RecognizeManagerTest {

	public RecognizeManagerTest() {
		//Start recognize manager test
		boolean ret;
		RecognizeManager rm = new RecognizeManager();
		

		//get the wav file 
		Path fileLocation = Paths.get("C:\\Users\\Gal\\Desktop\\shnei1.wav");
		byte[] data;
		try {
			data = Files.readAllBytes(fileLocation);
			
			rm.getWavList(data);
			//get the users of the record from sisso
			String users = "gal,roee";
			List<String> usersList = new LinkedList<String>(Arrays.asList(users.split(",")));
			
			//##CreateDataSet Test##
			ret = rm.CreateDataSet("http://193.106.55.106:5000/create_dataset",data, "omer");
			if(!ret) {
				System.out.println("CreateDataSet test failed");
			}
			
			//##SendWavToRecognize Test##
//			ArrayList<ProtocolLine> pl = new ArrayList<>();
//			pl = rm.SendWavToRecognize("http://193.106.55.106:5000/predict",data, users);		
//			for (ProtocolLine protocolLine : pl) {
//				System.out.println(protocolLine.getName()+": "+protocolLine.getText());
//			}

			System.out.println("finish");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
