package database.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import database.DbConnection;
import processing.PathFinder;

public class MinimapSegmentDao {
	private static final int tilesPerSegment = 25;
	private static final int segmentsPerRow = PathFinder.LENGTH / tilesPerSegment;
	private static Map<Integer, Map<Integer, String>> minimapSegments; // floor, <segment, base64>

	private static Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> tileIdsByFloorAndSegmentAndSpriteFrameId; // floor,
																													// <segment,
																													// <iconId,
																													// <tileIds>>>

	public static void setupCaches() throws FileNotFoundException, IOException, URISyntaxException {
		cacheMinimapSegments();
		cacheMinimapIconLocations();
	}

	private static void cacheMinimapSegments_() throws FileNotFoundException, IOException {
		minimapSegments = new HashMap<>();

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL url = loader.getResource("minimap");
		String path = url.getPath();
		File[] files = new File(path).listFiles();
		for (int i = 0; i < files.length; ++i) {
			int floor = Integer.parseInt(files[i].getName());
			if (!minimapSegments.containsKey(floor))
				minimapSegments.put(floor, new HashMap<>());
			File[] floorFiles = new File(files[i].getPath()).listFiles();
			for (int j = 0; j < floorFiles.length; ++j) {
				int segmentId = Integer.parseInt(floorFiles[j].getName());

				byte[] data = IOUtils.toByteArray(new FileInputStream(floorFiles[j]));
				minimapSegments.get(floor).put(segmentId, Base64.getEncoder().encodeToString(data));
			}
		}
	}

	public static void cacheMinimapSegments() throws IOException, URISyntaxException {
		minimapSegments = new HashMap<>();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL resourceUrl = loader.getResource("minimap");

		if (resourceUrl == null) {
			throw new FileNotFoundException("Resource directory 'minimap' not found on classpath.");
		}

		URI uri = resourceUrl.toURI();
		Path resourcePath;

		if (uri.getScheme().equals("jar")) {
			// Use try-with-resources to ensure the FileSystem is closed if we create it
			try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
				resourcePath = fileSystem.getPath("/minimap"); // Path inside the JAR
				processMinimapPath(resourcePath);
			} catch (FileSystemAlreadyExistsException e) {
				// If it already exists, just get it and use it (don't close it here)
				try (FileSystem fileSystem = FileSystems.getFileSystem(uri)) {
					resourcePath = fileSystem.getPath("/minimap");
					processMinimapPath(resourcePath);
				}
			}
		} else {
			// Running from IDE or unpacked directory
			resourcePath = Paths.get(uri);
			processMinimapPath(resourcePath);
		}
	}

	// Helper method to avoid code duplication
	private static void processMinimapPath(Path resourcePath) throws IOException {
		// --- Consistent listing logic using Path ---
		try (Stream<Path> floorDirs = Files.list(resourcePath)) {
			floorDirs.filter(Files::isDirectory).forEach(floorPath -> {
				try {
					int floor = Integer.parseInt(floorPath.getFileName().toString());
					minimapSegments.putIfAbsent(floor, new HashMap<>());

					try (Stream<Path> segmentFiles = Files.list(floorPath)) {
						segmentFiles.filter(Files::isRegularFile).forEach(segmentPath -> {
							try {
								int segmentId = Integer.parseInt(segmentPath.getFileName().toString());

								// --- Consistent reading logic ---
								byte[] data = Files.readAllBytes(segmentPath);
								// Or using InputStream if preferred
								// byte[] data;
								// try (InputStream is = Files.newInputStream(segmentPath)) {
								// data = IOUtils.toByteArray(is); // Make sure IOUtils is available
								// }

								minimapSegments.get(floor).put(segmentId, Base64.getEncoder().encodeToString(data));

							} catch (NumberFormatException e) {
								System.err.println(
										"Skipping non-numeric segment file name: " + segmentPath.getFileName());
							} catch (IOException e) {
								System.err.println("Error reading segment file " + segmentPath + ": " + e.getMessage());
							}
						});
					} // segmentFiles stream is auto-closed

				} catch (NumberFormatException e) {
					System.err.println("Skipping non-numeric floor directory name: " + floorPath.getFileName());
				} catch (IOException e) {
					System.err
							.println("Error listing segments in floor directory " + floorPath + ": " + e.getMessage());
				}
			});
		} // floorDirs stream is auto-closed
	}

	private static void cacheMinimapIconLocations() {
		tileIdsByFloorAndSegmentAndSpriteFrameId = new HashMap<>();
		Map<Integer, Integer> minimapIcons = loadMinimapIcons();

		for (int floor : GroundTextureDao.getDistinctFloors()) {
			tileIdsByFloorAndSegmentAndSpriteFrameId.put(floor, new HashMap<>());

			for (Map.Entry<Integer, Integer> entry : minimapIcons.entrySet()) {
				Set<Integer> tileIds = SceneryDao.getInstanceListByFloorAndSceneryId(floor, entry.getKey());
				for (int tileId : tileIds) {
					int segmentId = getSegmentIdFromTileId(tileId);
					if (!tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).containsKey(segmentId))
						tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).put(segmentId, new HashMap<>());

					if (!tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId)
							.containsKey(entry.getValue()))
						tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId).put(entry.getValue(),
								new HashSet<>());

					tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId).get(entry.getValue())
							.add(tileId);
				}
			}
		}
	}

	private static Map<Integer, Integer> loadMinimapIcons() {
		Map<Integer, Integer> sceneryIdIconId = new HashMap<>();
		DbConnection.load("select scenery_id, sprite_frame_id from minimap_icons",
				rs -> sceneryIdIconId.put(rs.getInt("scenery_id"), rs.getInt("sprite_frame_id")));
		return sceneryIdIconId;
	}

	public static String getMinimapSegmentDataByTileId(int floor, int tileId) {
		return getMinimapDataByFloorAndSegmentId(floor, getSegmentIdFromTileId(tileId));
	}

	public static String getMinimapDataByFloorAndSegmentId(int floor, int segmentId) {
		if (!minimapSegments.containsKey(floor))
			return null;
		return minimapSegments.get(floor).get(segmentId);
	}

	public static int getSegmentIdFromTileId(int tileId) {
		int tileX = tileId % PathFinder.LENGTH;
		int tileY = tileId / PathFinder.LENGTH;
		int segmentX = tileX / tilesPerSegment;
		int segmentY = tileY / tilesPerSegment;
		return (segmentY * segmentsPerRow) + segmentX;
	}

	public static Set<Integer> getSegmentIdsFromTileId(int tileId) {
		Set<Integer> segmentIds = new HashSet<>();

		// we take a 3x3 set of segments, where the tileId represents the central
		// segment
		int centralSegmentId = getSegmentIdFromTileId(tileId);
		segmentIds.add(centralSegmentId);

		// we know that the segments are 1000x1000
		if (centralSegmentId % segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - 1);// left

		if (centralSegmentId % segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + 1);// right

		if (centralSegmentId / segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - segmentsPerRow);// above

		if (centralSegmentId / segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + segmentsPerRow);// below

		if (centralSegmentId % segmentsPerRow > 0 && centralSegmentId / segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - segmentsPerRow - 1);// top left

		if (centralSegmentId % segmentsPerRow < segmentsPerRow - 1 && centralSegmentId / segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - segmentsPerRow + 1);// top right

		if (centralSegmentId % segmentsPerRow > 0 && centralSegmentId / segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + segmentsPerRow - 1);// bottom left

		if (centralSegmentId % segmentsPerRow < segmentsPerRow - 1
				&& centralSegmentId / segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + segmentsPerRow + 1);// bottom right

		return segmentIds;
	}

	public static Map<Integer, Set<Integer>> getMinimapIconLocationsByFloorAndSegment(int floor, int segmentId) {
		if (!tileIdsByFloorAndSegmentAndSpriteFrameId.containsKey(floor))
			return new HashMap<>();

		if (!tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).containsKey(segmentId))
			return new HashMap<>();

		return tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId);
	}
}
