package responses;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.dao.ArtisanMasterDao;
import database.dao.BrewableDao;
import database.dao.CastableDao;
import database.dao.ConstructableDao;
import database.dao.DoorDao;
import database.dao.EquipmentDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.PickableDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dao.StatsDao;
import database.dao.TeleportableDao;
import database.dao.UseItemOnItemDao;
import database.dto.CastableDto;
import database.dto.ConstructableDto;
import database.dto.InventoryItemDto;
import database.dto.UseItemOnItemDto;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.ArtisanManager;
import processing.managers.FightManager;
import processing.managers.FightManager.Fight;
import processing.managers.NPCManager;
import processing.scenery.Scenery;
import processing.scenery.SceneryManager;
import requests.AttackRequest;
import requests.ConstructionRequest;
import requests.Request;
import requests.UseRequest;
import types.DuelRules;
import types.Items;
import types.NpcAttributes;
import types.Stats;
import types.StorageTypes;

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
		case "npc":
			if (handleUseOnNpc(request, player, responseMaps))
				return;
			break;
		case "player":
			if (handleUseOnPlayer(request, player, responseMaps))
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
		
		boolean targetIsDoor = DoorDao.getDoorDtoByTileId(player.getFloor(), request.getDest()) != null;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getDest(), !targetIsDoor, true)) {
			if (targetIsDoor) {
				player.setPath(PathFinder.findPathToDoor(player.getFloor(), player.getTileId(), request.getDest()));
			} else {
				player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getDest(), false));
			}
			player.setState(PlayerState.walking);
			player.setSavedRequest(request);
			return true;
		}
		
		player.faceDirection(request.getDest(), responseMaps);
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getDest());
		Scenery scenery = SceneryManager.getScenery(sceneryId);
		if (scenery == null)
			return false;
		
		if (!scenery.use(request, player, responseMaps))
			return false;
		
		return true;
	}
	
	private boolean handleUseOnItem(UseRequest request, Player player, ResponseMaps responseMaps) {
		int src = request.getSrc();
		int dest = request.getDest();
		int srcSlot = request.getSrcSlot();
		int destSlot = request.getSlot();
		
		if (ConstructableDao.itemIsConstructionTool(request.getSrc()) || ConstructableDao.itemIsConstructionTool(request.getDest())) {
			// construction special case
			return handleConstruction(request, player, responseMaps, false);
		} else if (src == Items.FLOWER_SACK.getValue() || dest == Items.FLOWER_SACK.getValue()) {
			return handleFlowerSack(request, player, responseMaps);
		} else if (src == Items.PLANKCRAFTING_KNIFE.getValue() || dest == Items.PLANKCRAFTING_KNIFE.getValue()) {
			new SawmillResponse(true).process(request, player, responseMaps);
			return true;
		}
		
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
				if (player.getState() != PlayerState.idle && player.getState() != PlayerState.walking)
					player.setState(PlayerState.idle);
				return false;
			}
		}
		
		// slot check
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (invItemIds.get(srcSlot) != src) {
			srcSlot = invItemIds.indexOf(src);
			if (srcSlot == -1) {
				if (player.getState() != PlayerState.using) {
					setRecoAndResponseText(0, "you don't have the materials to make that.");
					responseMaps.addClientOnlyResponse(player, this);
				}
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		if (invItemIds.get(destSlot) != dest) {
			destSlot = invItemIds.indexOf(dest);
			if (destSlot == -1) {
				if (player.getState() != PlayerState.using) {
					setRecoAndResponseText(0, "you don't have the materials to make that.");
					responseMaps.addClientOnlyResponse(player, this);
				}
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		if (invItemIds.get(srcSlot) != src || invItemIds.get(destSlot) != dest) {
			// user-input slots don't match what's actually in the slots; bail with a "nothing interesting happens" message
			player.setState(PlayerState.idle);
			return false;
		}
		
		// level check by resulting item id
		if (BrewableDao.isBrewable(dto.getResultingItemId())) {
			int playerLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.HERBLORE, player.getId());
			int requiredLevel = BrewableDao.getRequiredLevelById(dto.getResultingItemId());
			if (playerLevel < requiredLevel) {
				setRecoAndResponseText(0, String.format("you need %d herblore to make that.", requiredLevel));
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		// you can't use things on equipped items (or use equipped items on things)
		if (EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), src, srcSlot) ||
				EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), dest, destSlot)) {
			setRecoAndResponseText(0, "you need to unequip it first.");
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return true;
		}
		
		if (player.getState() != PlayerState.using) {
			setRecoAndResponseText(1, "you use the items together...");
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.using);
			player.setSavedRequest(request);	
		}
		
		player.setTickCounter(3);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(dto.getResultingItemId())));
		return true;
	}

	private boolean handleUseOnNpc(UseRequest request, Player player, ResponseMaps responseMaps) {		
		// there aren't that many items that can be used on npcs right...
		// poison, magic scrolls, anything else?
		
		// when we use an item on an npc, don't do the actual check until you walk over into melee range of the npc.
		// only exception to this is if the npc doesn't exist, because then we wouldn't know where to walk in the first place.
		// also if the item is supposed to be used from range, i.e. magic scrolls!
		
		NPC targetNpc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getDest());
		if (targetNpc == null)
			return false;
		
		if (CastableDao.isCastable(request.getSrc())) {
			return handleCastableOnNpc(request, player, responseMaps);
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), targetNpc.getTileId())) {
			player.setTarget(targetNpc);	
			player.setSavedRequest(request);
			return true;
		} else {
			// using your assigned artisan task item on a master gives you points
			if (ArtisanMasterDao.npcIsArtisanMaster(targetNpc.getId())) {
				return ArtisanManager.handleUseItemOnMaster(player, ArtisanMasterDao.getArtisanMasterByNpcId(targetNpc.getId()), request.getSrc(), responseMaps);
			}
			
			// for now you can only use poison on enemies, which requires you to be in combat.
			Items item = Items.withValue(request.getSrc());
			if (item == null)
				return false;
			
			// before we figure out to do with the item, make sure we have it in our inventory.
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (!invItemIds.contains(request.getSrc()))
				return false;
			
			// if there's some mismatch with the slot + item then use the first 
			// slot in the inventory with said item instead (we know it exists in inventory now)
			int slot = request.getSrcSlot();
			if (slot >= invItemIds.size() || invItemIds.get(slot) != request.getSrc())
				slot = invItemIds.indexOf(request.getSrc());
			
			switch (item) {
			case POISON_FLASK_1:
			case POISON_FLASK_2:
			case POISON_FLASK_3:
			case POISON_FLASK_4:
			case POISON_FLASK_5:
			case POISON_FLASK_6:
			case POISON_1:
			case POISON_2:
			case POISON_3:
			case POISON_4: {
				// need to be in melee range, this also starts the fight so it works like an attack option
				// also obviously cannot be used on non-attackables.
				int becomesItemId = 0;
				if (item != Items.POISON_1 && item != Items.POISON_FLASK_1)
					becomesItemId = item.getValue() + 1;// the item ids are in incremental order ((4) -> (3) -> (2) -> (1))
				
				if (!NPCDao.npcHasAttribute(targetNpc.getId(), NpcAttributes.ATTACKABLE))
					return false;
				
				if (!FightManager.fightingWith(player, targetNpc) 
						&& !FightManager.fightWithFighterExists(player) 
						&& !FightManager.fightWithFighterExists(targetNpc)) {
					AttackRequest attackRequest = new AttackRequest();
					attackRequest.setObjectId(targetNpc.getInstanceId());
					new AttackResponse().process(attackRequest, player, responseMaps);
				}
				
				// by this point, whether or not we were previously in a fight with the targetNpc, we will be now.
				if (FightManager.fightingWith(player, targetNpc)) {
					setRecoAndResponseText(1, "you throw poison in your opponents face!");
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, becomesItemId, 1, ItemDao.getMaxCharges(becomesItemId));
					InventoryUpdateResponse.sendUpdate(player, responseMaps);
					targetNpc.inflictPoison(6);
				}
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
			
			default:
				return false;
			}
		}
	}
	
	private boolean handleCastableOnNpc(UseRequest request, Player player, ResponseMaps responseMaps) {
		NPC targetNpc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getDest());
		if (targetNpc == null)
			return false;
		
		if (!PathFinder.lineOfSightIsClear(player.getFloor(), player.getTileId(), targetNpc.getTileId(), 6)) {
			player.setTarget(targetNpc);
			player.setState(PlayerState.chasing_with_range);
			player.setRange(6);
			player.setSavedRequest(request);
			return true;
		} else {
			CastableDto castable = CastableDao.getCastableByItemId(request.getSrc());
			if (castable == null)
				return false;
			
			// you can't teleport an npc
			if (TeleportableDao.isTeleportable(castable.getItemId()))
				return false;
			
			if (!NPCDao.npcHasAttribute(targetNpc.getId(), NpcAttributes.ATTACKABLE))
				return false;
			
			if (FightManager.fightWithFighterExists(targetNpc) && !FightManager.fightingWith(targetNpc, player)) {
				setRecoAndResponseText(0, "someone is already fighting that.");
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
			
			if (FightManager.fightWithFighterExists(player) && !FightManager.fightingWith(targetNpc, player)) {
				setRecoAndResponseText(0, "you're already fighting something else.");
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
			
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (!invItemIds.contains(castable.getItemId()))
				return false;
			
			// in case of user fuckery, they modify the slot to a different slot.
			// if it's somehow different then set it to the correct slot.
			int slot = request.getSrcSlot();
			if (slot >= invItemIds.size() || invItemIds.get(slot) != castable.getItemId())
				slot = invItemIds.indexOf(castable.getItemId());
			
			// at this point we're good to cast the spell.		
			if (CastSpellResponse.castOffensiveSpell(castable, player, targetNpc, responseMaps)) {
				new CastSpellResponse(player.getId(), targetNpc.getInstanceId(), "npc", castable.getSpriteFrameId()).process(request, player, responseMaps);
			}
			
			player.setState(PlayerState.idle);
			
			return true;
		}
	}
	
	private boolean handleUseOnPlayer(UseRequest request, Player player, ResponseMaps responseMaps) {
		if (CastableDao.isCastable(request.getSrc())) {
			return handleCastableOnPlayer(request, player, responseMaps);
		}
		
		final Player targetPlayer = WorldProcessor.getPlayerById(request.getDest());
		if (targetPlayer == null) {
			return false;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), targetPlayer.getTileId())) {
			player.setTarget(targetPlayer);	
			player.setSavedRequest(request);
			return true;
		} else {
			// for now you can only use poison on enemies, which requires you to be in combat.
			Items item = Items.withValue(request.getSrc());
			if (item == null)
				return false;
			
			// before we figure out to do with the item, make sure we have it in our inventory.
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (!invItemIds.contains(request.getSrc()))
				return false;
			
			// if there's some mismatch with the slot + item then use the first 
			// slot in the inventory with said item instead (we know it exists in inventory now)
			int slot = request.getSrcSlot();
			if (slot >= invItemIds.size() || invItemIds.get(slot) != request.getSrc())
				slot = invItemIds.indexOf(request.getSrc());
			
			switch (item) {
			case POISON_FLASK_1:
			case POISON_FLASK_2:
			case POISON_FLASK_3:
			case POISON_FLASK_4:
			case POISON_FLASK_5:
			case POISON_FLASK_6:
			case POISON_1:
			case POISON_2:
			case POISON_3:
			case POISON_4: {
				// need to be in melee range, this also starts the fight so it works like an attack option
				// also obviously cannot be used on non-attackables.
				int becomesItemId = 0;
				if (item != Items.POISON_1 && item != Items.POISON_FLASK_1)
					becomesItemId = item.getValue() + 1;// the item ids are in incremental order ((4) -> (3) -> (2) -> (1))
				
				if (FightManager.fightingWith(player, targetPlayer)) {
					Fight fight = FightManager.getFightByPlayerId(player.getId());
					if (fight.getRules() != null && (fight.getRules() & DuelRules.no_poison.getValue()) > 0) {
						setRecoAndResponseText(0, "poison is not allowed in this duel.");
					} else {
						setRecoAndResponseText(1, "you throw poison in your opponents face!");
						PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, becomesItemId, 1, ItemDao.getMaxCharges(becomesItemId));
						InventoryUpdateResponse.sendUpdate(player, responseMaps);
						targetPlayer.inflictPoison(6);
						
						responseMaps.addClientOnlyResponse(targetPlayer, MessageResponse.newMessageResponse(String.format("%s threw poison in your face!", player.getDto().getName()), "white"));
					}
				} else {
					setRecoAndResponseText(0, "you need to be in a duel to poison them.");
				}
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
			
			default:
				return false;
			}
		}
	}
	
	private boolean handleCastableOnPlayer(UseRequest request, Player player, ResponseMaps responseMaps) {
		CastableDto castable = CastableDao.getCastableByItemId(request.getSrc());
		if (castable == null)
			return false;
		
		int playerMagicLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.MAGIC, player.getId());
		if (playerMagicLevel < castable.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d magic to cast that.", castable.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(castable.getItemId()))
			return false;
		
		int slot = invItemIds.indexOf(castable.getItemId());
		
		// are we using something on ourself?
		if (request.getDest() == player.getId()) {
			if (TeleportableDao.isTeleportable(castable.getItemId()))
				return CastSpellResponse.handleTeleport(castable, player, slot, responseMaps);
		} else {
			Player targetPlayer = WorldProcessor.getPlayerById(request.getDest());
			if (targetPlayer == null) {
				return false;
			}
			
			if (!PathFinder.lineOfSightIsClear(player.getFloor(), player.getTileId(), targetPlayer.getTileId(), 6)) {
				player.setTarget(targetPlayer);
				player.setState(PlayerState.chasing_with_range);
				player.setRange(6);
				player.setSavedRequest(request);
				return true;
			} else {
				Fight fight = FightManager.getFightByPlayerId(player.getId());
				if (fight == null) {
					setRecoAndResponseText(0, "you can only cast spells on other players during a duel.");
					responseMaps.addClientOnlyResponse(player, this);
					return true;
				} else if (!FightManager.fightingWith(player, targetPlayer)) {
					setRecoAndResponseText(0, "you can only attack your opponent.");
					responseMaps.addClientOnlyResponse(player, this);
					return true;
				} else if (fight.getRules() != null && (fight.getRules() & DuelRules.no_magic.getValue()) > 0) {
					setRecoAndResponseText(0, "magic isn't allowed in this duel!");
					responseMaps.addClientOnlyResponse(player, this);
					return true;
				}
				
				if (CastSpellResponse.castOffensiveSpell(castable, player, targetPlayer, responseMaps)) {
					new CastSpellResponse(player.getId(), targetPlayer.getId(), "player", castable.getSpriteFrameId()).process(request, player, responseMaps);
				}
				
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean handleConstruction(UseRequest request, Player player, ResponseMaps responseMaps, boolean flatpack) {
		final int src = request.getSrc();
		final int dest = request.getDest();
		
		int toolId = ConstructableDao.itemIsConstructionTool(src) ? src : dest;
		int materialId = toolId == src ? dest : src;
		
		final Set<Integer> allPotentialTools = new HashSet<>();
		allPotentialTools.add(toolId); // if it's an original, non-artisan tool
		allPotentialTools.addAll(ConstructableDao.getOriginalToolsFromArtisanTool(toolId)); // if it's an artisan tool
		
		// we could have the situation in the future where the tool we're using on the item functions as multiple tools.
		// for example, a tinderhammer that functions as both a tinderbox and a hammer.
		// we want to pull all possible constructables which use either tinderbox or hammer in this case, 
		// so we run through all matching tools and all matching constructables that use said tools and material
		final Set<ConstructableDto> constructables = new HashSet<>();
		allPotentialTools.forEach(e -> constructables.addAll(ConstructableDao.getAllConstructablesWithMaterials(e, materialId)));
		if (constructables.isEmpty()) {
			return false; // no matches; nothing interesting happens (e.g. use tinderbox on helmet or whatevs)
		}
		else if (constructables.size() == 1) {
			// one result means we don't show the menu, just build the thing
			// it's a set so just do a foreach to get the only entry
			constructables.forEach(e -> {
				ConstructionRequest constructionRequest = new ConstructionRequest();
				constructionRequest.setSceneryId(e.getResultingSceneryId());
				constructionRequest.setTileId(player.getTileId());
				constructionRequest.setFlatpack(flatpack);
				new ConstructionResponse().process(constructionRequest, player, responseMaps);
			});
		} else {
			// show a menu with all the constructables
			new ShowConstructionTableResponse(constructables, flatpack, player.getTileId()).process(null, player, responseMaps);
		}
		
		return true;
	}
	
	private boolean handleFlowerSack(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int src = request.getSrc();
		final int dest = request.getDest();
		
		// the thing that isn't the flower sack is the flower
		int flowerId = src == Items.FLOWER_SACK.getValue() ? dest : src;
		
		if (!PickableDao.isItemPickable(flowerId))
			return false;
		
		Map<Integer, InventoryItemDto> sackContents = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.FLOWER_SACK);
		int remainingSpace = 50 - sackContents.values().stream().reduce(0, (cumulative, item) -> cumulative + item.getCount(), Integer::sum);
		if (remainingSpace == 0) {
			setRecoAndResponseText(0, "the sack is full.");
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		// it's a flower, now put all the flowers of this type in the flower sack
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		for (int i = 0; i < invItemIds.size(); ++i) {
			if (invItemIds.get(i) == flowerId) {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.FLOWER_SACK, flowerId, 1, 0);
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, i, 0, 0, 0);
				if (--remainingSpace == 0)
					break;
			}
		}
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		return true;
	}
}
