package database.dao;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import lombok.Getter;

public class WanderingPetDao {
	@Getter private static Map<Integer, Set<SimpleImmutableEntry<Integer, Integer>>> wanderingPets; // petId, floor, tileId
	
	public static void setupCaches() {
		wanderingPets = new HashMap<>();
		DbConnection.load("select floor, tile_id, pet_id from wandering_pets", rs -> {
			wanderingPets.putIfAbsent(rs.getInt("pet_id"), new HashSet<>());
			wanderingPets.get(rs.getInt("pet_id")).add(new SimpleImmutableEntry<>(rs.getInt("floor"), rs.getInt("tile_id")));
		});
	}
	
	public static Set<SimpleImmutableEntry<Integer, Integer>> getWanderingPet(int key) {
		return wanderingPets.get(key);
	}
}
