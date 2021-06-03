package main.scenery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.database.dao.PlayerStorageDao;
import main.database.dao.SmithableDao;
import main.database.dto.SmithableDto;
import main.processing.ClientResourceManager;
import main.processing.Player;
import main.requests.RequestFactory;
import main.requests.UseRequest;
import main.responses.InventoryUpdateResponse;
import main.responses.ResponseMaps;
import main.responses.ShowSmithingTableResponse;
import main.types.Items;
import main.types.StorageTypes;

public class Furnace extends Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		
		switch (srcItemId) {
		case 3: // copper
		case 4: // iron
		case 6: // mithril
		case 179: // addy
		case 180: // runite
			// note that coal isn't in this list as you cannot use coal with the furnace.
			// you need to use a primary ore; coal is used as a secondary ore for the other ores.
			List<SmithableDto> smithingOptions = SmithableDao.getAllItemsThatUseMaterial(srcItemId);
			
			ShowSmithingTableResponse response = new ShowSmithingTableResponse();
			response.setOreId(srcItemId);
			response.setSmithingOptions(smithingOptions);
			response.setStoredCoal(PlayerStorageDao.getStoredCoalByPlayerId(player.getId()));
			responseMaps.addClientOnlyResponse(player, response);
			
			// if the client has never been sent the material information, send it now.
			Set<Integer> itemIds = new HashSet<>(); 
			itemIds.add(srcItemId);
			for (SmithableDto dto : smithingOptions) {
				itemIds.add(dto.getItemId());
				itemIds.add(dto.getMaterial1());
				itemIds.add(dto.getMaterial2());
				itemIds.add(dto.getMaterial3());
			}
			ClientResourceManager.addItems(player, itemIds);
			return true;
		case 5: // coal
			// add all the coal in inventory to the furnace storage
			int numCoal = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), Items.COAL_ORE.getValue(), StorageTypes.INVENTORY);
			if (numCoal > 0) {
				PlayerStorageDao.removeAllItemsFromInventoryByPlayerIdItemId(player.getId(), Items.COAL_ORE);
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.FURNACE, 0, numCoal);
			}
			
			InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
			invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			invUpdate.setResponseText(String.format("you store %d coal at the furnace.", numCoal));
			
			return true;
		default:
			break;
		}
		
		return false;
	}
	
}
