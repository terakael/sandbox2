package processing.scenery;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import processing.attackable.Player;
import requests.UseRequest;
import responses.ActionBubbleResponse;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.Items;
import types.StorageTypes;

public class Well implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		int slot = request.getSlot();
		
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem != Items.EMPTY_BUCKET)
			return false;
		
		final Integer itemId = PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.INVENTORY, slot);		
		if (itemId == null || itemId != srcItemId) {
			slot = PlayerStorageDao.getSlotOfItemId(player.getId(), StorageTypes.INVENTORY, srcItemId);
			if (slot == -1)
				return false;
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, Items.BUCKET_OF_WATER.getValue(), 1, ItemDao.getMaxCharges(Items.BUCKET_OF_WATER.getValue()));
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(Items.BUCKET_OF_WATER.getValue())));
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you fill the bucket from the well.", "white"));
		
		return true;
	}

}
