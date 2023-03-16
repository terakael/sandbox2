package responses;

import database.dao.ItemDao;
import database.dao.PlayerArtisanBlockedTaskDao;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import requests.Request;
import requests.UnblockArtisanTaskRequest;
import types.ArtisanShopTabs;

public class UnblockArtisanTaskResponse extends Response {

	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!ArtisanManager.playerIsNearMaster(player))
			return;
		
		if (!(req instanceof UnblockArtisanTaskRequest))
			return;
		
		final int itemIdToUnblock = ((UnblockArtisanTaskRequest)req).getItemId();
		final boolean successfullyUnblocked = PlayerArtisanBlockedTaskDao.unblockTask(player.getId(), itemIdToUnblock);
		
		if (successfullyUnblocked) {
			new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
			responseMaps.addClientOnlyResponse(player, 
					MessageResponse.newMessageResponse(String.format("you can once again be assigned %s.", ItemDao.getNameFromId(itemIdToUnblock, true)), "white"));
		}
	}

}
