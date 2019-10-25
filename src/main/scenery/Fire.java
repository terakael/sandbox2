package main.scenery;

import java.util.ArrayList;

import main.database.CookableDao;
import main.database.CookableDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.RequestFactory;
import main.responses.InventoryUpdateResponse;
import main.responses.ResponseMaps;
import main.types.Items;
import main.types.StorageTypes;

public class Fire extends Scenery {

	@Override
	public boolean use(int srcItemId, int slot, Player player, ResponseMaps responseMaps) {
		Items item = Items.withValue(srcItemId);
		if (item == null)
			return false;
		
		CookableDto cookable = CookableDao.getCookable(srcItemId);
		if (cookable != null) {
			ArrayList<Integer> inv = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
			
			if (inv.get(slot) != cookable.getRawItemId()) {// the passed-in slot doesn't have the correct item?  check other slots
				for (slot = 0; slot < inv.size(); ++slot) {
					if (inv.get(slot) == cookable.getRawItemId())
						break;
				}
			}
			
			if (slot < inv.size()) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), slot, cookable.getCookedItemId(), 1);
				InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
				invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
				invUpdate.setResponseText(String.format("you cook the %s.", ItemDao.getNameFromId(cookable.getCookedItemId())));
			}
			
			return true;
		}
		
		return false;
	}

}
