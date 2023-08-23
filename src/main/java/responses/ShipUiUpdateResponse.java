package responses;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import processing.attackable.Player;
import processing.attackable.Ship;
import requests.Request;

@Builder
@AllArgsConstructor
public class ShipUiUpdateResponse extends Response {
	@Setter private String fishPopulation = null;
	@Setter private Integer depth = null;
	@Setter private List<String> boardedPlayers = null;
	@Setter private List<String> disembarkedPlayers = null;
	@Setter private List<Integer> accessorySpriteIds = null;
	public ShipUiUpdateResponse() {
		setAction("ship_ui_update");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
	}
	
	public void clientOnlyResponse(Player player, ResponseMaps responseMaps) {
		setAction("ship_ui_update");
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	public void passengerResponse(Ship ship, ResponseMaps responseMaps) {
		setAction("ship_ui_update");
		ship.getPassengers().forEach(player -> responseMaps.addClientOnlyResponse(player, this));
	}
}
