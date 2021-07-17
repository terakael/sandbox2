package main.responses;

import java.util.List;
import java.util.stream.Collectors;

import main.database.dto.InventoryItemDto;
import main.processing.ClientResourceManager;
import main.processing.ConstructableManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.processing.scenery.constructable.Constructable;
import main.processing.scenery.constructable.StorageChest;
import main.requests.OpenRequest;
import main.requests.Request;

@SuppressWarnings("unused")
public class OpenStorageChestResponse extends Response {
	private List<InventoryItemDto> items = null;
	private int tileId;
	private String name;
	
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
			
			if (!(constructable instanceof StorageChest))
				return;
			
			StorageChest chest = (StorageChest)constructable;
			chest.open(player.getId()); // initial open sets up the storage space for the player, subsequent opens do nothign
			
			items = chest.getItems(player.getId());
			tileId = request.getTileId();
			name = chest.getName();
			responseMaps.addClientOnlyResponse(player, this);
			
			ClientResourceManager.addItems(player, items.stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
		}
	}
}
