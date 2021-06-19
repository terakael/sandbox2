package main.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.MineableDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SmeltableDao;
import main.database.dao.SmithableDao;
import main.database.dao.StatsDao;
import main.database.dto.SmithableDto;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.requests.SmithRequest;
import main.types.Stats;
import main.types.StorageTypes;

@SuppressWarnings("unused")
public class FinishSmithResponse extends Response {
	private int itemId;
	
	public FinishSmithResponse() {
		setAction("finish_smith");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof SmithRequest)) {
			return;
		}
		
		SmithRequest smithRequest = (SmithRequest)req;
		itemId = smithRequest.getItemId();
		
		SmithableDto dto = SmithableDao.getSmithableItemByItemId(smithRequest.getItemId());
		if (dto == null) {
			setRecoAndResponseText(0, "you can't smith that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int playerBarCount = Collections.frequency(playerInvIds, dto.getBarId());
		if (playerBarCount < dto.getRequiredBars()) {
			return;
		}
		
		// level check
		final int smithingLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.SMITHING, player.getId());
		if (smithingLevel < dto.getLevel()) {
			return;
		}

		for (int i = 0; i < dto.getRequiredBars(); ++i) {
			int barIndex = playerInvIds.indexOf(dto.getBarId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, barIndex, 0, 0, 0);
			playerInvIds.set(barIndex, 0);
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, dto.getItemId(), 1, ItemDao.getMaxCharges(dto.getItemId()));
		
		
		setResponseText(String.format("you smith a %s.", ItemDao.getNameFromId(dto.getItemId())));
		responseMaps.addClientOnlyResponse(player, this);
		
		final int exp = SmeltableDao.getSmeltableByBarId(dto.getBarId()).getLevel() * 10;
		new AddExpResponse().process(new AddExpRequest(player.getId(), Stats.SMITHING.getValue(), exp), player, responseMaps);
		
		// update the inventory for the client
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

	List<Integer> getAffectedSlots(List<Integer> inventoryList, int itemId, int count) {
		List<Integer> affectedSlots = new ArrayList<>();
		if (count == 0)
			return affectedSlots;
		for (int i = 0; i < inventoryList.size(); ++i) {
			if (inventoryList.get(i) == itemId) {
				affectedSlots.add(i);
				if (affectedSlots.size() == count)
					break;
			}
		}
		return affectedSlots;
	}
	
}
