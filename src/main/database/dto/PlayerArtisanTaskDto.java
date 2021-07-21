package main.database.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PlayerArtisanTaskDto {
	@Getter private int playerId;
	@Getter private int itemId;
	private List<Integer> progress;
	
	public int getProgress(int progressId) {
		if (progressId < 0 || progressId >= progress.size())
			return -1;
		return progress.get(progressId);
	}
}
