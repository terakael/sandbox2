package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import main.database.DbConnection;
import main.database.dto.ContextOptionsDto;

public class ContextOptionsDao {
	@Getter private static List<ContextOptionsDto> itemContextOptions;
	@Getter private static List<ContextOptionsDto> npcContextOptions;
	@Getter private static List<ContextOptionsDto> sceneryContextOptions;
	
	private ContextOptionsDao() {};
	
	public static void setupCaches() {
		itemContextOptions = cacheContextOptions("item");
		npcContextOptions = cacheContextOptions("npc");
		sceneryContextOptions = cacheContextOptions("scenery");
	}
	
	public static List<ContextOptionsDto> cacheContextOptions(String category) {
		List<ContextOptionsDto> list = new ArrayList<>();
		final String query = String.format("select id, name from %s_context_options", category);
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(new ContextOptionsDto(rs.getInt("id"), rs.getString("name")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
}
