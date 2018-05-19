package main;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class GroundItemManager {
	
	private static int groundItemIdSetter = 0;
	
	@Setter @Getter @AllArgsConstructor
	public static class GroundItem {
		private int id;
		private int x;
		private int y;
		private int groundItemId;
	}
	@Getter private static List<GroundItem> groundItems = new ArrayList<>();
	
	public static int generateGroundItemId() {
		if (++groundItemIdSetter > 999999)
			groundItemIdSetter = 0;
		return groundItemIdSetter;
	}
	
	public static void add(int itemId, int x, int y) {
		groundItems.add(new GroundItem(itemId, x, y, generateGroundItemId()));
	}
	
	public static void remove(int groundItemId) {
		GroundItem toRemove = null;
		for (GroundItem item : groundItems) {
			if (item.getGroundItemId() == groundItemId) {
				toRemove = item;
				break;
			}
		}
		
		if (toRemove != null)
			groundItems.remove(toRemove);
	}

	public static GroundItem getGroundItemByGroundItemId(int groundItemId) {
		for (GroundItem item : groundItems) {
			if (item.getGroundItemId() == groundItemId)
				return item;
		}
		return null;
	}
}
