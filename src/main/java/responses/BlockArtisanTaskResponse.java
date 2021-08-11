package responses;

import database.dao.ItemDao;
import database.dao.PlayerArtisanBlockedTaskDao;
import database.dao.PlayerArtisanTaskDao;
import database.dto.PlayerArtisanTaskDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import requests.Request;
import types.ArtisanShopTabs;

public class BlockArtisanTaskResponse extends Response {
	private static final int BLOCK_TASK_COST = 100; // costs 100 points to block a task

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!ArtisanManager.playerIsNearMaster(player))
			return;
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(player.getId());
		if (task == null) {
			// how tf are they blocking a task when they don't even have a task
			return;
		}
		
		if (task.getTotalPoints() < BLOCK_TASK_COST) {
			setRecoAndResponseText(0, String.format("you need %d points to block a task.", BLOCK_TASK_COST));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final boolean successfullyBlocked = PlayerArtisanBlockedTaskDao.blockTask(player.getId(), task.getItemId());
		if (successfullyBlocked) {
			final String blockedItemName = ItemDao.getNameFromId(task.getItemId(), true);
			PlayerArtisanTaskDao.cancelTask(player.getId());
			PlayerArtisanTaskDao.spendPoints(player.getId(), BLOCK_TASK_COST);
			new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(String.format("you will no longer be assigned %s; you can now choose a new task.", blockedItemName), "white"));
		} else {
			setRecoAndResponseText(0, "you can only block a maximum of five tasks.");
			responseMaps.addClientOnlyResponse(player, this);
		}
	}

}
