package database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import processing.PathFinder;
import database.DbConnection;
import database.dto.FishableDto;

public class FishableDao {
	private static List<FishableDto> fishables = new ArrayList<>();
	private static Map<Integer, Map<Integer, Set<Integer>>> fishableInstances = null;

	public static void setupCaches() {
		cacheFishables();
		moveFishingSpotsToShore();
		cacheFishableInstances();
	}

	public static void moveFishingSpotsToShore() {
		SceneryDao.replaceTileIdsForSceneries(
				fishables.stream().map(FishableDto::getSceneryId).collect(Collectors.toList()),
				FishableDao::moveToShore);
	}

	public static int moveToShore(int floor, int oldTileId) {
		final int closestLandTile = PathFinder.getClosestWalkableTile(floor, oldTileId, false);
		return PathFinder.getClosestSailableTile(floor, closestLandTile);
	}

	public static FishableDto getFishableDtoByTileId(int floor, int tileId) {
		if (!fishableInstances.containsKey(floor))
			return null;

		for (var instances : fishableInstances.get(floor).entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (FishableDto dto : fishables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}

	private static void cacheFishables() {
		final String query = "select scenery_id, level, exp, item_id, respawn_ticks, tool_id, bait_id from fishable";
		DbConnection.load(query, rs -> {
			fishables.add(new FishableDto(
					rs.getInt("scenery_id"),
					rs.getInt("level"),
					rs.getInt("exp"),
					rs.getInt("item_id"),
					rs.getInt("respawn_ticks"),
					rs.getInt("tool_id"),
					rs.getInt("bait_id")));
		});
	}

	private static void cacheFishableInstances() {
		fishableInstances = SceneryDao
				.filterSceneryByIds(fishables.stream().map(FishableDto::getSceneryId).collect(Collectors.toList()));
	}

	public static Set<FishableDto> getAllFishables() {
		return new HashSet<>(fishables);
	}
}
