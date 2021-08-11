package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlayerArtisanBlockedTaskDto {
	private int playerId;
	private int item1;
	private int item2;
	private int item3;
	private int item4;
	private int item5;
}
