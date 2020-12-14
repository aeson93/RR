public class Shadow {
	int x1, y1, x2, y2;
	boolean door = false;
	
	public Shadow() {
	}
	
	public Shadow(int x1, int y1, int x2, int y2) {
		this.x1 = x1;	//startX
		this.y1 = y1;	//startY
		this.x2 = x2;	//endX
		this.y2 = y2;	//endY
	}
	
	public Shadow(int x1, int y1, int x2, int y2, boolean isDoor) {
		this.x1 = x1;	//startX
		this.y1 = y1;	//startY
		this.x2 = x2;	//endX
		this.y2 = y2;	//endY
		this.door = isDoor;
	}
}