package responses;

import database.dao.ItemDao;
import database.dao.PlayerArtisanTaskDao;
import database.dto.PlayerArtisanTaskDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import requests.Request;
import types.ArtisanShopTabs;

public class SkipArtisanTaskResponse extends Response {
	private final static int SKIP_TASK_COST = 30;

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!ArtisanManager.playerIsNearMaster(player))
			return;
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(player.getId());
		if (task == null) {
			// how tf are they skipping a task when they don't even have a task
			return;
		}
		
		if (task.getTotalPoints() < SKIP_TASK_COST) {
			setRecoAndResponseText(0, String.format("you need %d points to skip a task.", SKIP_TASK_COST));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final String skippedItem = ItemDao.getNameFromId(task.getItemId(), true);
		PlayerArtisanTaskDao.cancelTask(player.getId());
		PlayerArtisanTaskDao.spendPoints(player.getId(), SKIP_TASK_COST);
		new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(String.format("skipped %s; you can now choose a new task.", skippedItem), "white"));
	}

}
