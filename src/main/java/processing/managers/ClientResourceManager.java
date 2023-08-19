package processing.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.CastableDao;
import database.dao.EquipmentDao;
import database.dao.GroundTextureDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.PlayerBaseAnimationsDao;
import database.dao.SceneryDao;
import database.dao.ShipAccessoryDao;
import database.dao.ShipDao;
import database.dao.SpriteFrameDao;
import database.dao.SpriteMapDao;
import database.dto.CastableDto;
import database.dto.GroundTextureDto;
import database.dto.ItemDto;
import database.dto.NPCDto;
import database.dto.PlayerAnimationDto;
import database.dto.SceneryDto;
import database.dto.ShipAccessoryDto;
import database.dto.ShipDto;
import database.dto.SpriteFrameDto;
import database.dto.SpriteMapDto;
import processing.attackable.Player;
import responses.AddResourceResponse;
import responses.ResponseMaps;

public class ClientResourceManager {
	// keeps track of what each player has loaded, decaches on player logout. key is playerId.
	private static Map<Integer, Set<Integer>> loadedSpriteMapIds = new HashMap<>();
	private static Map<Integer, Set<Integer>> loadedItemIds = new HashMap<>();
	private static Map<Integer, Set<Integer>> loadedSpriteFrameIds = new HashMap<>();
	private static Map<Integer, Set<Integer>> loadedSceneryIds = new HashMap<>();
	private static Map<Integer, Set<Integer>> loadedNpcIds = new HashMap<>();
	private static Map<Integer, Set<Integer>> loadedShipIds = new HashMap<>();
	private static Map<Integer, Set<Integer>> loadedGroundTextureIds = new HashMap<>();
	
	// loaded each tick, sent to the appropriate players, then cleared
	private static Map<Player, Set<SpriteMapDto>> spriteMaps = new HashMap<>();
//	private static Map<Player, Set<Integer>> groundTextureSpriteMapIds = new HashMap<>();
	private static Map<Player, Set<ItemDto>> items = new HashMap<>();
	private static Map<Player, Set<SpriteFrameDto>> spriteFrames = new HashMap<>();
	private static Map<Player, Set<SceneryDto>> scenery = new HashMap<>();
	private static Map<Player, Set<NPCDto>> npcs = new HashMap<>();
	private static Map<Player, Set<ShipDto>> ships = new HashMap<>();
	private static Map<Player, Set<GroundTextureDto>> groundTextures = new HashMap<>();
	
	public static void addGroundTextures(Player player, Set<Integer> groundTextureIds) {
		// we need to pull the corresponding spriteMapIds based on the unloaded textureIds,
		// then based on those spriteMapIds, pull all the remaining textureIds and pass those through as well.
		// when the client receives it, it's creating a texture map with the spriteIds, which requires all groundTextureIds.
		Set<GroundTextureDto> selectedGroundTextures = GroundTextureDao.getGroundTextures().stream()
			.filter(e -> groundTextureIds.contains(e.getId()))
			.collect(Collectors.toSet());
		
//		final Set<Integer> spriteMapIds = selectedGroundTextures.stream().map(GroundTextureDto::getSpriteMapId).collect(Collectors.toSet());
		
		// pull all the other ground textures that use these sprite maps and add them to our selected list
		// e.g. if the client loads one part of the water texture map, pull the rest of the textures in the water map and send them too.
//		selectedGroundTextures.addAll(GroundTextureDao.getGroundTextures().stream()
//				.filter(e -> spriteMapIds.contains(e.getSpriteMapId()))
//				.collect(Collectors.toSet()));
		
		Set<Integer> selectedGroundTextureIds = extractUnloadedGroundTextureIds(player, selectedGroundTextures.stream().map(GroundTextureDto::getId).collect(Collectors.toSet()));
		if (selectedGroundTextureIds.isEmpty())
			return;
		
		groundTextures.putIfAbsent(player, new HashSet<>());		
		groundTextures.get(player).addAll(selectedGroundTextures);
		addLoadedGroundTextureIds(player, selectedGroundTextureIds);
		
//		Set<Integer> selectedSpriteMapIds = extractUnloadedSpriteMapIds(player, spriteMapIds);
//		if (selectedSpriteMapIds.isEmpty())
//			return;
		
//		addLoadedSpriteMapIds(player, selectedSpriteMapIds);
		
//		if (!spriteMaps.containsKey(player))
//			spriteMaps.put(player, new HashSet<>());
		
//		if (!groundTextureSpriteMapIds.containsKey(player))
//			groundTextureSpriteMapIds.put(player, new HashSet<>());
		
//		for (Integer spriteMapId : selectedSpriteMapIds) {
//			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
//			groundTextureSpriteMapIds.get(player).add(spriteMapId);
//		}
	}
	
	public static void addSpriteFramesAndSpriteMaps(Player player, Set<Integer> spriteFrameIds) {
		// the scenery's sprite frames
		Set<Integer> selectedSpriteFrameIds = extractUnloadedSpriteFrameIds(player, spriteFrameIds);
				
		if (selectedSpriteFrameIds.isEmpty())
			return;
		
		Set<SpriteFrameDto> selectedSpriteFrames = SpriteFrameDao.getAllSpriteFrames().stream()
			.filter(e -> selectedSpriteFrameIds.contains(e.getId()))
			.collect(Collectors.toSet());

		if (!spriteFrames.containsKey(player))
			spriteFrames.put(player, new HashSet<>());
		addLoadedSpriteFrameIds(player, selectedSpriteFrameIds);
		spriteFrames.get(player).addAll(selectedSpriteFrames);
		
		// the sprite frame's sprite maps
		Set<Integer> selectedSpriteMapIds = extractUnloadedSpriteMapIds(player, selectedSpriteFrames.stream()
			.map(SpriteFrameDto::getSprite_map_id)
			.collect(Collectors.toSet()));
		
		if (selectedSpriteMapIds.isEmpty())
			return;
		
		addLoadedSpriteMapIds(player, selectedSpriteMapIds);
		
		if (!spriteMaps.containsKey(player))
			spriteMaps.put(player, new HashSet<>());
		
		for (Integer spriteMapId : selectedSpriteMapIds)
			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
	}
	
	public static void addScenery(Player player, Set<Integer> sceneryIds) {
		Set<Integer> selectedSceneryIds = extractUnloadedSceneryIds(player, sceneryIds);
		if (selectedSceneryIds.isEmpty())
			return;
		
		// the scenery itself
		Set<SceneryDto> selectedScenery = SceneryDao.getAllScenery().stream()
				.filter(e -> selectedSceneryIds.contains(e.getId()))
				.collect(Collectors.toSet());
		
		if (!scenery.containsKey(player))
			scenery.put(player, new HashSet<>());
		addLoadedSceneryIds(player, selectedScenery.stream().map(SceneryDto::getId).collect(Collectors.toSet()));
		scenery.get(player).addAll(selectedScenery);
		
		addSpriteFramesAndSpriteMaps(player, selectedScenery.stream()
					.map(SceneryDto::getSpriteFrameId)
					.collect(Collectors.toSet()));
	}
	
	public static void addLocalScenery(Player player, Set<Integer> sceneryIds) {
		// used for constructables
//		List<Player> localPlayers = WorldProcessor.getPlayersNearTile(player.getFloor(), player.getTileId(), 15);
//		for (Player localPlayer : localPlayers)
//			addScenery(localPlayer, sceneryIds);
		
		LocationManager.getLocalPlayers(player.getFloor(), player.getTileId(), 15)
			.forEach(localPlayer -> addScenery(localPlayer, sceneryIds));
	}
	
	public static void addNpcs(Player player, Set<Integer> npcIds) {
		Set<Integer> selectedNpcIds = extractUnloadedNpcIds(player, npcIds);
		if (selectedNpcIds.isEmpty())
			return;
		
		Set<NPCDto> selectedNpcs = NPCDao.getNpcList().stream()
				.filter(e -> selectedNpcIds.contains(e.getId()))
				.collect(Collectors.toSet());
		
		npcs.putIfAbsent(player, new HashSet<>());
		addLoadedNpcIds(player, selectedNpcs.stream().map(NPCDto::getId).collect(Collectors.toSet()));
		npcs.get(player).addAll(selectedNpcs);
		
		Set<Integer> selectedSpriteFrameIds = new HashSet<>();
		selectedSpriteFrameIds.addAll(selectedNpcs.stream().map(NPCDto::getUpId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedNpcs.stream().map(NPCDto::getDownId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedNpcs.stream().map(NPCDto::getLeftId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedNpcs.stream().map(NPCDto::getRightId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedNpcs.stream().map(NPCDto::getAttackId).collect(Collectors.toSet()));
		
		addSpriteFramesAndSpriteMaps(player, selectedSpriteFrameIds);
	}
	
	public static void addShips(Player player, Set<Integer> hullSceneryIds) {
		Set<Integer> selectedShipIds = extractUnloadedShipIds(player, hullSceneryIds);
		if (selectedShipIds.isEmpty())
			return;
		
		Set<ShipDto> selectedShips = ShipDao.getShipDtos().stream()
				.filter(e -> selectedShipIds.contains(e.getHullSceneryId()))
				.collect(Collectors.toSet());
		
		ships.putIfAbsent(player, new HashSet<>());
		addLoadedShipIds(player, selectedShips.stream().map(ShipDto::getHullSceneryId).collect(Collectors.toSet()));
		ships.get(player).addAll(selectedShips);
		
		Set<Integer> selectedSpriteFrameIds = new HashSet<>();
		selectedSpriteFrameIds.addAll(selectedShips.stream().map(ShipDto::getUpId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedShips.stream().map(ShipDto::getDownId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedShips.stream().map(ShipDto::getLeftId).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(selectedShips.stream().map(ShipDto::getRightId).collect(Collectors.toSet()));
		
		addSpriteFramesAndSpriteMaps(player, selectedSpriteFrameIds);
	}
	
	public static void addItems(Player player, Set<Integer> itemIds) {
		Set<Integer> selectedItemIds = extractUnloadedItemIds(player, itemIds);
		if (selectedItemIds.isEmpty())
			return;
		
		Set<ItemDto> selectedItems = ItemDao.getAllItems().stream()
				.filter(e -> selectedItemIds.contains(e.getId()))
				.collect(Collectors.toSet());
		
		items.putIfAbsent(player, new HashSet<>());
		addLoadedItemIds(player, selectedItems.stream().map(ItemDto::getId).collect(Collectors.toSet()));
		items.get(player).addAll(selectedItems);
		
		addSpriteFramesAndSpriteMaps(player, selectedItems.stream()
				.map(ItemDto::getSpriteFrameId)
				.collect(Collectors.toSet()));
	}
	
	public static void addLocalItems(Player player, Set<Integer> itemIds) {
//		List<Player> localPlayers = WorldProcessor.getPlayersNearTile(player.getFloor(), player.getTileId(), 15);
		LocationManager.getLocalPlayers(player.getFloor(), player.getTileId(), 15)
			.forEach(localPlayer -> addItems(localPlayer, itemIds));
//		for (Player localPlayer : localPlayers)
//			addItems(localPlayer, itemIds);
	}
	
	public static void addAnimationDtos(Player player, Set<PlayerAnimationDto> animations) {
		Set<Integer> selectedSpriteFrameIds = new HashSet<>();
		selectedSpriteFrameIds.addAll(animations.stream().map(PlayerAnimationDto::getUp).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(animations.stream().map(PlayerAnimationDto::getDown).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(animations.stream().map(PlayerAnimationDto::getLeft).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(animations.stream().map(PlayerAnimationDto::getRight).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(animations.stream().map(PlayerAnimationDto::getAttack_left).collect(Collectors.toSet()));
		selectedSpriteFrameIds.addAll(animations.stream().map(PlayerAnimationDto::getAttack_right).collect(Collectors.toSet()));
		
		addSpriteFramesAndSpriteMaps(player, selectedSpriteFrameIds);
	}
	
	public static void addAnimations(Player player, Set<Integer> playerIds) {
		Set<PlayerAnimationDto> animations = new HashSet<>();
		EquipmentDao.getPlayerEquipment().forEach((playerId, equipmentDtoMap) -> {
			if (playerIds.contains(playerId))
				equipmentDtoMap.forEach((equipmentId, slot) -> animations.add(EquipmentDao.getEquipmentByItemId(equipmentId).getAnimations()));
		});

		playerIds.forEach(playerId -> animations.addAll(PlayerBaseAnimationsDao.getBaseAnimationsBasedOnEquipmentTypes(playerId).values()));

		if (animations.isEmpty())
			return;
		
		addAnimationDtos(player, animations);
	}
	
	public static void addLocalAnimations(Player player, Set<Integer> playerIds) {
		// sometimes we want to make sure all the local players receive the resources.
		// for example, when a player equips a certain item, all the players around who haven't
		// yet loaded that item should get the resources sent.
//		List<Player> localPlayers = WorldProcessor.getPlayersNearTile(player.getFloor(), player.getTileId(), 15);
//		for (Player localPlayer : localPlayers)
//			addAnimations(localPlayer, playerIds);
		
		LocationManager.getLocalPlayers(player.getFloor(), player.getTileId(), 15)
			.forEach(localPlayer -> addAnimations(localPlayer, playerIds));
	}
	
	public static void addSpell(Player player, int itemId) {
		final CastableDto castable = CastableDao.getCastableByItemId(itemId);
		
//		List<Player> localPlayers = WorldProcessor.getPlayersNearTile(player.getFloor(), player.getTileId(), 15);
//		for (Player localPlayer : localPlayers) {
//			 the spell should be viewable by all local players, so provide them the sprite frames/sprite maps as well
//			addSpriteFramesAndSpriteMaps(localPlayer, Collections.singleton(castable.getSpriteFrameId()));	
//		}
		
		LocationManager.getLocalPlayers(player.getFloor(), player.getTileId(), 15)
			.forEach(localPlayer -> addSpriteFramesAndSpriteMaps(localPlayer, Collections.singleton(castable.getSpriteFrameId())));
	}

	
	public static void clear() {
		spriteMaps.clear();
//		groundTextureSpriteMapIds.clear();
		items.clear();
		spriteFrames.clear();
		scenery.clear();
		npcs.clear();
		ships.clear();
		groundTextures.clear();
	}
	
	public static void compileToResponseMaps(ResponseMaps responseMap) {
		Set<Player> players = new HashSet<>();
		players.addAll(spriteMaps.keySet());
//		players.addAll(groundTextureSpriteMapIds.keySet());
		players.addAll(items.keySet());
		players.addAll(spriteFrames.keySet());
		players.addAll(scenery.keySet());
		players.addAll(npcs.keySet());
		players.addAll(ships.keySet());
		players.addAll(groundTextures.keySet());
		
		for (Player player : players) {
			AddResourceResponse response = new AddResourceResponse();
			
			if (spriteMaps.containsKey(player))
				response.setSpriteMaps(spriteMaps.get(player).stream().filter(e -> e != null).collect(Collectors.toSet()));
			
			if (spriteFrames.containsKey(player))
				response.setSpriteFrames(spriteFrames.get(player));
			
//			if (groundTextureSpriteMapIds.containsKey(player))
//				response.setGroundTextureSpriteMaps(groundTextureSpriteMapIds.get(player));
			
			if (items.containsKey(player))
				response.setItems(items.get(player));
			
			if (scenery.containsKey(player))
				response.setScenery(scenery.get(player));
			
			if (npcs.containsKey(player))
				response.setNpcs(npcs.get(player));
		
			if (ships.containsKey(player))
				response.setShips(ships.get(player));
		
			if (groundTextures.containsKey(player))
				response.setGroundTextures(groundTextures.get(player));
			
			responseMap.addClientOnlyResponse(player, response);
		}

		clear();
	}
	
	public static Set<Integer> extractUnloadedSpriteMapIds(Player player, Set<Integer> spriteMapIds) {
		if (!loadedSpriteMapIds.containsKey(player.getId()))
			return spriteMapIds;
		return spriteMapIds.stream().filter(e -> !loadedSpriteMapIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static void addLoadedSpriteMapIds(Player player, Set<Integer> spriteMapId) {
		if (!loadedSpriteMapIds.containsKey(player.getId()))
			loadedSpriteMapIds.put(player.getId(), new HashSet<>());
		loadedSpriteMapIds.get(player.getId()).addAll(spriteMapId);
	}

	public static Set<Integer> extractUnloadedSpriteFrameIds(Player player, Set<Integer> spriteFrameIds) {
		if (!loadedSpriteFrameIds.containsKey(player.getId()))
			return spriteFrameIds;
		return spriteFrameIds.stream().filter(e -> !loadedSpriteFrameIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static void addLoadedSpriteFrameIds(Player player, Set<Integer> spriteFrameIds) {
		if (!loadedSpriteFrameIds.containsKey(player.getId()))
			loadedSpriteFrameIds.put(player.getId(), new HashSet<>());
		loadedSpriteFrameIds.get(player.getId()).addAll(spriteFrameIds);
	}
	
	public static Set<Integer> extractUnloadedSceneryIds(Player player, Set<Integer> sceneryIds) {
		if (!loadedSceneryIds.containsKey(player.getId()))
			return sceneryIds;
		return sceneryIds.stream().filter(e -> !loadedSceneryIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static void addLoadedSceneryIds(Player player, Set<Integer> sceneryIds) {
		if (!loadedSceneryIds.containsKey(player.getId()))
			loadedSceneryIds.put(player.getId(), new HashSet<>());
		loadedSceneryIds.get(player.getId()).addAll(sceneryIds);
	}
	
	public static Set<Integer> extractUnloadedNpcIds(Player player, Set<Integer> npcIds) {
		if (!loadedNpcIds.containsKey(player.getId()))
			return npcIds;
		return npcIds.stream().filter(e -> !loadedNpcIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static Set<Integer> extractUnloadedShipIds(Player player, Set<Integer> shipIds) {
		if (!loadedShipIds.containsKey(player.getId()))
			return shipIds;
		return shipIds.stream().filter(e -> !loadedShipIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static void addLoadedNpcIds(Player player, Set<Integer> npcIds) {
		if (!loadedNpcIds.containsKey(player.getId()))
			loadedNpcIds.put(player.getId(), new HashSet<>());
		loadedNpcIds.get(player.getId()).addAll(npcIds);
	}
	
	public static void addLoadedShipIds(Player player, Set<Integer> hullSceneryIds) {
		if (!loadedShipIds.containsKey(player.getId()))
			loadedShipIds.put(player.getId(), new HashSet<>());
		loadedShipIds.get(player.getId()).addAll(hullSceneryIds);
	}
	
	public static Set<Integer> extractUnloadedItemIds(Player player, Set<Integer> itemIds) {
		if (!loadedItemIds.containsKey(player.getId()))
			return itemIds;
		return itemIds.stream().filter(e -> !loadedItemIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static void addLoadedItemIds(Player player, Set<Integer> itemIds) {
		if (!loadedItemIds.containsKey(player.getId()))
			loadedItemIds.put(player.getId(), new HashSet<>());
		loadedItemIds.get(player.getId()).addAll(itemIds);
	}
	
	public static Set<Integer> extractUnloadedGroundTextureIds(Player player, Set<Integer> groundTextureIds) {
		if (!loadedGroundTextureIds.containsKey(player.getId()))
			return groundTextureIds;
		return groundTextureIds.stream().filter(e -> !loadedGroundTextureIds.get(player.getId()).contains(e)).collect(Collectors.toSet());
	}
	
	public static void addLoadedGroundTextureIds(Player player, Set<Integer> groundTextureIds) {
		if (!loadedGroundTextureIds.containsKey(player.getId()))
			loadedGroundTextureIds.put(player.getId(), new HashSet<>());
		loadedGroundTextureIds.get(player.getId()).addAll(groundTextureIds);
	}
	
	public static void decachePlayer(Player player) {
		loadedSpriteMapIds.remove(player.getId());
		loadedItemIds.remove(player.getId());
		loadedSpriteFrameIds.remove(player.getId());
		loadedSceneryIds.remove(player.getId());
		loadedNpcIds.remove(player.getId());
		loadedShipIds.remove(player.getId());
		loadedGroundTextureIds.remove(player.getId());
	}
}
