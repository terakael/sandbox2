package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UndeadArmyWavesDto {
	private int wave;
	private int npcId;
	private int tileId;
}
