package main.responses;

import java.util.ArrayList;
import java.util.List;

import main.database.ItemDao;
import main.database.MineableDao;
import main.database.PlayerStorageDao;
import main.database.SmithableDao;
import main.database.SmithableDto;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.requests.SmithRequest;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;

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
		
		// player has the correct materials, remove the materials and add the smithed item
		List<Integer> inventoryList = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		List<Integer> material1Slots = getAffectedSlots(inventoryList, dto.getMaterial1(), dto.getCount1());
		List<Integer> material2Slots = getAffectedSlots(inventoryList, dto.getMaterial2(), dto.getCount2());
		List<Integer> material3Slots = getAffectedSlots(inventoryList, dto.getMaterial3(), dto.getCount3());
		
		for (Integer slot : material1Slots)
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 1, 0);
		if (dto.getMaterial1() == Items.COAL_ORE.getValue() && material1Slots.size() < dto.getCount1()) {
			PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.FURNACE, 0, -(dto.getCount1() - material1Slots.size()));
		}
		
		for (Integer slot : material2Slots)
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 1, 0);
		if (dto.getMaterial2() == Items.COAL_ORE.getValue() && material2Slots.size() < dto.getCount2()) {
			PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.FURNACE, 0, -(dto.getCount2() - material2Slots.size()));
		}
		
		for (Integer slot : material3Slots)
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 1, 0);
		if (dto.getMaterial3() == Items.COAL_ORE.getValue() && material3Slots.size() < dto.getCount3()) {
			PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.FURNACE, 0, -(dto.getCount3() - material3Slots.size()));
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, dto.getItemId(), 1, ItemDao.getMaxCharges(dto.getItemId()));
		
		
		setResponseText(String.format("you smith a %s.", ItemDao.getNameFromId(dto.getItemId())));
		responseMaps.addClientOnlyResponse(player, this);
		
		int exp = MineableDao.getMineableExpByItemId(dto.getMaterial1()) * dto.getCount1();
		if (dto.getMaterial2() != 0)
			exp += MineableDao.getMineableExpByItemId(dto.getMaterial2()) * dto.getCount2();
		if (dto.getMaterial3() != 0)
			exp += MineableDao.getMineableExpByItemId(dto.getMaterial3()) * dto.getCount3();
		
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
