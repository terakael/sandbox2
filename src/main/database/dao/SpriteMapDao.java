package main.database.dao;

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

import main.database.DbConnection;
import main.database.dto.SpriteMapDto;

public class SpriteMapDao {
	private SpriteMapDao() {}
	
	private static Map<Integer, SpriteMapDto> spriteMaps = null;
	private static List<SpriteMapDto> alwaysLoadedSpriteMaps = null;
	
	public static void setupCaches() throws IOException {
		cacheSpriteMaps();
	}
	
	public static List<SpriteMapDto> getSpriteMaps() {
		return new ArrayList<>(spriteMaps.values());
	}
	
	public static SpriteMapDto getSpriteMap(int spriteMapId) {
		return spriteMaps.get(spriteMapId);
	}
	
	public static List<SpriteMapDto> getAlwaysLoadedSpriteMaps() {
		return alwaysLoadedSpriteMaps;
	}
	
	private static void cacheSpriteMaps() throws IOException {
		final String query = "select id, filename, always_load from sprite_maps where id > 0";
		
		spriteMaps = new HashMap<>();
		alwaysLoadedSpriteMaps = new ArrayList<>();
		
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
				
				SpriteMapDto dto = new SpriteMapDto(rs.getInt("id"), encodedString);
				spriteMaps.put(rs.getInt("id"), dto);
				
				if (rs.getInt("always_load") == 1)
					alwaysLoadedSpriteMaps.add(dto);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
