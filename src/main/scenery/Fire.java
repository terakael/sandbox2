package main.scenery;

import java.util.ArrayList;

import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.RequestFactory;
import main.responses.InventoryUpdateResponse;
import main.responses.ResponseMaps;
import main.types.Items;

public class Fire extends Scenery {

	@Override
	public boolean use(int srcItemId, int slot, Player player, ResponseMaps responseMaps) {
		Items item = Items.withValue(srcItemId);
		if (item == null)
			return false;
		
		switch (item) {
		case RAW_CHICKEN:
			ArrayList<Integer> inv = PlayerStorageDao.getInventoryListByPlayerId(player.getId());
			
			if (inv.get(slot) != srcItemId) {// the passed-in slot doesn't have the correct item?  check other slots
				for (slot = 0; slot < inv.size(); ++slot) {
					if (inv.get(slot) == srcItemId)
						break;
				}
			}
			
			if (slot < inv.size()) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), slot, Items.COOKED_CHICKEN.getValue());
				InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
				invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
				invUpdate.setResponseText("you cook the chicken.");
				return true;
			}
			
			break;
		default:
			break;
		}
		return false;
	}

}
