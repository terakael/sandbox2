package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class PlayerArtisanTaskDto {
	private int playerId;
	private int itemId;
	@Setter private int amount;
}
