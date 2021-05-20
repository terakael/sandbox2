package main.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import lombok.Getter;
import main.GroundItemManager;
import main.database.ItemDao;
import main.database.NPCDao;
import main.database.NPCDto;
import main.database.NpcDropDto;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.Player.PlayerState;
import main.responses.MessageResponse;
import main.responses.NpcUpdateResponse;
import main.responses.PvmStartResponse;
import main.responses.ResponseMaps;
import main.types.Buffs;
import main.types.DamageTypes;
import main.types.ItemAttributes;
import main.types.NpcAttributes;
import main.types.Prayers;
import main.types.Stats;
import main.utils.RandomUtil;

public class NPC extends Attackable {
	@Getter private NPCDto dto;
	
	private final transient int maxTickCount = 15;
	private final transient int minTickCount = 5;
	private transient int tickCounter = 0;
	private int deathTimer = 0;
	private final transient int MAX_HUNT_TIMER = 5;
	private transient int huntTimer = 0;
	@Getter private boolean moving = false;
		
	private int combatLevel = 0;
	
	private int lastProcessedTick = 0;
	
	public NPC(NPCDto dto) {
		this.dto = dto;
		tileId = dto.getTileId();
		floor = dto.getFloor();
		
		HashMap<Stats, Integer> stats = new HashMap<>();
		stats.put(Stats.STRENGTH, dto.getStr());
		stats.put(Stats.ACCURACY, dto.getAcc());
		stats.put(Stats.DEFENCE, dto.getDef());
		stats.put(Stats.PRAYER, dto.getPray());
		stats.put(Stats.HITPOINTS, dto.getHp());
		stats.put(Stats.MAGIC, dto.getMagic());
		setStats(stats);
		
		combatLevel = StatsDao.getCombatLevelByStats(dto.getStr(), dto.getAcc(), dto.getDef(), dto.getPray(), dto.getHp(), dto.getMagic());
		
		HashMap<Stats, Integer> bonuses = new HashMap<>();
		bonuses.put(Stats.STRENGTH, dto.getStrBonus());
		bonuses.put(Stats.ACCURACY, dto.getAccBonus());
		bonuses.put(Stats.DEFENCE, dto.getDefBonus());
		bonuses.put(Stats.PRAYER, dto.getPrayBonus());
//		bonuses.put(Stats.HITPOINTS, dto.getHpBonus());
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
		
		huntTimer = RandomUtil.getRandom(0, MAX_HUNT_TIMER);// just so all the NPCs aren't hunting on the same tick
//		tickCounter = RandomUtil.getRandom(1, maxTickCount);
	}
	
	public void process(int currentTick, ResponseMaps responseMaps) {
		// when the npc is out of range of all players, it is not being processed.
		// this can cause issues if, for example, a monster is dead and the player leaves its range.
		// the monster will stay dead and resume it's death count once the player returns.
		// to remedy this, we record the last tick it was processed, so we can figure out how long it has been since the last process.
		final int deltaTicks = currentTick - lastProcessedTick;
		lastProcessedTick = currentTick;
		
		if (currentHp == 0) {
			handleRespawn(responseMaps, deltaTicks);			
			return;
		}
		
		processPoison(responseMaps);
		
		if ((dto.getAttributes() & NpcAttributes.AGGRESSIVE.getValue()) == NpcAttributes.AGGRESSIVE.getValue() && !isInCombat()) {
			// aggressive monster; look for targets
			if (--huntTimer <= 0) { // technically could use the deltaTick here but who cares, it's not really noticeable.
				List<Player> closePlayers = WorldProcessor.getPlayersNearTile(floor, tileId, dto.getRoamRadius()/2);
				
				if (dto.getId() == 18 || dto.getId() == 22) { // goblins won't attack anyone with the goblin stank buff
					closePlayers = closePlayers.stream()
						.filter(player -> !player.hasBuff(Buffs.GOBLIN_STANK))
						.collect(Collectors.toList());
				}
				
				if (target != null && !closePlayers.contains(target))
					target = null;
				
				if (target == null) {
					// filter out of the list players that are in combat and players too high level to attack
					closePlayers = closePlayers.stream()
						.filter(player -> !player.isInCombat() && (StatsDao.getCombatLevelByStats(player.getStats()) < (combatLevel * 2)))
						.collect(Collectors.toList());
					
					while (!closePlayers.isEmpty()) {
						final int targetPlayer = RandomUtil.getRandom(0, closePlayers.size());
						
						if (closePlayers.get(targetPlayer).prayerIsActive(Prayers.STEALTH) && RandomUtil.getRandom(0, 100) < 85) {
							MessageResponse messageResponse = MessageResponse.newMessageResponse("the " + dto.getName() + " fails to notice you.", "white");
							responseMaps.addClientOnlyResponse(closePlayers.get(targetPlayer), messageResponse);
							closePlayers.remove(targetPlayer);
						}
						else {
							if (closePlayers.get(targetPlayer).prayerIsActive(Prayers.STEALTH)) {
								MessageResponse messageResponse = MessageResponse.newMessageResponse("the " + dto.getName() + " spotted you!", "red");
								responseMaps.addClientOnlyResponse(closePlayers.get(targetPlayer), messageResponse);
							}
							target = closePlayers.get(targetPlayer);
							break;
						}
					}
				}
			
				huntTimer = MAX_HUNT_TIMER;
			}
		}
		
		moving = popPath();
		if (moving) {
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(getInstanceId());
			updateResponse.setTileId(tileId);
			responseMaps.addLocalResponse(floor, tileId, updateResponse);
		}
		
		if (target == null) {
			if (--tickCounter < 0) {
				Random r = new Random();
				tickCounter = r.nextInt((maxTickCount - minTickCount) + 1) + minTickCount;
				
				int destTile = PathFinder.chooseRandomTileIdInRadius(dto.getTileId(), dto.getRoamRadius());
				path = PathFinder.findPath(floor, tileId, destTile, true, dto.getTileId(), dto.getRoamRadius());
			}
		} else {
			// chase the target if not next to it
			if (!PathFinder.isNextTo(floor, tileId, target.tileId)) {
				if (PathFinder.tileWithinRadius(target.tileId, dto.getTileId(), dto.getRoamRadius() + 2)) {
					path = PathFinder.findPath(floor, tileId, target.tileId, true);
				} else {
					int retreatTileId = PathFinder.findRetreatTile(target.tileId, tileId, dto.getTileId(), dto.getRoamRadius());
					System.out.println("retreating to tile " + retreatTileId + "(retreating from " + target.tileId + ", currently at " + tileId + ", anchor=" + dto.getTileId() + ", radius = " + dto.getRoamRadius() + ")");
					
					path = PathFinder.findPath(floor, tileId, retreatTileId, true);
					target = null;
				}
			} else {
				if (target.isInCombat()) {
					if (!FightManager.fightingWith(this, target)) {
						int retreatTileId = PathFinder.findRetreatTile(target.tileId, tileId, dto.getTileId(), dto.getRoamRadius());						
						path = PathFinder.findPath(floor, tileId, retreatTileId, true);
						target = null;
					}
				} else {
					Player p = (Player)target;
					p.setState(PlayerState.fighting);
					setTileId(p.getTileId());// npc is attacking the player so move to the player's tile
					p.clearPath();
					clearPath();
					FightManager.addFight(p, this, false);
					
					PvmStartResponse pvmStart = new PvmStartResponse();
					pvmStart.setPlayerId(p.getId());
					pvmStart.setMonsterId(getInstanceId());
					pvmStart.setTileId(getTileId());
					responseMaps.addBroadcastResponse(pvmStart);
				}
			}
		}
	}
	
	public int getInstanceId() {
		return dto.getTileId();// the spawn tileId is used for the id
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
		
		List<NpcDropDto> potentialDrops = NPCDao.getDropsByNpcId(dto.getId())
				.stream()
				.filter(dto -> {
					if (ItemDao.itemHasAttribute(dto.getItemId(), ItemAttributes.UNIQUE)) {
						int playerId = ((Player)killer).getId();
						if (PlayerStorageDao.itemExistsInPlayerStorage(playerId, dto.getItemId()))
							return false;
						
						if (GroundItemManager.itemIsOnGround(floor, playerId, dto.getItemId()))
							return false;
					}
					return true;
				})
				.collect(Collectors.toList());
		
		for (NpcDropDto dto : potentialDrops) {
			if (RandomUtil.getRandom(0, dto.getRate()) == 0) {
				GroundItemManager.add(floor, ((Player)killer).getId(), dto.getItemId(), tileId, dto.getCount(), ItemDao.getMaxCharges(dto.getItemId()));
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
		updateResponse.setInstanceId(dto.getTileId());
		updateResponse.setDamage(damage, type);
		updateResponse.setHp(currentHp);
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
		return deathTimer > 0;
	}
	
	// first two seconds of death; we don't want to send the clients that the npc is dead
	// (and therefore should no longer be drawn) as we want to show the death animation on the client.
	public boolean isDeadWithDelay() {
		return deathTimer > 0 && deathTimer < dto.getRespawnTicks() - 2;
	}
	
	private void handleRespawn(ResponseMaps responseMaps, int deltaTicks) {
		deathTimer -= deltaTicks;
		if (deathTimer <= 0) {
			deathTimer = 0;
			currentHp = dto.getHp();
			tileId = dto.getTileId();
			
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(getInstanceId());
			updateResponse.setHp(currentHp);
			updateResponse.setTileId(tileId);
			updateResponse.setSnapToTile(true);
			responseMaps.addLocalResponse(floor, tileId, updateResponse);
		}
	}
	
	public void clearPath() {
		path.clear();
	}
}
