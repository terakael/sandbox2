package responses;

import java.util.List;
import java.util.stream.Collectors;

import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.BankManager;
import processing.managers.ClientResourceManager;
import requests.Request;
import types.StorageTypes;

@SuppressWarnings("unused")
public class BankResponse extends WalkAndDoResponse {
	private List<InventoryItemDto> items;
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
		items = BankManager.getStorage(player.getId()).getItems();
//		items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK);
		tileId = request.getTileId();
		
		// TODO if player isn't next to bank tile and player isn't god then bail
		responseMaps.addClientOnlyResponse(player, this);
		ClientResourceManager.addItems(player, items.stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
	}
	
}
