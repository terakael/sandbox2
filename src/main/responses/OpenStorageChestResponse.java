package main.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.ClientResourceManager;
import main.processing.ConstructableManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.OpenRequest;
import main.requests.Request;
import main.scenery.constructable.Constructable;
import main.scenery.constructable.SmallStorageChest;
import main.types.StorageTypes;

public class OpenStorageChestResponse extends Response {
	private List<InventoryItemDto> items = null;
	private int tileId;
	private int slotCount;
	
	public OpenStorageChestResponse() {
		setAction("open_storage_chest");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenRequest request = (OpenRequest)req;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			
			Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
			if (constructable == null)
				return;
			
			if (!(constructable instanceof SmallStorageChest))
				return;
			
			SmallStorageChest chest = (SmallStorageChest)constructable;
			chest.open(player.getId()); // initial open sets up the storage space for the player, subsequent opens do nothign
			
			items = chest.getItems(player.getId());
			tileId = request.getTileId();
			slotCount = chest.getMaxSlots();
			responseMaps.addClientOnlyResponse(player, this);
			
			ClientResourceManager.addItems(player, items.stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
		}
	}
}
