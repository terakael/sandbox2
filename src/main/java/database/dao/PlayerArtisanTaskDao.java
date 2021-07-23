package database.dao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import database.DbConnection;
import database.dto.PlayerArtisanTaskDto;

public class PlayerArtisanTaskDao {
	private static final int progressElementSize = 10; // db has progress0 - progress9 columns
	private static final Map<Integer, PlayerArtisanTaskDto> playerArtisanTasks = new HashMap<>();
	
	public static void setupCaches() {
		// why? because i can
		final String query = String.format("select player_id, item_id, %s from player_artisan_tasks", IntStream.range(0, progressElementSize)
				.boxed()
				.map(e -> "progress" + e)
				.collect(Collectors.joining(",")));
		
		DbConnection.load(query, rs -> {
			final List<Integer> progress = Arrays.asList(new Integer[progressElementSize]);
			for (int i = 0; i < progressElementSize; ++i)
				progress.set(i, rs.getInt("progress" + i));
			
			PlayerArtisanTaskDto dto = new PlayerArtisanTaskDto(rs.getInt("player_id"), rs.getInt("item_id"), progress);
			playerArtisanTasks.put(rs.getInt("player_id"), dto);
		});
	}
}
