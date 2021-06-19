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

public class Furnace extends Scenery {

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
		
		ClientResourceManager.addItems(player, Collections.singleton(smeltable.getBarId()));
		
		ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), ItemDao.getItem(smeltable.getBarId()).getSpriteFrameId());
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), actionBubble);
		
//		switch (srcItemId) {
//		case 3: // copper
//		case 4: // iron
//		case 6: // mithril
//		case 179: // addy
//		case 180: // runite
//			// note that coal isn't in this list as you cannot use coal with the furnace.
//			// you need to use a primary ore; coal is used as a secondary ore for the other ores.
////			List<SmithableDto> smithingOptions = SmithableDao.getAllItemsThatUseMaterial(srcItemId);
////			
////			ShowSmithingTableResponse response = new ShowSmithingTableResponse();
////			response.setOreId(srcItemId);
////			response.setSmithingOptions(smithingOptions);
////			response.setStoredCoal(PlayerStorageDao.getStoredCoalByPlayerId(player.getId()));
////			responseMaps.addClientOnlyResponse(player, response);
////			
////			// if the client has never been sent the material information, send it now.
////			Set<Integer> itemIds = new HashSet<>(); 
////			itemIds.add(srcItemId);
////			for (SmithableDto dto : smithingOptions) {
////				itemIds.add(dto.getItemId());
////				itemIds.add(dto.getMaterial1());
////				itemIds.add(dto.getMaterial2());
////				itemIds.add(dto.getMaterial3());
////			}
////			ClientResourceManager.addItems(player, itemIds);
//			
//			
//			
//			return true;
////		case 5: // coal
////			// add all the coal in inventory to the furnace storage
////			int numCoal = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), Items.COAL_ORE.getValue(), StorageTypes.INVENTORY);
////			if (numCoal > 0) {
////				PlayerStorageDao.removeAllItemsFromInventoryByPlayerIdItemId(player.getId(), Items.COAL_ORE);
////				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.FURNACE, 0, numCoal);
////			}
////			
////			InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
////			invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
////			invUpdate.setResponseText(String.format("you store %d coal at the furnace.", numCoal));
////			
////			return true;
//		default:
//			break;
//		}
		
		return true;
	}
	
}
