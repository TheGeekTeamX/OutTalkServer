package Recognize;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

public class RecognizeManager {
	
	// Send the Wav to recognize service
	public void SendWavToRecognize(String wavPath,String eventId,String action) {
		// Divide wav file to chunks of 2 seconds
		WavSplitFixedTime ws = new WavSplitFixedTime(wavPath, 2);
		List<String> list = ws.getList();

		ArrayList<NameValuePair> postParameters;
		HttpClient httpclient = HttpClientBuilder.create().build();
		postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("meet_id", eventId));
		postParameters.add(new BasicNameValuePair("label", "sahar"));
		HttpPost post = new HttpPost("http://193.106.55.106:5000/predict");
		
		switch (action) {
		case "OpenEvent":
			
			break;
			
		case "SentToRecognize":
			StringBuilder sb = new StringBuilder();
			for (String str : list) {
				sb.append(str);
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			postParameters.add(new BasicNameValuePair("audio_parts", sb.toString()));
			
			try {
				post.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String responseBody;
				responseBody = httpclient.execute(post, responseHandler);
				JSONObject response = new JSONObject(responseBody);

				String users = response.getJSONArray("result").toString();
				List<String> usersList = new LinkedList<String>(Arrays.asList(users.split(",")));

				BuildProtocol(list, usersList);
				System.out.println(response.toString());

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}


	}

	// Get the list of user in the record from recognize service and compare with
	// the regular record
	public void BuildProtocol(List<String> wavBytes, List<String> usersList) {
		int startIndex = 0, endIndex = 0;
		String currentUser = usersList.size() == 0 ? "" : usersList.get(0);
		byte[] mergedBytes = null;
		for (int i = 0; i < usersList.size(); i++) {
			if (i + 1 == usersList.size() || !currentUser.equals(usersList.get(i)))// change user
			{
				mergedBytes = MergeWavList(wavBytes.subList(startIndex, endIndex + 1), "" + startIndex);
				try {
					String s = TranslateWithGoogleService(mergedBytes);
					System.out.println(currentUser + ":" + s);
					startIndex = i;
					endIndex = i;
					currentUser = usersList.get(i);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				endIndex = i;
		}
	}

	private byte[] MergeWavList(List<String> wavBytes, String user1) {
		AudioInputStream audio2;
		byte[] mergedBytes = null;
		File fileOut = new File("C:\\Users\\Gal\\Desktop\\" + user1 + ".wav");
		try {
			AudioInputStream audioBuild = AudioSystem
					.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(0).getBytes())));
			for (int i = 1; i < wavBytes.size(); i++) {
				audio2 = AudioSystem
						.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(i).getBytes())));
				audioBuild = new AudioInputStream(new SequenceInputStream(audioBuild, audio2), audioBuild.getFormat(),
						audio2.getFrameLength());
			}

			AudioSystem.write(audioBuild, AudioFileFormat.Type.WAVE, fileOut);
			Path path = Paths.get("C:\\Users\\Gal\\Desktop\\" + user1 + ".wav");
			mergedBytes = Files.readAllBytes(path);

		} catch (IOException | UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mergedBytes;
	}

	// set the record to GoogleSpeechToText
	private String TranslateWithGoogleService(byte[] wavByte) throws Exception {
		String res;
		SpeechToText st = new SpeechToText();
		res = st.getConvertText(wavByte);
		return res;
	}

}
