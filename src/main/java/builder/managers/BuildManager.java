package builder.managers;

import lombok.Getter;

public class BuildManager {
	private static BuildManager instance = null;
	@Getter private int tileId;
	@Getter private int floor;
	
	private BuildManager() {}
	
	public static BuildManager get() {
		if (instance == null)
			instance = new BuildManager();
		return instance;
	}
	
	public void setPosition(int floor, int tileId) {
		this.floor = floor;
		this.tileId = tileId;
	}
}
