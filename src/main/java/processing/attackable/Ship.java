package processing.attackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import database.dao.ConstructableDao;
import database.dao.FishingDepthDao;
import database.dao.ItemDao;
import database.dao.ShipAccessoryDao;
import database.dto.InventoryItemDto;
import database.dto.ShipAccessoryDto;
import database.dto.ShipDto;
import database.entity.update.UpdatePlayerEntity;
import lombok.Getter;
import lombok.Setter;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.attackable.Player.PlayerState;
import processing.managers.ClientResourceManager;
import processing.managers.DatabaseUpdater;
import processing.managers.LocationManager;
import processing.managers.OceanFishingManager;
import requests.Request;
import responses.ActionBubbleResponse;
import responses.CastSpellResponse;
import responses.MessageResponse;
import responses.PlayerUpdateResponse;
import responses.Response;
import responses.ResponseFactory;
import responses.ResponseMaps;
import responses.ShipUiUpdateResponse;
import responses.ShipUpdateResponse;
import types.DamageTypes;
import types.Items;
import types.Storage;
import utils.RandomUtil;

public class Ship extends Attackable {
	@Getter private final int captainId; // player who owns the ship
	@Getter private int remainingTicks;
	private final ShipDto dto;
	private int[] accessorySlots;
	@Getter private Set<Player> passengers = new HashSet<>();
	private int crewPoints = 0;
	private int fishingPoints = 0;
	private int offensePoints = 0;
	private int defensePoints = 0; // hp boost too? higher repair rates?
	private int storagePoints = 0;
	Storage storage = null;
	
	@Getter private int maxArmour = 0;
	private int currentArmour = 0;
	
	@Getter private int maxHp = 0;
	
	private static final Items[] cannonballs = new Items[] {Items.CANNONBALL, Items.GOLDEN_CANNONBALL};
	private final int cannonChargeTicks = 5;
	private int currentCannonChargeTicks = 0;
	private int readyCannons = 0;
	
	// multiple people can repair the ship at the same time
	private final int repairTicks = 5;
	private Map<Integer, Integer> currentRepairTicks = new HashMap<>();
	
	private final int fishingTicks = 5;
	private Map<Integer, Integer> currentFishingTicks = new HashMap<>();
	
	@Setter private Request savedRequest;
	
	public Storage getStorage() {
		return storage;
	}
	
	public Ship(int captainId, ShipDto dto) {
		this.captainId = captainId;
		this.dto = dto;
		accessorySlots = new int[dto.getSlotSize()];
		Arrays.fill(accessorySlots, 0);
	}
	
	public void onFinishBuilding() {
		storage = new Storage(25 + (storagePoints * 25));
		
		maxArmour = defensePoints * 100;
		currentArmour = maxArmour;
		
		maxHp = 100;
		currentHp = maxHp;
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
		return (int)Arrays.stream(accessorySlots).filter(e -> e == 0).count() > 0;
	}
	
	public int getNumCannons() {
		return offensePoints;
	}
	
	public boolean readyToFire() {
		return currentCannonChargeTicks >= cannonChargeTicks;
	}
	
	public boolean canFish() {
		return fishingPoints > 0;
	}
	
	public boolean setFreeSlot(int accessoryId) {
		ShipAccessoryDto dto = ShipAccessoryDao.getAccessoryById(accessoryId);
		if (dto == null)
			return false;
		
		for (int i = 0; i < accessorySlots.length; ++i) {
			if (accessorySlots[i] == 0) {
				accessorySlots[i] = accessoryId;
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
	
	public boolean boardPlayer(Player player, ResponseMaps responseMaps) {
		if (isFull() && captainId != player.getId())
			return false;
		
		// let the other passengers know a player is boarding
		ShipUiUpdateResponse.builder()
			.boardedPlayers(Collections.singletonList(player.getDto().getName()))
			.build().passengerResponse(this, responseMaps);
		
		passengers.add(player);
		player.setTileId(tileId);
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).boardedShipId(captainId).build());

		// send a boardedShip update to the local player.
		// other players boarding a ship will get a playerOutOfRange response
		PlayerUpdateResponse onboardResponse = new PlayerUpdateResponse();
		onboardResponse.setId(player.getId());
		onboardResponse.setBoardedShipId(captainId);
		onboardResponse.setTileId(getTileId());
		responseMaps.addClientOnlyResponse(player, onboardResponse);
		
		sendUiDataToClient(player, responseMaps);
		
		return true;
	}
	
	public boolean disembarkPlayer(Player player, ResponseMaps responseMaps) {
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(player.getId()).boardedShipId(0).build());
		boolean playerRemoved = passengers.remove(player);
		
		if (playerRemoved) {
			// tell the remaining passengers
			ShipUiUpdateResponse.builder()
				.disembarkedPlayers(List.of(player.getDto().getName()))
				.build().passengerResponse(this, responseMaps);
		}
		
		return playerRemoved;
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
					
						// clientOnlyResponse because other players can't see a player on a boat
						final PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
						playerUpdateResponse.setId(player.getId());
						playerUpdateResponse.setTileId(getTileId());
						responseMaps.addClientOnlyResponse(player, playerUpdateResponse);
						
						final ShipUiUpdateResponse uiUpdate = new ShipUiUpdateResponse();
						uiUpdate.setDepth(getDepth());
						
						if (canFish()) {
							uiUpdate.setFishPopulation(getFishPopulationString());
						}
						
						responseMaps.addClientOnlyResponse(player, uiUpdate);
					});
				} else if (captain.getState() != PlayerState.idle) {
					// we were in moving_ship state but path has become empty, so now set to idle
					captain.setState(PlayerState.idle);
					
					Request requestToUse = savedRequest;
					savedRequest = null;
					if (requestToUse != null) {
						Response response = ResponseFactory.create(requestToUse.getAction());
						response.process(requestToUse, captain, responseMaps);
					}
				}
			} else {
				// if the captain is not in moving_ship state then stop moving the ship
				path.clear();
			}
		}
	}
	
	private void processFishing(ResponseMaps responseMaps) {
		if (passengerWithState(PlayerState.casting_net) == null) {
			currentFishingTicks.clear();
		} else {
			passengers.stream()
				.filter(e -> e.getState() == PlayerState.casting_net)
				.forEach(e -> {
					if (currentFishingTicks.putIfAbsent(e.getId(), 0) != null) {
						currentFishingTicks.put(e.getId(), currentFishingTicks.get(e.getId()) + 1);
					} else {
						if (storage.getEmptySlotCount() > 0) {
							responseMaps.addLocalResponse(floor, tileId, 
								new ActionBubbleResponse(this, e.getId(), ItemDao.getItem(Items.NET.getValue())));
						} else {
							responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse("vessel storage is full.", "white"));
							e.setState(PlayerState.idle);
						}
					}
					
					if (currentFishingTicks.remove(e.getId(), fishingTicks)) {
						final int depth = getDepth();
						
						List<Integer> caughtItems = new ArrayList<>();
						for (int i = 0; i < fishingPoints; ++i) {
							if (storage.getEmptySlotCount() > 0) {
								// if this tile or nearby tiles have been fished on, this will get closer to 1.0
								final double tileDifficulty = getFishPopulation();
								
								if (RandomUtil.chance((int)(tileDifficulty * 100)))
									continue;
								
								// different fish live in different deepness
								final List<Pair<Double, Integer>> weightedItems = FishingDepthDao.getWeightedItems(depth);
								
								for (var weightedItem : weightedItems) {
									final int chance = (int)(weightedItem.getLeft() * (100 - depth));
									if (RandomUtil.chance(chance)) {
										caughtItems.add(weightedItem.getRight());
										storage.add(new InventoryItemDto(weightedItem.getRight(), 0, 1, 0), 1);
										OceanFishingManager.increaseTileDifficulty(floor, tileId, responseMaps);
										break;
									}
								}
							} else {
								
								e.setState(PlayerState.idle);
								break;
							}
						}
						
						String catchMessage = "you retrieve an empty net.";
						if (caughtItems.size() == 1) {
							catchMessage = String.format("you catch a %s.", ItemDao.getNameFromId(caughtItems.get(0), false));
						}
						else if (!caughtItems.isEmpty())
							catchMessage = String.format("you catch %s.", caughtItems.stream()
								.collect(Collectors.groupingBy(
									Integer::intValue,
									Collectors.counting())).entrySet().stream()
								.map(entry -> String.format("%dx %s", entry.getValue(), ItemDao.getNameFromId(entry.getKey(), entry.getValue() != 1)))
								.collect(Collectors.joining(", ")));
										
						responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse(catchMessage, "white"));
						
						if (storage.getEmptySlotCount() == 0)
							responseMaps.addClientOnlyResponse(e, MessageResponse.newMessageResponse("vessel storage is full.", "white"));
					}
				});
		}
	}
	
	public int getDepth() {
		final int closestLand = PathFinder.getClosestWalkableTile(floor, tileId);
		if (closestLand == -1) {
			// deep ocean; good fishing loot out here.
			return 50;
		}
		
		return (int)PathFinder.calculateDistance(closestLand, tileId);
	}
	
	public Double getFishPopulation() {
		if (!canFish())
			return null;
		return OceanFishingManager.getTileDifficulty(floor, tileId);
	}
	
	public String getFishPopulationString() {
		final Double difficulty = getFishPopulation();
		if (difficulty == null)
			return "unknown";
		
		if (difficulty < 0.2)
			return "lots";
		
		if (difficulty < 0.6)
			return "some";
		
		if (difficulty < 0.9)
			return "few";
		
		return "bare";
	}
	
	public void process(int tick, ResponseMaps responseMaps) {
		final Set<Player> loggedOutPassengers = passengers.stream()
				.filter(player -> !WorldProcessor.sessionExistsByPlayerId(player.getId()))
				.collect(Collectors.toSet());
		passengers.removeAll(loggedOutPassengers);
		ShipUiUpdateResponse.builder()
			.disembarkedPlayers(loggedOutPassengers.stream().map(player -> player.getDto().getName()).collect(Collectors.toList()))
			.build().passengerResponse(this, responseMaps);
		
		processCannon(responseMaps);
		processRepairs(responseMaps);
		processMovement(responseMaps);
		processFishing(responseMaps);
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
		
		final CastSpellResponse projectile = new CastSpellResponse(tileId, target.getInstanceId(), target.getType(), firstCannonballSpriteFrameId, 0.5);
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
	
	public void sendUiDataToClient(Player player, ResponseMaps responseMaps) {
		final List<Integer> accessorySpriteFrameIds = IntStream.of(accessorySlots)
				.map(accessoryId -> ShipAccessoryDao.getAccessoryById(accessoryId).getSpriteFrameId())
				.boxed()
				.collect(Collectors.toList());
		
		ShipUiUpdateResponse.builder()
			.accessorySpriteIds(accessorySpriteFrameIds)
			.depth(getDepth())
			.fishPopulation(getFishPopulationString())
			.boardedPlayers(passengers.stream()
					.map(passenger -> passenger.getDto().getName())
					.collect(Collectors.toList()))
			.build()
			.clientOnlyResponse(player, responseMaps);
		
		ClientResourceManager.addSpriteFramesAndSpriteMaps(player, new HashSet<>(accessorySpriteFrameIds));
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
		if (currentArmour > 0 && Math.random() < ((double)defensePoints / (double)(defensePoints + 1))) {
			type = DamageTypes.BLOCK;
			currentArmour -= damage;
			if (currentArmour < 0) {
				currentHp += currentArmour;
				currentArmour = 0;
			}
		} else {
			currentHp -= damage;
		}
		
		final ShipUpdateResponse response = new ShipUpdateResponse();
		response.setCaptainId(captainId);
		response.setHp(currentHp);
		response.setArmour(currentArmour);
		response.setDamage(damage, type);
		responseMaps.addLocalResponse(floor, tileId, response);
	}

	@Override
	public void onAttack(int damage, DamageTypes type, ResponseMaps responseMaps) {
		
	}

	@Override public void setStatsAndBonuses() {}

	@Override
	public int getExp() {
		return 0;
	}
	
	@Override
	public String getType() {
		return "ship";
	}
	
	@Override
	public int getInstanceId() {
		return captainId;
	}
}
