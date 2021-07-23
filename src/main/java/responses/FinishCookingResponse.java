package responses;

import java.util.List;

import database.dao.CookableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.CookableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.CookTaskUpdate;
import requests.AddExpRequest;
import requests.Request;
import requests.RequestFactory;
import requests.UseRequest;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

@SuppressWarnings("unused")
public class FinishCookingResponse extends Response {
	private int itemId;// so we can automatically re-cook
	private int tileId;// wherever the fire is
	private String type;
	
	public FinishCookingResponse() {
		setAction("finish_cooking");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// the request is actually a UseRequest, because we used the item on the fire to kick off the cooking process.
		if (!(req instanceof UseRequest))
			return;
		
		UseRequest request = (UseRequest)req;
		itemId = request.getSrc();// src is the raw item's itemId
		tileId = request.getDest();// dest is the fire's tileId
		type = request.getType();
		
		// if the fire is constructable it can run out between starting and finishing the cooking.
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), tileId);
		if (sceneryId == -1) {
			player.setState(PlayerState.idle);
			setRecoAndResponseText(0, "the fire ran out.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		CookableDto cookable = CookableDao.getCookable(itemId);
		if (cookable != null) {
			int cookingLevel = player.getStats().get(Stats.COOKING);
			if (cookingLevel < cookable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d cooking to cook that.", cookable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			List<Integer> inv = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			
			int slot = request.getSlot();
			if (inv.get(slot) != cookable.getRawItemId()) {// the passed-in slot doesn't have the correct item?  check other slots
				slot = inv.indexOf(cookable.getRawItemId());
				if (slot == -1)
					return; // item could have been dropped during cooking?
			}
			
			int baseChanceToBurn = (cookable.getLevel() - (cookable.getLevel() % 10)) + 10; // 1-10 cooking is 10% chance, 11-20 cooking is 20% chance etc
			int actualChanceToBurn = Math.max(baseChanceToBurn - cookingLevel, 0);
			
			boolean success = RandomUtil.getRandom(0, 100) >= actualChanceToBurn;
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, success ? cookable.getCookedItemId() : cookable.getBurntItemId(), 1, ItemDao.getMaxCharges(cookable.getCookedItemId()));
			InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
			invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			invUpdate.setResponseText(String.format("you %s the %s.", success ? "cook" : "burn", ItemDao.getNameFromId(cookable.getCookedItemId())));
			
			if (success) {
				AddExpRequest addExpReq = new AddExpRequest();
				addExpReq.setStatId(Stats.COOKING.getValue());
				addExpReq.setExp(cookable.getExp());
				
				new AddExpResponse().process(addExpReq, player, responseMaps);
			}
			
			responseMaps.addClientOnlyResponse(player, this);
			
			TybaltsTaskManager.check(player, new CookTaskUpdate(cookable.getCookedItemId(), !success), responseMaps);
		}
	}

}
