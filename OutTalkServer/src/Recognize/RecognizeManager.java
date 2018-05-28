package Recognize;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.io.StringWriter;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.google.api.client.util.IOUtils;

import ResponsesEntitys.ProtocolLine;

import org.apache.commons.codec.binary.Base64;

public class RecognizeManager {

	// Send identical record from user to Recognize Service
	public boolean CreateDataSet(String URL, byte[] wavByte, String user) {
		// Divide Wav file to chunks of 20 seconds
		WavSplitFixedTime ws = new WavSplitFixedTime(wavByte, 4);
		List<String> list = ws.getList();
		for (int i = 0; i < list.size(); i++) {
			ArrayList<NameValuePair> postParameters;
			postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("label", user));
			postParameters.add(new BasicNameValuePair("file", list.get(i)));
			boolean result = CreatePost(URL, postParameters);
			if (!result) {
				System.out.println("Failed to send post request to :" + URL);
				return false;
			}

		}
		ArrayList<NameValuePair> postParameters2 = new ArrayList<NameValuePair>();
		postParameters2.add(new BasicNameValuePair("label", user));
		URL = "http://193.106.55.106:5000/create_model_user";
		boolean result = CreatePost(URL, postParameters2);
		if (!result) {
			System.out.println("Failed to send post request to :" + URL);
			return false;
		}
		return true;

	}

	// Send event details to recognize server on event open
	public boolean onEventOpen(String URL, String eventId, String label, List<String> users) {

		ArrayList<NameValuePair> postParameters;
		postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("meet_id", eventId));
		postParameters.add(new BasicNameValuePair("label", label));
		boolean result = CreatePost(URL, postParameters);
		if (!result) {
			System.out.println("Failed to send post request to :" + URL);
			return false;
		}
		return true;

	}

	// Send the Wav to recognize service
	public ArrayList<ProtocolLine> SendWavToRecognize(String URL, byte[] wavByte, String usersList) {
		ArrayList<ProtocolLine> pl = new ArrayList<>();

		// Divide Wav file to chunks of 2 seconds
		WavSplitFixedTime ws = new WavSplitFixedTime(wavByte, 8);
		List<String> list = ws.getList();

		ArrayList<NameValuePair> postParameters;
		HttpClient httpclient = HttpClientBuilder.create().build();
		postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("list_users", usersList));
		HttpPost post = new HttpPost(URL); // "http://193.106.55.106:5000/predict"

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
			System.out.println(responseBody);
			JSONObject response = new JSONObject(responseBody);
			String usersRecognize = response.getString("result");
			List<String> usersListRecognize = new LinkedList<String>(Arrays.asList(usersRecognize.split(",")));

			pl = BuildProtocol(list, usersListRecognize);
			System.out.println(response.toString());

		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
		return pl;
	}

	// Comparing between list of users from recognize service and regular record
	public ArrayList<ProtocolLine> BuildProtocol(List<String> wavBytes, List<String> usersList) {
		int startIndex = 0, endIndex = 0;
		ArrayList<ProtocolLine> pl = new ArrayList<>();

		String currentUser = usersList.size() == 0 ? "" : usersList.get(0);
		byte[] mergedBytes = null;
		for (int i = 0; i < usersList.size(); i++) {
			if (i + 1 == usersList.size() || !currentUser.equals(usersList.get(i))) {
				mergedBytes = MergeWavList(wavBytes.subList(startIndex, endIndex + 1), "" + startIndex);
				try {
					String text = TranslateWithGoogleService(mergedBytes);

					pl.add(new ProtocolLine(currentUser, text));
					startIndex = i;
					endIndex = i;
					currentUser = usersList.get(i);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			} else
				endIndex = i;
		}
		return pl;

	}

	private byte[] MergeWavList(List<String> wavBytes, String user1) {
		AudioInputStream audio2;
		byte[] mergedBytes = null;
		try {
			File fileOut = File.createTempFile("tempFile", "wav");
			AudioInputStream audioBuild = AudioSystem
					.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(0).getBytes())));
			for (int i = 1; i < wavBytes.size(); i++) {
				audio2 = AudioSystem
						.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(i).getBytes())));
				audioBuild = new AudioInputStream(new SequenceInputStream(audioBuild, audio2), audioBuild.getFormat(),
						audio2.getFrameLength());
			}

			AudioSystem.write(audioBuild, AudioFileFormat.Type.WAVE, fileOut);
			Path path = Paths.get(fileOut.getPath());
			mergedBytes = Files.readAllBytes(path);

		} catch (IOException | UnsupportedAudioFileException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return mergedBytes;
	}

	// Set the record to GoogleSpeechToText
	private String TranslateWithGoogleService(byte[] wavByte) throws Exception {
		String res;
		SpeechToText st = new SpeechToText();
		res = st.getConvertText(wavByte);
		return res;
	}

	public List<String> getWavList(byte[] wavFile) {
		File temp;
		try {
			temp = File.createTempFile("tempFile","");
			temp.delete();
			temp.mkdir();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		try {
//			List<String> wavList = null;
//			File tempFolder = File.createTempFile("tempFile", "wav");
//			AudioInputStream audioBuild = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavFile));
//			AudioSystem.write(audioBuild, AudioFileFormat.Type.WAVE, tempFolder);
//
//			String python = "python C:\\Users\\Gal\\Desktop\\pydub_splitter.py";
//			Path tempFolderPath = Paths.get(tempFolder.getPath());
//			File splitWavFolder = File.createTempFile("tempFile", "splitWave");
//			Path splitedFolderPath= Paths.get(splitWavFolder.getPath());
//			Process p = Runtime.getRuntime().exec(python + " " + tempFolderPath.toString() + " " + splitedFolderPath.toString());
//			File[] directoryListing = splitWavFolder.listFiles();
//			  if (directoryListing != null) {
//				    for (File child : directoryListing) {
//				      // Do something with child
//				    	Path childPath = Paths.get(child.getPath());
//						byte[] mergedBytes = Files.readAllBytes(childPath);
//						wavList.add(Base64.encodeBase64String(mergedBytes));
//				    }
//				  } else {
//				    // Handle the case where dir is not really a directory.
//				    // Checking dir.isDirectory() above would not be sufficient
//				    // to avoid race conditions with another process that deletes
//				    // directories.
//				  }
//			  return wavList;
//		} catch (IOException | UnsupportedAudioFileException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return null;
	}

	private boolean CreatePost(String URL, ArrayList<NameValuePair> postParameters) {
		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(URL);
		try {
			post.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody;
			responseBody = httpclient.execute(post, responseHandler);
			JSONObject response = new JSONObject(responseBody);
			System.out.println("response: " + response);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;

	}

}
