package processing.attackable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.websocket.Session;

import database.dao.CastableDao;
import database.dao.EquipmentDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.PlayerStorageDao;
import database.dao.PrayerDao;
import database.dao.ReinforcementBonusesDao;
import database.dao.StatsDao;
import database.dao.TeleportableDao;
import database.dto.CastableDto;
import database.dto.EquipmentBonusDto;
import database.dto.InventoryItemDto;
import database.dto.NpcDialogueDto;
import database.dto.PlayerDto;
import database.dto.ReinforcementBonusesDto;
import database.dto.TeleportableDto;
import database.entity.update.UpdatePlayerEntity;
import lombok.Getter;
import lombok.Setter;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.managers.DatabaseUpdater;
import processing.managers.FightManager;
import processing.managers.FightManager.Fight;
import processing.managers.LocationManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.KillNpcTaskUpdate;
import requests.ConstructionRequest;
import requests.FishRequest;
import requests.MineRequest;
import requests.Request;
import requests.RequestFactory;
import requests.SmithRequest;
import responses.AddExpResponse;
import responses.ChopResponse;
import responses.ConstructionResponse;
import responses.DaylightResponse;
import responses.DeathResponse;
import responses.EquipResponse;
import responses.FinishAssembleResponse;
import responses.FinishChopResponse;
import responses.FinishConstructionResponse;
import responses.FinishCookingResponse;
import responses.FinishFishingResponse;
import responses.FinishMiningResponse;
import responses.FinishSawmillResponse;
import responses.FinishSmeltResponse;
import responses.FinishSmithResponse;
import responses.FinishThrowResponse;
import responses.FinishUseResponse;
import responses.FishResponse;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.MineResponse;
import responses.PlayerUpdateResponse;
import responses.Response;
import responses.ResponseFactory;
import responses.ResponseMaps;
import responses.SmithResponse;
import responses.StatBoostResponse;
import responses.TogglePrayerResponse;
import responses.UseResponse;
import system.GroundItemManager;
import types.Buffs;
import types.DamageTypes;
import types.DuelRules;
import types.EquipmentTypes;
import types.ItemAttributes;
import types.Items;
import types.NpcAttributes;
import types.Prayers;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

public class Player extends Attackable {
	public enum PlayerState {
		idle,
		walking,
		chasing,// used for walking to a moving target (following, moving to attack something etc)
		chasing_with_range, // if the player is behind a wall and they cast a spell, they'll run around in preparation of casting from range
		following,
		mining,
		smithing,
		smelting,
		woodcutting,
		sawmill,
		sawmill_knife,
		construction,
		assembling,
		fighting,
		using,
		climbing, // ladders etc, give a tick or so duration (like for climbing animation in the future)
		cooking,
		fishing,
		growing_zombie,
		dead
	};
	
	@Getter private PlayerDto dto;
	@Getter private Session session;
	@Getter private PlayerState state = PlayerState.idle;
	@Setter private Request savedRequest = null;
	@Setter private int tickCounter = 0;
	@Setter @Getter private NpcDialogueDto currentDialogue = null;
	@Setter @Getter private int shopId;// if the player is currently in a shop, this is the id
	private int postFightCooldown = 0;
	private Map<Integer, Integer> equippedSlotsByItemId = new HashMap<>();// itemId, slot
	private HashMap<Buffs, Integer> activeBuffs = new HashMap<>(); // buff, remaining ticks
	@Getter @Setter private Set<Integer> inRangeNpcs = new HashSet<>();
	@Getter @Setter private Set<Integer> inRangePlayers = new HashSet<>();
	@Getter @Setter private Set<Integer> inRangeConstructables = new HashSet<>();
	@Getter @Setter private Map<Integer, List<Integer>> inRangeGroundItems = new HashMap<>();
	@Getter @Setter private Set<Integer> localTiles = new HashSet<>();
	@Getter @Setter private int loadedFloor = 0;
	@Getter @Setter private Set<Integer> loadedMinimapSegments = new HashSet<>();
	@Getter private Set<Integer> activePrayers = new HashSet<>();
	@Getter private float prayerPoints = 0;
	@Setter private int range = 0; // if we're chasing with range (i.e. chasing to cast a spell or something), this is the range we want to be in
	private boolean slowburnBlockedStatDrain = false;
	private int tybaltsCapeTimer = 0; // tybalts cape gives 1hp per minute of wearing it (essentially doubling hp regen)
	
	// these two are used while the player is doing their death animation.
	// technically they are at the respawn point, and if they disconnect during the death animation
	// then that's where they'll log in.  However during the death animation we still want the client
	// to receive messages at the point of their death (to continue processing npcs, scenery etc until they respawn).
	private int preDeathFloor = 0;
	private int preDeathTileId = 0;
	
	private final int MAX_STAT_RESTORE_TICKS = 100;// 100 ticks == one minute
	private int statRestoreTicks = MAX_STAT_RESTORE_TICKS;
	
	private final int restorationBuffTriggerTicks = 3;
	private int restorationBuffCurrentTriggerTicks = restorationBuffTriggerTicks;
	
	public Player(PlayerDto dto, Session session) {
		this.session = session;
		
		if (dto != null) {
			this.dto = dto;
			tileId = dto.getTileId();
			floor = dto.getFloor();
			
			refreshStats();
			refreshBoosts();
			
			prayerPoints = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, dto.getId()) + StatsDao.getRelativeBoostsByPlayerId(dto.getId()).get(Stats.PRAYER);
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
	
	public void refreshStats(Map<Integer, Double> statExp) {
		for (Map.Entry<Integer, Double> stat : statExp.entrySet())
			stats.put(Stats.withValue(stat.getKey()), StatsDao.getLevelFromExp(stat.getValue().intValue()));
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
		bonuses.put(Stats.PRAYER, equipment.getPray());
		bonuses.put(Stats.HITPOINTS, equipment.getHp());
		bonuses.put(Stats.MAGIC, equipment.getMage());
	}
	
	public void process(int tick, ResponseMaps responseMaps) {
		// called each tick; build a response where necessary
		if (postFightCooldown > 0)
			--postFightCooldown;
		
		if (--statRestoreTicks <= 0) {
			statRestoreTicks = MAX_STAT_RESTORE_TICKS;
			restoreStats(responseMaps);
		}
		
		processPoison(responseMaps);
		
		if (prayerPoints > 0)
			processPrayer(responseMaps);
		
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
		
		if (EquipmentDao.isItemEquipped(getId(), 359)) { // tybalt's cape
			if (++tybaltsCapeTimer >= 100) { // once-per-minute hp regen; effectively doubling hp regen (still stackable with prayers etc)
				tybaltsCapeTimer = 0;
				incrementHitpoints(responseMaps);
			}
		} else {
			if (tybaltsCapeTimer > 0) // to stop the ability to flick the cape, the timer resets to 0 if the cape is unequipped
				tybaltsCapeTimer = 0;
		}
		
		switch (state) {
		case walking: {
			// popPath returns true if we've successfully moved to the next tile; false if a door is in the way or the path is empty
			if (popPath()) {
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdateResponse);
			} else if (state != PlayerState.idle){
				state = PlayerState.idle;
				
				// we do this because some requests set the saved request, but some don't.
				// we want to clear the saved request beforehand so we don't overwrite it if it sets it,
				// but we still want to pass it into the response's process.
				Request requestToUse = savedRequest;
				savedRequest = null;
				if (requestToUse != null) {
					Response response = ResponseFactory.create(requestToUse.getAction());
					response.process(requestToUse, this, responseMaps);
				}
			}
			
			break;
		}
		case following: {
			if (popPath()) {
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdateResponse);
			}
			
			if (floor != target.getFloor())// if the target goes down a ladder or something then stop following
				target = null;

			// maybe the target player logged out
			if (target == null) {
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(floor, tileId, target.getTileId())) {
				path = PathFinder.findPath(floor, tileId, target.getTileId(), false);
			}
			break;
		}
		case chasing: {
			if (target != null && floor != target.getFloor())// if the target goes down a ladder or something then stop following
				target = null;
			
			if (target == null) {
				// player could have logged out/died as they were chasing
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(floor, tileId, target.getTileId())) {
				path = PathFinder.findPath(floor, tileId, target.getTileId(), false);
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
			if (popPath()) {
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdateResponse);
			}

			break;
		}
		case chasing_with_range: {
			if (target != null && floor != target.getFloor())// if the target goes down a ladder or something then stop following
				target = null;
			
			if (target == null) {
				// player could have logged out/died as they were chasing
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.lineOfSightIsClear(floor, tileId, target.getTileId(), range)) {
				path = PathFinder.findPathInRange(floor, tileId, target.getTileId(), range);
			} else {
				// start the fight
				if (savedRequest != null) {
					Request req = savedRequest;
					savedRequest = null;
					
					Response response = ResponseFactory.create(req.getAction());
					response.process(req, this, responseMaps);
				}
				path.clear();
			}
			
			// similar to walking, but need to recalculate path each tick due to moving target
			if (popPath()) {
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTileId(getTileId());
				responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdateResponse);
			}
			break;
		}
		case mining:
			// waiting until the tick counter hits zero, then do the actual mining and create a finish_mining response
			if (--tickCounter <= 0) {
				if (!(savedRequest instanceof MineRequest)) {
					savedRequest = null;
					state = PlayerState.idle;
					return;
				}
				
				new FinishMiningResponse().process(savedRequest, this, responseMaps);
				
				// let's go for another mine!
				new MineResponse().process(savedRequest, this, responseMaps);
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
				
				// fish again
				new FishResponse().process(savedRequest, this, responseMaps);
			}
			break;
		case smithing:
			if (--tickCounter <= 0) {
				new FinishSmithResponse().process(savedRequest, this, responseMaps);
				
				SmithRequest smithReq = (SmithRequest)savedRequest;
				smithReq.setAmount(smithReq.getAmount() - 1);
				if (smithReq.getAmount() > 0) {
					new SmithResponse().process(smithReq, this, responseMaps);
				} else {
					setState(PlayerState.idle); // finished all the actions
				}
			}
			break;
			
		case smelting:
			if (--tickCounter <= 0) {
				new FinishSmeltResponse().process(savedRequest, this, responseMaps);
				new UseResponse().process(savedRequest, this, responseMaps);
			}
			break;
			
		case sawmill:
			if (--tickCounter <= 0) {
				new FinishSawmillResponse(false).process(savedRequest, this, responseMaps);
				new UseResponse().process(savedRequest, this, responseMaps);
			}
			break;
		case sawmill_knife:
			if (--tickCounter <= 0) {
				new FinishSawmillResponse(true).process(savedRequest, this, responseMaps);
				new UseResponse().process(savedRequest, this, responseMaps);
			}
			break;
			
		case construction:
			if (--tickCounter <= 0) {
				new FinishConstructionResponse().process(savedRequest, this, responseMaps);
				if (state == PlayerState.idle) // if we're flatpacking and the workbench disappears then the player state gets set to idle.
					break;
				
				// special case here: if we're making flatpacks we can chain multiple requests.
				ConstructionRequest constructionReq = (ConstructionRequest)savedRequest;
				if (constructionReq.isFlatpack()) {
					constructionReq.setAmount(constructionReq.getAmount() - 1);
					if (constructionReq.getAmount() > 0) {
						new ConstructionResponse().process(constructionReq, this, responseMaps);
					} else {
						setState(PlayerState.idle);
					}
				} else {
					savedRequest = null;
					setState(PlayerState.idle);
				}
			}
			break;
			
		case assembling:
			if (--tickCounter <= 0) {
				new FinishAssembleResponse().process(savedRequest, this, responseMaps);
				savedRequest = null;
				setState(PlayerState.idle);
			}
			break;
			
		case using:
			if (--tickCounter <= 0) {
				new FinishUseResponse().process(savedRequest, this, responseMaps);
				if (state == PlayerState.using)
					new UseResponse().process(savedRequest, this, responseMaps);
			}
			break;
			
		case cooking:
			if (--tickCounter <= 0) {
				new FinishCookingResponse().process(savedRequest, this, responseMaps);
				
				// finishCooking response can set the state back to idle if the fire runs out during the cook.
				// in this case, don't try and do another UseResponse as it'll result in an annoying "nothing interesting happens".
				if (state == PlayerState.cooking)
					new UseResponse().process(savedRequest, this, responseMaps);
			}
			break;
			
		case woodcutting:
			if (--tickCounter <= 0) {
				new FinishChopResponse().process(savedRequest, this, responseMaps);
				new ChopResponse().process(savedRequest, this, responseMaps);
			}
			break;
			
		case growing_zombie:
			if (--tickCounter <= 0) {
				new FinishThrowResponse().process(savedRequest, this, responseMaps);
				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
			
		case dead: {// note that while the player is in the dead state, every response (except Message) is ignored at the Response superclass level
			if (--tickCounter <= 0) {
				state = PlayerState.idle;
				
				// update the player inventory to show there's no more items
				new InventoryUpdateResponse().process(RequestFactory.create("dummy", getId()), this, responseMaps);
				
				PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
				playerUpdate.setId(getId());
				playerUpdate.setTileId(getTileId());
				playerUpdate.setSnapToTile(true);
				playerUpdate.setCurrentHp(currentHp);
				playerUpdate.setMaxHp(currentHp);
				playerUpdate.setCurrentPrayer((int)prayerPoints);
				playerUpdate.setRespawn(true);
				
				TogglePrayerResponse togglePrayerResponse = new TogglePrayerResponse();
				togglePrayerResponse.setActivePrayers(activePrayers);
				responseMaps.addClientOnlyResponse(this, togglePrayerResponse);
				
				// the reason this is local is so the other players know the player respawned
				// (they also receive the player_in_range response automatically but if they're around the spawn point it will be missing respawn data)
				responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdate);
				
				// mainly to reset the bonuses
				new EquipResponse().process(null, this, responseMaps);
			}
			break;
		}
		case idle:// fall through
		default:
			break;
		}
	}
	
	private void processPrayer(ResponseMaps responseMaps) {
		float prayerBonus = 1 - ((float)Math.min(49, bonuses.get(Stats.PRAYER)) / 50); // each prayer point is 2% pray reduction
		float currentPrayerPoints = prayerPoints;
		for (int prayerId : activePrayers) {
			currentPrayerPoints -= PrayerDao.getPrayerById(prayerId).getDrainRate() * prayerBonus;
		}
		
		if ((int)currentPrayerPoints < (int)prayerPoints)
			setPrayerPoints(currentPrayerPoints, responseMaps);
		else
			prayerPoints = currentPrayerPoints;

		if (prayerPoints <= 0) {
			activePrayers.clear();
			prayerPoints = 0;
			
			TogglePrayerResponse togglePrayerResponse = new TogglePrayerResponse();
			togglePrayerResponse.setActivePrayers(activePrayers);
			togglePrayerResponse.setResponseText("you have run out of prayer points.");
			responseMaps.addClientOnlyResponse(this, togglePrayerResponse);
		}
	}
	
	public void incrementPrayerPoints(ResponseMaps responseMaps) {
		if (prayerPoints < StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, getId()))
			setPrayerPoints(prayerPoints + 1, responseMaps);
	}
	
	public void incrementHitpoints(ResponseMaps responseMaps) {
		HashMap<Stats, Integer> relativeBoosts = StatsDao.getRelativeBoostsByPlayerId(getId());
		
		int newRelativeBoost = relativeBoosts.get(Stats.HITPOINTS) + 1;
		if (newRelativeBoost > getBonuses().get(Stats.HITPOINTS))// the hp relative boost is negative for how many hp lost
			newRelativeBoost = getBonuses().get(Stats.HITPOINTS);
		setCurrentHp(getDto().getMaxHp() + newRelativeBoost);
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.HITPOINTS, newRelativeBoost);
		
		PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
		playerUpdateResponse.setId(getId());
		playerUpdateResponse.setCurrentHp(getCurrentHp());
		responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdateResponse);
	}
	
	private void restoreStats(ResponseMaps responseMaps) {
		HashMap<Stats, Integer> boosts = StatsDao.getRelativeBoostsByPlayerId(getId());
		
		for (Map.Entry<Stats, Integer> entry : boosts.entrySet()) {
			if (entry.getKey().equals(Stats.PRAYER))
				continue;// prayer doesn't regenerate
			
			// max hitpoints depends on the hitpoints bonus (i.e. +5 means we can heal +5 over max hitpoints)
			int maxBoost = entry.getKey().equals(Stats.HITPOINTS) ? getBonuses().get(Stats.HITPOINTS) : 0;
						
			int relativeBoost = entry.getValue();
			if (relativeBoost > maxBoost) {
				if (prayerIsActive(Prayers.SLOW_BURN))
					slowburnBlockedStatDrain = !slowburnBlockedStatDrain;
				else
					slowburnBlockedStatDrain = false;
				
				if (!slowburnBlockedStatDrain)
					--relativeBoost;
			}
			else if (relativeBoost < maxBoost) {
				++relativeBoost;
				
				if (relativeBoost < maxBoost 
						&& ((entry.getKey().equals(Stats.HITPOINTS) && prayerIsActive(Prayers.RAPID_HEAL)) 
						|| (!entry.getKey().equals(Stats.HITPOINTS) && prayerIsActive(Prayers.RAPID_RESTORE))))
					++relativeBoost;
			}
			else
				continue;// we're at our default level, don't update
			
			StatsDao.setRelativeBoostByPlayerIdStatId(getId(), entry.getKey(), relativeBoost);
		}
		
		refreshBoosts();
		
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
			else
				continue;
			
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
		LocationManager.addPlayer(this);
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(dto.getId()).tileId(tileId).build());
	}
	
	public void setFloor(int floor, ResponseMaps responseMaps) {
		// if we go underground, or come up from underground, reset the brightness accordingly.
		// if it's nighttime then nothing changes if we go above or below ground
		if (WorldProcessor.isDaytime()) {
			if (this.floor >= 0 && floor < 0) {
				// we were above ground, now below ground
				responseMaps.addClientOnlyResponse(this, new DaylightResponse(false, true));
			} else if (this.floor < 0 && floor >= 0) {
				// we were below ground, now above ground
				responseMaps.addClientOnlyResponse(this, new DaylightResponse(true, true));
			}
		}
		
		this.floor = floor;
		LocationManager.addPlayer(this);
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(dto.getId()).floor(floor).build());
	}
	
	public int getTileId() {
		return state == PlayerState.dead ? preDeathTileId : tileId;
	}
	
	public int getFloor() {
		return state == PlayerState.dead ? preDeathFloor : floor;
	}
	
	public boolean isGod() {
		return dto.getId() == 3;// god id
	}
	
	public int getId() {
		return dto.getId();
	}
	
	@Override
	public void onDeath(Attackable killer, ResponseMaps responseMaps) {
		// so the client continues to receive messages and process correctly during their death animation
		preDeathTileId = tileId;
		preDeathFloor = floor;
		
		// respawn at the tyrotown teleport spot
		TeleportableDto respawnPoint = TeleportableDao.getTeleportableByItemId(Items.TYROTOWN_TELEPORT_RUNE.getValue());
		
		setTileId(respawnPoint.getTileId());
		setFloor(respawnPoint.getFloor());
		
		state = PlayerState.dead;
		tickCounter = 2;
		
		clearPoison();
		
		Fight fight = FightManager.getFightByPlayerId(getId()); // fight doesn't necessarily exist - the player could die of poison outside a fight for example.
		Integer duelRules = fight == null ? null : fight.getRules();
		if (duelRules == null || (duelRules & DuelRules.dangerous.getValue()) > 0) {
			// not a duel, or a dangerous duel
			// unequip and drop all the items in inventory
			EquipmentDao.clearAllEquppedItems(getId());
			
			int itemsToProtect = 3; // TODO 0 if skulled
			if (prayerIsActive(Prayers.PROTECT_SLOT))
				itemsToProtect += 1;
			else if (prayerIsActive(Prayers.PROTECT_SLOT_LVL_2))
				itemsToProtect += 2;
			
			Map<Integer, InventoryItemDto> inventoryList = PlayerStorageDao.getStorageDtoMapByPlayerId(getId(), StorageTypes.INVENTORY);
			for (InventoryItemDto dto : inventoryList.values()) {
				if (dto.getSlot() < itemsToProtect)
					continue;// you always protect your items in the first three slots (0, 1, 2)
				
				if (dto.getItemId() != 0) {
					int stack = ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.STACKABLE) ? dto.getCount() : 1;
					int charges = ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.CHARGED) ? dto.getCharges() : 1;
					
					if (killer instanceof Player && ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.TRADEABLE)) {
						// if it's a tradeable unique, the killer should only see it if they don't already have one.
						if (ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.UNIQUE)) {
							if (PlayerStorageDao.itemExistsInPlayerStorage(((Player)killer).getId(), dto.getItemId()) || GroundItemManager.itemIsOnGround(((Player)killer).getId(), dto.getItemId())) {
								continue;
							}
						}
						
						GroundItemManager.add(getFloor(), ((Player)killer).getId(), dto.getItemId(), getTileId(), stack, charges);
					} else {
						// for now, untradeables will drop on the ground for the owner to pick back up
						GroundItemManager.add(getFloor(), getId(), dto.getItemId(), getTileId(), stack, charges);
					}
				}
			}
			PlayerStorageDao.clearPlayerInventoryExceptFirstSlots(getId(), itemsToProtect);
		} 

		if (duelRules != null) { // not an "else" here because we still need to handle the dangerous duel stake stuff (which is irrelevant to do but still possible)
			// safe/dangerous duel: pull stuff from own trade (staked stuff) and throw it on killer's ground
			// no need to do the tradeable checks etc; this is all checked pre-duel when the duel offer is being done
			List<InventoryItemDto> itemsToDrop = PlayerStorageDao.getStorageDtoMapByPlayerId(getId(), StorageTypes.TRADE).values().stream()
					.filter(e -> e.getItemId() != 0)
					.collect(Collectors.toList());
			
			for (InventoryItemDto itemToDrop : itemsToDrop) {
				GroundItemManager.add(getFloor(), ((Player)killer).getId(), itemToDrop.getItemId(), getTileId(), itemToDrop.getCount(), itemToDrop.getCharges());
			}
			
			PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(getId(), StorageTypes.TRADE);
		}
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.HITPOINTS, 0);
		currentHp = StatsDao.getStatLevelByStatIdPlayerId(Stats.HITPOINTS, dto.getId());
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.PRAYER, 0);
		prayerPoints = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, dto.getId());
		
		activePrayers.clear();
		
		// let everyone around the dead player know they died 
		responseMaps.addLocalResponse(getFloor(), getTileId(), new DeathResponse(dto.getId()));
		
		
		
		// if the player's spawn point is not local to where they died, they won't receive the local death response.
		// it's not ideal to send an additional response but we have to set the respawn point immediately
		// otherwise the player can just disconnect during the death sequence then relogin in the same spot they died (with full hp)
//		if (respawnPoint.getFloor() != getFloor() || !Utils.areTileIdsWithinRadius(respawnPoint.getTileId(), getTileId(), 15))
//			responseMaps.addClientOnlyResponse(this, new DeathResponse(dto.getId()));

		lastTarget = null;
		if (FightManager.fightWithFighterExists(this)) {
			FightManager.cancelFight(this, responseMaps); // this unsets target which changes state to idle
			state = PlayerState.dead;
		}
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
			
			Fight fight = FightManager.getFightByPlayerId(getId()); // fight doesn't necessarily exist - the player could die of poison outside a fight for example.
			Integer duelRules = fight == null ? null : fight.getRules();
			if (duelRules != null) {
				// they won this duel (clearly); put their stake items back into their inventory
				Map<Integer, InventoryItemDto> stakedItems = PlayerStorageDao.getStorageDtoMapByPlayerId(getId(), StorageTypes.TRADE);
				for (InventoryItemDto item : stakedItems.values())
					PlayerStorageDao.addItemToFirstFreeSlot(getId(), StorageTypes.INVENTORY, item.getItemId(), item.getCount(), item.getCharges());
				PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(getId(), StorageTypes.TRADE);
				
				InventoryUpdateResponse.sendUpdate(this, responseMaps);
			}
		} else {
			TybaltsTaskManager.check(this, new KillNpcTaskUpdate(((NPC)killed).getId(), killed.getFloor(), killed.getTileId()), responseMaps);
		}
		
		// exp is doled out based on attackStyle and weapon type.
		// exp is split into five parts (called points) and the points are stored as follows:
		int weaponId = EquipmentDao.getWeaponIdByPlayerId(getId());
		EquipmentTypes weaponType = EquipmentDao.getEquipmentTypeByEquipmentId(weaponId);
		
		String weaponName = ItemDao.getNameFromId(weaponId);
		if (weaponName == null) {
			// error: invalid weaponId (0 is no weapon and returns the string "null")
			return;
		}
		
		Map<Integer, Double> expBefore = StatsDao.getAllStatExpByPlayerId(getId());
		
		// hammer/aggressive: 4str, 1hp
		// hammer/defensive: 4def, 1hp
		// hammer/shared: 2str, 2def, 1hp
		if (weaponType == EquipmentTypes.HAMMER) {
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
		else if (weaponType == EquipmentTypes.DAGGERS) {
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		// sword/aggressive: 2str, 2acc, 1hp
		// sword/defensive: 2def, 1hp
		// sword/shared: 1str, 1acc, 1def, 1agil, 1hp
		else {
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points);
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points);
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points * 2);
					break;
			}
		}
		
		Map<Integer, Double> currentStatExp = StatsDao.getAllStatExpByPlayerId(getId());
		
		AddExpResponse response = new AddExpResponse();
		for (Map.Entry<Integer, Double> statExp : currentStatExp.entrySet()) {
			double diff = statExp.getValue() - expBefore.get(statExp.getKey()); 
			if (diff > 0)
				response.addExp(statExp.getKey(), diff);
		}		
		responseMaps.addClientOnlyResponse(this, response);
		
		refreshStats(currentStatExp);
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(getId());
		playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(getId()));
		responseMaps.addLocalResponse(floor, tileId, playerUpdate);
	}
	
	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		// remove any buffs that don't work in combat
		if (activeBuffs.containsKey(Buffs.RESTORATION) && type != DamageTypes.POISON)// don't kill the buff if it's due to poison
			activeBuffs.put(Buffs.RESTORATION, 1);// kill it next tick
		
		if (type == DamageTypes.POISON && prayerIsActive(Prayers.FAITH_HEALING)) {
			int damageToPrayer = (int)prayerPoints - damage;
			if (damageToPrayer > 0) {
				// pull all the damage out of prayer and don't hit anything
				setPrayerPoints(prayerPoints - damage, responseMaps);
				damage = 1;
			} else {
				// drain the remainder of the prayer points, remainder of the damage still gets hit
				setPrayerPoints(0, responseMaps);
				damage += damageToPrayer; // damageToPrayer is negative
			}
		}
		
		if (!isImmuneToPoison() && damage > 0) {
			Fight fight = FightManager.getFightByPlayerId(getId());
			if (fight != null) {
				Attackable opponent = fight.getFighter1() == this ? fight.getFighter2() : fight.getFighter1();
				if ((opponent instanceof NPC)) {				
					if (NPCDao.npcHasAttribute(((NPC)opponent).getId(), NpcAttributes.VENOMOUS)) {
						if (RandomUtil.chance(20)) {
							inflictPoison(4);
							responseMaps.addClientOnlyResponse(this, MessageResponse.newMessageResponse("you have been poisoned!", "#00aa00"));
						}
					}
				}
			}
		}
		
		int hpLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.HITPOINTS, dto.getId());
		int hpBoost = StatsDao.getRelativeBoostsByPlayerId(dto.getId()).get(Stats.HITPOINTS);
		
		if (type != DamageTypes.POISON) // only for a standard/magic hit, poison doesn't degrade the reinforced armour
			damage = handleReinforcedItemDegradation(damage, responseMaps);
		
		hpBoost -= damage;
		if (hpBoost < -hpLevel)
			hpBoost = -hpLevel;
		
		currentHp = hpLevel + hpBoost;
		
		// you have 10 hp max, 1hp remaining
		// relative boost should be -9
		// therefore: -max + current
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.HITPOINTS, hpBoost);
		PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
		playerUpdateResponse.setId(getId());
		playerUpdateResponse.setDamage(damage, type);
		playerUpdateResponse.setCurrentHp(currentHp);
		responseMaps.addBroadcastResponse(playerUpdateResponse);	
		
		new StatBoostResponse().process(null, this, responseMaps);
	}
	
	@Override
	public void onAttack(int damage, DamageTypes type, ResponseMaps responseMaps) {
		PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
		playerUpdateResponse.setId(getId());
		playerUpdateResponse.setDoAttack(true);
		responseMaps.addLocalResponse(getFloor(), getTileId(), playerUpdateResponse);	
	}
	
	private int handleReinforcedItemDegradation(int damage, ResponseMaps responseMaps) {
		// reinforced armour has a chance each hit to soak part of the damage.
		// the damage that it soaks will come out of its charges, and once the
		// charges run out then the armour reverts to its base form.
		// chance to soak part of the damage (see reinforcement_bonuses table):
		// helmets: 5%
		// platelegs: 10%
		// platebody: 15%
		// shield: 20%
		// each piece is chosen randomly to soak, and multiple pieces can be chosen on the same hit.
		// amount the armour will soak (in pct, rounded UP to nearest int):
		// copper: 1, 2, 3, 4
		// iron: 2, 3, 4, 5
		// steel: 3, 4, 5, 6
		// mithril: 5, 7, 9, 11
		// addy, 7, 9, 11, 13
		// rune: 10, 13, 15, 18
		// dragon: doesn't soak as it cannot be reinforced
		
		Map<Integer, Integer> equippedSlotsCopy = equippedSlotsByItemId.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

		boolean itemUpdated = false;
		boolean itemCleared = false;
		for (Map.Entry<Integer, Integer> entry : equippedSlotsCopy.entrySet()) {
			ReinforcementBonusesDto reinforcementBonuses = ReinforcementBonusesDao.getReinforcementBonusesById(entry.getKey());
			if (reinforcementBonuses == null)
				continue;
			
			if (damage > 0) {
				if (RandomUtil.getRandom(0, 100) < reinforcementBonuses.getProcChance()) {
					// proc chance activated, soak some of the damage	
					int damageToSoak = (int)Math.ceil(damage * reinforcementBonuses.getSoakPct());
					if (damageToSoak > damage)
						damageToSoak = damage;// can only block the remainder of the damage
					InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(getId(), StorageTypes.INVENTORY, entry.getValue());
					if (damageToSoak > item.getCharges())
						damageToSoak = item.getCharges();
					damage -= damageToSoak;
					
					if (item.getCharges() > damageToSoak) {
						PlayerStorageDao.setItemFromPlayerIdAndSlot(
								getId(), 
								StorageTypes.INVENTORY, 
								entry.getValue(), 
								entry.getKey(), 1, item.getCharges() - damageToSoak);
					} else {
						final int degradedItemId = ItemDao.getDegradedItemId(entry.getKey());
						
						// ran out of charges; degrade to the base item
						PlayerStorageDao.setItemFromPlayerIdAndSlot(getId(), StorageTypes.INVENTORY, entry.getValue(), degradedItemId, 1, ItemDao.getMaxCharges(degradedItemId));
						
						// clear the equipped item first then reset it with the degraded base item
						EquipmentDao.clearEquippedItem(getId(), entry.getValue());
						
						if (EquipmentDao.isEquippable(degradedItemId))
							EquipmentDao.setEquippedItem(getId(), entry.getValue(), degradedItemId);
						itemCleared = true;
						
						// it degraded so throw up a message
						responseMaps.addClientOnlyResponse(this, 
								MessageResponse.newMessageResponse(String.format("Your %s degraded!", ItemDao.getNameFromId(item.getItemId())), "white"));
					}
					itemUpdated = true;
				}
			}
		}
		
		if (itemUpdated) {
			recacheEquippedItems();
			new InventoryUpdateResponse().process(RequestFactory.create("", getId()), this, responseMaps);
			if (itemCleared) {
				new EquipResponse().process(null, WorldProcessor.getPlayerById(getId()), responseMaps);
			}
		}
		
		return damage;
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
	
	public boolean prayerIsActive(Prayers prayerId) {
		return activePrayers.contains(prayerId.getValue());
	}
	
	public void togglePrayer(Prayers prayer) {
		if (activePrayers.contains(prayer.getValue()))
			activePrayers.remove(prayer.getValue());
		else
			activePrayers.add(prayer.getValue());
	}
	
	public void clearActivePrayers(ResponseMaps responseMaps) {
		if (activePrayers.isEmpty())
			return;// no need to send a response if there are already no prayers being used
		
		activePrayers.clear();
		
		TogglePrayerResponse prayerResponse = new TogglePrayerResponse();
		prayerResponse.setActivePrayers(activePrayers);
		responseMaps.addClientOnlyResponse(this, prayerResponse);
	}
	
	public void removeCombatBoosts(ResponseMaps responseMaps) {
		HashMap<Stats, Integer> boosts = StatsDao.getRelativeBoostsByPlayerId(getId());
		
		for (Map.Entry<Stats, Integer> entry : boosts.entrySet()) {
			switch (entry.getKey()) {
			case STRENGTH:
			case ACCURACY:
			case DEFENCE:
			case PRAYER:
			case MAGIC:
			case HITPOINTS:
				if (entry.getValue() > 0) {
					StatsDao.setRelativeBoostByPlayerIdStatId(getId(), entry.getKey(), 0);
				}
				break;
			default:
			}
		}
		
		refreshBoosts();
	
		new StatBoostResponse().process(null, this, responseMaps);
	}
	
	@Override
	protected int postDamageModifications(int damageBonus) {
		if (getDto().getAttackStyleId() == 1)
			damageBonus += 10;
		
//		EquipmentDao.getWeaponIdByPlayerId(getId())
		
		if (prayerIsActive(Prayers.BURST_OF_STRENGTH)) {
			float newBonus = (float)damageBonus * 1.05f;
			damageBonus = (int)Math.ceil(newBonus);
		} else if (prayerIsActive(Prayers.SUPERIOR_STRENGTH)) {
			float newBonus = (float)damageBonus * 1.1f;
			damageBonus = (int)Math.ceil(newBonus);
		} else if (prayerIsActive(Prayers.ULTIMATE_STRENGTH)) {
			float newBonus = (float)damageBonus * 1.15f;
			damageBonus = (int)Math.ceil(newBonus);
		}
		return damageBonus;
	}
	
	@Override
	protected int postAccuracyModifications(int accuracy) {
		if (getDto().getAttackStyleId() == 1)
			accuracy += 10;
		
		if (prayerIsActive(Prayers.CALM_MIND)) {
			float newBonus = (float)accuracy * 1.05f;
			accuracy = (int)Math.ceil(newBonus);
		} else if (prayerIsActive(Prayers.FOCUSSED_MIND)) {
			float newBonus = (float)accuracy * 1.1f;
			accuracy = (int)Math.ceil(newBonus);
		} else if (prayerIsActive(Prayers.ZEN_MIND)) {
			float newBonus = (float)accuracy * 1.15f;
			accuracy = (int)Math.ceil(newBonus);
		}
		return accuracy;
	}
	
	@Override
	protected int postBlockChanceModifications(int defenceBonus) {
		if (getDto().getAttackStyleId() == 1)
			defenceBonus += 10;
		
		if (prayerIsActive(Prayers.THICK_SKIN)) {
			float newBonus = (float)defenceBonus * 1.05f;
			defenceBonus = (int)Math.ceil(newBonus);
		} else if (prayerIsActive(Prayers.STONE_SKIN)) {
			float newBonus = (float)defenceBonus * 1.1f;
			defenceBonus = (int)Math.ceil(newBonus);
		} else if (prayerIsActive(Prayers.STEEL_SKIN)) {
			float newBonus = (float)defenceBonus * 1.15f;
			defenceBonus = (int)Math.ceil(newBonus);
		}
		return defenceBonus;
	}
	
	@Override
	public DamageTypes getDamageType() {
		switch (Items.withValue(EquipmentDao.getWeaponIdByPlayerId(getId()))) {
		case WHIRLWIND_WAND:
			return DamageTypes.MAGIC;
			
		default:
			return DamageTypes.STANDARD;
		}
	}
	
	@Override
	public int hit(Attackable target, ResponseMaps responseMaps) {
		switch (getDamageType()) {
		case STANDARD:
			return super.hit(target, responseMaps);
			
		case MAGIC: {
			CastableDto castable = CastableDao.getCastableByItemId(Items.WHIRLWIND_RUNE.getValue());
			return castSpell(castable, target, responseMaps);
		}
		
		default:
			return 0;
		}
	}
	
	public int castSpell(CastableDto castable, Attackable target, ResponseMaps responseMaps) {
		int playerMagicLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.MAGIC, getId());
		if (playerMagicLevel < castable.getLevel()) {
			MessageResponse response = new MessageResponse();
			response.setColour("white");
			response.setRecoAndResponseText(0, String.format("you need %d magic to cast that.", castable.getLevel()));
			responseMaps.addClientOnlyResponse(this, response);
			return 0;
		}
		return 0;
	}
	
	public void setPrayerPoints(float newPrayerPoints, ResponseMaps responseMaps) {
		prayerPoints = Math.max(0, newPrayerPoints);
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.PRAYER, -StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, getId()) + (int)prayerPoints);
		
		PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
		playerUpdateResponse.setId(getId());
		playerUpdateResponse.setCurrentPrayer((int)prayerPoints);
		responseMaps.addClientOnlyResponse(this, playerUpdateResponse);
	}
	
	public void faceDirection(int neighbouringTileId, ResponseMaps responseMaps) {
		final String direction = PathFinder.getDirection(tileId, neighbouringTileId);
		if (!direction.isEmpty()) {
			PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
			playerUpdateResponse.setId(getId());
			playerUpdateResponse.setFaceDirection(direction);
			responseMaps.addLocalResponse(floor, tileId, playerUpdateResponse);
		}
	}
}
