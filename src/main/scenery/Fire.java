package main.scenery;

import java.util.List;

import main.database.dao.CookableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.CookableDto;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.UseRequest;
import main.responses.ActionBubbleResponse;
import main.responses.MessageResponse;
import main.responses.ResponseMaps;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;

public class Fire implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		int srcItemId = request.getSrc();
		int slot = request.getSlot();
		
		CookableDto cookable = CookableDao.getCookable(srcItemId);
		if (cookable == null) {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you can't cook that.", null));
			return true;
		}
		
		int cookingLevel = player.getStats().get(Stats.COOKING);
		if (cookingLevel < cookable.getLevel()) {
			MessageResponse response = new MessageResponse();
			response.setRecoAndResponseText(0, String.format("you need %d cooking to cook that.", cookable.getLevel()));
			responseMaps.addClientOnlyResponse(player, response);
			return true;
		}
		
		List<Integer> inv = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		
		if (inv.get(slot) != cookable.getRawItemId()) {// the passed-in slot doesn't have the correct item?  check other slots
			slot = inv.indexOf(cookable.getRawItemId());
		}
		
		if (slot > 0) {
			if (player.getState() != PlayerState.cooking) {
				responseMaps.addClientOnlyResponse(player, 
						MessageResponse.newMessageResponse(String.format("you throw the %s on the fire...", ItemDao.getItem(cookable.getRawItemId()).getName()), null));
				
				player.setSavedRequest(request);
				player.setState(PlayerState.cooking);
			}
			player.setTickCounter(5);
			
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(cookable.getCookedItemId())));
			return true;
		} else {
			if (player.getState() == PlayerState.cooking) {
				// we've run out of raw food.
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		return false;
	}

}
