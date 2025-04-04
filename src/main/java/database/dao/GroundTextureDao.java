package database.dao;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.tuple.Pair;

import database.DbConnection;
import database.dto.GroundTextureDto;
import database.entity.delete.DeleteRoomGroundTextureEntity;
import database.entity.insert.InsertRoomGroundTextureEntity;
import lombok.Getter;
import processing.PathFinder;
import processing.managers.DatabaseUpdater;

public class GroundTextureDao {
	@Getter
	private static List<GroundTextureDto> groundTextures = new ArrayList<>();
	@Getter
	private static HashSet<Integer> distinctFloors = new HashSet<>();

	private static Map<Integer, Map<Integer, Set<Integer>>> tileIdsByGroundTextureId = new HashMap<>(); // floor,
																										// <groundTextureId,
																										// tileId>
	private static Map<Integer, Integer> spriteMapIdToTextureId = new HashMap<>(); // sprite_map_id, generated textureId
	private static Map<Integer, Boolean> walkableTextures = new HashMap<>();
	private static Map<Integer, Set<Integer>> customTiles = new HashMap<>();

	public static void setupCaches() {
		cacheDistinctFloors();
		generateGroundTextures();
		cacheTileIdsByGroundTextureId();
	}

	private static void generateGroundTextures() {
		// instead of having groundTextureIds stored in the database as we used to,
		// we now generate the groundTextureIds programmatically on startup.
		// steps are as follows:
		// - pull distinct fg/bg pairs from room_ground_textures
		// - for each pair, generate the 32x32 textures and store the base64

		Map<Integer, Integer> templates = new HashMap<>(); // foregroundId, templateId
		DbConnection.load("select sprite_map_id, template_id, walkable from ground_textures", rs -> {
			templates.put(rs.getInt("sprite_map_id"), rs.getInt("template_id"));
			walkableTextures.put(rs.getInt("sprite_map_id"), rs.getBoolean("walkable"));
		});

		Set<Pair<Integer, Integer>> texturePairs = new HashSet<>();
		DbConnection.load("SELECT DISTINCT foreground, background\n" +
				"FROM (\n" +
				"    SELECT foreground, background FROM room_ground_textures\n" +
				"    UNION ALL\n" + //
				"    SELECT foreground, background FROM custom_room_ground_textures\n" +
				") AS combined_textures;", rs -> {
					// generatedId = (fg * 1000000) + (bg * 1000) + template_tile_id
					// - 2147 possible foregrounds
					// - each foreground can have 1000 different backgrounds
					// - template allows up to 1000 tiles
					// however, because sprite_map_id has so many other sprite maps, we need to map
					// the texture maps from 0.
					texturePairs.add(Pair.of(rs.getInt("foreground"), rs.getInt("background")));
				});

		Set<Integer> distinctTextureIds = new HashSet<>();
		distinctTextureIds.addAll(texturePairs.stream().map(Pair::getLeft).collect(Collectors.toSet()));
		distinctTextureIds.addAll(texturePairs.stream().map(Pair::getRight).collect(Collectors.toSet()));

		int generatedKey = 0;
		for (int textureId : distinctTextureIds)
			spriteMapIdToTextureId.put(textureId, ++generatedKey);

		texturePairs.forEach(pair -> {
			int fgId = pair.getLeft();
			int bgId = pair.getRight();
			int templateId = templates.get(pair.getLeft());
			loadTextureMap(pair.getLeft(), pair.getRight(), templates.get(pair.getLeft()));
		});
	}

	private static void loadTextureMap(int foregroundId, int backgroundId, int templateId) {
		try {
			final String backgroundb64 = SpriteMapDao.getSpriteMap(backgroundId).getDataBase64();
			if (foregroundId == backgroundId) {
				groundTextures.add(new GroundTextureDto(generateTextureId(backgroundId, foregroundId, 0), backgroundb64,
						walkableTextures.get(backgroundId)));
				return;
			}

			BufferedImage template = ImageIO.read(new ByteArrayInputStream(
					Base64.getDecoder().decode(SpriteMapDao.getSpriteMap(templateId).getDataBase64())));
			BufferedImage foreground = ImageIO.read(new ByteArrayInputStream(
					Base64.getDecoder().decode(SpriteMapDao.getSpriteMap(foregroundId).getDataBase64())));
			BufferedImage background = ImageIO
					.read(new ByteArrayInputStream(Base64.getDecoder().decode(backgroundb64)));

			final int width = template.getWidth();
			final int tileWidth = 5;

			final int height = template.getHeight();
			final int tileHeight = 7;

			final int fgColor = 0xff000000 | 0xffffff;
			final int bgColor = 0xff000000 | 0x000000;

			BufferedImage[] textureMaps = new BufferedImage[tileWidth * tileHeight];
			for (int i = 0; i < tileWidth * tileHeight; ++i)
				textureMaps[i] = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					final int color = 0xff000000 | template.getRGB(x, y);

					final BufferedImage textureMap = textureMaps[((y / 32) * tileWidth) + (x / 32)];

					if (color == fgColor)
						textureMap.setRGB(x % 32, y % 32, foreground.getRGB(x % 32, y % 32));
					else if (color == bgColor)
						textureMap.setRGB(x % 32, y % 32, background.getRGB(x % 32, y % 32));
					else
						textureMap.setRGB(x % 32, y % 32, template.getRGB(x, y));
				}
			}

			for (int i = 0; i < tileWidth * tileHeight; ++i) {
				final ByteArrayOutputStream os = new ByteArrayOutputStream();

				try {
					ImageIO.write(textureMaps[i], "png", os);
					final String b64 = Base64.getEncoder().encodeToString(os.toByteArray());

					// basic rules for figuring out walkable:
					// - background/foreground walkable: true
					// - background/foreground non-walkable: false
					// - background walkable, foreground non-walkable: 23, 28 true, else false
					// - background non-walkable, foreground walkable: 23, 28 false, else true
					final boolean backgroundIsWalkable = walkableTextures.get(backgroundId);
					final boolean foregroundIsWalkable = walkableTextures.get(foregroundId);
					boolean isWalkable;
					if (backgroundIsWalkable && !foregroundIsWalkable)
						isWalkable = i + 1 == 23 || i + 1 == 28;
					else if (!backgroundIsWalkable && foregroundIsWalkable)
						isWalkable = i + 1 != 23 && i + 1 != 28;
					else
						isWalkable = backgroundIsWalkable;
					groundTextures.add(new GroundTextureDto(generateTextureId(backgroundId, foregroundId, i + 1), b64,
							isWalkable));
				} catch (final IOException ioe) {
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int generateTextureId(int backgroundId, int foregroundId, int templateTileId) {
		return spriteMapIdToTextureId.get(foregroundId) * 1000000
				+ spriteMapIdToTextureId.get(backgroundId) * 1000
				+ templateTileId;
	}

	private static void cacheTileIdsByGroundTextureId() {
		String customTextureQuery = "SELECT ca.floor + crgt.offset_floor AS floor, " +
				" ca.tile_id + (" + PathFinder.LENGTH + " * crgt.offset_y) + crgt.offset_x AS tile_id, " +
				" background, foreground, template_tile_id " +
				" FROM custom_room_ground_textures crgt " +
				" JOIN custom_area ca ON crgt.custom_area_id = ca.id;";

		DbConnection.load(customTextureQuery, rs -> {
			final int floor = rs.getInt("floor");
			final int groundTextureId = generateTextureId(rs.getInt("background"), rs.getInt("foreground"),
					rs.getInt("template_tile_id"));
			final int tileId = rs.getInt("tile_id");

			tileIdsByGroundTextureId.putIfAbsent(floor, new HashMap<>());
			tileIdsByGroundTextureId.get(floor).putIfAbsent(groundTextureId, new HashSet<>());
			tileIdsByGroundTextureId.get(floor).get(groundTextureId).add(tileId);

			// record which ones are custom so we don't load them in the standard
			// room_ground_textures part
			customTiles.putIfAbsent(floor, new HashSet<>());
			customTiles.get(floor).add(tileId);
		});

		String query = "select floor, tile_id, background, foreground, template_tile_id from room_ground_textures";

		DbConnection.load(query, rs -> {
			final int floor = rs.getInt("floor");
			final int tileId = rs.getInt("tile_id");

			if (!hasCustomTile(floor, tileId)) {
				final int groundTextureId = generateTextureId(rs.getInt("background"), rs.getInt("foreground"),
						rs.getInt("template_tile_id"));

				tileIdsByGroundTextureId.putIfAbsent(floor, new HashMap<>());
				tileIdsByGroundTextureId.get(floor).putIfAbsent(groundTextureId, new HashSet<>());
				tileIdsByGroundTextureId.get(floor).get(groundTextureId).add(tileId);
			}
		});

	}

	public static Integer getGroundTextureIdByTileId(int floor, int tileId) {
		if (!tileIdsByGroundTextureId.containsKey(floor))
			return 0;

		for (Map.Entry<Integer, Set<Integer>> entry : tileIdsByGroundTextureId.get(floor).entrySet()) {
			if (entry.getValue().contains(tileId))
				return entry.getKey();
		}

		return 0;
	}

	private static void cacheDistinctFloors() {
		DbConnection.load("select distinct floor from room_ground_textures",
				rs -> distinctFloors.add(rs.getInt("floor")));

		DbConnection.load("SELECT DISTINCT offset_floor + ca.floor AS floor " +
				"FROM custom_room_ground_textures crgt " +
				"JOIN custom_area ca ON crgt.custom_area_id = ca.id;",
				rs -> distinctFloors.add(rs.getInt("floor")));
	}

	public static Set<Integer> getAllWalkableTileIdsByFloor(int floor) {
		return getAllTileIdsByFloorAndWalkable(floor, true);
	}

	public static Set<Integer> getAllSailableTileIdsByFloor(int floor) {
		return getAllTileIdsByFloorAndWalkable(floor, false);
	}

	private static Set<Integer> getAllTileIdsByFloorAndWalkable(int floor, boolean walkable) {
		Set<Integer> allTileIdsByFloor = new HashSet<>();

		Set<Integer> walkableTextureIds = groundTextures.stream()
				.filter(e -> e.isWalkable() == walkable)
				.map(GroundTextureDto::getId)
				.collect(Collectors.toSet());

		tileIdsByGroundTextureId.get(floor)
				.forEach((groundTextureId, tileIds) -> {
					if (walkableTextureIds.contains(groundTextureId))
						allTileIdsByFloor.addAll(tileIds);
				});

		return allTileIdsByFloor;
	}

	public static void upsertGroundTexture(int floor, int tileId, int groundTextureId) {
		distinctFloors.add(floor);
		tileIdsByGroundTextureId.putIfAbsent(floor, new HashMap<>());

		deleteGroundTexture(floor, tileId);
		DatabaseUpdater.enqueue(new InsertRoomGroundTextureEntity(floor, tileId, groundTextureId));

		tileIdsByGroundTextureId.get(floor).putIfAbsent(groundTextureId, new HashSet<>());
		tileIdsByGroundTextureId.get(floor).get(groundTextureId).add(tileId);
	}

	public static boolean deleteGroundTexture(int floor, int tileId) {
		final Set<Integer> containedTexture = tileIdsByGroundTextureId.get(floor).values().stream()
				.filter(e -> e.contains(tileId))
				.findFirst()
				.orElse(null);

		if (containedTexture != null) {
			if (containedTexture.remove(tileId)) {
				DatabaseUpdater.enqueue(new DeleteRoomGroundTextureEntity(floor, tileId, null));
				return true;
			}
		}
		return false;
	}

	public static boolean hasCustomTile(int floor, int tileId) {
		return customTiles.containsKey(floor) && customTiles.get(floor).contains(tileId);
	}
}
