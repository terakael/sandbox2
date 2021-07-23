package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class PlayerTybaltsTaskDto {
	private int playerId;
	@Setter private int taskId;
	@Setter private int progress1;
	@Setter private int progress2;
	@Setter private int progress3;
	@Setter private int progress4;
}
