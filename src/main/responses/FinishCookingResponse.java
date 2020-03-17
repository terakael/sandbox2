package main.responses;

import java.util.ArrayList;

import lombok.Setter;
import main.database.CookableDao;
import main.database.CookableDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.UseRequest;
import main.types.Stats;
import main.types.StorageTypes;
import main.utils.RandomUtil;

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
		
		
		
		int slot = request.getSlot();
		CookableDto cookable = CookableDao.getCookable(itemId);
		if (cookable != null) {
			int cookingLevel = player.getStats().get(Stats.COOKING);
			if (cookingLevel < cookable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d cooking to cook that.", cookable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			ArrayList<Integer> inv = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
			
			if (inv.get(slot) != cookable.getRawItemId()) {// the passed-in slot doesn't have the correct item?  check other slots
				for (slot = 0; slot < inv.size(); ++slot) {
					if (inv.get(slot) == cookable.getRawItemId())
						break;
				}
			}
			
			boolean success = RandomUtil.getRandom(0, 100) > 50;
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), slot, success ? cookable.getCookedItemId() : cookable.getBurntItemId(), 1, ItemDao.getMaxCharges(cookable.getCookedItemId()));
			InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
			invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			invUpdate.setResponseText(String.format("you %s the %s.", success ? "cook" : "burn", ItemDao.getNameFromId(cookable.getCookedItemId())));
			
			if (success) {
				AddExpRequest addExpReq = new AddExpRequest();
				addExpReq.setId(player.getId());
				addExpReq.setStatId(Stats.COOKING.getValue());
				addExpReq.setExp(cookable.getExp());
				
				new AddExpResponse().process(addExpReq, player, responseMaps);
			}
			
			responseMaps.addClientOnlyResponse(player, this);
		}
	}

}
