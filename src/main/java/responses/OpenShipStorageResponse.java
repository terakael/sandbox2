package responses;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import database.dto.InventoryItemDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ClientResourceManager;
import processing.managers.ShipManager;
import requests.OpenShipStorageRequest;
import requests.Request;

public class OpenShipStorageResponse extends WalkAndDoResponse {
	private List<InventoryItemDto> items = null;
	private String name = "ship storage";
	private int tileId;
	private transient Ship ship = null;
	
	@Override
	protected boolean setTarget(Request req, Player player, ResponseMaps responseMaps) {
		ship = ShipManager.getShipByCaptainId(((OpenShipStorageRequest)req).getObjectId());
		if (ship != null) {
			walkingTargetTileId = PathFinder.getClosestWalkableTile(ship.getFloor(), ship.getTileId());
			return true;
		}
		
		return false;
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		if (ship.playerIsAboard(player.getId()))
			return true;
		
		return PathFinder.isNextTo(ship.getFloor(), player.getTileId(), walkingTargetTileId);
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		if (!ship.playerIsAboard(player.getId())) {
			setRecoAndResponseText(0, "you can't see the storage from outside the ship.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (ship.getCaptainId() != player.getId()) {
			setRecoAndResponseText(0, "the captain wouldn't want you snooping in there.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		items = ship.getStorage().getItems();
		ClientResourceManager.addItems(player, items.stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
		
		tileId = ship.getTileId();
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
