package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.GroundItemManager;
import main.database.AnimationDao;
import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.NpcDialogueDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.database.TeleportableDao;
import main.database.TeleportableDto;
import main.requests.FishRequest;
import main.requests.MineRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.SmithRequest;
import main.responses.AddExpResponse;
import main.responses.DeathResponse;
import main.responses.EquipResponse;
import main.responses.FinishClimbResponse;
import main.responses.FinishCookingResponse;
import main.responses.FinishFishingResponse;
import main.responses.FinishMiningResponse;
import main.responses.FinishSmithResponse;
import main.responses.FinishUseResponse;
import main.responses.InventoryUpdateResponse;
import main.responses.LoadRoomResponse;
import main.responses.MessageResponse;
import main.responses.PlayerUpdateResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.ResponseMaps;
import main.responses.StatBoostResponse;
import main.types.Buffs;
import main.types.DamageTypes;
import main.types.ItemAttributes;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;
import main.utils.Utils;

public class Player extends Attackable {
	public enum PlayerState {
		idle,
		walking,
		chasing,// used for walking to a moving target (following, moving to attack something etc)
		following,
		mining,
		smithing,
		fighting,
		using,
		climbing, // ladders etc, give a tick or so duration (like for climbing animation in the future)
		cooking,
		fishing,
		dead
	};
	
	@Getter private PlayerDto dto;
	@Getter private Session session;
	@Setter private Stack<Integer> path = new Stack<>();// stack of tile_ids
	@Getter private PlayerState state = PlayerState.idle;
	@Setter private Request savedRequest = null;
	@Setter private int tickCounter = 0;
	@Setter @Getter private NpcDialogueDto currentDialogue = null;
	@Setter @Getter private int shopId;// if the player is currently in a shop, this is the id
	private int postFightCooldown = 0;
	private HashMap<Integer, Integer> equippedSlotsByItemId = new HashMap<>();// itemId, slot
	private HashMap<Buffs, Integer> activeBuffs = new HashMap<>(); // buff, remaining ticks
	@Getter @Setter private Set<Integer> inRangeNpcs = new HashSet<>();
	@Getter @Setter private Set<Integer> inRangePlayers = new HashSet<>();
	@Getter @Setter private Map<Integer, List<Integer>> inRangeGroundItems = new HashMap<>();
	@Getter @Setter private Map<Integer, Set<Integer>> loadedSegments = new HashMap<>(); // roomId, <segments>
	
	private final int MAX_STAT_RESTORE_TICKS = 100;// 100 ticks == one minute
	private int statRestoreTicks = MAX_STAT_RESTORE_TICKS;
	
	private final int restorationBuffTriggerTicks = 3;
	private int restorationBuffCurrentTriggerTicks = restorationBuffTriggerTicks;
	
	public Player(PlayerDto dto, Session session) {
		this.session = session;
		
		if (dto != null) {
			this.dto = dto;
			tileId = dto.getTileId();
			roomId = dto.getRoomId();
			
			refreshStats();
			refreshBoosts();
			
			currentHp = StatsDao.getStatLevelByStatIdPlayerId(Stats.HITPOINTS, dto.getId()) + StatsDao.getRelativeBoostsByPlayerId(dto.getId()).get(Stats.HITPOINTS);
			setPostFightCooldown();
		}
	}
	
	public void addBuff(Buffs buff) {
		activeBuffs.put(buff, buff.getMaxTicks());
	}
	
	public boolean hasBuff(Buffs buff) {
		return activeBuffs.containsKey(buff);
	}
	
	public void refreshStats(Map<Integer, Integer> statExp) {
		for (Map.Entry<Integer, Integer> stat : statExp.entrySet())
			stats.put(Stats.withValue(stat.getKey()), StatsDao.getLevelFromExp(stat.getValue()));
	}
	
	public void refreshStats() {
		refreshStats(StatsDao.getAllStatExpByPlayerId(dto.getId()));
	}
	
	public void refreshBoosts() {
		setBoosts(StatsDao.getRelativeBoostsByPlayerId(getId()));
	}
	
	public void refreshBonuses() {
		EquipmentBonusDto equipment = EquipmentDao.getEquipmentBonusesByPlayerId(getId());
		refreshBonuses(equipment);
		
		int weaponCooldown = equipment.getSpeed();
		if (weaponCooldown == 0)
			weaponCooldown = 3;// no weapon equipped, default to speed between sword/daggers
		setMaxCooldown(weaponCooldown);
	}
	
	public void refreshBonuses(EquipmentBonusDto equipment) {
		bonuses.put(Stats.STRENGTH, equipment.getStr());
		bonuses.put(Stats.ACCURACY, equipment.getAcc());
		bonuses.put(Stats.DEFENCE, equipment.getDef());
		bonuses.put(Stats.AGILITY, equipment.getAgil());
		bonuses.put(Stats.HITPOINTS, equipment.getHp());
	}
	
	public void process(ResponseMaps responseMaps) {
		// called each tick; build a response where necessary
		if (postFightCooldown > 0)
			--postFightCooldown;
		
		if (--statRestoreTicks <= 0) {
			statRestoreTicks = MAX_STAT_RESTORE_TICKS;
			restoreStats(responseMaps);
		}
		
		// decrement the remaining ticks on every active buff
		activeBuffs.replaceAll((k, v) -> v -= 1);
		
		// remove all the buffs that just hit 0 ticks
		boolean buffsExpired = activeBuffs.entrySet().removeIf(e -> e.getValue() <= 0);
		if (buffsExpired) {
			MessageResponse resp = new MessageResponse();
			resp.setColour("white");
			resp.setResponseText("your buff has expired.");
			responseMaps.addClientOnlyResponse(this, resp);
		}
		
		if (activeBuffs.containsKey(Buffs.RESTORATION)) {
			if (--restorationBuffCurrentTriggerTicks <= 0) {
				restorationBuffCurrentTriggerTicks = restorationBuffTriggerTicks;
				restoreNegativeStats(responseMaps);
			}
		}
		
		switch (state) {
		case walking: {
			// if the player path stack isn't empty, then pop one off and create a player_updates response entry.
			if (!path.isEmpty()) {				
				setTileId(path.pop());

				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getRoomId(), getTileId(), playerUpdateResponse);
				
				if (path.isEmpty()) {
					if (savedRequest == null) // if not null then reprocess the saved request; this is a walkandaction.
						state = PlayerState.idle;
					else {
						Response response = ResponseFactory.create(savedRequest.getAction());
						response.process(savedRequest, this, responseMaps);
					}
				}
			}
			
			break;
		}
		case following: {
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getRoomId(), getTileId(), playerUpdateResponse);
			}
			
			if (roomId != target.getRoomId())// if the target goes down a ladder or something then stop following
				target = null;

			// maybe the target player logged out
			if (target == null) {
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(roomId, tileId, target.getTileId())) {
				path = PathFinder.findPath(roomId, tileId, target.getTileId(), false);
			}
			break;
		}
		case chasing: {
			if (target == null) {
				// player could have logged out/died as they were chasing
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(roomId, tileId, target.getTileId())) {
				path = PathFinder.findPath(roomId, tileId, target.getTileId(), false);
			} else {
				// start the fight
				if (savedRequest != null) {
					Request req = savedRequest;
					savedRequest = null;
					
					Response response = ResponseFactory.create(req.getAction());
					response.process(req, this, responseMaps);
				}
				state = PlayerState.fighting;
				path.clear();
			}
			
			// similar to walking, but need to recalculate path each tick due to moving target
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getRoomId(), getTileId(), playerUpdateResponse);
			}

			break;
		}
		case mining:
			// waiting until the tick counter hits zero, then do the actual mining and create a finish_mining response
			if (--tickCounter <= 0) {
				// TODO do checks:
				// is player close enough to target tile?
				// is target tile a rock?
				// does player have level to mine this rock?
				// does player have inventory space for the loot?
				
				// if yes to all above:
				// add rock loot to first empty inventory space
				// create storage_update response
				// create finish_mining response
				// set state to idle.
				if (!(savedRequest instanceof MineRequest)) {
					savedRequest = null;
					state = PlayerState.idle;
					return;
				}
				
				new FinishMiningResponse().process(savedRequest, this, responseMaps);

				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
		case fishing:
			if (--tickCounter <= 0) {
				if (!(savedRequest instanceof FishRequest)) {
					savedRequest = null;
					state = PlayerState.idle;
					return;
				}
				
				new FinishFishingResponse().process(savedRequest, this, responseMaps);

				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
		case smithing:
			if (--tickCounter <= 0) {
				if (!(savedRequest instanceof SmithRequest)) {
					savedRequest = null;
					state = PlayerState.idle;
					return;
				}
				
				new FinishSmithResponse().process(savedRequest, this, responseMaps);
				
				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
		case fighting:
			if (--tickCounter <= 0) {
				// calculate the actual attack, create hitspat_update response, reset tickCounter.
			}
			break;
		case climbing: {
			if (--tickCounter <= 0) {
				new FinishClimbResponse().process(savedRequest, this, responseMaps);
				savedRequest = null;
			}
			break;
		}
			
		case using:
			if (--tickCounter <= 0) {
				new FinishUseResponse().process(savedRequest, this, responseMaps);
				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
			
		case cooking:
			if (--tickCounter <= 0) {
				new FinishCookingResponse().process(savedRequest, this, responseMaps);
				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
			
		case dead: {// note that while the player is in the dead state, every response (except Message) is ignored at the Response superclass level
			if (--tickCounter <= 0) {
				// TODO check if the player died in the same room; reloading the room in that case is not necessary
				new LoadRoomResponse().process(null, this, responseMaps);
				
				// update the player inventory to show there's no more items
				new InventoryUpdateResponse().process(RequestFactory.create("dummy", getId()), this, responseMaps);
				
				PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
				playerUpdate.setId(getId());
				playerUpdate.setTileId(getTileId());
				playerUpdate.setRoomId(getRoomId());
				playerUpdate.setSnapToTile(true);
				playerUpdate.setCurrentHp(currentHp);
				playerUpdate.setMaxHp(currentHp);
				playerUpdate.setEquipAnimations(AnimationDao.getEquipmentAnimationsByPlayerId(getId()));
				playerUpdate.setRespawn(true);
				
				// the reason this is local is so the other players know the player respawned
				// (they also receive the player_in_range response automatically but if they're around the spawn point it will be missing respawn data)
				responseMaps.addLocalResponse(getRoomId(), getTileId(), playerUpdate);
				
				state = PlayerState.idle;
			}
			break;
		}
		case idle:// fall through
		default:
			break;
		}
	}
	
	private void restoreStats(ResponseMaps responseMaps) {
		HashMap<Stats, Integer> boosts = StatsDao.getRelativeBoostsByPlayerId(getId());
		
		for (Map.Entry<Stats, Integer> entry : boosts.entrySet()) {
			// max hitpoints depends on the hitpoints bonus (i.e. +5 means we can heal +5 over max hitpoints)
			int maxBoost = entry.getKey().equals(Stats.HITPOINTS) ? getBonuses().get(Stats.HITPOINTS) : 0;
						
			int relativeBoost = entry.getValue();
			if (relativeBoost > maxBoost)
				--relativeBoost;
			else if (relativeBoost < maxBoost)
				++relativeBoost;
			StatsDao.setRelativeBoostByPlayerIdStatId(getId(), entry.getKey(), relativeBoost);
		}
		
		setBoosts(boosts);
		
		new StatBoostResponse().process(null, this, responseMaps);
	}
	
	private void restoreNegativeStats(ResponseMaps responseMaps) {
		HashMap<Stats, Integer> boosts = StatsDao.getRelativeBoostsByPlayerId(getId());
		
		for (Map.Entry<Stats, Integer> entry : boosts.entrySet()) {
			int relativeBoost = entry.getValue();
			
			// max hitpoints depends on the hitpoints bonus (i.e. +5 means we can heal +5 over max hitpoints)
			int maxBoost = entry.getKey().equals(Stats.HITPOINTS) ? getBonuses().get(Stats.HITPOINTS) : 0;
			
			if (relativeBoost < maxBoost)
				++relativeBoost;
			StatsDao.setRelativeBoostByPlayerIdStatId(getId(), entry.getKey(), relativeBoost);
		}
		
		setBoosts(boosts);
		
		new StatBoostResponse().process(null, this, responseMaps);
	}
	
	public void setState(PlayerState state) {
		this.state = state;
		this.shopId = 0;
	}
	
	public void setTileId(int tileId) {
		this.tileId = tileId;
		PlayerDao.updateTileId(dto.getId(), tileId);
	}
	
	public void setRoomId(int roomId) {
		this.roomId = roomId;
		PlayerDao.updateRoomId(dto.getId(), roomId);
	}
	
	public int getTileId() {
		return tileId;
	}
	
	public boolean isGod() {
		return dto.getId() == 3;// god id
	}
	
	public int getId() {
		return dto.getId();
	}
	
	@Override
	public void onDeath(Attackable killer, ResponseMaps responseMaps) {
		lastTarget = null;
		clearPoison();
		if (FightManager.fightWithFighterExists(this)) {
			FightManager.cancelFight(this, responseMaps);
		}
		
		// unequip and drop all the items in inventory
		EquipmentDao.clearAllEquppedItems(getId());
		
		HashMap<Integer, InventoryItemDto> inventoryList = PlayerStorageDao.getStorageDtoMapByPlayerId(getId(), StorageTypes.INVENTORY.getValue());
		for (InventoryItemDto dto : inventoryList.values()) {
			if (dto.getSlot() < 3)
				continue;// you always protect your items in the first three slots (0, 1, 2)
			
			if (dto.getItemId() != 0) {
				int stack = ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.STACKABLE) ? dto.getCount() : 1;
				int charges = ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.CHARGED) ? dto.getCharges() : 1;
				
				if (killer instanceof Player && ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.TRADEABLE)) {
					// if it's a tradeable unique, the killer should only see it if they don't already have one.
					if (ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.UNIQUE)) {
						if (PlayerStorageDao.itemExistsInPlayerStorage(((Player)killer).getId(), dto.getItemId()) || GroundItemManager.itemIsOnGround(roomId, ((Player)killer).getId(), dto.getItemId())) {
							continue;
						}
					}
					
					GroundItemManager.add(roomId, ((Player)killer).getId(), dto.getItemId(), tileId, stack, charges);
				} else {
					// for now, untradeables will drop on the ground for the owner to pick back up
					GroundItemManager.add(roomId, getId(), dto.getItemId(), tileId, stack, charges);
				}
			}
		}
		PlayerStorageDao.clearPlayerInventoryExceptFirstThreeSlots(getId());
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.HITPOINTS, 0);
		currentHp = StatsDao.getStatLevelByStatIdPlayerId(Stats.HITPOINTS, dto.getId());
		
		// let everyone around the dead player know they died 
		responseMaps.addLocalResponse(getRoomId(), getTileId(), new DeathResponse(dto.getId()));
		
		// respawn at the tyrotown teleport spot
		TeleportableDto respawnPoint = TeleportableDao.getTeleportableByItemId(Items.TYROTOWN_TELEPORT_RUNE.getValue());
		
		// if the player's spawn point is not local to where they died, they won't receive the local death response.
		// it's not ideal to send an additional response but we have to set the respawn point immediately
		// otherwise the player can just disconnect during the death sequence then relogin in the same spot they died (with full hp)
		if (respawnPoint.getRoomId() != getRoomId() || !Utils.areTileIdsWithinRadius(respawnPoint.getTileId(), getTileId(), 15))
			responseMaps.addClientOnlyResponse(this, new DeathResponse(dto.getId()));
				
		setTileId(respawnPoint.getTileId());
		setRoomId(respawnPoint.getRoomId());

		state = PlayerState.dead;
		tickCounter = 2;
	}
	
	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		state = PlayerState.idle;
		setPostFightCooldown();
		int totalExp = killed.getExp();
		float points = ((float)totalExp / 5) * 4;
		
		if (killed instanceof Player) {
			MessageResponse messageResponse = new MessageResponse();
			messageResponse.setRecoAndResponseText(1, String.format("you have defeated %s!", ((Player)killed).getDto().getName()));
			messageResponse.setColour("white");
			responseMaps.addClientOnlyResponse(this, messageResponse);
		}
		
		// exp is doled out based on attackStyle and weapon type.
		// exp is split into five parts (called points) and the points are stored as follows:
		int weaponId = EquipmentDao.getWeaponIdByPlayerId(getId());
		String weaponName = ItemDao.getNameFromId(weaponId);
		if (weaponName == null) {
			// error: invalid weaponId (0 is no weapon and returns the string "null")
			return;
		}
		
		Map<Integer, Integer> expBefore = StatsDao.getAllStatExpByPlayerId(getId());
		
		// hammer/aggressive: 4str, 1hp
		// hammer/defensive: 4def, 1hp
		// hammer/shared: 2str, 2def, 1hp
		if (weaponName.contains(" hammer" )) {// TODO add weapon_type enum
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		// daggers/aggressive: 4acc, 1hp
		// daggers/defensive: 4agil, 1hp
		// daggers/shared: 2acc, 2agil, 1hp
		else if (weaponName.contains(" daggers")) {// TODO ad weapon_type enum
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		// sword/aggressive: 2str, 2acc, 1hp
		// sword/defensive: 2def, 2agil, 1hp
		// sword/shared: 1str, 1acc, 1def, 1agil, 1hp
		else {
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points);
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points);
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points);
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		Map<Integer, Integer> currentStatExp = StatsDao.getAllStatExpByPlayerId(getId());
		
		AddExpResponse response = new AddExpResponse();
		for (Map.Entry<Integer, Integer> statExp : currentStatExp.entrySet()) {
			int diff = statExp.getValue() - expBefore.get(statExp.getKey()); 
			if (diff > 0)
				response.addExp(statExp.getKey(), diff);
		}		
		responseMaps.addClientOnlyResponse(this, response);
		
		refreshStats(currentStatExp);
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(getId());
		playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(getId()));
		responseMaps.addLocalResponse(roomId, tileId, playerUpdate);
	}
	
	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		// remove any buffs that don't work in combat
		if (activeBuffs.containsKey(Buffs.RESTORATION) && type != DamageTypes.POISON)// don't kill the buff if it's due to poison
			activeBuffs.put(Buffs.RESTORATION, 1);// kill it next tick
		
		int hpLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.HITPOINTS, dto.getId());
		int hpBoost = StatsDao.getRelativeBoostsByPlayerId(dto.getId()).get(Stats.HITPOINTS);
		hpBoost -= damage;
		if (hpBoost < -hpLevel)
			hpBoost = -hpLevel;
		
		currentHp = hpLevel + hpBoost;
		
		if (type != DamageTypes.POISON) // only for a standard/magic hit, poison doesn't degrade the reinforced armour
			handleReinforcedItemDegradation(responseMaps);
		
		// you have 10 hp max, 1hp remaining
		// relative boost should be -9
		// therefore: -max + current
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.HITPOINTS, hpBoost);
		PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
		playerUpdateResponse.setId(getId());
		playerUpdateResponse.setDamage(damage);
		playerUpdateResponse.setDamageType(type.getValue());
		playerUpdateResponse.setCurrentHp(currentHp);
		responseMaps.addBroadcastResponse(playerUpdateResponse);	
		
		new StatBoostResponse().process(null, this, responseMaps);
	}
	
	private void handleReinforcedItemDegradation(ResponseMaps responseMaps) {
		boolean itemUpdated = false;
		boolean itemCleared = false;
		for (Map.Entry<Integer, Integer> entry : equippedSlotsByItemId.entrySet()) {
			int degradedItemId = EquipmentDao.getBaseItemFromReinforcedItem(entry.getKey());
			if (degradedItemId > 0) {
				InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(getId(), StorageTypes.INVENTORY.getValue(), entry.getValue());
				if (item.getCharges() > 1) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(
							getId(), 
							StorageTypes.INVENTORY.getValue(), 
							entry.getValue(), 
							entry.getKey(), 1, item.getCharges() - 1);
				} else {
					// ran out of charges; degrade to the base item
					PlayerStorageDao.setItemFromPlayerIdAndSlot(getId(), StorageTypes.INVENTORY.getValue(), entry.getValue(), degradedItemId, 1, ItemDao.getMaxCharges(degradedItemId));
					
					// clear the equipped item first then reset it with the degraded base item
					EquipmentDao.clearEquippedItem(getId(), entry.getValue());
					EquipmentDao.setEquippedItem(getId(), entry.getValue(), degradedItemId);
					itemCleared = true;
					
					// it degraded so throw up a message
					MessageResponse messageResponse = new MessageResponse();
					messageResponse.setRecoAndResponseText(0, String.format("Your %s degraded!", ItemDao.getNameFromId(item.getItemId())));
					messageResponse.setColour("white");
					responseMaps.addClientOnlyResponse(this, messageResponse);
				}
				itemUpdated = true;
			}
		}
		
		if (itemUpdated) {
			recacheEquippedItems();
			new InventoryUpdateResponse().process(RequestFactory.create("", getId()), this, responseMaps);
			if (itemCleared) {
//				PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
//				playerUpdate.setId(getId());
//				playerUpdate.setEquipAnimations(AnimationDao.getEquipmentAnimationsByPlayerId(getId()));
//				responseMaps.addLocalResponse(getRoomId(), getTileId(), playerUpdate);
				
				new EquipResponse().process(null, WorldProcessor.getPlayerById(getId()), responseMaps);
			}
		}
	}
	
	@Override
	public void setTarget(Attackable target) {
		this.target = target;
		this.state = target == null ? PlayerState.idle : PlayerState.chasing;
		if (target == null) {
			setPostFightCooldown();
		}
	}
	
	@Override
	public void setStatsAndBonuses() {
		refreshStats();
		refreshBonuses();
	}
	
	@Override
	public int getExp() {
		return StatsDao.getCombatLevelByPlayerId(getId());
	}
	
	@Override
	public boolean isInCombat() {
		return super.isInCombat() || postFightCooldown > 0;
	}
	
	public void setPostFightCooldown() {
		postFightCooldown = 5;
	}
	
	public void recacheEquippedItems() {
		equippedSlotsByItemId = EquipmentDao.getEquippedSlotsAndItemIdsByPlayerId(this.getId());
	}
	
	public void clearPath() {
		path.clear();
	}
}
