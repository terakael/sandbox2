package main.processing;

import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

import lombok.Getter;
import main.database.NPCDto;
import main.processing.Player.PlayerState;
import main.responses.NpcUpdateResponse;
import main.responses.ResponseMaps;

public class NPC extends Attackable {
	@Getter private NPCDto dto;
	
	private Stack<Integer> path = new Stack<>();
	
	private final transient int maxTickCount = 15;
	private final transient int minTickCount = 5;
	private transient int tickCounter = maxTickCount;
	
	public NPC(NPCDto dto) {
		this.dto = dto;
		tileId = dto.getTileId();
		
		HashMap<String, Integer> stats = new HashMap<>();
		stats.put("strength", dto.getStr());
		stats.put("accuracy", dto.getAcc());
		stats.put("defence", dto.getDef());
		stats.put("agility", dto.getAgil());
		stats.put("hitpoints", dto.getHp());
//		stats.put("magic", dto.getMagic());
		setStats(stats);
		
		HashMap<String, Integer> bonuses = new HashMap<>();
		bonuses.put("strength", dto.getStrBonus());
		bonuses.put("accuracy", dto.getAccBonus());
		bonuses.put("defence", dto.getDefBonus());
		stats.put("agility", dto.getAgilBonus());
//		stats.put("hitpoints", dto.getHp());
		setBonuses(bonuses);
		
		setCurrentHp(dto.getHp());
	}
	
	public void process(ResponseMaps responseMaps) {
		if (!path.isEmpty()) {
			tileId = path.pop();
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(dto.getTileId());
			updateResponse.setTileId(tileId);
			responseMaps.addBroadcastResponse(updateResponse);
		}
		
		if (target == null) {
			if (--tickCounter < 0) {
				Random r = new Random();
				tickCounter = r.nextInt((maxTickCount - minTickCount) + 1) + minTickCount;
				
				int destTile = PathFinder.chooseRandomTileIdInRadius(tileId, 3);
				path = PathFinder.findPath(tileId, destTile, true);
			}
		} else {
			// chase the target if not next to it
			if (!PathFinder.isNextTo(tileId, target.tileId)) {
				path = PathFinder.findPath(tileId, target.tileId, true);
			}
		}
	}
	
	public int getId() {
		return dto.getTileId();// the spawn tileId is used for the id
	}
	
	@Override
	public void onDeath(ResponseMaps responseMaps) {
		// TODO send npc_update broadcast
		currentHp = dto.getHp();
		
		// also drop an item
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
		responseMaps.addBroadcastResponse(updateResponse);
	}
	
	@Override
	public void setStatsAndBonuses() {
		// already set on instantiation
	}
}
