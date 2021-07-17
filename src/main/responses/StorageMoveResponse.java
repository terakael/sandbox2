package main.responses;

import java.util.List;

import main.database.dto.InventoryItemDto;
import main.processing.ConstructableManager;
import main.processing.Player;
import main.processing.scenery.constructable.Constructable;
import main.processing.scenery.constructable.StorageChest;
import main.requests.Request;
import main.requests.StorageMoveRequest;

@SuppressWarnings("unused")
public class StorageMoveResponse extends Response {
	private List<InventoryItemDto> items;
	
	public StorageMoveResponse() {
		setAction("storage_move");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		StorageMoveRequest request = (StorageMoveRequest)req;
		
		final Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
		if (constructable == null)
			return;
		
		if (!(constructable instanceof StorageChest))
			return;
		
		StorageChest chest = (StorageChest)constructable;
		chest.swapSlotContents(player.getId(), request.getSrc(), request.getDest());
		
		items = chest.getItems(player.getId());
	}

}
