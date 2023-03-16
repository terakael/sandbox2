package responses;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.EquipmentDao;
import database.dao.PlayerBaseAnimationsDao;
import database.dao.PlayerStorageDao;
import database.dto.EquipmentBonusDto;
import database.dto.EquipmentDto;
import database.dto.ItemDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.EquipRequest;
import requests.Request;
import types.EquipmentTypes;
import types.Stats;

@SuppressWarnings("unused")
public class EquipResponse extends Response {
	private Set<Integer> equippedSlots = new HashSet<>();
	private EquipmentBonusDto bonuses = null;

	public EquipResponse() {
		setAction("equip");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true; // all good to equip stuff during combat
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (req instanceof EquipRequest) {
			EquipRequest equipReq = (EquipRequest)req;
			ItemDto item = PlayerStorageDao.getItemFromPlayerIdAndSlot(player.getId(), equipReq.getSlot());		
			
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
				
				if (equip.getType() == EquipmentTypes.GLOVES) {
					Integer bodySlot = EquipmentDao.getEquippedSlotsAndItemIdsByPlayerId(player.getId()).entrySet().stream()
						.filter(e -> EquipmentDao.getEquipmentTypeByEquipmentId(e.getKey()) == EquipmentTypes.BODY)
						.map(e -> e.getValue())
						.findFirst()
						.orElse(null);
					
					if (bodySlot != null) {
						EquipmentDao.clearEquippedItem(player.getId(), bodySlot);
					}
					
				} else if (equip.getType() == EquipmentTypes.BODY) {
					EquipmentDao.clearEquippedItemByPartId(player.getId(), 17); // gloves
				}
				
				EquipmentDao.clearEquippedItemByPartId(player.getId(), equip.getPartId());
				EquipmentDao.setEquippedItem(player.getId(), equipReq.getSlot(), equip.getItemId());
			}
			
			
		}
		
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
		bonuses = EquipmentDao.getEquipmentBonusesByPlayerId(player.getId());
		
		player.recacheEquippedItems();
		player.refreshBonuses(bonuses);
		
		responseMaps.addClientOnlyResponse(player, this);
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setEquipAnimations(EquipmentDao.getEquipmentAnimationsByPlayerId(player.getId()));
		
		// literally only needed so we can check if the player is equipping daggers, so we can draw them correctly.
		playerUpdate.setWeaponType(EquipmentDao.getEquipmentTypeByEquipmentId((EquipmentDao.getWeaponIdByPlayerId(player.getId()))));
		
		// sometimes an equipped item overrides one or more base animations (e.g. full helmet removes hair and beard).
		playerUpdate.setBaseAnimations(PlayerBaseAnimationsDao.getBaseAnimationsBasedOnEquipmentTypes(player.getId()));
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), playerUpdate);
		
		ClientResourceManager.addLocalAnimations(player, Collections.singleton(player.getId()));
	}
	
	private boolean playerHasRequirements(Player player, EquipmentDto equip, ResponseMaps responseMaps) {
		switch (equip.getType()) {
		case HELMET_FULL:
		case HELMET_MED:
		case HAT:
		case BODY:
		case LEGS:
		case SHIELD:
		case CAPE:
		case CHAINBODY:
		case CHAINSKIRT:
		case GLOVES:
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
		case WAND:
			if (player.getStats().get(Stats.MAGIC) < equip.getRequirement()) {
				setRecoAndResponseText(0, String.format("you need %d magic to equip that.", equip.getRequirement()));
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
