package Recognize;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.commons.codec.binary.Base64;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class WavSplitFixedTime {
	String fileName = "";
	AudioInputStream audioIn = null;
	AudioFormat format = null;
	long readBytes = 0;
	long frameSize;
	long frameLength;
	byte[] buf;
	SourceDataLine line = null;
	DataLine.Info info;
	String wavFileName;
	String wavFileNamePre;
	String wavSplitDir;
	private List<String> list;

	public List<String> getList() {
		return list;
	}

	public WavSplitFixedTime(String path, float slotSec) {
		File wavFile = new File(path);
		wavFileName = wavFile.getName();
		wavFileNamePre = getPreffix(wavFileName);
		if (!wavFile.exists() || wavFileName.endsWith("wav") == false) {
			System.err.println("File is not exist or is not wav format.");
			return;
		}

		// Read wav file to audio input stream and bytes array.
		readWav(wavFile);

		// Calcurate bytes of the slot from sec.
		float bytesPerSec = format.getFrameRate() * format.getFrameSize();
		int slotBytes = (int) (bytesPerSec * slotSec);

		// Calcurate how many time we repeat to write splitted wav files.
		int loopTime = (int) (readBytes / slotBytes) + 1;

		// Write split wav file
		this.list = new ArrayList<>();
		for (int i = 0; i < loopTime; i++) {
			int startBytes = slotBytes * i;
			list.add(writeWavSplitToString(startBytes, slotBytes));
		}

	}
	
	public WavSplitFixedTime(byte[] wavByte, float slotSec) {

		// Read wav file to audio input stream and bytes array.
		readWavBytes(wavByte);

		// Calcurate bytes of the slot from sec.
		float bytesPerSec = format.getFrameRate() * format.getFrameSize();
		int slotBytes = (int) (bytesPerSec * slotSec);

		// Calcurate how many time we repeat to write splitted wav files.
		int loopTime = (int) (readBytes / slotBytes) + 1;

		// Write split wav file
		this.list = new ArrayList<>();
		for (int i = 0; i < loopTime; i++) {
			int startBytes = slotBytes * i;
			list.add(writeWavSplitToString(startBytes, slotBytes));
		}

	}

	public void readWav(File file) {
		try {

			FileInputStream fis = new FileInputStream(file);
			InputStream fileIn = new BufferedInputStream(fis);
			audioIn = AudioSystem.getAudioInputStream(fileIn);
			format = audioIn.getFormat();
			buf = new byte[audioIn.available()];
			audioIn.read(buf, 0, buf.length);
			frameSize = format.getFrameSize();
			frameLength = audioIn.getFrameLength();
			readBytes = frameSize * frameLength;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readWavBytes(byte[] wavBytes) {
		try {			
			InputStream fileIn = new ByteArrayInputStream(wavBytes);
			audioIn = AudioSystem.getAudioInputStream(fileIn);
			format = audioIn.getFormat();
			buf = new byte[audioIn.available()];
			audioIn.read(buf, 0, buf.length);
			frameSize = format.getFrameSize();
			frameLength = audioIn.getFrameLength();
			readBytes = frameSize * frameLength;

		}  catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String writeWavSplitToString(int startBytes, int slotBytes) {
		try {
			InputStream byteIn = new BufferedInputStream(new ByteArrayInputStream(buf, startBytes, slotBytes));
			AudioInputStream ais = new AudioInputStream(byteIn, format, byteIn.available());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
			byte[] audioBytes = out.toByteArray();
			String splittedWav = Base64.encodeBase64String(audioBytes);
			return splittedWav;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getPreffix(String fileName) {
		if (fileName == null)
			return null;
		int point = fileName.lastIndexOf(".");
		if (point != -1) {
			return fileName.substring(0, point);
		}
		return fileName;
	}

	public String getWavFileSplitDir() {
		return wavSplitDir;
	}

}
