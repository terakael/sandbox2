package main.processing;

import java.util.Random;
import java.util.Stack;

import lombok.Getter;
import main.database.NPCDto;
import main.responses.NpcUpdateResponse;
import main.responses.ResponseMaps;

public class NPC {
	@Getter private NPCDto dto;
	private int currentHp = 0;
	@Getter private int tileId = 0;
	
	private Stack<Integer> path = new Stack<>();
	
	private final transient int maxTickCount = 15;
	private final transient int minTickCount = 5;
	private transient int tickCounter = maxTickCount;
	
	public NPC(NPCDto dto) {
		this.dto = dto;
		this.currentHp = dto.getHp();
		this.tileId = dto.getTileId();
	}
	
	public void process(ResponseMaps responseMaps) {
		if (!path.isEmpty()) {
			tileId = path.pop();
			NpcUpdateResponse updateResponse = new NpcUpdateResponse();
			updateResponse.setInstanceId(dto.getTileId());
			updateResponse.setTileId(tileId);
			responseMaps.addBroadcastResponse(updateResponse);
		}
		
		if (--tickCounter < 0) {
			Random r = new Random();
			tickCounter = r.nextInt((maxTickCount - minTickCount) + 1) + minTickCount;
			
			int destTile = PathFinder.chooseRandomTileIdInRadius(this.tileId, 3);
			path = PathFinder.findPath(this.tileId, destTile, true);
		}
	}
}
