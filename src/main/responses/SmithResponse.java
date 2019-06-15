package main.responses;

import java.util.ArrayList;

import main.FightManager;
import main.database.ItemDao;
import main.database.MineableDao;
import main.database.PlayerStorageDao;
import main.database.SmithableDao;
import main.database.SmithableDto;
import main.database.StatsDao;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.SmithRequest;

public class SmithResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof SmithRequest))
			return;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		SmithRequest smithRequest = (SmithRequest)req;
		
		// checks:
		// is player next to furnace
		
		// is item smithable
		SmithableDto dto = SmithableDao.getSmithableItemByItemId(smithRequest.getItemId());
		if (dto == null) {
			setRecoAndResponseText(0, "you can't smith that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// does player have level to smith
		int smithingLevel = StatsDao.getStatLevelByStatIdPlayerId(7, player.getId());// 7 is smithing
		if (smithingLevel < dto.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d smithing to smith that.", dto.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// does player have correct materials in inventory		
		if (!playerHasItemsInInventory(player.getId(), dto.getMaterial1(), dto.getCount1()) ||
			!playerHasItemsInInventory(player.getId(), dto.getMaterial2(), dto.getCount2()) ||
			!playerHasItemsInInventory(player.getId(), dto.getMaterial3(), dto.getCount3())) {
			setRecoAndResponseText(0, "you don't have the correct materials to smith that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// player has the correct materials, remove the materials and add the smithed item
		ArrayList<Integer> inventoryList = PlayerStorageDao.getInventoryListByPlayerId(player.getId());
		ArrayList<Integer> material1Slots = getAffectedSlots(inventoryList, dto.getMaterial1(), dto.getCount1());
		ArrayList<Integer> material2Slots = getAffectedSlots(inventoryList, dto.getMaterial2(), dto.getCount2());
		ArrayList<Integer> material3Slots = getAffectedSlots(inventoryList, dto.getMaterial3(), dto.getCount3());
		
		for (Integer slot : material1Slots)
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), slot, 0, 1);
		if (dto.getMaterial1() == 5 && material1Slots.size() < dto.getCount1()) {
			PlayerStorageDao.addStorageItemIdCountByPlayerIdStorageIdSlotId(player.getId(), 3, 0, -(dto.getCount1() - material1Slots.size()));
		}
		
		for (Integer slot : material2Slots)
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), slot, 0, 1);
		if (dto.getMaterial2() == 5 && material2Slots.size() < dto.getCount2()) {
			PlayerStorageDao.addStorageItemIdCountByPlayerIdStorageIdSlotId(player.getId(), 3, 0, -(dto.getCount2() - material2Slots.size()));
		}
		
		for (Integer slot : material3Slots)
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), slot, 0, 1);
		if (dto.getMaterial3() == 5 && material3Slots.size() < dto.getCount3()) {
			PlayerStorageDao.addStorageItemIdCountByPlayerIdStorageIdSlotId(player.getId(), 3, 0, -(dto.getCount3() - material3Slots.size()));
		}
		
		PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), dto.getItemId(), 1);
		
		// update the inventory for the client
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		
		setResponseText(String.format("you smith a %s.", ItemDao.getNameFromId(dto.getItemId())));
		responseMaps.addClientOnlyResponse(player, this);
		
		// TODO add smithing experience
		int exp = MineableDao.getMineableExpByItemId(dto.getMaterial1()) * dto.getCount1();
		if (dto.getMaterial2() != 0)
			exp += MineableDao.getMineableExpByItemId(dto.getMaterial2()) * dto.getCount2();
		if (dto.getMaterial3() != 0)
			exp += MineableDao.getMineableExpByItemId(dto.getMaterial3()) * dto.getCount3();
		
		new AddExpResponse().process(new AddExpRequest(player.getId(), 7, exp), player, responseMaps);
	}

	private boolean playerHasItemsInInventory(int playerId, int materialId, int count) {
		if (materialId == 0)
			return true;// item doesn't require this material
		
		int itemsInInventory = PlayerStorageDao.getNumStorageItemsByPlayerIdItemIdStorageTypeId(playerId, materialId, 1);
		if (itemsInInventory < count && materialId == 5) {// coal is a special case; check coal storage
			// itemId 5 is coal
			// storageTypeId 3 is furnace
			itemsInInventory += PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(playerId, 5, 3);
		}
		return itemsInInventory >= count;
	}
	
	ArrayList<Integer> getAffectedSlots(ArrayList<Integer> inventoryList, int itemId, int count) {
		ArrayList<Integer> affectedSlots = new ArrayList<>();
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
