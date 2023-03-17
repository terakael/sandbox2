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
public class BankResponse extends WalkAndDoResponse {
	private Map<Integer, InventoryItemDto> items;
	private int tileId;
	
	public BankResponse() {
		// actually required because this is being instantiated from the OpenResponse controller instead of the ResponseFactory
		// should probs clean it up
		setAction("bank");
	}
	
	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId != 53)// storage chest scenery id
			return false;
		
		walkingTargetTileId = request.getTileId();
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK);
		tileId = request.getTileId();
		
		// TODO if player isn't next to bank tile and player isn't god then bail
		responseMaps.addClientOnlyResponse(player, this);
		ClientResourceManager.addItems(player, items.values().stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
	}
	
}
