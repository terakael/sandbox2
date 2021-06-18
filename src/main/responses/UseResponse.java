package main.responses;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import main.database.dao.BrewableDao;
import main.database.dao.CastableDao;
import main.database.dao.DoorDao;
import main.database.dao.EquipmentDao;
import main.database.dao.ItemDao;
import main.database.dao.NPCDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dao.StatsDao;
import main.database.dao.TeleportableDao;
import main.database.dao.UseItemOnItemDao;
import main.database.dto.CastableDto;
import main.database.dto.EquipmentBonusDto;
import main.database.dto.InventoryItemDto;
import main.database.dto.TeleportableDto;
import main.database.dto.UseItemOnItemDto;
import main.processing.Attackable;
import main.processing.ClientResourceManager;
import main.processing.FightManager;
import main.processing.FightManager.Fight;
import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.processing.WorldProcessor;
import main.requests.AttackRequest;
import main.requests.Request;
import main.requests.UseRequest;
import main.scenery.Scenery;
import main.scenery.SceneryManager;
import main.types.DamageTypes;
import main.types.DuelRules;
import main.types.Items;
import main.types.NpcAttributes;
import main.types.Prayers;
import main.types.Stats;
import main.types.StorageTypes;
import main.utils.RandomUtil;

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
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getDest(), !targetIsDoor)) {
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
		
		ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), ItemDao.getItem(dto.getResultingItemId()).getSpriteFrameId());
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), actionBubble);
		
		ClientResourceManager.addItems(player, Collections.singleton(dto.getResultingItemId()));
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
			case POISON_1:
			case POISON_2:
			case POISON_3:
			case POISON_4: {
				// need to be in melee range, this also starts the fight so it works like an attack option
				// also obviously cannot be used on non-attackables.
				int becomesItemId = 0;
				if (item != Items.POISON_1)
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
			castOffensiveSpell(castable, player, targetNpc, responseMaps);
			
			int chanceToSaveRune = 0;
			if (player.prayerIsActive(Prayers.RUNELESS_MAGIC))
				chanceToSaveRune = 15;
			else if (player.prayerIsActive(Prayers.RUNELESS_MAGIC_LVL_2))
				chanceToSaveRune = 30;
			else if (player.prayerIsActive(Prayers.RUNELESS_MAGIC_LVL_3))
				chanceToSaveRune = 45;
			
			if (RandomUtil.getRandom(0, 100) > chanceToSaveRune) { // chance failed, so use up the rune.  seems to be 1/100 chance to save a rune by default (0 == 0)
				InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot);
				if (item.getCount() > 1) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(
							player.getId(), 
							StorageTypes.INVENTORY, 
							slot, 
							item.getItemId(), item.getCount() - 1, item.getCharges());
				} else {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(
							player.getId(), 
							StorageTypes.INVENTORY, 
							slot, 
							0, 1, 0);
				}
				InventoryUpdateResponse.sendUpdate(player, responseMaps);
			}
			
			CastSpellResponse castSpellResponse = new CastSpellResponse(player.getId(), targetNpc.getInstanceId(), "npc", castable.getSpriteFrameId());
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), castSpellResponse);
			
			player.setState(PlayerState.idle);
			
			return true;
		}
	}
	
	private void castOffensiveSpell(CastableDto castable, Player player, Attackable opponent, ResponseMaps responseMaps) {
		// from here, either neither player or npc are in combat, or player and npc are in combat with eachother.
		int magicLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.MAGIC, player.getId());
		if (magicLevel < castable.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d magic to cast that.", castable.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
				
		// failure chance based off magic bonus, magic level and requirement levels
		EquipmentBonusDto equipmentBonuses = EquipmentDao.getEquipmentBonusesByPlayerId(player.getId());
		int chanceToFail = Math.max(castable.getLevel() - (equipmentBonuses.getMage() + magicLevel) + 25, 0);
		if (new Random().nextInt(100) < chanceToFail) {
			// failed
			setRecoAndResponseText(1, "you failed to cast the spell!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
				
		// TODO rune saving based off magic bonus, magic level and requirement level
		int damage = new Random().nextInt(castable.getMaxHit() + 1);// +1 to include the max hit
		
		// higher the magic bonus, the more chance of hitting higher
		for (int i = 0; i < Math.floor(player.getBonuses().get(Stats.MAGIC) / 11); ++i) {
			if (damage > castable.getMaxHit() / 2)
				break;
			damage = new Random().nextInt(castable.getMaxHit() + 1);
		}
		
		opponent.onHit(damage, DamageTypes.MAGIC, responseMaps);
		if (opponent.getCurrentHp() == 0) {
			opponent.onDeath(player, responseMaps);
		} else {
			if (!FightManager.fightWithFighterExists(opponent))
				opponent.setTarget(player);
			
			// handle rune special effects
			switch (Items.withValue(castable.getItemId())) {
			case DISEASE_RUNE:
				// 1/6 chance to poison for 3
				if (new Random().nextInt(6) == 0)
					opponent.inflictPoison(3);
				break;
				
			case DECAY_RUNE:
				// 1/4 chance poison for 6
				if (new Random().nextInt(4) == 0)
					opponent.inflictPoison(6);
				break;
				
			case BLOOD_TITHE_RUNE:
				HashMap<Stats, Integer> relativeBoosts = StatsDao.getRelativeBoostsByPlayerId(player.getId());
				int newRelativeBoost = relativeBoosts.get(Stats.HITPOINTS) + damage;
				if (newRelativeBoost > 0)
					newRelativeBoost = 0;

				player.setCurrentHp(player.getDto().getMaxHp() + newRelativeBoost);
				StatsDao.setRelativeBoostByPlayerIdStatId(player.getId(), Stats.HITPOINTS, newRelativeBoost);
				
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(player.getId());
				playerUpdateResponse.setCurrentHp(player.getCurrentHp());
				responseMaps.addBroadcastResponse(playerUpdateResponse);
				break;
				
			default:
				break;
			}
		}
		
		int exp = castable.getExp() + (damage * 4);
		
		Map<Integer, Double> currentStatExp = StatsDao.getAllStatExpByPlayerId(player.getId());
		AddExpResponse addExpResponse = new AddExpResponse();
		addExpResponse.addExp(Stats.MAGIC.getValue(), exp);		
		responseMaps.addClientOnlyResponse(player, addExpResponse);
		
		StatsDao.addExpToPlayer(player.getId(), Stats.MAGIC, exp);
		player.refreshStats(currentStatExp);
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(player.getId()));
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), playerUpdate);// should be local
		
		ClientResourceManager.addSpell(player, castable.getItemId());
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
			case POISON_1:
			case POISON_2:
			case POISON_3:
			case POISON_4: {
				// need to be in melee range, this also starts the fight so it works like an attack option
				// also obviously cannot be used on non-attackables.
				int becomesItemId = 0;
				if (item != Items.POISON_1)
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
				return handleTeleport(castable, player, slot, responseMaps);
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
				
				castOffensiveSpell(castable, player, targetPlayer, responseMaps);
				
				InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot);
				if (item.getCount() > 1) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(
							player.getId(), 
							StorageTypes.INVENTORY, 
							slot, 
							item.getItemId(), item.getCount() - 1, item.getCharges());
				} else {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(
							player.getId(), 
							StorageTypes.INVENTORY, 
							slot, 
							0, 1, 0);
				}
				InventoryUpdateResponse.sendUpdate(player, responseMaps);
				
				CastSpellResponse castSpellResponse = new CastSpellResponse(player.getId(), targetPlayer.getId(), "player", castable.getSpriteFrameId());
				responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), castSpellResponse);
				
				player.setState(PlayerState.idle);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean handleTeleport(CastableDto castable, Player player, int slot, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't retreat yet!");
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		// we cancel this fight slightly differently because the usual case is the fight ends and sends a local response
		// but we just teleported so we don't receive the response, therefore we need to send a special client-only response
		Fight fight = FightManager.getFightByPlayerId(player.getId());
		if (fight != null) {
			// if there is a fight, then cancel it
			if (fight.getFighter2() instanceof NPC) {
				PvmEndResponse resp = new PvmEndResponse();
				resp.setPlayerId(((Player)fight.getFighter1()).getId());
				resp.setMonsterId(((NPC)fight.getFighter2()).getInstanceId());
//				resp.setPlayerTileId(fight.getFighter1().getTileId());
				resp.setMonsterTileId(fight.getFighter2().getTileId());
				responseMaps.addClientOnlyResponse(player, resp);
			} else {
				fight.getFighter2().setTarget(null);
				PvpEndResponse resp = new PvpEndResponse();
				resp.setPlayer1Id(((Player)fight.getFighter1()).getId());
				resp.setPlayer2Id(((Player)fight.getFighter2()).getId());
				
				// if this is the current player then we don't wanna set the tileId as we're currently teleporting
				if (fight.getFighter1() != player)
					resp.setPlayer1TileId(fight.getFighter1().getTileId());
				
				if (fight.getFighter2() != player)
					resp.setPlayer2TileId(fight.getFighter2().getTileId());
				
				responseMaps.addClientOnlyResponse(player, resp);
			}
			
			FightManager.cancelFight(player, responseMaps);
		}
		
		TeleportableDto teleportable = TeleportableDao.getTeleportableByItemId(castable.getItemId());
		if (teleportable == null) {
			return false;
		}
		
		// send teleport explosions to both where the player teleported from, and where they're teleporting to
		// that way players on both sides of the teleport will see it
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), new TeleportExplosionResponse(player.getTileId()));
		responseMaps.addLocalResponse(teleportable.getFloor(), teleportable.getTileId(), new TeleportExplosionResponse(teleportable.getTileId()));
		
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setTileId(teleportable.getTileId());
		playerUpdate.setSnapToTile(true);
		
		responseMaps.addClientOnlyResponse(player, playerUpdate);
		responseMaps.addLocalResponse(teleportable.getFloor(), teleportable.getTileId(), playerUpdate);
		
		player.setFloor(teleportable.getFloor());
		player.setTileId(teleportable.getTileId());
		
		player.clearPath();
		
		Map<Integer, Double> currentStatExp = StatsDao.getAllStatExpByPlayerId(player.getId());
		AddExpResponse addExpResponse = new AddExpResponse();
		addExpResponse.addExp(Stats.MAGIC.getValue(), castable.getExp());		
		responseMaps.addClientOnlyResponse(player, addExpResponse);
		StatsDao.addExpToPlayer(player.getId(), Stats.MAGIC, castable.getExp());
		player.refreshStats(currentStatExp);
		
		InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot);
		if (item.getCount() > 1) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					player.getId(), 
					StorageTypes.INVENTORY, 
					slot, 
					item.getItemId(), item.getCount() - 1, item.getCharges());
		} else {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					player.getId(), 
					StorageTypes.INVENTORY, 
					slot, 
					0, 1, 0);
		}
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		return true;
	}
}
