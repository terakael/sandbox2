package database.dao;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import database.DbConnection;
import database.dto.FishingDepthDto;

public class FishingDepthDao {
	private static Set<FishingDepthDto> dtos = new HashSet<>();
	
	public static void setupCaches() {
		DbConnection.load("select item_id, ideal_depth, variance from fishing_depths", rs -> {
			dtos.add(new FishingDepthDto(
					rs.getInt("item_id"),
					rs.getInt("ideal_depth"),
					rs.getInt("variance")
				));
		});
	}
	
	public static List<Pair<Double, Integer>> getWeightedItems(int depth) {
		return dtos.stream()
			.filter(e -> depth >= e.getIdealDepth() - e.getVariance() && depth <= e.getIdealDepth() + e.getVariance())
			.map(e -> Pair.of(1.0 - ((double)Math.abs(depth - e.getIdealDepth()) / e.getVariance()), e.getItemId()))
			.sorted(Comparator.comparing(Pair::getLeft))
			.collect(Collectors.toCollection(LinkedList::new));
	}
}
