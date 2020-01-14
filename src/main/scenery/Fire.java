package main.scenery;

import java.util.ArrayList;

import main.database.CookableDao;
import main.database.CookableDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.UseRequest;
import main.responses.ActionBubbleResponse;
import main.responses.ResponseMaps;
import main.responses.StartCookingResponse;
import main.types.Items;
import main.types.StorageTypes;

public class Fire extends Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		int srcItemId = request.getSrc();
		int slot = request.getSlot();
		
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
				StartCookingResponse startCookingResponse = new StartCookingResponse();
				responseMaps.addClientOnlyResponse(player, startCookingResponse);
				
				ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), ItemDao.getItem(cookable.getCookedItemId()).getSpriteFrameId());
				responseMaps.addLocalResponse(player.getTileId(), actionBubble);

				player.setSavedRequest(request);
				player.setState(PlayerState.cooking);
				player.setTickCounter(5);
			}
			
			return true;
		}
		
		return false;
	}

}
