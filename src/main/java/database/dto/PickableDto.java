package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import types.SceneryAttributes;

@Getter
@AllArgsConstructor
public class PickableDto {
	private int sceneryId;
	private int itemId;
	private int respawnTicks;
	private int sceneryAttributes;
	
	public boolean isNocturnal() {
		return (sceneryAttributes & SceneryAttributes.NOCTURNAL.getValue()) > 0;
	}
	
	public boolean isDiurnal() {
		return (sceneryAttributes & SceneryAttributes.DIURNAL.getValue()) > 0;
	}
}
