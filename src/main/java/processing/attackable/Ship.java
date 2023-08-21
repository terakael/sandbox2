package processing.attackable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.dao.ConstructableDao;
import database.dao.ItemDao;
import database.dao.ShipAccessoryDao;
import database.dto.InventoryItemDto;
import database.dto.ShipAccessoryDto;
import database.dto.ShipDto;
import database.entity.update.UpdatePlayerEntity;
import lombok.Getter;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.attackable.Player.PlayerState;
import processing.managers.ClientResourceManager;
import processing.managers.ConstructableManager;
import processing.managers.DatabaseUpdater;
import processing.managers.LocationManager;
import responses.ActionBubbleResponse;
import responses.CastSpellResponse;
import responses.MessageResponse;
import responses.PlayerUpdateResponse;
import responses.ResponseMaps;
import responses.ShipUpdateResponse;
import types.DamageTypes;
import types.Items;
import types.Storage;
import utils.RandomUtil;

public class Ship extends Attackable {
	@Getter private final int captainId; // player who owns the ship
	@Getter private int remainingTicks;
	private final ShipDto dto;
	private int[] slots;
	private Set<Player> passengers = new HashSet<>();
	private int crewPoints = 0;
	private int fishingPoints = 0;
	private int offensePoints = 0;
	private int defensePoints = 0; // hp boost too? higher repair rates?
	private int storagePoints = 0;
	Storage storage = null;
	
	private static final Items[] cannonballs = new Items[] {Items.CANNONBALL, Items.GOLDEN_CANNONBALL};
	private final int cannonChargeTicks = 5;
	private int currentCannonChargeTicks = 0;
	private int readyCannons = 0;
	
	// multiple people can repair the ship at the same time
	private final int repairTicks = 5;
	private Map<Integer, Integer> currentRepairTicks = new HashMap<>();
	
	public Storage getStorage() {
		if (storage == null) {
			storage = new Storage(25 + (storagePoints * 25));
		}
		
		return storage;
	}
	
	public Ship(int captainId, ShipDto dto) {
		this.captainId = captainId;
		this.dto = dto;
		slots = new int[dto.getSlotSize()];
		Arrays.fill(slots, 0);
	}
	
	public Player getCaptain() {
		final Player captain = WorldProcessor.getPlayerById(captainId);
		if (captain == null)
			return null; // captain logged out
		
		if (!passengers.contains(captain))
			return null; // captain logged in but not onboard
		
		return captain;
	}
	
	public boolean hasFreeSlots() {
		return (int)Arrays.stream(slots).filter(e -> e == 0).count() > 0;
	}
	
	public int getNumCannons() {
		return offensePoints;
	}
	
	public boolean readyToFire() {
		return currentCannonChargeTicks >= cannonChargeTicks;
	}
	
	public boolean setFreeSlot(int accessoryId) {
		ShipAccessoryDto dto = ShipAccessoryDao.getAccessoryById(accessoryId);
		if (dto == null)
			return false;
		
		for (int i = 0; i < slots.length; ++i) {
			if (slots[i] == 0) {
				slots[i] = accessoryId;
				fishingPoints += dto.getFishing();
				offensePoints += dto.getOffense();
				defensePoints += dto.getDefense();
				storagePoints += dto.getStorage();
				crewPoints += dto.getCrew();
				return true;
			}
		}
		return false;
	}
	
	public int getHullSceneryId() {
		return dto.getHullSceneryId();
	}
	
	private int getMaxPassengers() {
		return crewPoints * 2;
	}
	
	public boolean isFull() {
		// don't count the captain when checking the crew count
		return passengers.stream()
				.filter(e -> e.getId() != captainId)
				.count() >= getMaxPassengers();
	}
	
	public boolean boardPlayer(Player player) {
		if (isFull() && captainId != player.getId())
			return false;
		
		passengers.add(player);
		player.setTileId(tileId);
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).boardedShipId(captainId).build());
		return true;
	}
	
	public boolean disembarkPlayer(Player player) {
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).boardedShipId(0).build());
		return passengers.remove(player);
	}
	
	public void removeLoggedOutPlayer(Player player) {
		passengers.remove(player);
	}
	
	public boolean playerIsAboard(int playerId) {
		return passengers.stream().anyMatch(player -> player.getId() == playerId);
	}
	
	private Player passengerWithState(PlayerState state) {
		return passengers.stream()
				.filter(e -> e.getState() == state)
				.findFirst()
				.orElse(null);
	}
	
	private void processCannon(ResponseMaps responseMaps) {
		final Player cannoner = passengerWithState(PlayerState.charging_cannon);
		if (cannoner == null) {
			currentCannonChargeTicks = 0;
			return;
		}
		
		if (currentCannonChargeTicks == 0) {
			// just started charging
			final InventoryItemDto firstCannonball = storage.getFirstItemOf(cannonballs);
			if (firstCannonball == null) {
				// somehow verified successfully but now there's no cannonballs?
				changePassengerState(PlayerState.charging_cannon, PlayerState.idle);
				return;
			}
			
			responseMaps.addLocalResponse(floor, tileId, 
					new ActionBubbleResponse(this, cannoner.getId(), ItemDao.getItem(firstCannonball.getItemId())));
		}
		
		if (++currentCannonChargeTicks >= cannonChargeTicks) {
			if (currentCannonChargeTicks == cannonChargeTicks) {
				// we've just become ready, so set the ready cannons
				readyCannons = offensePoints;
			}
			fireCannon(responseMaps);
			
			if (--readyCannons <= 0)
				currentCannonChargeTicks = 0;
		}
	}
	
	private void changePassengerState(PlayerState from, PlayerState to) {
		passengers.stream()
			.filter(e -> e.getState() == from)
			.forEach(e -> e.setState(to));
	}
	
	private void processRepairs(ResponseMaps responseMaps) {
		if (passengerWithState(PlayerState.repairing_ship) == null) {
			currentRepairTicks.clear();
		} else {
			// we repair the ship with the same planks used to build it
			final int repairPlankId = ConstructableDao.getConstructableBySceneryId(dto.getHullSceneryId()).getPlankId();
			final String noSuppliesMessage = String.format("there's no %s available in storage to repair the ship.", ItemDao.getItem(repairPlankId).getNamePlural());
			
			passengers.stream()
				.filter(e -> e.getState() == PlayerState.repairing_ship)
				.forEach(e -> {
					if (currentRepairTicks.putIfAbsent(e.getId(), 0) != null) {
						currentRepairTicks.put(e.getId(), currentRepairTicks.get(e.getId()) + 1);
					} else {
						if (storage.contains(repairPlankId)) {
							responseMaps.addLocalResponse(floor, tileId, 
								new ActionBubbleResponse(this, e.getId(), ItemDao.getItem(Items.HAMMER.getValue())));
						} else {
							responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse(noSuppliesMessage, "white"));
							e.setState(PlayerState.idle);
						}
					}
					
					if (currentRepairTicks.remove(e.getId(), repairTicks)) {
						InventoryItemDto firstPlank = storage.getItemById(repairPlankId);
						if (firstPlank != null) {
							responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse("you make some repairs.", "white"));
							storage.remove(firstPlank.getSlot());
						} else {
							responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse(noSuppliesMessage, "white"));
							e.setState(PlayerState.idle);
						}
					}
				});
		}
	}
	
	private void processMovement(ResponseMaps responseMaps) {
		// the idea here is the captain can only do one thing at a time:
		// moving ship, firing cannons, repairing etc.
		// if you're firing cannons then your move/repair states get cancelled out.
		// if you want to do multiple actions at once, you need crew members to help.
		final Player captain = getCaptain();
		if (captain != null) {
			if (captain.getState() == PlayerState.moving_ship) {
				if (popPath(responseMaps)) {
					LocationManager.addShip(this);
					
					ShipUpdateResponse updateResponse = new ShipUpdateResponse();
					updateResponse.setCaptainId(captainId);
					updateResponse.setTileId(tileId);
					responseMaps.addLocalResponse(floor, tileId, updateResponse);
					
					passengers.forEach(player -> {
						player.setTileId(tileId);
					
						PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
						playerUpdateResponse.setId(player.getId());
						playerUpdateResponse.setTileId(getTileId());
						responseMaps.addClientOnlyResponse(player, playerUpdateResponse);
					});
				} else {
					// we were in moving_ship state but path has become empty, so now set to idle
					captain.setState(PlayerState.idle);
				}
			} else {
				// if the captain is not in moving_ship state then stop moving the ship
				path.clear();
			}
		}
	}
	
	public void process(int tick, ResponseMaps responseMaps) {
		processCannon(responseMaps);
		processRepairs(responseMaps);
		processMovement(responseMaps);
	}
	
	private void fireCannon(ResponseMaps responseMaps) {
		if (target == null)
			return;
		
		final String ret = verifyFireCannon(target);
		if (!ret.isEmpty()) {
			passengers.stream()
				.filter(e -> e.getState() == PlayerState.charging_cannon)
				.forEach(e -> {
					e.setState(PlayerState.idle);
					responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse(ret, "white"));
				});
			return;
		}
		
		final double distanceToTarget = Math.max(1, PathFinder.calculateDistance(tileId, target.getTileId()));
		
		// accuracy is basically 100% when adjacent, then linearly fading to 10% at getCannonRange()
		final double accuracy = 1 - (distanceToTarget / (double)getCannonRange());
		
		// based on the distance, we can also calculate how many ticks to wait before damaging the target
		
		// TODO fire cannon local response
		Player shooter = passengerWithState(PlayerState.charging_cannon); 
		if (shooter == null)
			shooter = passengers.iterator().next();
		
		final InventoryItemDto firstCannonball = storage.getFirstItemOf(cannonballs);
		final int firstCannonballSpriteFrameId = ItemDao.getItem(firstCannonball.getItemId()).getSpriteFrameId();
		
		LocationManager.getLocalPlayers(floor, tileId, 15)
			.forEach(localPlayer -> ClientResourceManager.addSpriteFramesAndSpriteMaps(localPlayer, Collections.singleton(firstCannonballSpriteFrameId)));
		
		final CastSpellResponse projectile = new CastSpellResponse(tileId, ((NPC)target).getInstanceId(), "npc", firstCannonballSpriteFrameId);
		responseMaps.addLocalResponse(floor, tileId, projectile);
		
		// TODO hit should take opponent defense into account
		target.onHit(RandomUtil.getRandom(0, (getCannonRange() - (int)distanceToTarget) * 2), DamageTypes.STANDARD, responseMaps);
		if (target.getCurrentHp() == 0) {
			target.onDeath(shooter, responseMaps);
			target = null;
			
			passengers.stream()
				.filter(e -> e.getState() == PlayerState.charging_cannon)
				.forEach(e -> e.setState(PlayerState.idle));
		}
		
		storage.remove(firstCannonball.getSlot());
	}
	
	public String verifyFireCannon(Attackable attackable) {
		if (offensePoints == 0) {
			return "the ship has no cannons mounted!";
		}
		
		if (!PathFinder.tileWithinRadius(attackable.getTileId(), tileId, getCannonRange())) {
			return "you need to get closer.";
		}
		
		if (!PathFinder.lineOfSightIsClear(floor, tileId, attackable.getTileId(), getCannonRange())) {
			return "you don't have a clear shot.";
		}
		
		if (!storage.contains(Items.CANNONBALL) && !storage.contains(Items.GOLDEN_CANNONBALL)) {
			return "you don't have any cannonballs.";
		}
		
		return "";
	}
	
	public int getCannonRange() {
		return 10;
	}

	@Override
	public void onDeath(Attackable killer, ResponseMaps responseMaps) {
		// TODO shipwreck scenery in its place
	}

	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		
	}

	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		
	}

	@Override
	public void onAttack(int damage, DamageTypes type, ResponseMaps responseMaps) {
		
	}

	@Override public void setStatsAndBonuses() {}

	@Override
	public int getExp() {
		return 0;
	}
}
