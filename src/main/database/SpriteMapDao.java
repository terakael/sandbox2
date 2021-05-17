package main.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public class SpriteMapDao {
	private SpriteMapDao() {}
	
	private static Map<Integer, SpriteMapDto> spriteMaps = null;
	
	public static void setupCaches() throws IOException {
		cacheSpriteMaps();
	}
	
	public static List<SpriteMapDto> getSpriteMaps() {
		return new ArrayList<>(spriteMaps.values());
	}
	
	private static void cacheSpriteMaps() throws IOException {
		final String query = "select id, filename from sprite_maps where id > 0";
		
		spriteMaps = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			
			while (rs.next()) {
				String encodedString = "";
				final String filename = rs.getString("filename"); 
				
				if (!filename.isEmpty()) {
					byte[] data = IOUtils.toByteArray(classloader.getResourceAsStream("spritemaps/" + filename));
					encodedString = Base64.getEncoder().encodeToString(data);
				}
				spriteMaps.put(rs.getInt("id"), new SpriteMapDto(rs.getInt("id"), encodedString));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
