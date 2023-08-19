package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ShipAccessoryDao;
import database.dto.ShipAccessoryDto;
import lombok.Setter;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ClientResourceManager;
import processing.managers.ShipManager;
import requests.Request;

@SuppressWarnings("unused")
public class ShowShipAccessoriesResponse extends Response {
	private Set<ShipAccessoryDto> accessories;
	@Setter private int tileId; // the tileId of the ship we're potentially building
	
	public ShowShipAccessoriesResponse() {
		setAction("show_ship_accessories");
		setCombatInterrupt(false);
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		final Ship ship = ShipManager.getHullByPlayerId(player.getId());
		if (ship == null)
			return;
		tileId = ship.getTileId();
		
		// TODO will want to filter based off ship type
		accessories = ShipAccessoryDao.getShipAccessories();
		
		ClientResourceManager.addSpriteFramesAndSpriteMaps(player, accessories.stream().map(ShipAccessoryDto::getSpriteFrameId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, accessories.stream().map(ShipAccessoryDto::getPrimaryMaterialId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, accessories.stream().map(ShipAccessoryDto::getSecondaryMaterialId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
