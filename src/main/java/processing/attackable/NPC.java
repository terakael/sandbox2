package processing.attackable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import database.dao.BuryableDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import database.dto.NPCDto;
import database.dto.NpcDropDto;
import lombok.Getter;
import lombok.Setter;
import processing.PathFinder;
import processing.attackable.Player.PlayerState;
import processing.managers.ConstructableManager;
import processing.managers.FightManager;
import processing.managers.LocationManager;
import processing.managers.ShipManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.ItemDropFromNpcUpdate;
import responses.BuryResponse;
import responses.MessageResponse;
import responses.NpcUpdateResponse;
import responses.PvmStartResponse;
import responses.ResponseMaps;
import responses.TalkToResponse;
import system.GroundItemManager;
import types.Buffs;
import types.DamageTypes;
import types.ItemAttributes;
import types.Items;
import types.NpcAttributes;
import types.Prayers;
import types.Stats;
import utils.RandomUtil;

public class NPC extends Attackable {
	@Getter
	protected NPCDto dto;

	private final transient int maxTickCount = 15;
	private final transient int minTickCount = 5;
	private transient int tickCounter = 0;
	protected int deathTimer = 0;
	private final transient int MAX_HUNT_TIMER = 5;
	private transient int huntTimer = 0;
	@Setter
	private transient int postCombatCooldown = 0;
	@Getter
	protected transient int instanceId = 0;
	protected transient List<Integer> walkableTiles = null;
	private int combatLevel = 0;

	protected int lastProcessedTick = 0;
	private Map<Integer, Integer> playerSteathCooldown = new HashMap<>();

	public NPC(NPCDto dto, int floor, int instanceId) {
		this.dto = dto;
		this.instanceId = instanceId;
		this.tileId = instanceId;
		this.floor = floor;

		init();
	}

	protected void init() {
		HashMap<Stats, Integer> stats = new HashMap<>();
		stats.put(Stats.STRENGTH, dto.getStr());
		stats.put(Stats.ACCURACY, dto.getAcc());
		stats.put(Stats.DEFENCE, dto.getDef());
		stats.put(Stats.PRAYER, dto.getPray());
		stats.put(Stats.HITPOINTS, dto.getHp());
		stats.put(Stats.MAGIC, dto.getMagic());
		setStats(stats);

		combatLevel = dto.getCmb();// StatsDao.getCombatLevelByStats(dto.getStr(), dto.getAcc(), dto.getDef(),
									// dto.getPray(), dto.getHp(), dto.getMagic());

		HashMap<Stats, Integer> bonuses = new HashMap<>();
		bonuses.put(Stats.STRENGTH, dto.getStrBonus());
		bonuses.put(Stats.ACCURACY, dto.getAccBonus());
		bonuses.put(Stats.DEFENCE, dto.getDefBonus());
		bonuses.put(Stats.PRAYER, dto.getPrayBonus());
		setBonuses(bonuses);

		HashMap<Stats, Integer> boosts = new HashMap<>();
		boosts.put(Stats.STRENGTH, 0);
		boosts.put(Stats.ACCURACY, 0);
		boosts.put(Stats.DEFENCE, 0);
		boosts.put(Stats.PRAYER, 0);
		boosts.put(Stats.MAGIC, 0);
		setBoosts(boosts);

		setCurrentHp(dto.getHp());
		setMaxCooldown(dto.getAttackSpeed());

		// pre-calculate all the walkable tiles on server startup to save processing
		// time over the long run
		// (i.e. instead of checking if a tile can be walked to every time we move, we
		// already guaranteed it)
		walkableTiles = PathFinder.calculateWalkableTiles(floor, tileId, dto.getRoamRadius());

		huntTimer = RandomUtil.getRandom(0, MAX_HUNT_TIMER);// just so all the NPCs aren't hunting on the same tick
	}

	public void process(int currentTick, ResponseMaps responseMaps) {
		// when the npc is out of range of all players, it is not being processed.
		// this can cause issues if, for example, a monster is dead and the player
		// leaves its range.
		// the monster will stay dead and resume it's death count once the player
		// returns.
		// to remedy this, we record the last tick it was processed, so we can figure
		// out how long it has been since the last process.
		final int deltaTicks = currentTick - lastProcessedTick;
		lastProcessedTick = currentTick;

		super.process(responseMaps);

		if (postCombatCooldown > 0) {
			postCombatCooldown -= deltaTicks;
			if (postCombatCooldown < 0)
				postCombatCooldown = 0;
		}
		if (currentHp == 0) {
			handleRespawn(responseMaps, deltaTicks);
			return;
		}

		processPoison(responseMaps);

		findTarget(currentTick, responseMaps);

		if (popPath(responseMaps)) {
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(instanceId);
			updateResponse.setTileId(tileId);
			responseMaps.addLocalResponse(floor, tileId, updateResponse);
		}

		if (target == null) {
			if (--tickCounter < 0) {
				Random r = new Random();
				tickCounter = r.nextInt((maxTickCount - minTickCount) + 1) + minTickCount;

				setPathToRandomTileInRadius(responseMaps);
			}
		} else if (postCombatCooldown <= 0) {
			handleActiveTarget(responseMaps);
		}

		playerSteathCooldown.entrySet().removeIf(entry -> entry.getValue() < currentTick);
	}

	protected void findTarget(int currentTick, ResponseMaps responseMaps) {
		if ((dto.getAttributes() & NpcAttributes.AGGRESSIVE.getValue()) == NpcAttributes.AGGRESSIVE.getValue()
				&& !isInCombat()) {
			// aggressive monster; look for targets
			if (--huntTimer <= 0) { // technically could use the deltaTick here but who cares, it's not really
									// noticeable.
				List<Player> closePlayers = new ArrayList<>(
						LocationManager.getLocalPlayers(floor, tileId, dto.getRoamRadius() / 2));

				if (dto.getId() == 18 || dto.getId() == 22) { // goblins won't attack anyone with the goblin stank buff
					closePlayers = closePlayers.stream()
							.filter(player -> !player.hasBuff(Buffs.GOBLIN_STANK))
							.collect(Collectors.toList());
				}

				if (target != null && !closePlayers.contains(target))
					target = null;

				if (target == null) {
					// filter out of the list players that are in combat and players too high level
					// to attack
					closePlayers = closePlayers.stream()
							.filter(player -> {
								return !player.isInCombat()
										&& StatsDao.getCombatLevelByStats(player.getStats()) < (combatLevel * 2)
										&& ShipManager.getShipWithPlayer(player) == null
										&& !playerSteathCooldown.containsKey(player.getId());
							})
							.collect(Collectors.toList());

					while (!closePlayers.isEmpty()) {
						final int targetPlayerIndex = RandomUtil.getRandom(0, closePlayers.size());
						final Player targetPlayer = closePlayers.get(targetPlayerIndex);

						int targetPlayerStealth = 0;
						if (targetPlayer.prayerIsActive(Prayers.STEALTH))
							targetPlayerStealth += 50;
						if (targetPlayer.isItemEquipped(Items.GHOSTLY_ROBE_TOP))
							targetPlayerStealth += 20;
						if (targetPlayer.isItemEquipped(Items.GHOSTLY_ROBE_BOTTOMS))
							targetPlayerStealth += 15;

						if (RandomUtil.getRandom(0, 100) < targetPlayerStealth) {
							MessageResponse messageResponse = MessageResponse
									.newMessageResponse("the " + dto.getName() + " fails to notice you.", "white");
							responseMaps.addClientOnlyResponse(closePlayers.get(targetPlayerIndex), messageResponse);
							playerSteathCooldown.put(targetPlayer.getId(), currentTick + 40); // 20 second cooldown
							closePlayers.remove(targetPlayerIndex);
						} else {
							if (targetPlayerStealth > 0) {
								MessageResponse messageResponse = MessageResponse
										.newMessageResponse("the " + dto.getName() + " spotted you!", "red");
								responseMaps.addClientOnlyResponse(closePlayers.get(targetPlayerIndex),
										messageResponse);
							}
							target = closePlayers.get(targetPlayerIndex);
							final TalkToResponse talkToResponse = new TalkToResponse(getInstanceId(), "!");
							responseMaps.addLocalResponse(target.getFloor(), target.getTileId(), talkToResponse);
							break;
						}
					}
				}

				huntTimer = MAX_HUNT_TIMER;
			}
		}
	}

	protected void handleActiveTarget(ResponseMaps responseMaps) {
		// chase the target if not next to it
		if (!PathFinder.isNextTo(floor, tileId, target.getTileId())) {
			if (target.getFloor() == floor
					&& PathFinder.tileWithinRadius(target.getTileId(), instanceId, dto.getRoamRadius() + 2)) {
				path = PathFinder.findPath(floor, tileId, target.getTileId(), true);
			} else {
				int retreatTileId = PathFinder.findRetreatTile(target.getTileId(), tileId, instanceId,
						dto.getRoamRadius());
				System.out.println("retreating to tile " + retreatTileId + "(retreating from " + target.getTileId()
						+ ", currently at " + tileId + ", anchor=" + instanceId + ", radius = " + dto.getRoamRadius()
						+ ")");

				path = PathFinder.findPath(floor, tileId, retreatTileId, true);
				target = null;
			}
		} else {
			if (target.isInCombat()) {
				if (!FightManager.fightingWith(this, target)) {
					int retreatTileId = PathFinder.findRetreatTile(target.getTileId(), tileId, instanceId,
							dto.getRoamRadius());
					path = PathFinder.findPath(floor, tileId, retreatTileId, true);
					target = null;
				}
			} else {
				Player p = (Player) target;
				p.setState(PlayerState.fighting);
				setTileId(p.getTileId());// npc is attacking the player so move to the player's tile
				p.clearPath();
				clearPath();
				FightManager.addFight(p, this, false);

				PvmStartResponse pvmStart = new PvmStartResponse();
				pvmStart.setPlayerId(p.getId());
				pvmStart.setMonsterId(instanceId);
				pvmStart.setTileId(getTileId());
				responseMaps.addLocalResponse(p.getFloor(), getTileId(), pvmStart);
			}
		}
	}

	protected void setPathToRandomTileInRadius(ResponseMaps responseMaps) {
		// responseMaps can be used by overriding npcs (such as necromancer who
		// teleports around)
		path = PathFinder.findPath(floor, tileId, chooseRandomWalkableTile(), true, false);
	}

	protected int chooseRandomWalkableTile() {
		if (walkableTiles == null || walkableTiles.isEmpty()) {
			// if walkableTiles weren't initialized (i.e. pets)
			// then just choose a random tile near current tile.
			// pets can wander as far as they need to; they'll despawn after a little while
			// anyway
			return PathFinder.chooseRandomTileIdInRadius(tileId, dto.getRoamRadius());
		}
		return walkableTiles.get(RandomUtil.getRandom(0, walkableTiles.size()));
	}

	public int getId() {
		return dto.getId();
	}

	@Override
	public void onDeath(Attackable killer, ResponseMaps responseMaps) {
		currentHp = 0;
		deathTimer = dto.getRespawnTicks();
		target = null;
		lastTarget = null;
		clearPoison();

		// if they die of poison during a fight, the fight should end right away
		if (FightManager.fightWithFighterExists(this)) {
			FightManager.cancelFight(this, responseMaps);
		}

		// also drop an item
		handleLootDrop((Player) killer, responseMaps);
	}

	protected void handleLootDrop(Player killer, ResponseMaps responseMaps) {
		final List<NpcDropDto> potentialDrops = NPCDao.getDropsByNpcId(dto.getId())
				.stream()
				.filter(dto -> {
					if (ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.UNIQUE)) {
						if (PlayerStorageDao.itemExistsInPlayerStorage(killer.getId(), dto.getItemId()))
							return false;

						if (GroundItemManager.itemIsOnGround(floor, killer.getId(), dto.getItemId()))
							return false;
					}
					return true;
				})
				.collect(Collectors.toList());

		for (NpcDropDto drop : potentialDrops) {
			if (RandomUtil.getRandom(0, drop.getRate()) == 0) {
				if (ConstructableManager.constructableIsInRadius(floor, tileId, 129, 3)
						&& BuryableDao.isBuryable(drop.getItemId())) {
					// give the player the corresponding prayer exp instead of dropping it
					BuryResponse.handleBury(killer, drop.getItemId(), responseMaps);
				} else {
					GroundItemManager.add(floor, killer.getId(), drop.getItemId(), tileId, drop.getCount(),
							ItemDao.getMaxCharges(drop.getItemId()));
					TybaltsTaskManager.check(killer, new ItemDropFromNpcUpdate(drop.getItemId(), drop.getCount()),
							responseMaps);
				}
			}
		}
	}

	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		target = null;
	}

	@Override
	public void onHit(int damage, DamageTypes type, ResponseMaps responseMaps) {
		currentHp -= damage;
		if (currentHp < 0)
			currentHp = 0;

		NpcUpdateResponse updateResponse = new NpcUpdateResponse();
		updateResponse.setInstanceId(instanceId);
		updateResponse.setDamage(damage, type);
		updateResponse.setHp(currentHp);
		responseMaps.addLocalResponse(floor, tileId, updateResponse);
	}

	@Override
	public void onAttack(int damage, DamageTypes type, ResponseMaps responseMaps) {
		NpcUpdateResponse updateResponse = new NpcUpdateResponse();
		updateResponse.setInstanceId(instanceId);
		updateResponse.setDoAttack(true);
		responseMaps.addLocalResponse(floor, tileId, updateResponse);
	}

	@Override
	public void setStatsAndBonuses() {
		// already set on instantiation
	}

	@Override
	public int getExp() {
		return combatLevel;
	}

	public boolean isDead() {
		return currentHp == 0;
	}

	// first two seconds of death; we don't want to send the clients that the npc is
	// dead
	// (and therefore should no longer be drawn) as we want to show the death
	// animation on the client.
	public boolean isDeadWithDelay() {
		return isDead() && deathTimer < dto.getRespawnTicks() - 2;
	}

	protected void handleRespawn(ResponseMaps responseMaps, int deltaTicks) {
		deathTimer -= deltaTicks;
		if (deathTimer <= 0) {
			onRespawn(responseMaps);
		}
	}

	protected void onRespawn(ResponseMaps responseMaps) {
		deathTimer = 0;
		currentHp = dto.getHp();
		tileId = instanceId;

		NpcUpdateResponse updateResponse = new NpcUpdateResponse();
		updateResponse.setInstanceId(instanceId);
		updateResponse.setHp(currentHp);
		updateResponse.setTileId(tileId);
		updateResponse.setSnapToTile(true);
		responseMaps.addLocalResponse(floor, tileId, updateResponse);
	}

	public void clearPath() {
		path.clear();
	}

	public boolean isDiurnal() {
		return (dto.getAttributes() & NpcAttributes.DIURNAL.getValue()) > 0;
	}

	public boolean isNocturnal() {
		return (dto.getAttributes() & NpcAttributes.NOCTURNAL.getValue()) > 0;
	}

	public int getOwnerId() {
		return -1; // pets override this to return the id of the player which owns it
	}

	@Override
	public String getType() {
		return "npc";
	}
}
