package main.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import main.database.AnimationDao;
import main.database.AnimationDto;
import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.EquipmentDto;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.EquipRequest;
import main.requests.Request;
import main.types.EquipmentTypes;
import main.types.PlayerPartType;
import main.types.Stats;

public class EquipResponse extends Response {
	private HashSet<Integer> equippedSlots = new HashSet<>();
	private Map<PlayerPartType, AnimationDto> equipAnimations = new HashMap<>();
	private EquipmentBonusDto bonuses = null;

	public EquipResponse() {
		setAction("equip");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (req instanceof EquipRequest) {
			EquipRequest equipReq = (EquipRequest)req;
			ItemDto item = PlayerStorageDao.getItemFromPlayerIdAndSlot(equipReq.getId(), equipReq.getSlot());		
			
			// so we have the item from the requested slot, but is it equippable?
			EquipmentDto equip = EquipmentDao.getEquipmentByItemId(item.getId());
			if (equip == null) {
				// item isn't equippable.
				setRecoAndResponseText(0, "you can't equip that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (!playerHasRequirements(player, equip, responseMaps))
				return;
			
			// we also handle the unequipping here too, so if it's already equipped then unequip it.
			if (EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), item.getId(), equipReq.getSlot())) {
				EquipmentDao.clearEquippedItem(player.getId(), equipReq.getSlot());
			} else {
				// if we are already wearing this part (e.g. helmet) then unequip it before equipping the new item
				if (equip.getType() == EquipmentTypes.DAGGERS || equip.getType() == EquipmentTypes.HAMMER) {// two-handed weapon; unequip shield as well
					EquipmentDao.clearEquippedItemByPartId(player.getId(), 5);// 5 == shield
				} else if (equip.getType() == EquipmentTypes.SHIELD) {
					EquipmentTypes weaponType = EquipmentDao.getEquipmentTypeByEquipmentId((EquipmentDao.getWeaponIdByPlayerId(player.getId())));
					if (weaponType == EquipmentTypes.DAGGERS || weaponType == EquipmentTypes.HAMMER)
						EquipmentDao.clearEquippedItemByPartId(player.getId(), 4);// 4 == onhand
				}
				EquipmentDao.clearEquippedItemByPartId(player.getId(), equip.getPartId());
				EquipmentDao.setEquippedItem(player.getId(), equipReq.getSlot(), equip.getItemId());
			}
		}
		
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
		equipAnimations = AnimationDao.getEquipmentAnimationsByPlayerId(player.getId());
		bonuses = EquipmentDao.getEquipmentBonusesByPlayerId(player.getId());
		
		player.recacheEquippedItems();
		player.refreshBonuses(bonuses);
		
		responseMaps.addClientOnlyResponse(player, this);
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setEquipAnimations(equipAnimations);
		responseMaps.addLocalResponse(player.getRoomId(), player.getTileId(), playerUpdate);
	}
	
	private boolean playerHasRequirements(Player player, EquipmentDto equip, ResponseMaps responseMaps) {
		switch (equip.getType()) {
		case HELMET:
		case BODY:
		case LEGS:
		case SHIELD:
		case CAPE:
			if (player.getStats().get(Stats.DEFENCE) < equip.getRequirement()) {
				setRecoAndResponseText(0, String.format("you need %d defence to equip that.", equip.getRequirement()));
				responseMaps.addClientOnlyResponse(player, this);
				return false;
			}
			
			break;
			
		case DAGGERS:
			if (player.getStats().get(Stats.ACCURACY) < equip.getRequirement()) {
				setRecoAndResponseText(0, String.format("you need %d accuracy to equip that.", equip.getRequirement()));
				responseMaps.addClientOnlyResponse(player, this);
				return false;
			}
			break;
		case HAMMER:
			if (player.getStats().get(Stats.STRENGTH) < equip.getRequirement()) {
				setRecoAndResponseText(0, String.format("you need %d strength to equip that.", equip.getRequirement()));
				responseMaps.addClientOnlyResponse(player, this);
				return false;
			}
			break;
		case SWORD:
			if (player.getStats().get(Stats.ACCURACY) < equip.getRequirement() && player.getStats().get(Stats.STRENGTH) < equip.getRequirement()) {
				setRecoAndResponseText(0, String.format("you need %d strength or accuracy to equip that.", equip.getRequirement()));
				responseMaps.addClientOnlyResponse(player, this);
				return false;
			}
			break;
			
		default:
			break;
		}
		
		return true;
	}
	
}
