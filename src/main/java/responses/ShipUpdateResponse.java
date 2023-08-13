package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;
import types.DamageTypes;

@SuppressWarnings("unused")
public class ShipUpdateResponse extends Response {
	@Setter private Integer captainId = null;
	@Setter private Integer remainingTicks = null;
	@Setter private Integer tileId = null;
	
	public ShipUpdateResponse() {
		setAction("ship_update");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
