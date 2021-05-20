package main.responses;

import java.util.Collections;
import java.util.List;

import main.database.BrewableDao;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.database.UseItemOnItemDao;
import main.database.UseItemOnItemDto;
import main.processing.Player;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.UseRequest;
import main.types.Stats;
import main.types.StorageTypes;

@SuppressWarnings("unused")
public class FinishUseResponse extends Response {
	private int src;
	private int dest;
	private String type;
	
	public FinishUseResponse() {
		setAction("finish_use");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof UseRequest))
			return;
		
		UseRequest request = (UseRequest)req;
		
		src = request.getSrc();
		dest = request.getDest();
		type = request.getType();
//		int srcSlot = request.getSrcSlot();
//		int destSlot = request.getSlot();
		
		UseItemOnItemDto dto = UseItemOnItemDao.getEntryBySrcIdDestId(src, dest);
		if (dto == null) {
			// try switching the src and dest
			src = request.getDest();
			dest = request.getSrc();
//			srcSlot = request.getSlot();
//			destSlot = request.getSrcSlot();
			
			dto = UseItemOnItemDao.getEntryBySrcIdDestId(src, dest);
			if (dto == null) {
				// nope no match; nothing interesting happens when you use these two items together.
				setRecoAndResponseText(0, "nothing interesting happens.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		}
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		int srcSlot = invItemIds.indexOf(src);
		int destSlot = invItemIds.indexOf(dest);
		
		if (srcSlot == -1 || destSlot == -1) {
			setRecoAndResponseText(0, "you have no more materials or something.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int srcItemsInInv = Collections.frequency(invItemIds, src);
		if (srcItemsInInv >= dto.getRequiredSrcCount()) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, destSlot, dto.getResultingItemId(), 1, ItemDao.getMaxCharges(dto.getResultingItemId()));
			
			if (!dto.isKeepSrcItem()) {// sometimes you'll have src items like hammers, knives etc that don't get used up
				int usedSrcItems = 1;
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, srcSlot, 0, 1, 0);
				invItemIds.set(srcSlot, 0);// just so we don't hit the same slot twice 
				
				for (int i = 0; i < invItemIds.size() && usedSrcItems < dto.getRequiredSrcCount(); ++i) {
					if (invItemIds.get(i) == src) {
						PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, i, 0, 1, 0);
						++usedSrcItems;
					}
				}
			}
			
			setRecoAndResponseText(1, String.format("you create your %s.", ItemDao.getNameFromId(dto.getResultingItemId())));
			responseMaps.addClientOnlyResponse(player, this);
			
			if (BrewableDao.isBrewable(dto.getResultingItemId())) {
				int exp = BrewableDao.getExpById(dto.getResultingItemId());
				StatsDao.addExpToPlayer(player.getId(), Stats.HERBLORE, exp);
				
				AddExpResponse expResponse = new AddExpResponse();
				expResponse.addExp(Stats.HERBLORE.getValue(), exp);
				responseMaps.addClientOnlyResponse(player, expResponse);
			}
		} else {
			String itemName = ItemDao.getNameFromId(src);
			if (!itemName.endsWith("s"))
				itemName += "s";
			
			setRecoAndResponseText(0, String.format("you need %d %s to do that.", dto.getRequiredSrcCount(), itemName));
			responseMaps.addClientOnlyResponse(player, this);
		}
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}

}
