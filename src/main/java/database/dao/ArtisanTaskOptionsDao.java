package database.dao;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.ArtisanTaskOptionsDto;

public class ArtisanTaskOptionsDao {
	private static Map<Integer, ArtisanTaskOptionsDto> taskOptions = new LinkedHashMap<>(); // id, dto
	
	public static void setupCaches() {
		DbConnection.load("select * from artisan_task_options", rs -> {
			taskOptions.put(rs.getInt("id"), new ArtisanTaskOptionsDto(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getString("description"),
						rs.getInt("icon_id"),
						rs.getInt("num_points")
					));
		});
	}
	
	public static Set<ArtisanTaskOptionsDto> getTaskOptions() {
		return new LinkedHashSet<>(taskOptions.values());
	}
}
