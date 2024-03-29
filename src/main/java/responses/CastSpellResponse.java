package responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import database.dao.CastableDao;
import database.dao.EquipmentDao;
import database.dao.NPCDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import database.dao.TeleportableDao;
import database.dto.CastableDto;
import database.dto.EquipmentBonusDto;
import database.dto.InventoryItemDto;
import database.dto.TeleportableDto;
import processing.attackable.Attackable;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.ConstructableManager;
import processing.managers.FightManager;
import processing.managers.FightManager.Fight;
import processing.managers.HousingManager;
import requests.Request;
import requests.UseRequest;
import types.DamageTypes;
import types.Items;
import types.NpcAttributes;
import types.Prayers;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

@SuppressWarnings("unused")
public class CastSpellResponse extends Response {
	private int sourceTileId;
	private int targetId;
	private String targetType = null;
	private int spriteFrameId;
	private double lifetime;
	private double speed;

	public CastSpellResponse(int sourceTileId, int targetId, String targetType, int spriteFrameId, double lifetime, double speed) {
		setAction("cast_spell");
		this.sourceTileId = sourceTileId;
		this.targetId = targetId;
		this.targetType = targetType;
		this.spriteFrameId = spriteFrameId;
		this.lifetime = lifetime;
		this.speed = speed;
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true; // spells can be cast during combat or not
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		CastableDto castable = CastableDao.getCastableByItemId(((UseRequest)req).getSrc());
		if (castable == null)
			return;
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(castable.getItemId()))
			return;
		final int slot = invItemIds.indexOf(castable.getItemId());
		
		int chanceToSaveRune = 0;
		if (player.prayerIsActive(Prayers.RUNELESS_MAGIC))
			chanceToSaveRune = 15;
		else if (player.prayerIsActive(Prayers.RUNELESS_MAGIC_LVL_2))
			chanceToSaveRune = 30;
		else if (player.prayerIsActive(Prayers.RUNELESS_MAGIC_LVL_3))
			chanceToSaveRune = 45;
		
		if (ConstructableManager.constructableIsInRadius(player.getFloor(), player.getTileId(), 136, 3))
			chanceToSaveRune += 30;
		
		if (!RandomUtil.chance(chanceToSaveRune)) {
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
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), this);
	}
	
	public static boolean castOffensiveSpell(CastableDto castable, Player player, Attackable opponent, ResponseMaps responseMaps) {
		// from here, either neither player or npc are in combat, or player and npc are in combat with eachother.
		int magicLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.MAGIC, player.getId());
		if (magicLevel < castable.getLevel()) {
			final String message = String.format("you need %d magic to cast that.", castable.getLevel());
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, "white"));
			return false;
		}
				
		// failure chance based off magic bonus, magic level and requirement levels
		EquipmentBonusDto equipmentBonuses = EquipmentDao.getEquipmentBonusesByPlayerId(player.getId());
		int chanceToFail = Math.max(castable.getLevel() - (equipmentBonuses.getMage() + magicLevel) + 25, 0);
		if (new Random().nextInt(100) < chanceToFail) {
			// failed
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you failed to cast the spell!", "white"));
			return false;
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
		if (castable.getItemId() == Items.CRUMBLE_UNDEAD_RUNE.getValue()) { // crumble undead rune
			if (opponent instanceof NPC && NPCDao.npcHasAttribute(((NPC)opponent).getId(), NpcAttributes.UNDEAD)) {
				opponent.onHit(new Random().nextInt(castable.getMaxHit() + 1), DamageTypes.MAGIC, responseMaps);
			}
		}
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
		return true;
	}
	
	public static boolean handleTeleport(CastableDto castable, Player player, int slot, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you can't retreat yet!", "white"));
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
		
		if (castable.getItemId() == Items.HOUSE_TELEPORT_RUNE.getValue()) {
			// house teleport is a special case, because everyone's location is different.
			final int houseId = HousingManager.getHouseIdByPlayerId(player.getId());
			if (houseId == -1) {
				responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you don't have a house to teleport to.", "white"));
				return true;
			}
			
			int[] floorAndTileId = HousingManager.getRandomWalkableTileByPlayerId(player.getId());
			if (floorAndTileId == null)
				return false;
			
			teleportable = new TeleportableDto(castable.getItemId(), floorAndTileId[0], floorAndTileId[1]);
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
		
		player.setFloor(teleportable.getFloor(), responseMaps);
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
