package main.scenery;

import java.util.List;
import java.util.Set;

import main.database.EquipmentDao;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.RequestFactory;
import main.responses.InventoryUpdateResponse;
import main.responses.MessageResponse;
import main.responses.ResponseMaps;
import main.types.Items;
import main.types.StorageTypes;
import main.utils.RandomUtil;

public abstract class Obelisk extends Scenery {
	protected int enchantChance = 0;
	
	protected boolean attemptToEnchant(Items src, Items dest, int slot, Player player, ResponseMaps responseMaps) {
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		
		if (invItemIds.get(slot) != src.getValue()) {// mismatching slot/itemId, find first slot with correct itemId
			for (slot = 0; slot < invItemIds.size(); ++slot) {
				if (invItemIds.get(slot) == src.getValue())
					break;
			}
		}
		
		if (slot < invItemIds.size()) {
			Set<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
			if (equippedSlots.contains(slot)) {
				MessageResponse resp = new MessageResponse();
				resp.setRecoAndResponseText(0, "you need to unequip it first.");
				responseMaps.addClientOnlyResponse(player, resp);
				return false;
			}
			
			boolean success = RandomUtil.getRandom(0,  100) <= enchantChance;
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, success ? dest.getValue() : 0, 1, ItemDao.getMaxCharges(dest.getValue()));
			
			InventoryUpdateResponse updateResponse = new InventoryUpdateResponse();
			updateResponse.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			
			String itemName = ItemDao.getNameFromId(src.getValue());
			String responseText = success
					? String.format("you successfully enchant the %s.", itemName)
					: String.format("the %s fails to enchant, and is destroyed in the process.", itemName);
			updateResponse.setRecoAndResponseText(1, responseText);
			return true;
		}
		
		return false;
	}
}
