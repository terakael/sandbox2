package main.database;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

public class SpriteMapDao {
	private SpriteMapDao() {}
	
	private static Map<Integer, SpriteMapDto> spriteMaps = null;
	
	private static int nextFreeSpriteMapId = -1;
	
	
	public static void setupCaches() throws IOException {
		cacheNextFreeSpriteMapId();
		cacheSpriteMaps();
	}
	
	public static List<SpriteMapDto> getSpriteMaps() {
		return new ArrayList<>(spriteMaps.values());
	}
	
	public static int addSpriteMap(String name, String base64) {
		spriteMaps.put(nextFreeSpriteMapId, new SpriteMapDto(nextFreeSpriteMapId, name, base64));
		
		return nextFreeSpriteMapId++;
	}
	
	public static SpriteMapDto getSpriteMapDataById(int id) {
		final String query = "select id, name, to_base64(data) data from sprite_maps where id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query); 
		) {
			ps.setInt(1,  id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new SpriteMapDto(rs.getInt("id"), rs.getString("name"), rs.getString("data"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String tintImage(String base64, int color)  {
		try {
			byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
			BufferedImage loadImg = ImageIO.read(new ByteArrayInputStream(bytes));
			
			BufferedImage img = new BufferedImage(loadImg.getWidth(), loadImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D graphics = img.createGraphics();
			graphics.drawImage(loadImg, null, 0, 0);
			graphics.setComposite(AlphaComposite.SrcAtop);
			
			Color c = new Color(color);
			graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
			graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
			
			graphics.dispose();
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(img, "png", os);

			return Base64.getEncoder().encodeToString(os.toByteArray());
		} catch (IOException e) {
			
		}
		
		return "";
	}
	
	private static void cacheSpriteMaps() {
		final String query = "select id, name, to_base64(data) data from sprite_maps";
		
		spriteMaps = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			while (rs.next())
				spriteMaps.put(rs.getInt("id"), new SpriteMapDto(rs.getInt("id"), rs.getString("name"), rs.getString("data").replace("\n", "")));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static InputStream getSpriteMapImageBinaryStreamById(int id) {
		final String query = "select data from sprite_maps where id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Blob blob = rs.getBlob("data");
					return blob.getBinaryStream();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void cacheNextFreeSpriteMapId() {
		String query = "select max(id) id from sprite_maps";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query); 
		) {
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					nextFreeSpriteMapId = rs.getInt("id") + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
