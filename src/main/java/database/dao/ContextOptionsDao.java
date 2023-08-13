package database.dao;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import database.DbConnection;
import database.dto.ContextOptionsDto;

public class ContextOptionsDao {
	@Getter private static List<ContextOptionsDto> itemContextOptions;
	@Getter private static List<ContextOptionsDto> npcContextOptions;
	@Getter private static List<ContextOptionsDto> sceneryContextOptions;
	@Getter private static List<ContextOptionsDto> shipContextOptions;
	
	private ContextOptionsDao() {};
	
	public static void setupCaches() {
		itemContextOptions = cacheContextOptions("item");
		npcContextOptions = cacheContextOptions("npc");
		sceneryContextOptions = cacheContextOptions("scenery");
		shipContextOptions = cacheContextOptions("ship");
	}
	
	public static List<ContextOptionsDto> cacheContextOptions(String category) {
		List<ContextOptionsDto> list = new ArrayList<>();
		DbConnection.load(String.format("select id, name from %s_context_options", category), 
				rs -> list.add(new ContextOptionsDto(rs.getInt("id"), rs.getString("name"))));
		return list;
	}
}
