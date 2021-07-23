package processing.scenery.constructable;

import lombok.Getter;
import database.dto.ConstructableDto;
import responses.ResponseMaps;

public class Constructable {
	@Getter protected ConstructableDto dto;
	@Getter protected int remainingTicks;
	protected int floor;
	protected int tileId;
	
	public Constructable(int floor, int tileId, ConstructableDto dto) {
		remainingTicks = dto.getLifetimeTicks();
		this.dto = dto;
		this.floor = floor;
		this.tileId = tileId;
	}
	
	public final void process(int tickId, ResponseMaps responseMaps) {
		--remainingTicks;
		
		if (remainingTicks > 0) {
			processConstructable(tickId, responseMaps);
		} else {
			onDestroy(responseMaps);
		}
	}
	
	public void processConstructable(int tickId, ResponseMaps responseMaps) {
		
	}
	
	public void onDestroy(ResponseMaps responseMaps) {
		
	}
	
	public void repair() {
		remainingTicks = dto.getLifetimeTicks();
	}
}
