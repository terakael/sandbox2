package responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Setter;
import processing.attackable.Player;
import processing.attackable.Ship;
import requests.Request;

@Setter
@SuppressWarnings("unused")
public class ShipInRangeResponse extends Response {
	@AllArgsConstructor
	public static class ShipLocation {
		private int hullSceneryId;
		private int captainId;
		private int tileId;
		private int remainingTicks;
		private int maxHp;
		private int maxArmour;
	}
	private List<ShipLocation> ships = new ArrayList<>();
	
	public ShipInRangeResponse() {
		setAction("ship_in_range");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	public void addInstances(Set<Ship> instances) {
		instances.forEach(ship -> ships.add(new ShipLocation(
				ship.getHullSceneryId(), 
				ship.getCaptainId(), 
				ship.getTileId(), 
				ship.getRemainingTicks(),
				ship.getMaxHp(),
				ship.getMaxArmour())));
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
