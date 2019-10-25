package main.responses;

import java.util.ArrayList;
import java.util.Collections;

import main.database.EquipmentDao;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.SceneryDao;
import main.database.UseItemOnItemDao;
import main.database.UseItemOnItemDto;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.UseRequest;
import main.scenery.Scenery;
import main.scenery.SceneryManager;
import main.types.Items;
import main.types.StorageTypes;

public class UseResponse extends Response {

	public UseResponse() {
		setAction("use");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof UseRequest))
			return;
		
		UseRequest request = (UseRequest)req;
		switch (request.getType()) {
		case "scenery":
			if (handleUseOnScenery(request, player, responseMaps))
				return;
			break;
		case "item":
			if (handleUseOnItem(request, player, responseMaps))
				return;
			break;
		default:
			break;
		}
		
		setRecoAndResponseText(0, "nothing interesting happens.");
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	private boolean handleUseOnScenery(UseRequest request, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		if (!PathFinder.isNextTo(player.getTileId(), request.getDest())) {
			player.setPath(PathFinder.findPath(player.getTileId(), request.getDest(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(request);
			return true;
		}
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(request.getDest());
		Scenery scenery = SceneryManager.getScenery(sceneryId);
		if (scenery == null)
			return false;
		
		if (!scenery.use(request.getSrc(), request.getSlot(), player, responseMaps))
			return false;
		
		return true;
	}
	
	private boolean handleUseOnItem(UseRequest request, Player player, ResponseMaps responseMaps) {
		int src = request.getSrc();
		int dest = request.getDest();
		int srcSlot = request.getSrcSlot();
		int destSlot = request.getSlot();
		
		UseItemOnItemDto dto = UseItemOnItemDao.getEntryBySrcIdDestId(src, dest);
		if (dto == null) {
			// try switching the src and dest
			src = request.getDest();
			dest = request.getSrc();
			srcSlot = request.getSlot();
			destSlot = request.getSrcSlot();
			
			dto = UseItemOnItemDao.getEntryBySrcIdDestId(src, dest);
			if (dto == null) {
				// nope no match; nothing interesting happens when you use these two items together.
				return false;
			}
		}
		
		// slot check
		ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		if (invItemIds.get(srcSlot) != src || invItemIds.get(destSlot) != dest) {
			// user-input slots don't match what's actually in the slots; bail with a "nothing interesting happens" message
			return false;
		}
		
		// you can't use things on equipped items (or use equipped items on things)
		if (EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), src, srcSlot) ||
				EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), dest, destSlot)) {
			setRecoAndResponseText(0, "you need to unequip it first.");
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		int srcItemsInInv = Collections.frequency(invItemIds, src);
		if (srcItemsInInv >= dto.getRequiredSrcCount()) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), destSlot, dto.getResultingItemId(), ItemDao.getMaxCharges(dto.getResultingItemId()));
			
			if (!dto.isKeepSrcItem()) {// sometimes you'll have src items like hammers, knives etc that don't get used up
				int usedSrcItems = 1;
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), srcSlot, 0, 0);
				invItemIds.set(srcSlot, 0);// just so we don't hit the same slot twice 
				
				for (int i = 0; i < invItemIds.size() && usedSrcItems < dto.getRequiredSrcCount(); ++i) {
					if (invItemIds.get(i) == src) {
						PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), i, 0, 0);
						++usedSrcItems;
					}
				}
			}
			
			setRecoAndResponseText(1, String.format("you create your %s.", ItemDao.getNameFromId(dto.getResultingItemId())));
			responseMaps.addClientOnlyResponse(player, this);
		} else {
			String itemName = ItemDao.getNameFromId(src);
			if (!itemName.endsWith("s"))
				itemName += "s";
			
			setRecoAndResponseText(0, String.format("you need %d %s to do that.", dto.getRequiredSrcCount(), itemName));
			responseMaps.addClientOnlyResponse(player, this);
		}
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		
		
		return true;
	}
}
