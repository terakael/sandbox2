package responses;

import java.util.Map;
import java.util.stream.Collectors;

import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.InventoryItemDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.ClientResourceManager;
import requests.OpenRequest;
import requests.Request;
import types.StorageTypes;

@SuppressWarnings("unused")
public class BankResponse extends Response {
	private Map<Integer, InventoryItemDto> items;
	private int tileId;
	
	public BankResponse() {
		setAction("bank");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {		
		if (!(req instanceof OpenRequest))
			return;
		
		OpenRequest request = (OpenRequest)req;
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId != 53)// storage chest scenery id
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK);
			tileId = request.getTileId();
			
			// TODO if player isn't next to bank tile and player isn't god then bail
			responseMaps.addClientOnlyResponse(player, this);
			ClientResourceManager.addItems(player, items.values().stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
		}
	}
	
}
