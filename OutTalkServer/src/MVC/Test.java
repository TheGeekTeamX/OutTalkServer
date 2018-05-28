package MVC;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Test {
	
	public void test(String s)
	{

			try {
				System.out.println("current: "+s);
			BufferedImage im = ImageIO.read(getClass().getResource(s));
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
