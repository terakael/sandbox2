package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.AnimationDto;
import database.dto.BaseAnimationsDto;
import database.dto.PlayerAnimationDto;
import database.dto.PlayerBaseAnimationsDto;
import database.entity.delete.DeletePlayerBaseAnimationsEntity;
import database.entity.insert.InsertPlayerBaseAnimationsEntity;
import processing.managers.DatabaseUpdater;
import types.EquipmentTypes;
import types.PlayerPartType;

public class PlayerBaseAnimationsDao {
	private static Map<Integer, Set<PlayerBaseAnimationsDto>> playerBaseAnimations = new HashMap<>();
	
	private static Map<EquipmentTypes, Set<PlayerPartType>> playerPartsToIgnoreByEquipmentType = new HashMap<>();
	static {
		// TODO move to db?
		playerPartsToIgnoreByEquipmentType.put(EquipmentTypes.HELMET_FULL, Set.<PlayerPartType>of(PlayerPartType.HAIR, PlayerPartType.BEARD));
		playerPartsToIgnoreByEquipmentType.put(EquipmentTypes.HELMET_MED, Set.<PlayerPartType>of(PlayerPartType.HAIR));
		playerPartsToIgnoreByEquipmentType.put(EquipmentTypes.BODY, Set.<PlayerPartType>of(PlayerPartType.TORSO, PlayerPartType.SHIRT));
		playerPartsToIgnoreByEquipmentType.put(EquipmentTypes.LEGS, Set.<PlayerPartType>of(PlayerPartType.LEGS, PlayerPartType.PANTS, PlayerPartType.SHOES));
		playerPartsToIgnoreByEquipmentType.put(EquipmentTypes.CHAINSKIRT, Set.<PlayerPartType>of(PlayerPartType.PANTS));
	}
	
	public static void setupCaches() {
		DbConnection.load("select player_id, player_part_id, base_animation_id, color from player_base_animations", rs -> {
			playerBaseAnimations.putIfAbsent(rs.getInt("player_id"), new HashSet<>());
			playerBaseAnimations.get(rs.getInt("player_id")).add(new PlayerBaseAnimationsDto(
					rs.getInt("player_id"), 
					PlayerPartType.withValue(rs.getInt("player_part_id")), 
					rs.getInt("base_animation_id"), 
					rs.getInt("color")));
		});
	}
	
	public static Map<PlayerPartType, PlayerAnimationDto> loadAnimationsByPlayerId(int playerId) {		
		Map<PlayerPartType, PlayerAnimationDto> animationMap = new HashMap<>();
		if (!playerBaseAnimations.containsKey(playerId))
			return animationMap;
		
		return playerBaseAnimations.get(playerId).stream()
				.collect(Collectors.toMap(PlayerBaseAnimationsDto::getPlayerPartType, PlayerBaseAnimationsDao::toPlayerAnimationDto));
	}
	
	public static Map<PlayerPartType, PlayerAnimationDto> getBaseAnimationsBasedOnEquipmentTypes(int playerId) {
		if (!playerBaseAnimations.containsKey(playerId))
			return new HashMap<>();
		
		 
		Set<PlayerPartType> partsToIgnore = new HashSet<>();
		EquipmentDao.getPlayerEquipmentTypes(playerId).forEach(equipmentType -> {
			if (playerPartsToIgnoreByEquipmentType.containsKey(equipmentType))
				partsToIgnore.addAll(playerPartsToIgnoreByEquipmentType.get(equipmentType));
		});
		
		return playerBaseAnimations.get(playerId).stream()
				.filter(e -> !partsToIgnore.contains(e.getPlayerPartType()))
				.collect(Collectors.toMap(PlayerBaseAnimationsDto::getPlayerPartType, PlayerBaseAnimationsDao::toPlayerAnimationDto));
	}
	
	public static boolean setAnimation(int playerId, PlayerPartType type, int animationId, int color) {
		if (!BaseAnimationsDao.getCustomizableParts().contains(type)) // a non-updateable type was attempted to be updated?
			return false;
		
		// if animationId is 0 then we're deleting it
		if (animationId == 0) {
			if (playerBaseAnimations.containsKey(playerId)) {
				playerBaseAnimations.get(playerId).removeIf(e -> e.getPlayerPartType() == type);
			}
			DatabaseUpdater.enqueue(DeletePlayerBaseAnimationsEntity.builder().playerId(playerId).playerPartId(type.getValue()).build());
		} else {
			playerBaseAnimations.putIfAbsent(playerId, new HashSet<>());
			
			// if there's already an animation under this type, then we'll remove it first
			playerBaseAnimations.get(playerId).removeIf(e -> e.getPlayerPartType() == type);
			DatabaseUpdater.enqueue(DeletePlayerBaseAnimationsEntity.builder().playerId(playerId).playerPartId(type.getValue()).build());
			
			playerBaseAnimations.get(playerId).add(new PlayerBaseAnimationsDto(playerId, type, animationId, color));
			DatabaseUpdater.enqueue(InsertPlayerBaseAnimationsEntity.builder().playerId(playerId).playerPartId(type.getValue()).baseAnimationId(animationId).color(color).build());
		}
		
		return true;
	}
	
	private static PlayerAnimationDto toPlayerAnimationDto(PlayerBaseAnimationsDto dto) {
		BaseAnimationsDto baseAnimationDto = BaseAnimationsDao.getBaseAnimationById(dto.getBaseAnimationId());
		if (baseAnimationDto == null)
			return null;
		
		AnimationDto animation = AnimationDao.getAnimationDtoById(baseAnimationDto.getAnimationId());
		if (animation == null)
			return null;
		
		return new PlayerAnimationDto(
				animation.getUpId(), 
				animation.getDownId(),
				animation.getLeftId(),
				animation.getRightId(),
				animation.getAttackLeftId(),
				animation.getAttackRightId(),
				dto.getColor());
	}
}
