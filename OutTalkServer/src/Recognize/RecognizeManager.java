package Recognize;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.commons.codec.binary.Base64;

import com.google.api.client.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RecognizeManager {

	public List<String> SendToRecognizeService() {

		// Divide wav file to chunks of 2 seconds
		WavSplitFixedTime ws = new WavSplitFixedTime("C:\\Users\\Gal\\Desktop\\wavTest\\fix.wav", 4);
		List<String> list = ws.getList();

		// Send the wav file to Recognize Service
		Gson gson = new Gson();
		String wav = gson.toJson(list, List.class);
		JsonObject jo = new JsonObject();
		jo.addProperty("eventId", "312312");

		int i = 0;
		for (String s : list) {
			jo.addProperty("part" + (i + 1), s);
			i++;
		}

		HttpClient httpClient = HttpClientBuilder.create().build(); // Use this instead
		try {
			HttpPost request = new HttpPost("http://requestbin.fullcontact.com/xnhhofxn");
			StringEntity params = new StringEntity(jo.toString());
			request.addHeader("content-type", "application/x-www-form-urlencoded");
			request.setEntity(params);
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();

			// get response from Recognize Service
			entity.getContent();
			if (entity != null) {
				String res = EntityUtils.toString(entity);

			}
		} catch (Exception ex) {

		} finally {
			// Deprecated
			// httpClient.getConnectionManager().shutdown();
		}
		return null;
	}

	// Get the list of user in the record from recognize service and compare with
	// the regular record
	public void BuildProtocol(List<String> wavBytes, List<String> usersList) {
		if (usersList != null && usersList.size() > 0) {
			List<String> protocolList = null;
			int startIndex = 0;
			int endIndex = 0;
			String currUser = usersList.get(0);
			for (int i = 0; i < usersList.size(); i++) {
				String user = usersList.get(i);
				if (user.equals(currUser))
					endIndex = i;
				else// change user
				{
					// function to merge wav file
					try {
						MergeWavList(wavBytes.subList(startIndex, endIndex));
					} catch (UnsupportedAudioFileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			try {
				protocolList = TranslateWithGoogleService(wavBytes);
			} catch (Exception e) {
				e.printStackTrace();
			}

			int i = 0;
			for (String text : protocolList) {
				System.out.println(usersList.get(i) + ":" + " " + text.toString());
				i++;
			}
		}

	}

	// set the record to GoogleSpeechToText
	public List<String> TranslateWithGoogleService(List<String> wavBytes) throws Exception {
		List<String> res = new ArrayList<>();
		SpeechToText st = new SpeechToText();
		for (String b : wavBytes) {
			byte[] encodedAudio = Base64.decodeBase64(b);
			res.add(st.getConvertText(encodedAudio).toString());
		}
		return res;
	}

	//TODO: https://stackoverflow.com/questions/6381012/java-trouble-combining-more-than-2-wav-files
	public void MergeWavList(List<String> wavBytes) throws UnsupportedAudioFileException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		AudioInputStream audioBuild = AudioSystem.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(0).getBytes())));
		for (int i = 0; i < wavBytes.size(); i++) {
			
			AudioInputStream audio1 = AudioSystem.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(i).getBytes())));
			AudioInputStream audio2 = AudioSystem.getAudioInputStream(new ByteArrayInputStream(Base64.decodeBase64(wavBytes.get(i+1).getBytes())));
			
			AudioInputStream audio = new AudioInputStream(new SequenceInputStream(audio1, audio2), audio1.getFormat(),
					audio1.getFrameLength() + audio2.getFrameLength());

			 audioBuild = new AudioInputStream(new SequenceInputStream(audioBuild, audio),audioBuild.getFormat(), audioBuild.getFrameLength() + audio2.getFrameLength());

			
		}

		AudioSystem.write(audioBuild, AudioFileFormat.Type.WAVE, out);

		
	
	}

}
