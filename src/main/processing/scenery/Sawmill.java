package main.processing.scenery;

import java.util.Collections;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SawmillableDao;
import main.database.dao.StatsDao;
import main.database.dto.SawmillableDto;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.requests.UseRequest;
import main.responses.ActionBubbleResponse;
import main.responses.MessageResponse;
import main.responses.ResponseMaps;
import main.types.Stats;
import main.types.StorageTypes;

public class Sawmill implements Scenery {
	private static int REQUIRED_LOGS = 3;

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final SawmillableDto sawmillable = SawmillableDao.getSawmillableByLogId(request.getSrc());
		if (sawmillable == null)
			return false;
		
		// level check
		if (StatsDao.getStatLevelByStatIdPlayerId(Stats.CONSTRUCTION, player.getId()) < sawmillable.getRequiredLevel()) {
			final String message = String.format("you need %d construction to make these planks.", sawmillable.getRequiredLevel());
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, null));
			player.setState(PlayerState.idle);
			return true;
		}
		
		// need three of a log
		List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int numRequiredLogs = Collections.frequency(inventoryItemIds, sawmillable.getLogId());
		if (numRequiredLogs < REQUIRED_LOGS) {
			final String message = player.getState() != PlayerState.sawmill
						? "you need three logs to make a plank."
						: "you have run out of logs.";
				
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, null));
			player.setState(PlayerState.idle);
			return true;
		}
		
		if (player.getState() != PlayerState.sawmill) {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you run the logs through the sawmill...", null));
			player.setState(PlayerState.sawmill);
			player.setSavedRequest(request);
		}
		player.setTickCounter(5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(sawmillable.getLogId())));
		return true;
	}

}
