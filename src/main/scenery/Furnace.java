package main.scenery;

import java.util.Collections;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SmeltableDao;
import main.database.dao.StatsDao;
import main.database.dto.SmeltableDto;
import main.processing.ClientResourceManager;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.UseRequest;
import main.responses.ActionBubbleResponse;
import main.responses.MessageResponse;
import main.responses.ResponseMaps;
import main.types.Stats;
import main.types.StorageTypes;

public class Furnace implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		// required ore check
		List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
				
		SmeltableDto smeltable = SmeltableDao.getSmeltableByOreId(srcItemId, playerInvIds.contains(5));
		if (smeltable == null) {
			player.setState(PlayerState.idle);
			return false;
		}
		
		
		if (!playerInvIds.contains(srcItemId)) {
			player.setState(PlayerState.idle);
			return true;
		}
		
		// level check
		final int smithingLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.SMITHING, player.getId());
		if (smithingLevel < smeltable.getLevel()) {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(String.format("you need %d smithing to do that.", smeltable.getLevel()), null));
			player.setState(PlayerState.idle);
			return true;
		}
		
		// required coal check
		final int playerCoalCount = Collections.frequency(playerInvIds, 5);
		if (playerCoalCount < smeltable.getRequiredCoal()) {
			final String message = player.getState() == PlayerState.smelting
					? "you have run out of coal."
					: String.format("you need %d coal to do that.", smeltable.getRequiredCoal());
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, null));
			player.setState(PlayerState.idle);
			return true;
		}
		
		if (player.getState() != PlayerState.smelting) {
			final String requiredCoalSubtext = String.format(" and %d coal", smeltable.getRequiredCoal());
			final String messageText = String.format("you place the %s%s into the furnace...", ItemDao.getItem(smeltable.getOreId()).getName(), smeltable.getRequiredCoal() > 0 ? requiredCoalSubtext : "");
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(messageText, null));
			player.setState(PlayerState.smelting);
			player.setSavedRequest(request);
		}
		player.setTickCounter(5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(smeltable.getBarId())));
		return true;
	}
	
}
