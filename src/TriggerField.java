import java.util.ArrayList;

public class TriggerField {
	int x1, y1, x2, y2;
	String name; //triggering token
	ArrayList<Stuff> spawnables = new ArrayList<Stuff>();
	
	public TriggerField() {
	}
	
	public TriggerField(int x1, int y1, int x2, int y2) {
		this.x1 = x1;		//startX
		this.y1 = y1;		//startY
		this.x2 = x2;		//endX
		this.y2 = y2;		//endY
	}
	
	public TriggerField(String name, int x1, int y1, int x2, int y2) {
		this.name = name;
		this.x1 = x1;		//startX
		this.y1 = y1;		//startY
		this.x2 = x2;		//endX
		this.y2 = y2;		//endY
	}
}