import java.awt.image.BufferedImage;

public class Stuff {
	String name = null;
	double xSize = 1, ySize = 1;
	int xPos = 10000, yPos = 10000;
	BufferedImage token = null;

	public Stuff(String name, int xPos, int yPos, double xSize, double ySize, BufferedImage token) {
		this.name = name.toUpperCase();
		this.xSize= xSize;
		this.ySize = ySize;
		this.xPos = xPos;
		this.yPos = yPos;
		this.token = token;
	}
	
	public Stuff(String name, int xPos, int yPos, double xSize, double ySize) {
		this.name = name.toUpperCase();
		this.xSize = xSize;
		this.ySize = ySize;
		this.xPos = xPos;
		this.yPos = yPos;
	}
	
	public Stuff(String name, int xPos, int yPos) {
		this.name = name;		
		this.xPos = xPos;
		this.yPos = yPos;
	}
	
	public Stuff(String name, BufferedImage token) {
		this.name = name;
		this.token = token;
	}
	
	public Stuff(String name) {
		this.name = name;
	}
	
	public Stuff() {
	}
	
	public void setImage(BufferedImage i) {
		this.token = i;
	}
}