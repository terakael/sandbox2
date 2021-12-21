package processing.scenery;

import java.util.Collections;
import java.util.List;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SmeltableDao;
import database.dao.StatsDao;
import database.dto.SmeltableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import requests.UseRequest;
import responses.ActionBubbleResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.ItemAttributes;
import types.Stats;
import types.StorageTypes;

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
		
		final String oreName = ItemDao.getItem(smeltable.getOreId()).getName();
		final int playerOreCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), smeltable.getOreId(), StorageTypes.INVENTORY);
		if (playerOreCount < smeltable.getRequiredOre()) {
			final String message = player.getState() == PlayerState.smelting
					? String.format("you have run out of %s.", oreName)
					: String.format("you need %d %s to do that.", smeltable.getRequiredOre(), oreName);
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, null));
			player.setState(PlayerState.idle);
			return true;
		}
		
		if (Collections.frequency(playerInvIds, 0) == 0) {
			// full inventory, but first check if the ore is stackable and we have the exact number
			if (ItemDao.itemHasAttribute(smeltable.getOreId(), ItemAttributes.STACKABLE) && playerOreCount > smeltable.getRequiredOre()) {
				// we don't have room because the stackable ore won't deplete this run
				final String message = player.getState() == PlayerState.smelting
						? "you have run out of inventory space."
						: "you don't have enough inventory space.";
				responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, null));
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		if (player.getState() != PlayerState.smelting) {
			final String requiredCoalSubtext = String.format(" and %d coal", smeltable.getRequiredCoal());
			final String messageText = String.format("you place the %s%s into the furnace...", oreName, smeltable.getRequiredCoal() > 0 ? requiredCoalSubtext : "");
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
