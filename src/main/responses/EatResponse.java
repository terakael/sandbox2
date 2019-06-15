package main.responses;

import java.util.ArrayList;
import java.util.HashMap;

import main.FightManager;
import main.database.ConsumableDao;
import main.database.ConsumableDto;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.Player;
import main.requests.EatRequest;
import main.requests.Request;
import main.requests.RequestFactory;

public class EatResponse extends Response {
	public EatResponse() {
		setAction("eat");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof EatRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		EatRequest request = (EatRequest)req;
		ArrayList<ConsumableDto> consumables = ConsumableDao.getConsumablesByItemId(request.getObjectId());
		if (consumables.isEmpty()) {
			setRecoAndResponseText(0, "you can't eat that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		Integer itemId = PlayerStorageDao.getItemIdInSlot(player.getId(), 1, request.getSlot());
		if (itemId == request.getObjectId()) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), request.getSlot(), 0, 1);
		} else {// the slot didn't have the correct item? check other slots instead.
			ArrayList<Integer> itemIds = PlayerStorageDao.getInventoryListByPlayerId(player.getId());
			int slot;
			for (slot = 0; slot < itemIds.size(); ++slot) {
				if (itemIds.get(slot) == request.getObjectId())
					break;
			}
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), slot, 0, 1);// remove the consumable from the inventory
		}
		boolean hpModified = false;
		HashMap<Integer, Integer> relativeBoosts = StatsDao.getRelativeBoostsByPlayerId(player.getId());
		for (ConsumableDto dto : consumables) {
			if (!relativeBoosts.containsKey(dto.getStatId()))
				continue;
			
			int newRelativeBoost = relativeBoosts.get(dto.getStatId()) + dto.getAmount();
			
			if (dto.getStatId() == 5) {
				if (newRelativeBoost > 0)// the hp relative boost is negative for how many hp lost
					newRelativeBoost = 0;
				hpModified = true;
				player.setCurrentHp(player.getDto().getMaxHp() + newRelativeBoost);
			}
			
			StatsDao.setRelativeBoostByPlayerIdStatId(player.getId(), dto.getStatId(), newRelativeBoost);
		}
		
		if (hpModified) {
			PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
			playerUpdateResponse.setId(player.getId());
			playerUpdateResponse.setHp(player.getCurrentHp());
			responseMaps.addBroadcastResponse(playerUpdateResponse);
		}
		
		InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
		invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		invUpdate.setResponseText(String.format("you eat the %s.", ItemDao.getNameFromId(request.getObjectId())));
		
	}

}
