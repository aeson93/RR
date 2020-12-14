import java.awt.*;
import javax.swing.*;

public class App { 
	
	/*
	v5.0 complete Overhaul
	v5.1 implemented Multiplayer - requires separate server
	v5.2 cleaning up MP changes
	v5.3 fixing lift&drop to be single UTF command
	v5.4 more fixes, adding save/load
	v5.5 fixing screen size issues
	v5.6 adding cloud support							[https://sites.google.com/d/1GsLNXg9rv7_9r5kw578GObw9vDfiHWGt/p/1prhGM0SaKcN-seMWH2FTXSzfM8RRKtWW/edit	OR	domassets]
	v5.7 fixing cloud, adding motionListener/timer
	v5.8 changing to sendBytes
	v5.9 adding graphics, resource images, distance measure
	v6.0 release version
	*/

	public static void main(String[] args) {
		
		if(JOptionPane.showConfirmDialog(null, "Are you the host?") == 0) {
			new GameServer();
			System.out.println("Starting Server /n  Ya Gat Hugged");
		}
		else {
			Toolkit tk = Toolkit.getDefaultToolkit();
			JFrame frame = new JFrame("[ROOMS AND RECTANGLES]");
			frame.setSize((int) tk.getScreenSize().getWidth(), (int) tk.getScreenSize().getHeight());
			frame.setUndecorated(true);
			frame.setResizable(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			GUI gui = new GUI((int) tk.getScreenSize().getWidth(), (int) tk.getScreenSize().getHeight());
			frame.add(gui);
			frame.setVisible(true);
			
			Timer t = new Timer(50, gui);
			t.start();
		} 
	}
} 