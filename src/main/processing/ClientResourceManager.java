package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.CastableDao;
import main.database.EquipmentDao;
import main.database.GroundTextureDao;
import main.database.ItemDao;
import main.database.NPCDao;
import main.database.PlayerAnimationDao;
import main.database.SceneryDao;
import main.database.SpriteMapDao;
import main.database.SpriteMapDto;
import main.responses.AddResourceResponse;
import main.responses.ResponseMaps;

public class ClientResourceManager {
	private static Map<Player, Set<SpriteMapDto>> spriteMaps = new HashMap<>();
	private static Map<Player, Set<Integer>> groundTextureSpriteMapIds = new HashMap<>();
	
	public static void addGroundTextures(Player player, Set<Integer> groundTextureIds) {
		Set<Integer> spriteMapIds = player.notLoadedSpriteMapIds(GroundTextureDao.getSpriteMapIdsByGroundTextureIds(groundTextureIds));
		if (spriteMapIds.isEmpty())
			return;
		
		for (int spriteMapId : spriteMapIds) {
			player.addLoadedSpriteMapId(spriteMapId);
		}
		
		if (!spriteMaps.containsKey(player))
			spriteMaps.put(player, new HashSet<>());
		
		if (!groundTextureSpriteMapIds.containsKey(player))
			groundTextureSpriteMapIds.put(player, new HashSet<>());
		
		for (Integer spriteMapId : spriteMapIds) {
			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
			groundTextureSpriteMapIds.get(player).add(spriteMapId);
		}
	}
	
	public static void addScenery(Player player, Set<Integer> sceneryIds) {
		Set<Integer> spriteMapIds = player.notLoadedSpriteMapIds(SceneryDao.getSpriteMapIdsBySceneryIds(sceneryIds));
		if (spriteMapIds.isEmpty())
			return;
		
		for (int spriteMapId : spriteMapIds) {
			player.addLoadedSpriteMapId(spriteMapId);
		}
		
		if (!spriteMaps.containsKey(player))
			spriteMaps.put(player, new HashSet<>());
		
		for (Integer spriteMapId : spriteMapIds)
			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
	}
	
	public static void addNpcs(Player player, Set<Integer> npcIds) {
		Set<Integer> spriteMapIds = player.notLoadedSpriteMapIds(NPCDao.getSpriteMapIdsByNpcIds(npcIds));
		if (spriteMapIds.isEmpty())
			return;
		
		for (int spriteMapId : spriteMapIds) {
			player.addLoadedSpriteMapId(spriteMapId);
		}
		
		if (!spriteMaps.containsKey(player))
			spriteMaps.put(player, new HashSet<>());
		
		for (Integer spriteMapId : spriteMapIds)
			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
	}
	
	public static void addItems(Player player, Set<Integer> itemIds) {
		Set<Integer> spriteMapIds = player.notLoadedSpriteMapIds(ItemDao.getSpriteMapIdsByItemIds(itemIds));
		if (spriteMapIds.isEmpty())
			return;
		
		for (int spriteMapId : spriteMapIds) {
			player.addLoadedSpriteMapId(spriteMapId);
		}
		
		if (!spriteMaps.containsKey(player))
			spriteMaps.put(player, new HashSet<>());
		
		for (Integer spriteMapId : spriteMapIds)
			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
	}
	
	public static void addAnimations(Player player, Set<Integer> playerIds) {
		Set<Integer> spriteMapIds = player.notLoadedSpriteMapIds(PlayerAnimationDao.getSpriteMapIdsByPlayerIds(playerIds));
		
		Set<Integer> equippedItemIds = new HashSet<>();
		for (int playerId : playerIds)
			equippedItemIds.addAll(EquipmentDao.getEquippedSlotsAndItemIdsByPlayerId(playerId).keySet());
		
		spriteMapIds.addAll(player.notLoadedSpriteMapIds(EquipmentDao.getSpriteMapIdsByItemIds(equippedItemIds)));
		if (spriteMapIds.isEmpty())
			return;
		
		for (int spriteMapId : spriteMapIds) {
			player.addLoadedSpriteMapId(spriteMapId);
		}
		
		if (!spriteMaps.containsKey(player))
			spriteMaps.put(player, new HashSet<>());
		
		for (Integer spriteMapId : spriteMapIds)
			spriteMaps.get(player).add(SpriteMapDao.getSpriteMap(spriteMapId));
	}
	
	public static void addLocalAnimations(Player player, Set<Integer> playerIds) {
		// sometimes we want to make sure all the local players receive the resources.
		// for example, when a player equips a certain item, all the players around who haven't
		// yet loaded that item should get the resources sent.
		List<Player> localPlayers = WorldProcessor.getPlayersNearTile(player.getFloor(), player.getTileId(), 15);
		for (Player localPlayer : localPlayers)
			addAnimations(localPlayer, playerIds);
	}
	
	public static void addSpell(Player player, int itemId) {
		int spriteMapId = CastableDao.getSpriteMapIdByItemId(itemId);
		List<Player> localPlayers = WorldProcessor.getPlayersNearTile(player.getFloor(), player.getTileId(), 15);
		for (Player localPlayer : localPlayers) {
			if (!localPlayer.hasLoadedSpriteMapId(spriteMapId)) {
				localPlayer.addLoadedSpriteMapId(spriteMapId);
				
				if (!spriteMaps.containsKey(localPlayer))
					spriteMaps.put(localPlayer, new HashSet<>());
				spriteMaps.get(localPlayer).add(SpriteMapDao.getSpriteMap(spriteMapId));
			}
		}
	}

	
	public static void clear() {
		spriteMaps.clear();
		groundTextureSpriteMapIds.clear();
	}
	
	public static void compileToResponseMaps(ResponseMaps responseMap) {
		for (Map.Entry<Player, Set<SpriteMapDto>> entry : spriteMaps.entrySet()) {
			AddResourceResponse response = new AddResourceResponse();
			response.setSpriteMaps(entry.getValue().stream().filter(e -> e != null).collect(Collectors.toSet()));
			if (groundTextureSpriteMapIds.containsKey(entry.getKey()))
				response.setGroundTextureSpriteMaps(groundTextureSpriteMapIds.get(entry.getKey()));
			responseMap.addClientOnlyResponse(entry.getKey(), response);
		}
		clear();
	}
}
