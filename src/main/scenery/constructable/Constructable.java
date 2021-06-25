package main.scenery.constructable;

import lombok.Getter;
import main.database.dao.PlayerStorageDao;
import main.database.dto.ConstructableDto;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.responses.AddExpResponse;
import main.responses.InventoryUpdateResponse;
import main.responses.ResponseMaps;
import main.types.Stats;
import main.types.StorageTypes;

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
