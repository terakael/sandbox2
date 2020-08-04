package main.processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.websocket.Session;

import com.google.gson.Gson;

import main.Endpoint;
import main.GroundItemManager;
import main.database.GroundTextureDao;
import main.database.MinimapSegmentDao;
import main.database.SceneryDao;
import main.database.ShopDao;
import main.processing.FightManager.Fight;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.responses.AddGroundTextureInstancesResponse;
import main.responses.AddMinimapSegmentsResponse;
import main.responses.AddSceneryInstancesResponse;
import main.responses.GroundItemInRangeResponse;
import main.responses.GroundItemOutOfRangeResponse;
import main.responses.LogonResponse;
import main.responses.NpcInRangeResponse;
import main.responses.NpcOutOfRangeResponse;
import main.responses.PlayerInRangeResponse;
import main.responses.PlayerOutOfRangeResponse;
import main.responses.PvmStartResponse;
import main.responses.PvpStartResponse;
import main.responses.RemoveGroundTextureInstancesResponse;
import main.responses.RemoveMinimapSegmentsResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.ResponseMaps;
import main.responses.ShopResponse;
import main.utils.Stopwatch;

public class WorldProcessor implements Runnable {
	private Thread thread;
	private static final int TICK_DURATION_MS = 600;
	private static Gson gson = new Gson();
	
	public static Map<Session, Player> playerSessions = new HashMap<>();
	
	public void start() {
		if (thread == null) {
			thread = new Thread(this, "worldprocessor");
			thread.start();
		}
	}
	
	@Override
	public void run() {		
		while (true) {
			long prevTime = System.nanoTime();
			
			process();
			
			try {
				// run for 0.6 seconds, minus the processing time so the ticks are always 0.6s.
				// if the processing time is more than 0.6 seconds then don't sleep at all (this happens when breakpoints are hit)
				long processTimeMs = (System.nanoTime() - prevTime) / 1000000;
				if (processTimeMs >= TICK_DURATION_MS / 2) {
					System.out.println(String.format("WARNING: process time took %dms (%d%% of total allowed processing time)", processTimeMs, (int)(((float)processTimeMs / TICK_DURATION_MS) * 100)));
					Stopwatch.dump();
				}
				Thread.sleep(Math.max(0, TICK_DURATION_MS - processTimeMs));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void process() {		
		Stopwatch.reset();

		Stopwatch.start("request map");
		// pull requestmap contents from Endpoint and clear it so it can collect for the next tick
		Map<Session, List<Request>> requestMap = new HashMap<>();
		for (Map.Entry<Session, Request> entry : Endpoint.requestMap.entrySet()) {
			if (!requestMap.containsKey(entry.getKey()))
				requestMap.put(entry.getKey(), new ArrayList<>());
			requestMap.get(entry.getKey()).add(entry.getValue());
		}
		Endpoint.requestMap.clear();
		
		for (Map.Entry<Session, List<Request>> entry : Endpoint.multiRequestMap.entrySet()) {
			if (!requestMap.containsKey(entry.getKey()))
				requestMap.put(entry.getKey(), new ArrayList<>());
			requestMap.get(entry.getKey()).addAll(entry.getValue());
		}
		Endpoint.multiRequestMap.clear();
		
		Stopwatch.end("request map");
		
		
		// process all requests and add all responses to this object which will be compiled into the response list for each player
		ResponseMaps responseMaps = new ResponseMaps();

		Stopwatch.start("player requests");
		// process player requests for this tick
		for (Map.Entry<Session, List<Request>> entry : requestMap.entrySet()) {
			for (Request request : entry.getValue()) {// most cases there's only one request, but MultiRequest types exist (equipping etc)
				Response response = ResponseFactory.create(request.getAction());
				
				if (request.getAction().equals("logon")) {
					// player isn't created at this point; it's created within this function
					LogonResponse logonResponse = (LogonResponse)response;
					logonResponse.processLogon(request, entry.getKey(), responseMaps);
				} else {
					// if the player logs out before its requests are processed then don't process
					Player player = playerSessions.get(entry.getKey());
					if (player != null)
						response.processSuper(request, player, responseMaps);
				}
			}
		}
		Stopwatch.end("player requests");
		
		Stopwatch.start("process players");
		// process players
		for (Map.Entry<Session, Player> entry : playerSessions.entrySet()) {
			entry.getValue().process(responseMaps);
		}
		Stopwatch.end("process players");
		
		updateInRangePlayers(responseMaps);
		
		Stopwatch.start("process npcs");
		NPCManager.get().process(responseMaps);
		Stopwatch.end("process npcs");
		
		// process fight manager
		Stopwatch.start("process fight manager");
		FightManager.process(responseMaps);
		Stopwatch.end("process fight manager");
		
		Stopwatch.start("ground item manager");
		GroundItemManager.process();
		Stopwatch.end("ground item manager");
		
		Stopwatch.start("shops");
		ShopManager.process(responseMaps);
		Stopwatch.end("shops");
		
		Stopwatch.start("rocks");
		RockManager.process(responseMaps);
		Stopwatch.end("rocks");
		
		Stopwatch.start("flowers");
		FlowerManager.process(responseMaps);
		Stopwatch.end("flowers");
		
		refreshGroundItems(responseMaps);
		updateShopStock(responseMaps);
		updateLocalGroundTexturesAndScenery(responseMaps);
		updateLocalNpcLocations(responseMaps);
		
		
		// take all the responseMaps and compile the responses to send to each player
		Stopwatch.start("compile response maps");
		Map<Player, List<Response>> clientResponses = new HashMap<>();
		compileBroadcastResponses(clientResponses, responseMaps);
		compileBroadcastExcludeResponses(clientResponses, responseMaps);
		compileLocalResponses(clientResponses, responseMaps);
		compileClientOnlyResponses(clientResponses, responseMaps);
		Stopwatch.end("compile response maps");
		
		ArrayList<Session> sessionsToKill = new ArrayList<>();
		
		// go through the clientResponses and send the response array to each player
		Stopwatch.start("send responses");
		for (Map.Entry<Player, List<Response>> responses : clientResponses.entrySet()) {
			try {
				responses.getKey().getSession().getBasicRemote().sendText(gson.toJson(responses.getValue()));
				
				// if there were any failed logon responses then kill the connection
				for (Response response : responses.getValue()) {
					if (response instanceof LogonResponse) {
						if (response.getSuccess() == 0) {
							sessionsToKill.add(responses.getKey().getSession());
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Stopwatch.end("send responses");
		
		Stopwatch.start("kill sessions");
		for (Session session : sessionsToKill) {
			try {
				session.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Stopwatch.end("kill sessions");
	}
	
	private void compileBroadcastResponses(Map<Player, List<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Session, Player> playerSession : playerSessions.entrySet()) {
			// every player gets the broadcast responses
			for (Response broadcastResponse : responseMaps.getBroadcastResponses()) {
				if (!clientResponses.containsKey(playerSession.getValue()))
					clientResponses.put(playerSession.getValue(), new ArrayList<>());
				clientResponses.get(playerSession.getValue()).add(broadcastResponse);
			}
		}
	}
	
	private void compileBroadcastExcludeResponses(Map<Player, List<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Player, List<Response>> broadcastResponseMap : responseMaps.getBroadcastExcludeClientResponses().entrySet()) {
			for (Map.Entry<Session, Player> playerSession : playerSessions.entrySet()) {
				if (playerSession.getValue().equals(broadcastResponseMap.getKey()))
					continue;// don't send to client
				
				if (!clientResponses.containsKey(playerSession.getValue()))
					clientResponses.put(playerSession.getValue(), new ArrayList<>());
				
				for (Response response : broadcastResponseMap.getValue())
					clientResponses.get(playerSession.getValue()).add(response);
			}
		}
	}
	
	private void compileLocalResponses(Map<Player, List<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Entry<Integer, Map<Integer, List<Response>>> localResponseMapByFloor : responseMaps.getLocalResponses().entrySet()) {
			for (Entry<Integer, List<Response>> localResponseMap : localResponseMapByFloor.getValue().entrySet()) {
				List<Player> localPlayers = getPlayersNearTile(localResponseMapByFloor.getKey(), localResponseMap.getKey(), 15);
				for (Player localPlayer : localPlayers) {
					if (!clientResponses.containsKey(localPlayer))
						clientResponses.put(localPlayer, new ArrayList<>());
					
					for (Response response : localResponseMap.getValue())
						clientResponses.get(localPlayer).add(response);
				}
			}
		}
	}

	private void compileClientOnlyResponses(Map<Player, List<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Player, List<Response>> privateResponseMap : responseMaps.getClientOnlyResponses().entrySet()) {
			// only individual players get these responses
			for (Response privateResponse : privateResponseMap.getValue()) {
				if (!clientResponses.containsKey(privateResponseMap.getKey()))
					clientResponses.put(privateResponseMap.getKey(), new ArrayList<>());
				clientResponses.get(privateResponseMap.getKey()).add(privateResponse);
			}
		}
	}
	
	public static List<Player> getPlayersNearTile(int floor, int tileId, int radius) {
		List<Player> localPlayers = new ArrayList<>();
		
		int tileX = tileId % PathFinder.LENGTH;
		int tileY = tileId / PathFinder.LENGTH;
		for (Player player : WorldProcessor.playerSessions.values()) {
			int testTileX = player.getTileId() % PathFinder.LENGTH;
			int testTileY = player.getTileId() / PathFinder.LENGTH;
			
			if ((testTileX >= tileX - radius && testTileX <= tileX + radius) &&
				(testTileY >= tileY - radius && testTileY <= tileY + radius) && player.getFloor() == floor) {
				localPlayers.add(player);
			}
		}
		
		return localPlayers;
	}
	
	public static Player getPlayerById(int id) {
		for (Player player : playerSessions.values()) {
			if (player.getDto().getId() == id)
				return player;
		}
		return null;
	}
	
	public static Set<Integer> getLocalTiles(int tileId, int radius) {
		int topLeft = tileId - radius - (radius * 25000);
		
		Set<Integer> localTiles = new HashSet<>();
		
		for (int y = 0; y < radius * 2; ++y) {
			for (int x = 0; x < radius * 2; ++x)
				localTiles.add(topLeft + (y * 25000) + x);
		}
		
		return localTiles;
	}
	
	private void updateInRangePlayers(ResponseMaps responseMaps) {
		Stopwatch.start("updating in-range players");
		for (Map.Entry<Session, Player> entry : playerSessions.entrySet()) {
			Set<Integer> currentInRangePlayers = entry.getValue().getInRangePlayers();
			Set<Integer> newInRangePlayers = WorldProcessor.getPlayersNearTile(entry.getValue().getFloor(), entry.getValue().getTileId(), 15)
														   .stream()
														   .map(Player::getId)
														   .collect(Collectors.toSet());
			
			Set<Integer> removedPlayers = currentInRangePlayers.stream().filter(e -> entry.getValue().getId() != e && !newInRangePlayers.contains(e)).collect(Collectors.toSet());
			if (!removedPlayers.isEmpty()) {
				PlayerOutOfRangeResponse playerOutOfRangeResponse = new PlayerOutOfRangeResponse();
				playerOutOfRangeResponse.setPlayerIds(removedPlayers);
				responseMaps.addClientOnlyResponse(entry.getValue(), playerOutOfRangeResponse);
			}
			
			Set<Integer> addedPlayers = newInRangePlayers.stream().filter(e -> entry.getValue().getId() != e && !currentInRangePlayers.contains(e)).collect(Collectors.toSet());
			if (!addedPlayers.isEmpty()) {
				PlayerInRangeResponse playerInRangeResponse = new PlayerInRangeResponse();
				playerInRangeResponse.addPlayers(addedPlayers);
				responseMaps.addClientOnlyResponse(entry.getValue(), playerInRangeResponse);
			}
			entry.getValue().setInRangePlayers(newInRangePlayers);
			
			// if a player comes into range, we need to check if they're fighting something
			Set<Integer> checkedPlayerIds = new HashSet<>();
			for (int inRangePlayerId : addedPlayers) {			
				if (checkedPlayerIds.contains(inRangePlayerId))
					continue;// already checked this (i.e. this was the opponent of a fight we already created a response for)
				
				Fight fight = FightManager.getFightByPlayerId(inRangePlayerId);
				if (fight == null)
					continue;
				
				int fighter1id = ((Player)fight.getFighter1()).getId();
				checkedPlayerIds.add(fighter1id);
				
				if (fight.getFighter2() instanceof Player) {
					int fighter2id = ((Player)fight.getFighter2()).getId();
					checkedPlayerIds.add(fighter2id);
					
					PvpStartResponse pvpStartResponse = new PvpStartResponse();
					pvpStartResponse.setPlayer1Id(fighter1id);
					pvpStartResponse.setPlayer2Id(fighter2id);
					pvpStartResponse.setTileId(fight.getFighter1().getTileId());
					responseMaps.addClientOnlyResponse(entry.getValue(), pvpStartResponse);
				} else if (fight.getFighter2() instanceof NPC) {
					PvmStartResponse pvmStartResponse = new PvmStartResponse();
					pvmStartResponse.setPlayerId(fighter1id);
					pvmStartResponse.setMonsterId(((NPC)fight.getFighter2()).getInstanceId());
					pvmStartResponse.setTileId(fight.getFighter1().getTileId());
					responseMaps.addClientOnlyResponse(entry.getValue(), pvmStartResponse);
				}
			}
		}
		Stopwatch.end("updating in-range players");
	}
	
	private void refreshGroundItems(ResponseMaps responseMaps) {
		Stopwatch.start("refresh ground items");
		for (Map.Entry<Session, Player> entry : playerSessions.entrySet()) {
			Map<Integer, List<Integer>> currentInRangeGroundItems = entry.getValue().getInRangeGroundItems();
			Map<Integer, List<Integer>> newInRangeGroundItems = GroundItemManager.getItemIdsNearTile(entry.getValue().getFloor(), entry.getValue().getId(), entry.getValue().getTileId(), 15);
			
			Map<Integer, List<Integer>> removedGroundItems = new HashMap<>();
			for (Map.Entry<Integer, List<Integer>> currentEntry : currentInRangeGroundItems.entrySet()) {
				if (!newInRangeGroundItems.containsKey(currentEntry.getKey())) {
					removedGroundItems.put(currentEntry.getKey(), currentEntry.getValue());
					continue;
				}
				
				List<Integer> currentList = new ArrayList<>(currentEntry.getValue());
				List<Integer> newList = newInRangeGroundItems.get(currentEntry.getKey());
				for (int newItemId : newList) {
					if (currentList.contains(newItemId))
						currentList.remove(currentList.indexOf(newItemId));
				}
				
				if (!currentList.isEmpty())
					removedGroundItems.put(currentEntry.getKey(), currentList);
			}
			
			if (!removedGroundItems.isEmpty()) {
				GroundItemOutOfRangeResponse groundItemOutOfRangeResponse = new GroundItemOutOfRangeResponse();
				groundItemOutOfRangeResponse.setGroundItems(removedGroundItems);
				responseMaps.addClientOnlyResponse(entry.getValue(), groundItemOutOfRangeResponse);
			}
			
			Map<Integer, List<Integer>> addedGroundItems = new HashMap<>();
			for (Map.Entry<Integer, List<Integer>> newEntry : newInRangeGroundItems.entrySet()) {
				if (!currentInRangeGroundItems.containsKey(newEntry.getKey())) {
					addedGroundItems.put(newEntry.getKey(), newEntry.getValue());
					continue;
				}

				List<Integer> currentList = currentInRangeGroundItems.get(newEntry.getKey());
				List<Integer> newList = new ArrayList<>(newEntry.getValue());
				for (int currentItemId : currentList) {
					if (newList.contains(currentItemId))
						newList.remove(newList.indexOf(currentItemId));
				}

				if (!newList.isEmpty())
					addedGroundItems.put(newEntry.getKey(), newList);
			}
			if (!addedGroundItems.isEmpty()) {
				GroundItemInRangeResponse groundItemInRangeResponse = new GroundItemInRangeResponse();
				groundItemInRangeResponse.setGroundItems(addedGroundItems);
				responseMaps.addClientOnlyResponse(entry.getValue(), groundItemInRangeResponse);
			}
			
			entry.getValue().setInRangeGroundItems(newInRangeGroundItems);
		}
		Stopwatch.end("refresh ground items");
	}
	
	private void updateShopStock(ResponseMaps responseMaps) {
		Stopwatch.start("update shop stock");
		for (Store store : ShopManager.getShops()) {
			if (store.isDirty()) {
				ShopResponse shopResponse = new ShopResponse();
				shopResponse.setShopStock(store.getStock());
				shopResponse.setShopName(ShopDao.getShopNameById(store.getShopId()));
				
				for (Player player : playerSessions.values()) {
					if (player.getShopId() == store.getShopId()) {
						responseMaps.addClientOnlyResponse(player, shopResponse);
					}
				}
				
				store.setDirty(false);
			}
		}
		Stopwatch.end("update shop stock");
	}
	
	private void updateLocalGroundTexturesAndScenery(ResponseMaps responseMaps) {
		Stopwatch.start("updating local ground textures and scenery");
		for (Player player : playerSessions.values()) {
			if (player.getState() == PlayerState.dead)
				continue; // we dont want to update the client while the "you are dead" screen is fading in

			Set<Integer> currentLocalTiles = player.getLocalTiles();
			Set<Integer> newLocalTiles = WorldProcessor.getLocalTiles(player.getTileId(), 12);
			
			Set<Integer> removedTileIds = currentLocalTiles.stream().filter(e -> player.getLoadedFloor() != player.getFloor() || !newLocalTiles.contains(e)).collect(Collectors.toSet());
			if (!removedTileIds.isEmpty()) {
				RemoveGroundTextureInstancesResponse removeResponse = new RemoveGroundTextureInstancesResponse();
				removeResponse.setTileIds(removedTileIds);
				responseMaps.addClientOnlyResponse(player, removeResponse);
			}
			
			Set<Integer> addedTileIds = newLocalTiles.stream().filter(e -> player.getLoadedFloor() != player.getFloor() || !currentLocalTiles.contains(e)).collect(Collectors.toSet());
			if (!addedTileIds.isEmpty()) {
				Map<Integer, Set<Integer>> tileIdsByGroundTextureId = new HashMap<>();
				Map<Integer, Set<Integer>> addedSceneryIds = new HashMap<>();
				
				for (int tileId : addedTileIds) {
					int groundTextureId = GroundTextureDao.getGroundTextureIdByTileId(player.getFloor(), tileId);
					if (!tileIdsByGroundTextureId.containsKey(groundTextureId))
						tileIdsByGroundTextureId.put(groundTextureId, new HashSet<>());
					tileIdsByGroundTextureId.get(groundTextureId).add(tileId);
					
					int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), tileId);
					if (sceneryId != -1) {
						if (!addedSceneryIds.containsKey(sceneryId))
							addedSceneryIds.put(sceneryId, new HashSet<>());
						addedSceneryIds.get(sceneryId).add(tileId);
					}
				}
				
				AddGroundTextureInstancesResponse addResponse = new AddGroundTextureInstancesResponse();
				addResponse.setInstances(tileIdsByGroundTextureId);
				responseMaps.addClientOnlyResponse(player, addResponse);
				
				if (!addedSceneryIds.isEmpty()) {
					AddSceneryInstancesResponse addSceneryResponse = new AddSceneryInstancesResponse();
					addSceneryResponse.setInstances(addedSceneryIds);
					responseMaps.addClientOnlyResponse(player, addSceneryResponse);
					
					HashSet<Integer> depletedScenery = new HashSet<>();					
					
					depletedScenery.addAll(RockManager.getDepletedRockTileIds(player.getFloor())
												      .stream()
												      .filter(rockTileId -> newLocalTiles.contains(rockTileId))
												      .collect(Collectors.toSet()));
					
					depletedScenery.addAll(FlowerManager.getDepletedFlowerTileIds(player.getFloor())
														.stream()
														.filter(flowerTileId -> newLocalTiles.contains(flowerTileId))
														.collect(Collectors.toSet()));
					
					addSceneryResponse.setDepletedScenery(depletedScenery);
				}
			}
			
			// minimap segments
			Set<Integer> newSegments = MinimapSegmentDao.getSegmentIdsFromTileId(player.getTileId());
			Set<Integer> currentSegments = player.getLoadedMinimapSegments();
			
			Set<Integer> removedSegments = currentSegments.stream().filter(e -> player.getLoadedFloor() != player.getFloor() || !newSegments.contains(e)).collect(Collectors.toSet());
			if (!removedSegments.isEmpty()) {
				// remove minimap segments response
				RemoveMinimapSegmentsResponse minimapResponse = new RemoveMinimapSegmentsResponse();
				minimapResponse.setSegments(removedSegments);
				responseMaps.addClientOnlyResponse(player, minimapResponse);
			}
			
			Set<Integer> addedSegments = newSegments.stream().filter(e -> player.getLoadedFloor() != player.getFloor() || !currentSegments.contains(e)).collect(Collectors.toSet());
			if (!addedSegments.isEmpty()) {
				Map<Integer, Set<Integer>> minimapIconLocations = new HashMap<>();
				Map<Integer, String> segments = new HashMap<>();
				for (int segmentId : addedSegments) {
					segments.put(segmentId, MinimapSegmentDao.getMinimapDataByFloorAndSegmentId(player.getFloor(), segmentId));
					
					Map<Integer, Set<Integer>> minimapIconLocationsByFloorAndSegment = MinimapSegmentDao.getMinimapIconLocationsByFloorAndSegment(player.getFloor(), segmentId);
					for (Map.Entry<Integer, Set<Integer>> entry : minimapIconLocationsByFloorAndSegment.entrySet()) {
						if (!minimapIconLocations.containsKey(entry.getKey()))
							minimapIconLocations.put(entry.getKey(), new HashSet<>());
						minimapIconLocations.get(entry.getKey()).addAll(entry.getValue());
					}
				}
				
				
				AddMinimapSegmentsResponse minimapResponse = new AddMinimapSegmentsResponse();
				minimapResponse.setSegments(segments);
				minimapResponse.setMinimapIconLocations(minimapIconLocations);
				responseMaps.addClientOnlyResponse(player, minimapResponse);
			}
			
			player.setLocalTiles(newLocalTiles);
			player.setLoadedFloor(player.getFloor());
			player.setLoadedMinimapSegments(newSegments);
		}
		Stopwatch.end("updating local ground textures and scenery");
	}
	
	private void updateLocalNpcLocations(ResponseMaps responseMaps) {
		Stopwatch.start("refresh npc locations");
		for (Map.Entry<Session, Player> entry : playerSessions.entrySet()) {
			Set<Integer> currentInRangeNpcs = entry.getValue().getInRangeNpcs();
			Set<Integer> newInRangeNpcs = NPCManager.get().getNpcsNearTile(entry.getValue().getFloor(), entry.getValue().getTileId(), 15)
												    .stream()
												    .filter(e -> !e.isDeadWithDelay())	// the delay of two ticks gives the client time for the death animation
												    .map(NPC::getInstanceId)
												    .collect(Collectors.toSet());
			
			Set<Integer> removedNpcs = currentInRangeNpcs.stream().filter(e -> !newInRangeNpcs.contains(e)).collect(Collectors.toSet());
			if (!removedNpcs.isEmpty()) {
				NpcOutOfRangeResponse npcOutOfRangeResponse = new NpcOutOfRangeResponse();
				npcOutOfRangeResponse.setInstances(removedNpcs);
				responseMaps.addClientOnlyResponse(entry.getValue(), npcOutOfRangeResponse);
			}
			
			Set<Integer> addedNpcs = newInRangeNpcs.stream().filter(e -> !currentInRangeNpcs.contains(e)).collect(Collectors.toSet());
			if (!addedNpcs.isEmpty()) {
				NpcInRangeResponse npcInRangeResponse = new NpcInRangeResponse();
				npcInRangeResponse.addInstances(entry.getValue().getFloor(), addedNpcs);
				responseMaps.addClientOnlyResponse(entry.getValue(), npcInRangeResponse);
			}
			entry.getValue().setInRangeNpcs(newInRangeNpcs);
		}
		Stopwatch.end("refresh npc locations");
	}
	
	public static boolean sessionExistsByPlayerId(int playerId) {
		return playerSessions.values().stream().anyMatch(e -> e.getId() == playerId);
	}
}
