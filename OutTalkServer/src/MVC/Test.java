package MVC;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class Test {
	
	public void test(String s)
	{

			try {
				System.out.println("current: "+s);
			BufferedImage im = ImageIO.read(new File(s));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(im, "jpg", baos);
				baos.flush();
				System.out.println("GOOD");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			
	}

}
