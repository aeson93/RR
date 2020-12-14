public class Player {
	String name = null;
	int initBonus = 0;
	boolean adv = false;

	public Player(String name, int initBonus, boolean adv) {
		this.name = name.toUpperCase();
		this.initBonus = initBonus;
		this.adv = adv;
	}
	
	public Player() {
	}
}