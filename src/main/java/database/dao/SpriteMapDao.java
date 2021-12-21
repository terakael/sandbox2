package database.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import database.DbConnection;
import database.dto.SpriteMapDto;

public class SpriteMapDao {
	private SpriteMapDao() {}
	
	private static Map<Integer, SpriteMapDto> spriteMaps = new HashMap<>();
	private static List<SpriteMapDto> alwaysLoadedSpriteMaps = new ArrayList<>();
	
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
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		DbConnection.load(query, rs -> {
			String encodedString = "";
			final String filename = rs.getString("filename"); 
			
			if (!filename.isEmpty()) {
				try {
					byte[] data = IOUtils.toByteArray(classloader.getResourceAsStream("spritemaps/" + filename));
					encodedString = Base64.getEncoder().encodeToString(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			SpriteMapDto dto = new SpriteMapDto(rs.getInt("id"), encodedString);
			spriteMaps.put(rs.getInt("id"), dto);
			
			if (rs.getInt("always_load") == 1)
				alwaysLoadedSpriteMaps.add(dto);
		});
	}
}
