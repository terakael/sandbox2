package main.responses;

import java.util.HashMap;

import main.database.InventoryItemDto;
import main.database.PlayerStorageDao;
import main.database.SceneryDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.BankRequest;
import main.requests.Request;
import main.types.StorageTypes;

public class BankResponse extends Response {
	HashMap<Integer, InventoryItemDto> items;
	
	public BankResponse() {
		setAction("bank");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof BankRequest))
			return;
		
		BankRequest request = (BankRequest)req;
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId != 53)// storage chest scenery id
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK.getValue());
			
			// TODO if player isn't next to bank tile and player isn't god then bail
			responseMaps.addClientOnlyResponse(player, this);
		}
	}
	
}
