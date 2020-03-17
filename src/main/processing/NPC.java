package main.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;
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
import main.responses.NpcUpdateResponse;
import main.responses.PvmStartResponse;
import main.responses.ResponseMaps;
import main.types.Buffs;
import main.types.DamageTypes;
import main.types.ItemAttributes;
import main.types.NpcAttributes;
import main.types.Stats;
import main.utils.RandomUtil;

public class NPC extends Attackable {
	@Getter private NPCDto dto;
	
	private Stack<Integer> path = new Stack<>();
	
	private final transient int maxTickCount = 15;
	private final transient int minTickCount = 5;
	private transient int tickCounter = maxTickCount;
	private int respawnTime = 10;
	private int deathTimer = 0;
	private final transient int MAX_HUNT_TIMER = 5;
	private transient int huntTimer = 0;
	@Getter private boolean moving = false;
		
	private int combatLevel = 0;
	
	public NPC(NPCDto dto) {
		this.dto = dto;
		tileId = dto.getTileId();
		roomId = dto.getRoomId();
		
		HashMap<Stats, Integer> stats = new HashMap<>();
		stats.put(Stats.STRENGTH, dto.getStr());
		stats.put(Stats.ACCURACY, dto.getAcc());
		stats.put(Stats.DEFENCE, dto.getDef());
		stats.put(Stats.AGILITY, dto.getAgil());
		stats.put(Stats.HITPOINTS, dto.getHp());
		stats.put(Stats.MAGIC, dto.getMagic());
		setStats(stats);
		
		combatLevel = StatsDao.getCombatLevelByStats(dto.getStr(), dto.getAcc(), dto.getDef(), dto.getAgil(), dto.getHp(), dto.getMagic());
		
		HashMap<Stats, Integer> bonuses = new HashMap<>();
		bonuses.put(Stats.STRENGTH, dto.getStrBonus());
		bonuses.put(Stats.ACCURACY, dto.getAccBonus());
		bonuses.put(Stats.DEFENCE, dto.getDefBonus());
		bonuses.put(Stats.AGILITY, dto.getAgilBonus());
//		bonuses.put(Stats.HITPOINTS, dto.getHpBonus());
		setBonuses(bonuses);
		
		HashMap<Stats, Integer> boosts = new HashMap<>();
		boosts.put(Stats.STRENGTH, 0);
		boosts.put(Stats.ACCURACY, 0);
		boosts.put(Stats.DEFENCE, 0);
		boosts.put(Stats.AGILITY, 0);
		boosts.put(Stats.MAGIC, 0);
		setBoosts(boosts);
		
		setCurrentHp(dto.getHp());
		setMaxCooldown(dto.getAttackSpeed());
		
		huntTimer = RandomUtil.getRandom(0, MAX_HUNT_TIMER);// just so all the NPCs aren't hunting on the same tick
	}
	
	public void process(ResponseMaps responseMaps) {
		if (currentHp == 0) {
			handleRespawn(responseMaps);			
			return;
		}
		
		processPoison(responseMaps);
		
		if ((dto.getAttributes() & NpcAttributes.AGGRESSIVE.getValue()) == NpcAttributes.AGGRESSIVE.getValue() && !isInCombat()) {
			// aggressive monster; look for targets
			if (--huntTimer <= 0) {
				List<Player> closePlayers = WorldProcessor.getPlayersNearTile(roomId, tileId, dto.getRoamRadius()/2);
				
				if (dto.getId() == 18 || dto.getId() == 22) { // goblins won't attack anyone with the goblin stank buff
					closePlayers = closePlayers.stream().filter(player -> !player.hasBuff(Buffs.GOBLIN_STANK)).collect(Collectors.toList());
				}
				
				if (target != null && !closePlayers.contains(target))
					target = null;
				
				if (target == null) {
					for (Attackable player : closePlayers) {
						int playerCombat = StatsDao.getCombatLevelByStats(player.getStats());
						if (!player.isInCombat() && playerCombat < combatLevel * 2) { 
							target = player;
							break;
						}
					}
				}
			
				huntTimer = MAX_HUNT_TIMER;
			}
		}
		
		moving = false;
		if (!path.isEmpty()) {
			tileId = path.pop();
			moving = true;
		}
		
		if (target == null) {
			if (--tickCounter < 0) {
				Random r = new Random();
				tickCounter = r.nextInt((maxTickCount - minTickCount) + 1) + minTickCount;
				
				int destTile = PathFinder.chooseRandomTileIdInRadius(dto.getTileId(), dto.getRoamRadius());
				path = PathFinder.findPath(roomId, tileId, destTile, true, dto.getTileId(), dto.getRoamRadius());
			}
		} else {
			// chase the target if not next to it
			if (!PathFinder.isNextTo(tileId, target.tileId)) {
				if (PathFinder.tileWithinRadius(target.tileId, dto.getTileId(), dto.getRoamRadius() + 2)) {
					path = PathFinder.findPath(roomId, tileId, target.tileId, true);
				} else {
					int retreatTileId = PathFinder.findRetreatTile(target.tileId, tileId, dto.getTileId(), dto.getRoamRadius());
					System.out.println("retreating to tile " + retreatTileId + "(retreating from " + target.tileId + ", currently at " + tileId + ", anchor=" + dto.getTileId() + ", radius = " + dto.getRoamRadius() + ")");
					
					path = PathFinder.findPath(roomId, tileId, retreatTileId, true);
					target = null;
				}
			} else {
				if (target.isInCombat()) {
					if (!FightManager.fightingWith(this, target)) {
						int retreatTileId = PathFinder.findRetreatTile(target.tileId, tileId, dto.getTileId(), dto.getRoamRadius());						
						path = PathFinder.findPath(roomId, tileId, retreatTileId, true);
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
		//currentHp = dto.getHp();
		deathTimer = respawnTime;
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
						
						if (GroundItemManager.itemIsOnGround(playerId, dto.getItemId()))
							return false;
					}
					return true;
				})
				.collect(Collectors.toList());
		
		for (NpcDropDto dto : potentialDrops) {
			if (RandomUtil.getRandom(0, dto.getRate()) == 0) {
				GroundItemManager.add(((Player)killer).getId(), dto.getItemId(), tileId, dto.getCount(), ItemDao.getMaxCharges(dto.getItemId()));
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
		updateResponse.setDamage(damage);
		updateResponse.setDamageType(type.getValue());
		updateResponse.setHp(currentHp);
		responseMaps.addLocalResponse(roomId, tileId, updateResponse);
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
	
	private void handleRespawn(ResponseMaps responseMaps) {
		if (--deathTimer <= 0) {
			deathTimer = 0;
			currentHp = dto.getHp();
			tileId = dto.getTileId();
			
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(getInstanceId());
			updateResponse.setHp(currentHp);
			updateResponse.setTileId(tileId);
			responseMaps.addLocalResponse(roomId, tileId, updateResponse);
		}
	}
	
	public void clearPath() {
		path.clear();
	}
}
