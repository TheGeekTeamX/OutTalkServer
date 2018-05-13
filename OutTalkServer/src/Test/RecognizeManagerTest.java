package Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import Recognize.RecognizeManager;
import Recognize.WavSplitFixedTime;

public class RecognizeManagerTest {

	public RecognizeManagerTest() {
		//Start recognize manager 
		RecognizeManager rm = new RecognizeManager();
		
		//get the wav file 
		WavSplitFixedTime ws = new WavSplitFixedTime("C:\\Users\\Gal\\Desktop\\wavTest\\fix.wav", 1);
		List<String> wavBytesList = ws.getList();
		
		//get the users of the record from sisso
		String users = "gal,gal,gal,maayan,sisso,sapir";
		List<String> usersList = new LinkedList<String>(Arrays.asList(users.split(",")));
		
		//algo
		rm.BuildProtocol(wavBytesList, usersList);

	}

}
