package main.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import main.responses.DropResponse;
import main.responses.NpcUpdateResponse;
import main.responses.ResponseMaps;
import main.types.ItemAttributes;
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
	
	private int combatLevel = 0;
	
	public NPC(NPCDto dto) {
		this.dto = dto;
		tileId = dto.getTileId();
		
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
		
		setCurrentHp(dto.getHp());
		setMaxCooldown(dto.getAttackSpeed());
	}
	
	public void process(ResponseMaps responseMaps) {
		if (currentHp == 0) {
			if (--deathTimer <= 0) {
				deathTimer = 0;
				currentHp = dto.getHp();
				tileId = dto.getTileId();
				
				NpcUpdateResponse updateResponse = new NpcUpdateResponse();
				updateResponse.setNpc(this);
				responseMaps.addLocalResponse(tileId, updateResponse);
			}
			
			return;
		}
		
		if (!path.isEmpty())
			tileId = path.pop();
		
		if (target == null) {
			if (--tickCounter < 0) {
				Random r = new Random();
				tickCounter = r.nextInt((maxTickCount - minTickCount) + 1) + minTickCount;
				
				int destTile = PathFinder.chooseRandomTileIdInRadius(dto.getTileId(), dto.getRoamRadius());
				path = PathFinder.findPath(tileId, destTile, true);
			}
		} else {
			// chase the target if not next to it
			if (!PathFinder.isNextTo(tileId, target.tileId)) {
				path = PathFinder.findPath(tileId, target.tileId, true);
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
			if (RandomUtil.getRandom(0, dto.getRate()) == 0)
				GroundItemManager.add(((Player)killer).getId(), dto.getItemId(), tileId, dto.getCount());
		}
	}
	
	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		target = null;
	}
	
	@Override
	public void onHit(int damage, ResponseMaps responseMaps) {
		currentHp -= damage;
		if (currentHp < 0)
			currentHp = 0;
		
		NpcUpdateResponse updateResponse = new NpcUpdateResponse();
		updateResponse.setInstanceId(dto.getTileId());
		updateResponse.setDamage(damage);
		updateResponse.setHp(currentHp);
		responseMaps.addLocalResponse(tileId, updateResponse);
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
}
