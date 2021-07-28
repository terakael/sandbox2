package database.dao;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.BaseAnimationsDto;
import lombok.Getter;
import types.PlayerPartType;

public class BaseAnimationsDao {
	private static Map<Integer, BaseAnimationsDto> baseAnimations = new HashMap<>();
	
	// for when the player cycles through the animations in the character customiazation screen
	// it's a (linked) list because we do random access when we find the player's currently-displayed animation
	private static Map<PlayerPartType, List<Integer>> animationCycleOrderByPart = new HashMap<>();
	
	@Getter private static final Set<PlayerPartType> customizableParts = new LinkedHashSet<>();
	static {
		customizableParts.add(PlayerPartType.HAIR);
		customizableParts.add(PlayerPartType.BEARD);
		customizableParts.add(PlayerPartType.SHIRT);
		customizableParts.add(PlayerPartType.PANTS);
		customizableParts.add(PlayerPartType.SHOES);
	}
	
	public static void setupCaches() {
		cacheBaseAnimations();
		cacheCycleOrdering();
	}
	
	private static void cacheBaseAnimations() {
		DbConnection.load("select id, player_part_id, animation_id from base_animations", rs -> 
			baseAnimations.put(rs.getInt("id"), new BaseAnimationsDto(rs.getInt("id"), PlayerPartType.withValue(rs.getInt("player_part_id")), rs.getInt("animation_id"))));
	}
	
	private static void cacheCycleOrdering() {
		baseAnimations.values().forEach(animationDto -> {
			animationCycleOrderByPart.putIfAbsent(animationDto.getPlayerPartType(), new LinkedList<>());
			animationCycleOrderByPart.get(animationDto.getPlayerPartType()).add(animationDto.getAnimationId());
		});
	}
	
	public static BaseAnimationsDto getBaseAnimationById(int id) {
		return baseAnimations.get(id);
	}
	
	public static BaseAnimationsDto getBaseAnimationByAnimationId(int animationId) {
		return baseAnimations.values().stream().filter(e -> e.getAnimationId() == animationId).findFirst().orElse(null);
	}
	
	public static int getNextAnimationId(PlayerPartType type, int currentAnimationId) {
		if (!animationCycleOrderByPart.containsKey(type))
			return 0;
		
		if (currentAnimationId == 0)
			return animationCycleOrderByPart.get(type).get(0);
		
		// if we get to the last animation in the list (or the thing passed in is invalid) then wrap back to the first element
		final int currentAnimationIndex = animationCycleOrderByPart.get(type).indexOf(currentAnimationId);
		if (currentAnimationIndex == -1 || currentAnimationIndex == animationCycleOrderByPart.get(type).size() - 1)
			return 0;
		
		return animationCycleOrderByPart.get(type).get(currentAnimationIndex + 1);
	}
	
	public static int getPreviousAnimationId(PlayerPartType type, int currentAnimationId) {
		if (!animationCycleOrderByPart.containsKey(type))
			return 0;
		
		if (currentAnimationId == 0)
			return animationCycleOrderByPart.get(type).get(animationCycleOrderByPart.get(type).size() - 1);
		
		final int currentAnimationIndex = animationCycleOrderByPart.get(type).indexOf(currentAnimationId);		
		if (currentAnimationIndex <= 0)
			return 0;
		
		return animationCycleOrderByPart.get(type).get(currentAnimationIndex - 1);
	}
}
