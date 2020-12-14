import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.*;


@SuppressWarnings("serial")
public class GUI  extends JPanel implements Runnable, ActionListener{

	// server
	private String ip = "localhost";
	private int port = 22222;
	private Thread thread;
	private Socket socket;
	ObjectOutputStream dos;
	ObjectInputStream dis;
	private boolean accepted = false;
	private boolean unableToCommunicateWithOpponent = false;

	// screenwidth & height
	int sW, sH, H, W, Hx;

	// UI
	BufferedImage fieldImg;
	BufferedImage block;
	BufferedImage go;
	BufferedImage stop;
	BufferedImage key;
	BufferedImage init;
	BufferedImage d4;
	BufferedImage d6;
	BufferedImage d8;
	BufferedImage d10;
	BufferedImage d12;
	BufferedImage d20;
	BufferedImage pingIcon;
	BufferedImage notFound;
	BufferedImage menuBG;
	BufferedImage recycle;

	// arrays
	ArrayList<Stuff> stuff = new ArrayList<Stuff>(); //tokens
	ArrayList<Shadow> shade = new ArrayList<Shadow>(); //walls
	ArrayList<Stuff> followers = new ArrayList<Stuff>(); //players
	ArrayList<TriggerField> tf = new ArrayList<TriggerField>(); //triggers
	ArrayList<String> chat = new ArrayList<String>(); //message log
	ArrayList<Player> players = new ArrayList<Player>(); //players
	ArrayList<Integer> rolls = new ArrayList<Integer>(); //dice rolls
	ArrayList<Patrol> patrols = new ArrayList<Patrol>(); //automatic moves
	ArrayList<Stuff> namedImg = new ArrayList<Stuff>(); //loading images from site

	// general variables
	JTextField text = new JTextField("Free"); //input field
	JTextField modifier = new JTextField("0"); //roll modifier field
	int plusMod = 0; //roll modifier
	BufferedReader br; // text input reader
	int lift = 1000000; // lifted stuff ID
	int xInset, yInset; // map scrolling inset
	double zoom = 1; // current map zoom (higher = closer)
	String mode = "PLAY"; // mode of operation
	String viewer; // current center of vision
	boolean follow = false; // checks if followers should follow the viewer
	boolean firstShade = true; // checks if new shade is part of a continued line
	boolean door = false; // enable to create doors instead of shadows
	boolean DM = false;
	String myName;
	Font font; 
	Stuff ping; //ping location
	boolean showTriggers = false; //shows triggerfield areas
	boolean testTriggers = false; //triggerfields are not delete on trigger
	Toolkit tk = Toolkit.getDefaultToolkit();
	Double[] createShadow = new Double[4]; //stage of wall creation
	int initBonus = 0; //initiative value
	boolean haveAdv = false; //roll 2 d20
	PrintWriter pr; //write to txt
	boolean showShadows = false; //show wall/door lines
	boolean moveMenu = false; // move menu freely
	Stuff moveTo; //move distance calculation
	boolean breakWalls = false; //open walls instead of doors
	boolean unrestricted = false; //let players act
	
	String content = null; //read from HTML line
	String[] htmlLines;
	
	Color nameColor = Color.BLACK; //textColor
	Color reverse = Color.WHITE; //textBorderColor
	

	public GUI(int screenWidth, int screenHeight) {
		
		//login setup	
		ip = JOptionPane.showInputDialog(null,
				 "What is the host IP?",
				 "",
				 JOptionPane.QUESTION_MESSAGE);
		
		myName = JOptionPane.showInputDialog(null,
				 "What is your name?",
				 "Set to viewer token",
				 JOptionPane.QUESTION_MESSAGE).toUpperCase();
		
		if(myName.equals("DM")) { //DM can see everything
			DM = true;
			showTriggers = true;
			showShadows = true;	
		}
		
		viewer = myName; //Players can see from their token
				
		connect();
		thread = new Thread(this, "MultiplayerRR");
		thread.start();	
			
		// general settings
		this.sW = screenWidth;
		this.sH = screenHeight;
		this.H = sH / 15;
		this.Hx = 0;
		
		font = new Font("Monospaced", Font.BOLD, 18 * (sW / 1920));

		setBackground(Color.BLACK);
		setLayout(null);

		// import UI images
		ImageIO.setUseCache(false);
		try {	
			fieldImg = ImageIO.read(GUI.class.getResource("images/feld.png"));
			block = ImageIO.read(GUI.class.getResource("images/deleteWindow.png"));
			go = ImageIO.read(GUI.class.getResource("images/go.png"));
			stop = ImageIO.read(GUI.class.getResource("images/stop.png"));
			key = ImageIO.read(GUI.class.getResource("images/key.png"));
			init = ImageIO.read(GUI.class.getResource("images/init.png"));
			d4 = ImageIO.read(GUI.class.getResource("images/d4.png"));
			d6 = ImageIO.read(GUI.class.getResource("images/d6.png"));
			d8 = ImageIO.read(GUI.class.getResource("images/d8.png"));
			d10 = ImageIO.read(GUI.class.getResource("images/d10.png"));
			d12 = ImageIO.read(GUI.class.getResource("images/d12.png"));
			d20 = ImageIO.read(GUI.class.getResource("images/d20.png"));
			pingIcon = ImageIO.read(GUI.class.getResource("images/pingIcon.png"));
			notFound = ImageIO.read(GUI.class.getResource("images/notFound.png"));
			menuBG = ImageIO.read(GUI.class.getResource("images/menuBG.png"));
			recycle = ImageIO.read(GUI.class.getResource("images/recycle.png"));
		} catch (IOException e3) {
			e3.printStackTrace();
		}

//		importFollowers();	

		// text input field
		text.setBounds(23 * H, 0, 4 * H, H);
		text.setFont(font);
		text.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!text.getText().contains("NULL") && !text.getText().contains("_") && !text.getText().equalsIgnoreCase("FREE") && unrestricted) { //regular input is parsed
					command(text.getText().toUpperCase());
				}
				else if(text.getText().equalsIgnoreCase("FREE") && DM) { //player input is unrestricted
					try {
						String message = "UNRESTRICT";					
						dos.writeObject(message);
						dos.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				else if(!unrestricted) { 
					char[] wait = "wait for other players".toCharArray();
					for(int i = 0; i < wait.length; i++){
						if (Math.random() > 0.5) {
							wait[i] = Character.toUpperCase(wait[i]);
						}
					}
					text.setText(new String(wait));
				}
			}
		});
		add(text);	
		
		// roll modifier input field
		modifier.setBounds((int) (26 * H), (int) (4.5 * H), H/2, H/2);
		modifier.setFont(font);
		modifier.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				plusMod = Integer.parseInt(modifier.getText());
			}
		});
		add(modifier);
		
		// keyboard inputs
		Action drag = new AbstractAction() { //move textfield
			public void actionPerformed(ActionEvent e) {
				text.setBounds(23 * H, 0, 4 * H, H); System.out.print(sW / H);
				modifier.setBounds((int) (26 * H), (int) (4.5 * H), H/2, H/2);
				Hx = 0;
				moveMenu = !moveMenu;
				text.setText("Moving");
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("T"), "T");
		this.getActionMap().put("T", drag);
		
		Action escape = new AbstractAction() { //exit game
			public void actionPerformed(ActionEvent e) {
				if(JOptionPane.showConfirmDialog(null, "Exit?") == 0) {
					System.exit(0);
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
		this.getActionMap().put("ESCAPE", escape);

		Action down = new AbstractAction() { //scroll down
			public void actionPerformed(ActionEvent e) {
				yInset += H;
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "DOWN");
		this.getActionMap().put("DOWN", down);

		Action up = new AbstractAction() { //scroll up
			public void actionPerformed(ActionEvent e) {
				if (yInset > 0) {
					yInset -= H;
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("UP"), "UP");
		this.getActionMap().put("UP", up);

		Action left = new AbstractAction() { //scroll left
			public void actionPerformed(ActionEvent e) {
				if (xInset > 0) {
					xInset -= H;
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "LEFT");
		this.getActionMap().put("LEFT", left);

		Action right = new AbstractAction() { //scroll right
			public void actionPerformed(ActionEvent e) {
				xInset += H;
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "RIGHT");
		this.getActionMap().put("RIGHT", right);
		
		Action ping = new AbstractAction() { //set ping
			public void actionPerformed(ActionEvent e) {
				if (unrestricted) {
					mode = "PING";
					text.setText("PINGING");
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("P"), "P");
		this.getActionMap().put("P", ping);
		
		Action q = new AbstractAction() { //compare distance before/after move
			public void actionPerformed(ActionEvent e) {
				if(moveTo == null && lift != 1000000) {
					moveTo = new Stuff(stuff.get(lift).name, stuff.get(lift).xPos, stuff.get(lift).yPos, stuff.get(lift).xSize, stuff.get(lift).ySize);
				}
				else if(moveTo != null) {
					moveTo = null;
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("Q"), "Q");
		this.getActionMap().put("Q", q);

		Action x = new AbstractAction() { // mode switch: play -> set walls/doors -> set triggers -> play
			public void actionPerformed(ActionEvent e) {
				if(DM) {
					if (mode == "PLAY") {
						mode = "SHADE";
						firstShade = true;
						text.setText("Creating Walls");
					} else if (mode == "SHADE") {
						mode = "TF_START";
						text.setText("Set upper left corner");
					} else if (mode.contains("TF_")) {
						mode = "PLAY";
						text.setText("Play mode");
					}
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("X"), "X");
		this.getActionMap().put("X", x);

		Action o = new AbstractAction() { // add new patrol
			public void actionPerformed(ActionEvent e) {
				if(lift != 1000000 && DM) {
//					patrols.add(new Patrol(stuff.get(lift)));
					text.setText("SETTING PATROL");
					
					try {
						String message = "PATROL_" + lift;
						dos.writeObject(message);
						dos.flush();
					} catch (IOException e1) {
					}
					
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("O"), "O");
		this.getActionMap().put("O", o);
		
		Action i = new AbstractAction() { //patrol mode
			public void actionPerformed(ActionEvent e) {
				if(DM) {
					if (mode == "PATROL") {
						mode = "PLAY";
					} else {
						mode = "PATROL";
					}
				}
			}
		};
		this.getInputMap().put(KeyStroke.getKeyStroke("I"), "I");
		this.getActionMap().put("I", i);

		// mouse movement
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if(lift != 1000000) { //update token location
					stuff.get(lift).xPos = (int) ((e.getX() + xInset / zoom - (stuff.get(lift).xSize / 2) / zoom) * zoom);
					stuff.get(lift).yPos = (int) ((e.getY() + yInset / zoom - (stuff.get(lift).ySize / 2) / zoom) * zoom);
				}
				if(moveTo != null) { //calculate distance moved from origin
					double x = Math.pow(
							(moveTo.xPos + (moveTo.xSize / 2)) / zoom - xInset / zoom - e.getX(),
							2);
					double y = Math.pow(
							(moveTo.yPos + (moveTo.ySize / 2)) / zoom - yInset / zoom - e.getY(),
							2);
					double z = Math.sqrt(x + y);
					
					DecimalFormat df = new DecimalFormat("#.##");
										
					text.setText("Distance = " + df.format(z / H));
				}
				if(moveMenu) { //move menu to new origin
					text.setText("Moving by " + (23 * H - e.getX()) );
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {

			}
		});
		
		// on-click listener
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (moveMenu) { //set origin of menu box
					requestFocusInWindow();
					text.setBounds(e.getX(), 0, text.getWidth(), text.getHeight());
					modifier.setBounds((int) (3 * H) + e.getX(), (int) (4.5 * H), H/2, H/2);
					Hx = 23 * H - e.getX();
					moveMenu = false;
				}
				
				if(unrestricted) {
					requestFocusInWindow();

					if (e.getX() < 23 * H - Hx && (mode == "PLAY" || mode.contains("TF_"))) { // find closest token
						int slot = 0;
						double min = Integer.MAX_VALUE;
						for (int i = 0; i < stuff.size(); i++) { // calculate nearest token by center
							if (!stuff.get(i).name.contains("#")) {
								double x = Math.pow((stuff.get(i).xPos + (Math.abs(stuff.get(i).xSize) / 2)) / zoom
										- xInset / zoom - e.getX(), 2);
								double y = Math.pow((stuff.get(i).yPos + (Math.abs(stuff.get(i).ySize) / 2)) / zoom
										- yInset / zoom - e.getY(), 2);
								double z = Math.sqrt(x + y);

								if (z < min) {
									min = z;
									slot = i;
								}
							}
						}

						if (mode == "PLAY") {
							if (lift == 1000000 && stuff.size() != 0 && min < Math.abs(stuff.get(slot).xSize) / 2
									&& !stuff.get(slot).name.contains("#")) { // pick up nearest token in range
								lift = slot;

							} else if (lift != 1000000) { // drop lifted token at cursor

								for (int i = 0; i < tf.size(); i++) { // check for triggers
									if (tf.get(i).name.equals(stuff.get(lift).name) && e.getX() >= tf.get(i).x1
											&& e.getX() <= tf.get(i).x2 && e.getY() >= tf.get(i).y1
											&& e.getY() <= tf.get(i).y2) {
										try { //spawn triggered tokens
											String message = "TRIGGER";
											for (int j = 0; j < tf.get(i).spawnables.size(); j++) {
												String temp = "_" + tf.get(i).spawnables.get(j).name + "_"
														+ (double) (tf.get(i).spawnables.get(j).xPos) / H + "_"
														+ (double) (tf.get(i).spawnables.get(j).yPos) / H + "_"
														+ tf.get(i).spawnables.get(j).xSize / H + "_"
														+ tf.get(i).spawnables.get(j).ySize / H;
												message = message.concat(temp);
											}
											System.out.println(message);
											dos.writeObject(message);
											dos.flush();
										} catch (IOException e1) {
										}
										if (!testTriggers) {
											tf.remove(i);
										}
									}
								}

								try { //move token to new origin
									String message = "MOVE_" + lift + "_" + stuff.get(lift).name + "_"
											+ ((e.getX() + xInset / zoom - (stuff.get(lift).xSize / 2) / zoom) * zoom)
													/ H
											+ "_"
											+ ((e.getY() + yInset / zoom - (stuff.get(lift).ySize / 2) / zoom) * zoom)
													/ H
											+ "_" + stuff.get(lift).xSize / H + "_" + stuff.get(lift).ySize / H;
									dos.writeObject(message);
									dos.flush();
								} catch (IOException e1) {
								}
								if (moveTo != null) {
									moveTo = null;
								}
								lift = 1000000;
								text.requestFocusInWindow();
							}
						}

						// go through the steps of creating a triggerfield, each step activates the next - press x to finish
						else if (mode == "TF_START") {
							tf.add(new TriggerField((int) ((e.getX() + xInset / zoom) * zoom),
									(int) ((e.getY() + yInset / zoom) * zoom),
									(int) ((e.getX() + xInset / zoom) * zoom),
									(int) ((e.getY() + yInset / zoom) * zoom)));
							text.setText("Set lower right corner");
							mode = "TF_END";
						}

						else if (mode == "TF_END") {
							tf.get(tf.size() - 1).x2 = (int) ((e.getX() + xInset / zoom) * zoom);
							tf.get(tf.size() - 1).y2 = (int) ((e.getY() + yInset / zoom) * zoom);
							text.setText("Set triggering token");
							mode = "TF_TRIGGERING";
						}

						else if (mode == "TF_TRIGGERING") {
							tf.get(tf.size() - 1).name = stuff.get(slot).name;
							text.setText("Add tokens to spawn");
							mode = "TF_ADDSPAWNS";
						}

						else if (mode == "TF_ADDSPAWNS") {
							Stuff addMe = new Stuff(stuff.get(slot).name, stuff.get(slot).xPos, stuff.get(slot).yPos,
									stuff.get(slot).xSize, stuff.get(slot).ySize, stuff.get(slot).token);
							tf.get(tf.size() - 1).spawnables.add(addMe);
							text.setText(stuff.get(slot).name + " added");
						}
					}

					else if (mode == "PATROL") { //add patrol wayxpoints
						// text.setText("Added waypoint at " + e.getX() + "/" + e.getY());

						try { // add waypoint
							String message = "WAYPOINT_" + ((e.getX() + xInset / zoom) * zoom) / H + "_"
									+ ((e.getY() + yInset / zoom) * zoom) / H;
							dos.writeObject(message);
							dos.flush();
						} catch (IOException e1) {
						}
					}

					else if (e.getX() < 23 * H - Hx && mode == "MANUALTRIGGER") { // manualy trigger
						for (int i = 0; i < tf.size(); i++) { // check for triggers
							if (e.getX() >= (tf.get(i).x1 - xInset) / zoom && e.getX() <= (tf.get(i).x2 - xInset) / zoom
									&& e.getY() >= (tf.get(i).y1 - yInset) / 2
									&& e.getY() <= (tf.get(i).y2 - yInset) / zoom) {
								try { //spawn triggered tokens
									String message = "TRIGGER";
									for (int j = 0; j < tf.get(i).spawnables.size(); j++) {
										String temp = "_" + tf.get(i).spawnables.get(j).name + "_"
												+ (double) (tf.get(i).spawnables.get(j).xPos) / H + "_"
												+ (double) (tf.get(i).spawnables.get(j).yPos) / H + "_"
												+ tf.get(i).spawnables.get(j).xSize / H + "_"
												+ tf.get(i).spawnables.get(j).ySize / H;
										message = message.concat(temp);
									}
									System.out.println(message);
									dos.writeObject(message);
									dos.flush();
								} catch (IOException e1) {
								}
								if (!testTriggers) {
									tf.remove(i);
								}
								mode = "PLAY";
							}
						}
					}

					else if (e.getX() < 23 * H - Hx && mode == "OPEN") { // find shadow to open doors
						int slot = 0;
						double min = Integer.MAX_VALUE;
						for (int i = 0; i < shade.size(); i++) { // calculate nearest shadow
							if (shade.get(i).door == true || breakWalls) {
								double x = Math.pow(
										(((shade.get(i).x1 + shade.get(i).x2) / 2) / zoom) - (xInset / zoom) - e.getX(), 2);
								double y = Math.pow(
										(((shade.get(i).y1 + shade.get(i).y2) / 2) / zoom) - (yInset / zoom) - e.getY(), 2);
								double z = Math.sqrt(x + y);

								if (z < min) {
									min = z;
									slot = i;
								}
							}
							if (min < H / zoom) { // remove nearst door in range
								try { // open door
									String message = "OPEN_" + slot;
									dos.writeObject(message);
									dos.flush();
								} catch (IOException e1) {
								}
							}
						}
						mode = "PLAY";
						setCursor(Cursor.getDefaultCursor());
					}

					else if (e.getX() < 23 * H  - Hx && mode == "PING") {
						try { // send ping at location
							String message = "PING_" + myName + "_" + ((e.getX() + xInset / zoom) * zoom) / H + "_"
									+ ((e.getY() + yInset / zoom) * zoom) / H;
							dos.writeObject(message);
							dos.flush();
						} catch (IOException e1) {
						}
						mode = "PLAY";
						setCursor(Cursor.getDefaultCursor());
					}

					else if (e.getX() < 23 * H - Hx&& mode == "SHADE") { // create shadows
						if (firstShade) { // create startpoint of shadow
							createShadow[0] = (e.getX() + xInset / zoom) * zoom;
							createShadow[1] = (e.getY() + yInset / zoom) * zoom;
							createShadow[2] = (e.getX() + xInset / zoom) * zoom;
							createShadow[3] = (e.getY() + yInset / zoom) * zoom;
							firstShade = false;
						} else { // create endpoint of shadow, then create start of new shadow at end of previous
							createShadow[2] = (e.getX() + xInset / zoom) * zoom;
							createShadow[3] = (e.getY() + yInset / zoom) * zoom;

							try { // send shadow to others
								String message = "SHADE_" + door + "_" + createShadow[0] / H + "_" + createShadow[1] / H
										+ "_" + createShadow[2] / H + "_" + createShadow[3] / H;
								createShadow[0] = (e.getX() + xInset / zoom) * zoom;
								createShadow[1] = (e.getY() + yInset / zoom) * zoom;
								dos.writeObject(message);
								dos.flush();
							} catch (IOException e1) {
							}
						}
					}

					else { // menu

						// left aligned
						if (e.getX() >= (int) (23.5 * H) - Hx && e.getX() <= (int) (24.5 * H) - Hx) {

							// menu for both players and DM
							for (int i = 0; i < followers.size(); i++) { // select viewer from follower icons
								if (e.getY() >= (int) (3 * H) + (1.5 * H * i)
										&& e.getY() <= (int) (4 * H) + (1.5 * H * i)) {
									for (int j = 0; j < stuff.size(); j++) {
										if (stuff.get(j).name.equals(followers.get(i).name)) {
											viewer = followers.get(i).name;
											text.setText("You are now " + viewer);
											break;
										}
									}
								}
							}

							if (e.getY() >= (int) (1.5 * H) && e.getY() <= (int) (2.5 * H)) { // delete lifted
								if (lift != 1000000 && !stuff.get(lift).name.equals(viewer)) {
									try { // send info to remove old token
										String message = "DELETE_" + lift + "_" + myName;
										dos.writeObject(message);
										dos.flush();
									} catch (IOException e1) {
									}
								}
							}
						}

						// right aligned
						else if (DM && e.getX() >= (int) (25.5 * H) - Hx && e.getX() <= (int) (26.5 * H) - Hx) { // DM
							if (e.getY() >= (int) (1.5 * H) && e.getY() <= (int) (2.5 * H)) { // switch follower mode
								follow = !follow;
							}

							else if (e.getY() >= (int) (3 * H) && e.getY() <= (int) (4 * H)) { // set mode to open
								mode = "OPEN";
								Cursor c = tk.createCustomCursor(key, new Point(0, 0), "img");
								setCursor(c);
							}

							else if (e.getY() >= (int) (4.5 * H) && e.getY() <= (int) (5.5 * H)) { // roll init
								command("ROLLINIT");
							}
						}

						else if (!DM && e.getX() >= (int) (25 * H) - Hx && e.getX() <= (int) (25.5 * H) - Hx) { // left player dice

							if (e.getY() >= (int) (1.5 * H) && e.getY() <= (int) (2 * H)) { // d20
								text.setText("Added D20");
								rolls.add(20);
							} else if (e.getY() >= (int) (2.5 * H) && e.getY() <= (int) (3 * H)) { // d10
								text.setText("Added D10");
								rolls.add(10);
							} else if (e.getY() >= (int) (3.5 * H) && e.getY() <= (int) (4 * H)) { // d6
								text.setText("Added D6");
								rolls.add(6);
							} else if (e.getY() >= (int) (4.5 * H) && e.getY() <= (int) (5 * H)) { // player roll
								text.setText("ROLLING" + rolls);

								String rolledDie = "";
								int rollSum = 0;

								for (int each : rolls) {
									int dieRoll = (int) (Math.random() * each) + 1;

									rolledDie = rolledDie
											.concat(Integer.toString(each) + ">" + Integer.toString(dieRoll) + " ");
									rollSum += dieRoll;
								}
								plusMod = Integer.parseInt(modifier.getText());
								rollSum += plusMod;
								rolledDie = rolledDie.concat(" + " + plusMod + " = " + Integer.toString(rollSum));

								try {
									dos.writeObject("MESSAGE_" + myName + "_" + rolledDie);
									dos.flush();
								} catch (IOException e2) {
									e2.printStackTrace();
								}
							} else if (e.getY() >= (int) (4.5 * H) && e.getY() <= (int) (6 * H)) { // player roll
								command("DROP");
							}
						}

						else if (!DM && e.getX() >= (int) (26 * H) - Hx && e.getX() <= (int) (26.5 * H) - Hx) { // right player dice

							if (e.getY() >= (int) (1.5 * H) && e.getY() <= (int) (2 * H)) { // d12
								text.setText("Added D12");
								rolls.add(12);
							} else if (e.getY() >= (int) (2.5 * H) && e.getY() <= (int) (3 * H)) { // d8
								text.setText("Added D8");
								rolls.add(8);
							} else if (e.getY() >= (int) (3.5 * H) && e.getY() <= (int) (4 * H)) { // d4
								text.setText("Added D4");
								rolls.add(4);
							}
						}
					}
				} else {
					char[] wait = "wait for other players".toCharArray();
					for(int i = 0; i < wait.length; i++){
						if (Math.random() > 0.5) {
							wait[i] = Character.toUpperCase(wait[i]);
						}
					}
					text.setText(new String(wait));
				}
			}
		});		
	}

	protected void command(String input) { //interpret text input
		String[] temp = input.split("\\s+");

		if (temp[0].equals("PUT")) { // create token
			if(temp.length == 2) { //not size specific
				try {
					dos.writeObject("CREATE_" + temp[1].toUpperCase() + "_1_1_" + myName);
					dos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(temp.length == 4) { //uses given x/y sizes
				try {
					dos.writeObject("CREATE_" + temp[1].toUpperCase() + "_" + Double.parseDouble(temp[2]) + "_" + Double.parseDouble(temp[3]) + "_" + myName);
					dos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
//		else if ((temp[0].equals("FOLLOW") || temp[0].equals("FOLLOWER") || temp[0].equals("FOLLOWERS")) && temp.length >= 2) { // change followers
//			try {
//				pr = new PrintWriter("C:\\SPRITES\\Encounter\\config.txt");
//				
//				followers = new ArrayList<Stuff>();
//				
//				for(int i = 1; i < temp.length; i++) {
//					pr.println(temp[i]);
//					followers.add(new Stuff(temp[i]));
//					
//					new imageLoader(temp[i]); 
//					
//					if(!DM && i == 2) { //stop at two followers for players
//						break;
//					}
//				}
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//			pr.close();
//		}

		else if (temp[0].equals("ZOOM")) { // change map zoom
			zoom = Double.parseDouble(temp[1]);
		}
		
		else if (temp[0].equals("DROP")) { // reset all dice
			rolls = new ArrayList<Integer>();
		}
		
		else if (temp[0].equals("PING")) { // ping
			mode = "PING";
		}
		
		else if (temp[0].equals("NOPING")) { // remove ping
			try { //send ping off screen
				String message = "PING_" + myName + "_" + -1000 + "_" + -1000;
				dos.writeObject(message);
				dos.flush();
			} catch (IOException e1) {
			}
		}
		
		else if (temp[0].equals("NAME") && temp.length == 2) { // change user name
			try {
				dos.writeObject("RENAME_" + myName + "_" + temp[1]);
				dos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		else if (temp[0].equals("VIEWER") && temp.length == 2) { // change viewer to valid token
			for (int j = 0; j < stuff.size(); j++) {
				if (stuff.get(j).name.equals(temp[1])) {
					viewer = temp[1];
					text.setText("You are now " + viewer);
					break;
				}
			}
		}
		
		else if (temp[0].equals("INIT") && (temp.length == 2 || (temp.length == 3 && temp[2].contains("ADV")))) { // set initiative bonus
			initBonus = Integer.parseInt(temp[1]);
			String x = ("SETINIT_" + myName + "_" + initBonus + "_false");
			if(temp.length > 2 && temp[2].contains("ADV")) {
				haveAdv = true;
				x = ("SETINIT_" + myName + "_" + initBonus + "_true");
			}
			text.setText("INIATIVE SET");
			try {
				dos.writeObject(x);
				dos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		else if (temp[0].equals("SAVE") && temp.length == 2) { //save current setup to txt
			try {
				pr = new PrintWriter("C:\\SPRITES\\Encounter\\" + temp[1].toUpperCase() + ".txt"); //txt name
				
				pr.println((double) xInset / H); //xInset
				pr.println((double) yInset / H); //yInset
				pr.println(zoom); //zoom
				
				
				pr.println("STUFFARRAY");
				for(Stuff each : stuff) {
					pr.println(each.name +"_"+ (double) each.xPos / H +"_"+ (double) each.yPos / H +"_"+ each.xSize / H +"_"+ each.ySize / H);
				}
				
				pr.println("SHADOWARRAY");
				for(Shadow each : shade) {
					pr.println((double) each.x1 / H +"_"+ (double) each.y1 / H +"_"+ (double) each.x2 / H +"_"+ (double) each.y2 / H +"_"+ each.door);
				}
				
				pr.println("TRIGGERARRAY");
				for(TriggerField each : tf) {
					pr.println(each.name +"_"+ (double) each.x1 / H +"_"+ (double) each.y1 / H +"_"+ (double) each.x2 / H +"_"+ (double) each.y2 / H);
					for(Stuff each2 : each.spawnables) {
						pr.println(each2.name +"_"+ (double) each2.xPos / H +"_"+ (double) each2.yPos / H +"_"+ each2.xSize / H +"_"+ each2.ySize / H);
					}
					pr.println("NEXT"); // marker between each trigger because of undefined length
				}
				pr.close();
				text.setText("Saved to " + temp[1]);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				text.setText("Failed to save to " + temp[1]);
			}
		}
		
		else if (temp[0].equals("MASTER")) { //swap player/DM mode
			DM = !DM;

			if(DM && chat.size() >= 10) { //reduce chat size to fit DM window
				 ArrayList<String> smallChat = new ArrayList<String>();
				 for(int i = chat.size() - 11; i < chat.size() - 1; i++) {
					 smallChat.add(chat.get(i));
				 }
				 chat.clear();
				 chat = smallChat;
			}
		}

		else if (temp[0].contains("/")) { //send message to others
			try {
				String message = input.substring(1, input.length());
				
				dos.writeObject("MESSAGE_" + myName + "_" + message);
				dos.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		else if (temp[0].equals("HELP")) {	//show help [TOO LARGE!]
			chat = new ArrayList<String>();
			chat.add("Add a token: ");
			chat.add("'Put [NAME]' or 'Put [NAME] [xSIZE] [ySIZE]'");
			
//			chat.add("Change followers: ");
//			chat.add("'Follow [NAME] [NAME] [NAME] ... '");
			
			chat.add("Change zoom: ");
			chat.add("'Zoom [NUMBER FROM 0.1-10.0]'");
			
			chat.add("Set a ping: ");
			chat.add("'Ping'");
			
			chat.add("Change user name: ");
			chat.add("'Name [NAME]'");
			
			chat.add("Change viewing token: ");
			chat.add("'Viewer [NAME]'");
			
			chat.add("Set initiative bonus: 'Init [NUMBER OF INITBONUS] ['adv' if you have advantage]'");
			chat.add("'Init [NUMBER OF INITBONUS] ['adv' if you have advantage]'");
			
			chat.add("Save game to file: ");
			chat.add("'Save [NAME]'");
			
			chat.add("Swap between DM and player: ");
			chat.add("'Master'");
		}
	
		
		if(DM) { //DM commands
			if ((temp[0].equals("LOAD") || temp[0].equals("PRE")) && temp.length == 2) { // everyone preloads the tokens of map without showing them
				
				try {
					br = new BufferedReader(new FileReader("C:\\SPRITES\\Encounter\\" + temp[1] + ".txt"));
					
					String load = "";
					String line;
					while((line = br.readLine()) != null) {
						load = load.concat(line +  "\n");
					}
					try {
						dos.writeObject(temp[0] + "_" + load);
						dos.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			else if (temp[0].equals("OPEN") && temp.length == 2) { // opens numbered door
				try {				
					dos.writeObject("OPEN_" + temp[1]);
					dos.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			
			else if (temp[0].equals("ROLLINIT")) { //roll initiative
				try {
					dos.writeObject("MESSAGE_" + myName + "_<INIATIVE ROLLS>");
					dos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				for(Player each : players) {
					try {
						String playerInit = rollInit(each.initBonus, each.adv);					
						dos.writeObject("MESSAGE_" + each.name + "_" + playerInit);
						dos.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}

			else if (temp[0].equals("CLEAR")) { // clear map
				stuff.clear();
				shade.clear();
				tf.clear();
				text.setText("CLEARED");
			}

			else if (temp[0].equals("DOOR")) { // create doors instead of shadows
				door = !door;
			}
			
			else if (temp[0].equals("BREAK")) { // delete walls AND doors when opening
				breakWalls = !breakWalls;
			}

			else if (temp[0].equals("SHOWTRIGGERS")) { // show trigger boxes
				showTriggers = !showTriggers;
			}
			
			else if (temp[0].equals("SHOWSHADOWS")) { // show shadows
				showShadows = !showShadows;
			}

			else if (temp[0].equals("TESTTRIGGERS")) { // trigger without deleting field
				testTriggers = !testTriggers;
			}

			else if (temp[0].equals("TRIGGER")) { // manual trigger on click
				mode = "MANUALTRIGGER";
				text.setText("MANUAL TRIGGER");
			}

			else if (temp[0].equals("SIGHT")) { // enable full vision
				viewer = "LOL";
			}
			
			else if (temp[0].equals("HALT")) { // stop patroling token [DELETES ALL PATROLS!]
				mode = "PLAY";
				try {
					String message = "HALT_" + lift;					
					dos.writeObject(message);
					dos.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setFont(font);
		
		for(Stuff each : stuff) { //find loaded images and apply to "NOTFOUND" tokens
			if(each.token == null) {
				for(Stuff each2 : namedImg) {
					if(each.name.equalsIgnoreCase(each2.name)) {
						each.token = each2.token;
					}
				}
			}
		}
		
//		for(Stuff each : followers) { //find loaded images
//			if(each.token == null) {
//				for(Stuff each2 : namedImg) {
//					if(each.name.equalsIgnoreCase(each2.name)) {
//						each.token = each2.token;
//					}
//				}
//			}
//		}		
		
		// draw things in order of layering

		// draw field
		g.drawImage(fieldImg, 0 - (xInset), 0 - (yInset), 23 * H, sH, null);
//		g.drawImage(fieldImg, 0 - (xInset) + 23 * H, 0 - (yInset), 23 * H, sH, null);
//		g.drawImage(fieldImg, 0 - (xInset), 0 - (yInset) + sH, 23 * H, sH, null);
//		g.drawImage(fieldImg, 0 - (xInset) + 23 * H, 0 - (yInset) + sH, 23 * H, sH, null);

		// draw tokens
		for (Stuff each : stuff) {
			if(each.token != null) { //draws "NOTFOUND" placeholder for tokens with no image
				g.drawImage(each.token, (int) ((each.xPos - xInset) / zoom), (int) ((each.yPos - yInset) / zoom), (int) (each.xSize / zoom), (int) (each.ySize / zoom), null);
			}
			else { //draws tokens with images
				g.drawImage(notFound, (int) ((each.xPos - xInset) / zoom + ((each.xSize / zoom) / 2))  , (int) ((each.yPos - yInset) / zoom + ((each.ySize / zoom) / 2)), (int) (H / zoom), (int) (H / zoom), null);
			}
		}

		// draw shadows
		g.setColor(Color.BLACK);
		for (int i = 0; i < stuff.size(); i++) { // search viewer as center of line-of-sight - multiple instances of viewer OVERLAP!
			if (stuff.get(i).name.equals(viewer)) { // find correct viewer position
				int xPlayer = (int) ((stuff.get(i).xPos - xInset + (stuff.get(i).xSize / 2)) / zoom);
				int yPlayer = (int) ((stuff.get(i).yPos - yInset + (stuff.get(i).ySize / 2)) / zoom);
				for (int j = 0; j < shade.size(); j++) { // create four corners of shadow-trapezoid, then add to polygon and draw
					Point a = new Point((int) ((shade.get(j).x1 - xInset) / zoom),
							(int) ((shade.get(j).y1 - yInset) / zoom));
					Point b = new Point((int) ((shade.get(j).x2 - xInset) / zoom),
							(int) ((shade.get(j).y2 - yInset) / zoom));
					Point c = new Point(b.x + (b.x - xPlayer) * 100, b.y + (b.y - yPlayer) * 100);
					Point d = new Point(a.x + (a.x - xPlayer) * 100, a.y + (a.y - yPlayer) * 100);

					Polygon shadows = new Polygon();
					shadows.addPoint(a.x, a.y);
					shadows.addPoint(b.x, b.y);
					shadows.addPoint(c.x, c.y);
					shadows.addPoint(d.x, d.y);
					g.fillPolygon(shadows);
				}
				break;
			}
		}
		
		if(ping != null) { //draw last ping in players color
			Color playerColor = new Color((int) stringToSeed(ping.name));
			
			BufferedImage tinted =  applyColorFilter(pingIcon, (int) (((double) playerColor.getRed() / 255) * 100),  (int) (((double) playerColor.getGreen() / 255) * 100), (int) (((double) playerColor.getBlue() / 255) * 100));
			g.drawImage(tinted, (int) ((ping.xPos - xInset - (H/2)) / zoom)    , (int) ((ping.yPos - yInset - (H/2)) / zoom)    , (int) (H / zoom), (int) (H / zoom), null);
		}

		// draw visible shadow/door lines
		if (mode == "SHADE" || showShadows) {
			for (int i = 0; i < shade.size(); i++) {
				if (shade.get(i).door) {
					g.setColor(Color.GREEN);
					g.drawString(String.valueOf(i) ,(int) ((shade.get(i).x1 - xInset) / zoom), (int) ((shade.get(i).y1 - yInset) / zoom));
				} else {
					g.setColor(Color.RED);
				}
				g.drawLine((int) ((shade.get(i).x1 - xInset) / zoom), (int) ((shade.get(i).y1 - yInset) / zoom), (int) ((shade.get(i).x2 - xInset) / zoom), (int) ((shade.get(i).y2 - yInset) / zoom));
				}
		}

		// draw visible triggerfields
		if (mode.contains("TF_") || showTriggers == true) {
			g.setColor(Color.GREEN);
			for (TriggerField each : tf) {
				g.drawRect((int) ((each.x1 - xInset) / zoom), (int) ((each.y1 - yInset) / zoom), (int) ((each.x2 - each.x1) / zoom), (int) ((each.y2 - each.y1) / zoom));
				for(int i = 0; i < each.spawnables.size(); i++) {
					g.drawString(each.spawnables.get(i).name, (int) ((each.x1 - xInset) / zoom) + 10, (int) ((each.y1 - yInset) / zoom) + 12 * (i+1) );
				}
			}
		}
		
		// draw visible waypoints
		if(mode == "PAROL") {
			g.setColor(Color.WHITE);
			for (Patrol each : patrols) {
				for(int i = 0; i < each.wayPoints.size() - 1; i++) {
					g.drawLine(each.wayPoints.get(i).xPos, each.wayPoints.get(i).yPos, each.wayPoints.get(i + 1).xPos, each.wayPoints.get(i + 1).yPos);
				}
			}
		}

		// menu

		// gray background
		g.setColor(Color.BLACK);
		g.drawImage(menuBG, 23 * H - Hx, 0, 4 * H, sH, null);

		// draw lifted block/token
		g.drawImage(block, (int) (23.5 * H) - Hx, (int) (1.5 * H), H, H, null);
		if (lift != 1000000) {
			if(stuff.get(lift).token != null) {
				g.drawImage(stuff.get(lift).token, (int) (23.5 * H) - Hx, (int) (1.5 * H), H, H, null);
			}
			else {
				g.drawImage(notFound, (int) (23.5 * H) - Hx, (int) (1.5 * H), H, H, null);
			}
		}
		
//		// draw followers
//		for (int i = 0; i < followers.size(); i++) {
//			g.drawImage(followers.get(i).token, (int) (23.5 * H), (int) (3 * H) + (int) (1.5 * H * i), H, H, null);
//		}

		g.setColor(Color.BLACK);
		if(DM) {
			g.drawString(mode, 23 * H, (int) (H * 1.3)); //shows current mode
			
			// draw follower toggle button
			if (follow) {
				g.drawImage(go, (int) (25.5 * H) - Hx, (int) (1.5 * H), H, H, null);
			} else {
				g.drawImage(stop, (int) (25.5 * H) - Hx, (int) (1.5 * H), H, H, null);
			}

			// draw open button
			g.drawImage(key, (int) (25.5 * H) - Hx, (int) (3 * H), H, H, null);
			
			// draw roll init button
			g.drawImage(init, (int) (25.5 * H) - Hx, (int) (4.5 * H), H, H, null);
			
//			//draw small chat
//			for (int i = 0; i < chat.size(); i++) {
//				
//				String[] s = chat.get(i).split("_");
//				
//				if(s.length > 1) {	
//					nameColor = new Color((int) stringToSeed(s[0]));
//					reverse = ((299 * nameColor.getRed() + 587 * nameColor.getGreen() + 114 * nameColor.getBlue()) / 1000) >= 128 ? Color.black : Color.white;
//					
//					g.setColor(reverse);
//					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) + 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) - 1);
//					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) + 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) + 1);
//					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) - 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) - 1);
//					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) - 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) + 1);
//
//					g.setColor(nameColor);
//					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) - Hx, (int) (12 * H) + (int) (0.3 * H * i));
//					
//				}
//				else {
////					Color nameColor = new Color((int) stringToSeed(s[0]));
////					Color reverse = ((299 * nameColor.getRed() + 587 * nameColor.getGreen() + 114 * nameColor.getBlue()) / 1000) >= 128 ? Color.black : Color.white;
//					
//					g.setColor(reverse);
//					g.drawString(chat.get(i), (int) (23.5 * H) + 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) - 1);
//					g.drawString(chat.get(i), (int) (23.5 * H) + 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) + 1);
//					g.drawString(chat.get(i), (int) (23.5 * H) - 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) - 1);
//					g.drawString(chat.get(i), (int) (23.5 * H) - 1 - Hx, (int) (12 * H) + (int) (0.3 * H * i) + 1);
//
//					g.setColor(nameColor);
//					g.drawString(chat.get(i), (int) (23.5 * H) - Hx, (int) (12 * H) + (int) (0.3 * H * i));
//				}
//			}
		}
		
		else {
			//draw dice
			g.drawImage(d20, (int) (25 * H) - Hx, (int) (1.5 * H), H/2, H/2, null);
			g.drawImage(d12, (int) (26 * H) - Hx, (int) (1.5 * H), H/2, H/2, null);
			
			g.drawImage(d10, (int) (25 * H) - Hx, (int) (2.5 * H), H/2, H/2, null);
			g.drawImage(d8, (int) (26 * H) - Hx, (int) (2.5 * H), H/2, H/2, null);
			
			g.drawImage(d6, (int) (25 * H) - Hx, (int) (3.5 * H), H/2, H/2, null);
			g.drawImage(d4, (int) (26 * H) - Hx, (int) (3.5 * H), H/2, H/2, null);
			
			g.drawImage(init, (int) (25 * H) - Hx, (int) (4.5 * H), H/2, H/2, null);
			
			g.drawImage(recycle, (int) (25 * H) - Hx, (int) (5.5 * H), H/2, H/2, null);
			
			int twenty = 0, twelve = 0, ten = 0, eight = 0, six = 0, four = 0;
			for(int each : rolls) {
				if(each == 20) {twenty++;}
				else if(each == 12) {twelve++;}
				else if(each == 10) {ten++;}
				else if(each == 8) {eight++;}
				else if(each == 6) {six++;}
				else if(each == 4) {four++;}
			}
			//draw number of dice
			g.drawString(Integer.toString(twenty), (int) (25 * H) - Hx, (int) (1.5 * H));
			g.drawString(Integer.toString(twelve), (int) (26 * H) - Hx, (int) (1.5 * H));
			g.drawString(Integer.toString(ten), (int) (25 * H) - Hx, (int) (2.5 * H));
			g.drawString(Integer.toString(eight), (int) (26 * H) - Hx, (int) (2.5 * H));
			g.drawString(Integer.toString(six), (int) (25 * H) - Hx, (int) (3.5 * H));
			g.drawString(Integer.toString(four), (int) (26 * H) - Hx, (int) (3.5 * H));
			
			g.drawString("+", (int) (25.82 * H) - Hx, (int) (4.82 * H));
			
		}
		
			//draw chat for all
			for (int i = 0; i < chat.size(); i++) {
				
				String[] s = chat.get(i).split("_");

				if(s.length > 1) {
					
					nameColor = new Color((int) stringToSeed(s[0]));
					reverse = ((299 * nameColor.getRed() + 587 * nameColor.getGreen() + 114 * nameColor.getBlue()) / 1000) >= 128 ? Color.black : Color.white;
					
					g.setColor(reverse); //draw outline
					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) + 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) - 1);
					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) + 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) + 1);
					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) - 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) - 1);
					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) - 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) + 1);

					g.setColor(nameColor); //draw text
					g.drawString(s[0] + ":" + s[1], (int) (23.5 * H) - Hx, (int) (6.5 * H) + (int) (0.3 * H * i));
					
				}
				else {
					g.setColor(reverse); //draw outline
					g.drawString(chat.get(i), (int) (23.5 * H) + 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) - 1);
					g.drawString(chat.get(i), (int) (23.5 * H) + 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) + 1);
					g.drawString(chat.get(i), (int) (23.5 * H) - 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) - 1);
					g.drawString(chat.get(i), (int) (23.5 * H) - 1 - Hx, (int) (6.5 * H) + (int) (0.3 * H * i) + 1);

					g.setColor(nameColor); //draw text
					g.drawString(chat.get(i), (int) (23.5 * H) - Hx, (int) (6.5 * H) + (int) (0.3 * H * i));
				}
			}
	}
	
	//Multiplayer functions
	
	private void interpretMessage(String message) { //interpret received messages from other players through the server
		System.out.println("Received: " + message);
		
		String[] temp = new String[1];
		temp[0] = "";
		if(!message.contains("LOAD")) { //load and preload messages are too long and are cut here
			temp = message.split("_");
		}
		else if(message.contains("LOAD") && !message.contains("PRELOAD")) {
			temp = message.split("LOAD_");
			temp[0] = "LOAD";
		}
		else if(message.contains("PRELOAD")){
			temp = message.split("PRELOAD_");
			temp[0] = "PRE";
		}
		
		if (temp[0].equals("UNRESTRICT")) { //moves token
			unrestricted = true;
			text.setText("Restriction lifted");
			if(DM) {
				text.requestFocusInWindow();
			}
		}
		
		if (temp[0].equals("MOVE")) { //moves token
			stuff.get(Integer.parseInt(temp[1])).xPos = (int) (Double.parseDouble(temp[3]) * H);
			stuff.get(Integer.parseInt(temp[1])).yPos = (int) (Double.parseDouble(temp[4]) * H);
			stuff.get(Integer.parseInt(temp[1])).xSize = Double.parseDouble(temp[5]) * H;
			stuff.get(Integer.parseInt(temp[1])).ySize = Double.parseDouble(temp[6]) * H;
		}
		
		else if (temp[0].equals("CREATE")) { //puts down new token outside visible area before adding it to the map
			BufferedImage token = null;
			new imageLoader(temp[1]);
						
			if(temp[1].contains("#")) { //adds map at fixed coordinates
				stuff.add(new Stuff(temp[1], 0, 0, Double.parseDouble(temp[2]) * H, Double.parseDouble(temp[3]) * H, token));
			}
			else { //add token
				stuff.add(new Stuff(temp[1], -1000, -1000, Double.parseDouble(temp[2]) * H, Double.parseDouble(temp[3]) * H, token));
				
				if(myName.equals(temp[4])) { //if the command was send by yourself, sets lift to new token
					lift = stuff.size() - 1;
				}
			}
		}
		
		else if (temp[0].equals("DELETE")) { //deletes token
			stuff.remove(Integer.parseInt(temp[1]));
			if(lift == Integer.parseInt(temp[1])) { //deleted by yourself
				lift = 1000000;
			}
			else if(lift != 1000000 && Integer.parseInt(temp[1]) < lift) { //deleted by someone else
				lift--;
			}
		}
		
		else if(temp[0].equals("SHADE")){ //makes new wall/door
			shade.add(new Shadow((int) (Double.parseDouble(temp[2]) * H), (int) (Double.parseDouble(temp[3]) * H), (int) (Double.parseDouble(temp[4]) * H), (int) (Double.parseDouble(temp[5]) * H), Boolean.parseBoolean(temp[1])));
		}
		
		else if(temp[0].equals("OPEN")){ //opens door
			shade.remove(Integer.parseInt(temp[1]));
		}
		
		else if(temp[0].equals("PATROL")){ //adds new patrol
			patrols.add(new Patrol(stuff.get(Integer.parseInt(temp[1]))));
		}
		
		else if(temp[0].equals("WAYPOINT")){ //adds new waypoint to partol
			patrols.get(patrols.size() - 1).addWayPoint(new Stuff("X", (int) (Double.parseDouble(temp[1]) * H) , (int) (Double.parseDouble(temp[2]) * H)));
		}
		
		else if(temp[0].equals("HALT")){ //stops all patrol(s)
//			int check = Integer.parseInt(temp[1]);
//			for (Patrol each : patrols) {
//				if(each.moving == stuff.get(check)) {
//					patrols.remove(each);
//				}
//			}
			patrols = new ArrayList<Patrol>();
		}
		
		else if(temp[0].equals("RENAME")){ //renames player
			for(Player each : players) {
				if(each.name.equalsIgnoreCase(temp[1])) {
					if(myName.equalsIgnoreCase(temp[1])) { // if renaming self
						myName = temp[2];
					}
					each.name = temp[2];
				}
			}
		}
		
		else if(temp[0].equals("SETINIT")){ //sets player init bonus/advantage
			boolean exists = false;
			for(Player each : players) {
				if(each.name.equalsIgnoreCase(temp[1])) {
					exists = true;
					each.initBonus = Integer.parseInt(temp[2]);
					each.adv = Boolean.parseBoolean(temp[3]);
				}
			}
			if(!exists) { //adds new player if not previously known
				players.add(new Player(temp[1], Integer.parseInt(temp[2]), Boolean.parseBoolean(temp[3])));
			}
		}
				
		else if(temp[0].equals("MESSAGE") && temp.length >= 3){ //displays message
			ArrayList<String> toChat = new ArrayList<String>();
			String received = temp[1] + "_" + temp[2];
			
			for(int i = 0; i < (int) (received.length()/20) + 1; i++) { //chops message into 20 characters long paragraphs
				if(received.length() > (i+1)*20) {
					toChat.add(received.substring(i * 20, (i+1) * 20));
				}
				else {
					toChat.add(received.substring(i * 20, received.length()));
				}
			}
			
			for(String each : toChat) { //adds message to chat
				chat.add(each);
				if(chat.size() >= 30) {
					chat.remove(0);
				}
			}
		}
		
		else if(temp[0].equals("PING")){ //displays ping
			ping = new Stuff(temp[1], (int) (Double.parseDouble(temp[2]) * H) , (int) (Double.parseDouble(temp[3]) * H), 1, 1, null);
		}
		
		else if(temp[0].equals("TRIGGER")){ //creates trigger tokens
			for(int i = 1; i < temp.length; i++) {
				if(i % 5 == 1) {
					stuff.add(new Stuff(temp[i]));
					
					new imageLoader(temp[i]);
				}
				else if(i % 5 == 2) {
					stuff.get(stuff.size() - 1).xPos = (int) (Double.parseDouble(temp[i]) * H);
				}
				else if(i % 5 == 3) {
					stuff.get(stuff.size() - 1).yPos = (int) (Double.parseDouble(temp[i]) * H);
				}
				else if(i % 5 == 4) {
					stuff.get(stuff.size() - 1).xSize = Double.parseDouble(temp[i]) * H;
				}
				else if(i % 5 == 0) {
					stuff.get(stuff.size() - 1).ySize = Double.parseDouble(temp[i]) * H;
				}
			}
		}

		else if(temp[0].equals("LOAD")) { //loads file given by DM
			try {
				br = new BufferedReader(new StringReader(temp[1]));

				//resetting variables
				stuff = new ArrayList<Stuff>();
				shade = new ArrayList<Shadow>();
				tf = new ArrayList<TriggerField>();
				lift = 1000000;
				
				String line;

				xInset = (int) (Double.parseDouble(br.readLine()) * H);
				yInset = (int) (Double.parseDouble(br.readLine()) * H);
				zoom = Double.parseDouble(br.readLine());
				br.readLine();
				
				while((line = br.readLine()) != null) { //read stuff array
					if(line.equals("SHADOWARRAY")) {
						break;
					}
					String[] s = line.split("_");
					stuff.add(new Stuff(s[0], (int) (Double.parseDouble(s[1]) * H), (int) (Double.parseDouble(s[2]) * H), Double.parseDouble(s[3]) * H, Double.parseDouble(s[4]) * H));
					new imageLoader(s[0]);

				}
				while((line = br.readLine()) != null) { //read shadow array
					if(line.equals("TRIGGERARRAY")) {
						break;
					}
					String[] s = line.split("_");
					shade.add(new Shadow((int) (Double.parseDouble(s[0]) * H), (int) (Double.parseDouble(s[1]) * H), (int) (Double.parseDouble(s[2]) * H), (int) (Double.parseDouble(s[3]) * H), Boolean.parseBoolean(s[4])));
				}
				while((line = br.readLine()) != null) { //read triggerfield array
					String[] s = line.split("_");
					tf.add(new TriggerField(s[0], (int) (Double.parseDouble(s[1]) * H), (int) (Double.parseDouble(s[2]) * H), (int) (Double.parseDouble(s[3]) * H),  (int) (Double.parseDouble(s[4]) * H)));
					
					while((line = br.readLine()) != null) { //add spawnable tokens to triggerfield
						if(line.equals("NEXT")) {
							break;
						}
						String[] s2 = line.split("_");
						tf.get(tf.size() - 1).spawnables.add(new Stuff(s2[0], (int) (Double.parseDouble(s2[1]) * H), (int) (Double.parseDouble(s2[2]) * H), Double.parseDouble(s2[3]) * H, Double.parseDouble(s2[4]) * H));
						new imageLoader(s2[0]);
					}
				}
				
			}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			text.setText("Loading new map");
		}
		
		else if(temp[0].equals("PRE")){ //preload images from file
			try {
				br = new BufferedReader(new StringReader(temp[1]));
				String line;
				br.readLine();
				br.readLine();
				br.readLine();
				br.readLine();
				
				while((line = br.readLine()) != null) { //read stuff array
					if(line.equals("SHADOWARRAY")) {
						break;
					}
					String[] s = line.split("_");
					new imageLoader(s[0]);

				}
				while((line = br.readLine()) != null) { //read shadow array
					if(line.equals("TRIGGERARRAY")) {
						break;
					}
				}
				while((line = br.readLine()) != null) { //read triggerfield array
					
					while((line = br.readLine()) != null) { //add spawnable tokens ti triggerfield
						if(line.equals("NEXT")) {
							break;
						}
						String[] s2 = line.split("_");
						new imageLoader(s2[0]);
					}
				}
				
			}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private boolean connect() { //attempt to connect to host
		try {
			socket = new Socket(ip, port);
			dos = new ObjectOutputStream(socket.getOutputStream());
			dis = new ObjectInputStream(socket.getInputStream());
			accepted = true;
		} catch (IOException e) {
			System.out.println("Unable to connect to the address");
			return false;
		}
		System.out.println("Successfully connected to the server");
		return true;
	}
	
	public void run() { //waits for messages and updates
		while (true) {
			tick();
		}
	}
	
	private void tick() {
		if (accepted && !unableToCommunicateWithOpponent) {	//checks if your opponent has sent
			try {
				String message = dis.readObject().toString();
				interpretMessage(message);
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	static long stringToSeed(String s) { //turns the given string into a seed that can be used as a color: name -> color
	    if (s == null) {
	        return 0;
	    }
	    long hash = 0;
	    for (char c : s.toCharArray()) {
	        hash = 31L*hash + c;
	    }
	    return hash;
	}
	
	private String rollInit(int bonus, boolean playerAdv) { //rolls initiative
		int myInit = (int) (Math.random() * 20) + 1;
		if(playerAdv) {
			int second = (int) (Math.random() * 20) + 1;
			if(second > myInit) {
				myInit = second;
			}
		}
		return Integer.toString(myInit) + " + " + Integer.toString(bonus) + " = " + Integer.toString(myInit + bonus);	
	}
	
	public void importFollowers() {
		try { // import followers from config file
			br = new BufferedReader(new FileReader("C:\\SPRITES\\Encounter\\config.txt"));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					followers.add(new Stuff(line.toUpperCase()));
					new imageLoader(line);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}	

    private BufferedImage applyColorFilter(BufferedImage image, int redPercent, int greenPercent, int bluePercent) { //makes token redder?
    	
    	BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());    	 
        for(int y = 0; y < image.getHeight(); y++){
            for(int x = 0; x < image.getWidth(); x++){
                int pixel = image.getRGB(x,y);

                int alpha = (pixel>>24)&0xff;
                int red = (pixel>>16)&0xff;
                int green = (pixel>>8)&0xff;
                int blue = pixel&0xff;

                pixel = (alpha<<24) | (redPercent*red/100<<16) | (greenPercent*green/100<<8) | (bluePercent*blue/100);

                tinted.setRGB(x, y, pixel);
            }
        }
        return tinted;
    }
   
    
    class imageLoader implements Runnable { //loads images from URL
    	private Thread t;
    	String inputName;
    	
    	public imageLoader(String inputName) {
    		this.inputName = inputName;
    		start();
    	}
    	
    	public void start () {
    	      if (t == null) {
    	         t = new Thread (this);
    	         t.start ();
    	      }
    	   }
    	
    	@Override
    	public void run() {
    		for(Stuff each : namedImg) {
    			if(each.name.equalsIgnoreCase(inputName)) {
    	    		return;
    			}
    		}
    		
    		URLConnection connection = null;
    		try {
    			connection = new URL("https://sites.google.com/view/domassets").openConnection();
    			Scanner scanner = new Scanner(connection.getInputStream());
    			scanner.useDelimiter("\\Z");
    			content = scanner.next();
    			scanner.close();
    			htmlLines = content.split(">");
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}

    		String imageName = "\"" + inputName + "\"";

    		String rightLine = null;
    		for (String each : htmlLines) {
    			if (each.toUpperCase().contains(imageName.toUpperCase())) {
    				rightLine = each;
    				break;
    			}
    		}
    		if(rightLine == null) {
    			System.out.println("Can't find " + imageName);
    			return;
    		}

    		String[] nameLine = rightLine.split("\\s+");
    		String tokenNameLine = null;
    		for (String each : nameLine) {
    			if (each.contains("src=")) {
    				tokenNameLine = each;
    				break;
    			}
    		}

    		String[] cutFront = tokenNameLine.split("src=");
    		String finalURL = cutFront[1].substring(1, cutFront[1].length() - 1);

    		URL url;
    		BufferedImage output = null;
    		
    		for(Stuff each : namedImg) {
    			if(each.name.equalsIgnoreCase(inputName)) {
    	    		return;
    			}
    		}
    		
    		try {
    			url = new URL(finalURL);
    			output = ImageIO.read(url);
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		
    		for(Stuff each : namedImg) {
    			if(each.name.equalsIgnoreCase(inputName)) {
    	    		return;
    			}
    		}
    		
    		namedImg.add(new Stuff(inputName, output));
    		System.out.println("Loaded " + imageName);
    	}
    }


	@Override
	public void actionPerformed(ActionEvent e) { //repaints on timer
//		if(mode != "PATROL") {
			for(Patrol each : patrols) {
				each.move();
			}
//		}
		repaint(); // this acts as a clock for all moving parts
	}
}