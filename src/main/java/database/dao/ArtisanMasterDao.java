package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.dto.ArtisanMasterDto;

public class ArtisanMasterDao {
	private static final Map<Integer, ArtisanMasterDto> artisanMasters = new HashMap<>(); // npcId, dto
	
	public static void setupCaches() {
		DbConnection.load("select npc_id, artisan_requirement, assignment_level_range_min, assignment_level_range_max from artisan_masters", rs -> {
			artisanMasters.put(rs.getInt("npc_id"), new ArtisanMasterDto(
							rs.getInt("npc_id"), 
							rs.getInt("artisan_requirement"), 
							rs.getInt("assignment_level_range_min"), 
							rs.getInt("assignment_level_range_max")));
		});
	}
	
	public static ArtisanMasterDto getArtisanMasterByNpcId(int npcId) {
		return artisanMasters.get(npcId);
	}
	
	public static boolean npcIsArtisanMaster(int npcId) {
		return getArtisanMasterByNpcId(npcId) != null;
	}
}
