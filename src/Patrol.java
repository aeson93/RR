import java.util.ArrayList;

public class Patrol {
	
	Stuff moving;
	ArrayList<Stuff> wayPoints = new ArrayList<Stuff>();
	int current = 0;
	double added = 0.01;
	double wayPercent = added;
	boolean returning = false;
	
	Patrol(){
		
	}
	
	Patrol(Stuff moving){
		this.moving = moving;
		wayPoints.add(new Stuff(moving.name, moving.xPos, moving.yPos));
	}
	
	public void addWayPoint(Stuff newPoint) {
		wayPoints.add(newPoint);
	}
	
	public void move(){
		if(current < wayPoints.size() - 1) {
			moving.xPos = (int) (wayPoints.get(current).xPos + (wayPoints.get(current + 1).xPos - wayPoints.get(current).xPos) * wayPercent);
			moving.yPos = (int) (wayPoints.get(current).yPos + (wayPoints.get(current + 1).yPos - wayPoints.get(current).yPos) * wayPercent);
			
			if(wayPercent < 0.99) {
				wayPercent += added;
			}
			else {
				current++;
				wayPercent = added;
			}
		}
		else {
			reverse(wayPoints);
			current = 0;
		}
	}
	
	public ArrayList<Stuff> reverse(ArrayList<Stuff> list) {
	    if(list.size() > 1) {                   
	    	Stuff value = list.remove(0);
	        reverse(list);
	        list.add(value);
	    }
	    return list;
	}
}
