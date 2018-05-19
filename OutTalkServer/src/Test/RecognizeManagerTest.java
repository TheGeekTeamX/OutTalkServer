package Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import Recognize.RecognizeManager;
import Recognize.WavSplitFixedTime;
import ResponsesEntitys.ProtocolLine;

public class RecognizeManagerTest {

	public RecognizeManagerTest() {
		//Start recognize manager 
		RecognizeManager rm = new RecognizeManager();
		//rm.SendWavToRecognize("C:\\Users\\Gal\\Desktop\\wavTest\\fix.wav","10","SentToRecognize");
		
		//get the wav file 
		WavSplitFixedTime ws = new WavSplitFixedTime("C:\\Users\\Gal\\Desktop\\wavTest\\fix.wav", 2);
		List<String> wavBytesList = ws.getList();

		//get the users of the record from sisso
		String users = "maayan,gal,maayan,eden,eden,eden";
		List<String> usersList = new LinkedList<String>(Arrays.asList(users.split(",")));
		
		//algo
		ArrayList<ProtocolLine> pl = rm.BuildProtocol(wavBytesList, usersList);
		System.out.println("finish");
	}

}
