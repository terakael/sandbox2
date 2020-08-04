package main.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import main.database.GroundTextureDao;
import main.database.SceneryDao;
import main.types.ImpassableTypes;
import main.utils.Stopwatch;
import main.utils.Utils;

public class PathFinder {
	private static PathFinder instance;
	
	public static final int LENGTH = 25000;
	private static Map<Integer, Map<Integer, PathNode>> nodesByFloor; // floor, <tileId, node> (the tileId map is so we can quickly retrieve by tileId)
	
	private PathFinder() {
		nodesByFloor = new HashMap<>();
		Set<Integer> distinctFloors = GroundTextureDao.getDistinctFloors();
		for (int floor : distinctFloors) {
			Set<Integer> tileIds = GroundTextureDao.getAllWalkableTileIdsByFloor(floor);
			if (tileIds.isEmpty())
				continue;
			
			nodesByFloor.put(floor, new HashMap<>());
			HashMap<Integer, Integer> tileIdImpassability = SceneryDao.getImpassableTileIdsByFloor(floor);
			
			for (int tileId : tileIds) {
				PathNode node = new PathNode();
				node.setId(tileId);
				node.setImpassableTypes(tileIdImpassability.get(tileId) == null ? 0 : tileIdImpassability.get(tileId));
				nodesByFloor.get(floor).put(tileId, node);
			}
			
			for (int tileId : tileIds) {
				int[] tileIdsToCheck = {
					tileId - LENGTH - 1,	// top left
					tileId - LENGTH,		// top
					tileId - LENGTH + 1,	// top right
					tileId - 1,				// left
					tileId + 1,				// right
					tileId + LENGTH - 1,	// bottom left
					tileId + LENGTH,		// bottom
					tileId + LENGTH + 1		// bottom right
				};
				
				PathNode currentNode = nodesByFloor.get(floor).get(tileId); 
				PathNode[] siblings = new PathNode[tileIdsToCheck.length];
				for (int i = 0; i < tileIdsToCheck.length; ++i) {
					siblings[i] = nodesByFloor.get(floor).get(tileIdsToCheck[i]);
				}
				
				currentNode.setSiblings(siblings);
			}
		}
	}
	
	
	public static PathFinder get() {
		if (instance == null)
			instance = new PathFinder();
		return instance;
	}
	
	@Setter @Getter
	public class PathNode {
		private PathNode[] siblings;
		int impassableTypes = 0;
		int weight = 1;
		int id = 0;
		
		double g = 0;
		double h = 0;
		PathNode parent = null;
		
		public void setAsParent() {
			for (PathNode node : siblings) {
				if (node != null && node != parent) {
					node.setParent(this);
				}
			}
		}
		
		public PathNode getSibling(int elementId) {
			if (elementId >= 0 && elementId < siblings.length)
				return siblings[elementId];
			return null;
		}
		
		public boolean isSiblingPassable(int elementId) {
			PathNode sibling = getSibling(elementId);
			if (sibling == null)
				return false;// if it doesn't exist then it's obviously not passable
			
			switch (elementId) {
			case 0: {
				// sibling is upper-left
				if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.LEFT, impassableTypes)) {
					return false;
				}
				
				if (ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, sibling.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, sibling.impassableTypes)) {
					return false;
				}
				PathNode upper = getSibling(1);
				if (upper == null || ImpassableTypes.isImpassable(ImpassableTypes.LEFT, upper.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, upper.impassableTypes)) {
					return false;
				}
				PathNode left = getSibling(3);
				if (left == null || ImpassableTypes.isImpassable(ImpassableTypes.TOP, left.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, left.impassableTypes)) {
					return false;
				}
				return true;
			}
			case 1: {
				// sibling is above
				return !ImpassableTypes.isImpassable(ImpassableTypes.TOP, impassableTypes) &&
					   !ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, sibling.impassableTypes);
			}
			case 2: {
				// sibling is upper-right
				if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, impassableTypes)) {
					return false;
				}
				
				if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT, sibling.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, sibling.impassableTypes)) {
					return false;
				}
				PathNode upper = getSibling(1);
				if (upper == null || ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, upper.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, upper.impassableTypes)) {
					return false;
				}
				PathNode right = getSibling(4);
				if (right == null || ImpassableTypes.isImpassable(ImpassableTypes.TOP, right.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.LEFT, right.impassableTypes)) {
					return false;
				}
				return true;
			}
			case 3: {
				// sibling is to the left
				return !ImpassableTypes.isImpassable(ImpassableTypes.LEFT, impassableTypes) &&
					   !ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, sibling.impassableTypes);
			}
			case 4: {
				// sibling is to the right
				return !ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, impassableTypes) &&
					   !ImpassableTypes.isImpassable(ImpassableTypes.LEFT, sibling.impassableTypes);
			}
			case 5: {
				// sibling is lower-left
				if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.LEFT, impassableTypes)) {
					return false;
				}
				
				if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, sibling.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, sibling.impassableTypes)) {
					return false;
				}
				PathNode lower = getSibling(6);
				if (lower == null || ImpassableTypes.isImpassable(ImpassableTypes.LEFT, lower.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.TOP, lower.impassableTypes)) {
					return false;
				}
				PathNode left = getSibling(3);
				if (left == null || ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, left.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, left.impassableTypes)) {
					return false;
				}
				return true;
			}
			case 6: {
				// siblings is below:
				return !ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, impassableTypes) &&
					   !ImpassableTypes.isImpassable(ImpassableTypes.TOP, sibling.impassableTypes);
			}
			case 7: {
				// sibling is lower-right
				if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, impassableTypes)) {
					return false;
				}
				
				if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT, sibling.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.TOP, sibling.impassableTypes)) {
					return false;
				}
				PathNode lower = getSibling(6);
				if (lower == null || ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, lower.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.TOP, lower.impassableTypes)) {
					return false;
				}
				PathNode right = getSibling(4);
				if (right == null || ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, right.impassableTypes) ||
					ImpassableTypes.isImpassable(ImpassableTypes.LEFT, right.impassableTypes)) {
					return false;
				}
				return true;
			}
			default:
				return false;
			}
			
//			return sibling != null && sibling.getWeight() != -1;
		}
		
		public double getF() {
			return g + h;
		}
	}
	
	public static Stack<Integer> findPath(int floor, int from, int to, boolean includeToTile) {
		return findPath(floor, from, to, includeToTile, 0, 0);
	}
	
	public static Stack<Integer> findPath(int floor, int from, int to, boolean includeToTile, int spawnTileId, int maxRadius) {
		Stopwatch.start("find path");
		Stack<Integer> output = new Stack<>();
		if (from == to)
			return output;

		Map<Integer, PathNode> nodes = nodesByFloor.get(floor);
		
		if (!nodes.containsKey(to)) {
//			System.out.println(String.format("trying to go from %d to tile %d which is outside of range (floor %d)", from, to, floor));
			// find the closest walkable tile from the "to" tile closest to the "from"
			
			int fromX = from % LENGTH;
			int fromY = from / LENGTH;
			
			int toX = to % LENGTH;
			int toY = to / LENGTH;
			
			// get that ratio: if fromX and toX are the same X (i.e. diffY is zero) then only modify X to find the closest.
			// if diffX and diffY are equal then alternate: modify X, modify Y, modify X, modify Y
			// if diffX is twice diffY then alternate: modifyX, modifyX, modifyY, modifyX, modifyX, modifyY etc
			int diffX = fromX - toX;
			int diffY = fromY - toY;
			
			// 469418857
			// 469318867
			
//			int gcm = Utils.gcm(Math.abs(diffX), Math.abs(diffY));
//			int ratioX = Math.abs(diffX) / gcm;
//			int ratioY = Math.abs(diffY) / gcm;
			
			do {
				if (diffX != 0) {
					if (nodes.containsKey(to + (diffX / Math.abs(diffX)))) {
						to += (diffX / Math.abs(diffX));
						break;
					}
				}
				
				if (diffY != 0) {
					if (nodes.containsKey(to + ((diffY / Math.abs(diffY)) * LENGTH))) {
						to += (diffY / Math.abs(diffY)) * LENGTH;
						break;
					}
				}
				
				
				if (diffX != 0) {
					to += (diffX / Math.abs(diffX));
					diffX -= diffX / Math.abs(diffX);
				}
				
				if (diffY != 0) {
					to += ((diffY / Math.abs(diffY)) * LENGTH);
					diffY -= (diffY / Math.abs(diffY));
				}

//				if (diffX != 0) {
//					for (int i = 0; i < ratioX; ++i) {
//						to += diffX / Math.abs(diffX); // +1 or -1 depending on direction
//						if (nodes.containsKey(to))
//							break;
//					}
//				}
//				
//				if (diffY != 0) {
//					for (int i = 0; i < ratioY; ++i) {
//						to += (diffY / Math.abs(diffY)) * LENGTH; // up one tile or down one tile depending on direction
//						if (nodes.containsKey(to))
//							break;
//					}
//				}
			} while (!(diffX == 0 && diffY == 0));
			
			if (to == from || !nodes.containsKey(to)) // we've ended up on the same tile or the start tile was invalid
				return output;
		}
		
		// cannot move to an impassable tile.
		if (nodes.get(to).getImpassableTypes() == 15) // TODO inaccurate
			includeToTile = false;
		
		ArrayList<PathNode> open = new ArrayList<>();
		ArrayList<PathNode> closed = new ArrayList<>();
		
		nodes.get(from).setParent(null);
		open.add(nodes.get(from));
		
		while (!open.isEmpty() ) {
			// get lowest F node from open list
			PathNode q = Collections.min(open, Comparator.comparing(s -> s.getF()));

			open.remove(q);
			closed.add(q);
			if (closed.size() > 100) {
				// if we hit 500 checked tiles then bail, thats way too many and its probably an impossible path
				//System.out.println(String.format("500+ closed: ms=%d, open=%d, closed=%d, from=%d, to=%d", Stopwatch.getMs("find path"), open.size(), closed.size(), from, to));
				return output;
			}
			
			for (int i = 0; i < q.getSiblings().length; ++i) {
				// sometimes the sibling isn't passable, but the sibling happens to be the destination node.
				// we obviously want to find the destination node so we cannot "continue" in this case.
				if (!q.isSiblingPassable(i) && q.getSibling(i) != nodes.get(to))
					continue;
				
				PathNode successor = q.getSibling(i);				
				if (successor == null || (successor.getWeight() == -1 && successor != nodes.get(to)))// corner and edge nodes have some null siblings
					continue;
				
				// we're at the final step - if the final step is completely impassable we want to keep processing
				// but if there is some passable way and we're not actually next to it then continue.
				if (successor == nodes.get(to) && successor.impassableTypes != 15 && !isNextTo(floor, q.id, successor.id))
					continue;
				
				if (spawnTileId > 0 && maxRadius > 0) {
					int tileX = successor.getId() % LENGTH;
					int tileY = successor.getId() / LENGTH;
					
					int spawnTileX = spawnTileId % LENGTH;
					int spawnTileY = spawnTileId / LENGTH;
					
					if (tileX < spawnTileX - maxRadius || tileX > spawnTileX + maxRadius)
						continue;
					
					if (tileY < spawnTileY - maxRadius || tileY > spawnTileY + maxRadius)
						continue;
				}
				
				if (closed.contains(successor))
					continue;

				boolean isDiagonal = i == 0 || i == 2 || i == 5 || i == 7;
				
				double newG = q.getG() + (isDiagonal ? 1.414 : 1.0);
				double newH = calculateManhattan(successor.getId(), to);
				double newF = newG + newH;

				if ((newF < successor.getF() || !open.contains(successor)) && (!(successor == nodes.get(to) && isDiagonal))) {
					successor.setG(newG);
					successor.setH(newH);
					successor.setParent(q);
					
					if (!open.contains(successor))
						open.add(successor);
				}
				
				if (successor == nodes.get(to)) {
					if (isDiagonal)
						continue;
					
					// found it
					if (!includeToTile)
						successor = successor.getParent();
					
					while (successor.getParent() != null) {
						output.push(successor.getId());
						successor = successor.getParent();
					}
					
					Stopwatch.end("find path");
					if (Stopwatch.getMs("find path") > 100) {
						System.out.println(String.format("WEIRD PATHFIND: ms=%d, open=%d, closed=%d, from=%d, to=%d, getLocalPathNodes=%d", Stopwatch.getMs("find path"), open.size(), closed.size(), from, to, Stopwatch.getMs("getLocalPathNodes")));
						
						Stopwatch.dump();
					}
					return output;
				}
			}
		}
		
		Stopwatch.end("find path");
		if (Stopwatch.getMs("find path") > 100) {
			System.out.println(String.format("BAD PATHFIND: ms=%d, open=%d, closed=%d, from=%d, to=%d", Stopwatch.getMs("find path"), open.size(), closed.size(), from, to));
		}
		return output;
	}
	
	private static double calculateManhattan(int src, int dest) {
		return Math.abs(src % LENGTH - dest % LENGTH) + Math.abs(src / LENGTH - dest / LENGTH);
	}
	
	public static boolean isNextTo(int floor, int srcTile, int destTile) {
		// returns true if srcTile and destTile are touching horizontally or vertically (or are the same tile)
		// if the tiles are next to eachother but there's a barrier between them (non-zero impassableType)
		// then they are not technically next to eachother (i.e. there's a wall between the tiles).
		// the exception to this is if the dest tile is completely impassable i.e. impassableType 15 like a rock etc
		// in this case we are next to it, as there's no way to get any closer.
		int srcImpassableType = SceneryDao.getImpassableTypeByFloor(floor, srcTile);		
		int destImpassableType = SceneryDao.getImpassableTypeByFloor(floor, destTile);
		
		return  
			// destTile is to the left, destTile doesn't have a right-facing wall, srcTile doesn't have a left-facing wall
			((destTile == srcTile - 1 || (srcTile % LENGTH == 0 && destTile == srcTile + LENGTH - 1))  && 
				((!ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, destImpassableType) && 
					!ImpassableTypes.isImpassable(ImpassableTypes.LEFT, srcImpassableType)) || destImpassableType == 15)) ||
			
			// destTile is to the right; destTile doesn't have a left-facing wall, srcTile doesn't have a right-facing wall
			((destTile == srcTile + 1 || (srcTile % LENGTH == LENGTH - 1 && destTile == srcTile - LENGTH + 1))&& 
				((!ImpassableTypes.isImpassable(ImpassableTypes.LEFT, destImpassableType) && 
					!ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, srcImpassableType)) || destImpassableType == 15)) ||
			
			// destTile is above; destTile doesn't have a bottom-facing wall, srcTile doesn't have a top-facing wall
			(destTile == srcTile - LENGTH && 
				((!ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, destImpassableType) && 
					!ImpassableTypes.isImpassable(ImpassableTypes.TOP, srcImpassableType)) || destImpassableType == 15)) ||
			
			// destTile is below; destTile doesn't have a top-facing wall, srcTile doesn't have a bottom-facing wall
			(destTile == srcTile + LENGTH &&
				((!ImpassableTypes.isImpassable(ImpassableTypes.TOP, destImpassableType) && 
					!ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, srcImpassableType)) || destImpassableType == 15))
			
			|| destTile == srcTile;// same tile
	}
	
	public static int chooseRandomTileIdInRadius(int tileId, int radius) {
		
		int tileX = tileId % LENGTH;
		int tileY = tileId / LENGTH;
		
		int max = radius;
		int min = -radius;
		
		int newTile = 0;
		do {
			Random r = new Random();
			tileX += r.nextInt((max - min) + 1) + min;
			tileY += r.nextInt((max - min) + 1) + min;
			
			newTile = tileX + (tileY * LENGTH);
		} while (newTile < 0 || newTile >= (LENGTH * LENGTH));
		
		return newTile;
	}
	
	public static boolean tileWithinRadius(int tileToCheck, int radiusCentreTile, int radius) {
		// use manhattan algorithm to check
		int centreX = radiusCentreTile % LENGTH;
		int centreY = radiusCentreTile / LENGTH;
		
		int checkX = tileToCheck % LENGTH;
		int checkY = tileToCheck / LENGTH;
		
		return Math.abs(centreX - checkX) + Math.abs(centreY - checkY) <= radius;
	}
	
	public static int findRetreatTile(int retreatFromTile, int startTile, int anchorTile, int radius) {
		// TODO might be better to precalc each npcs available tiles and choose from that list
		int retreatFromX = retreatFromTile % LENGTH;
		int retreatFromY = retreatFromTile / LENGTH;
		
		int startX = startTile % LENGTH;
		int startY = startTile / LENGTH;
		
		boolean retreatToRight = retreatFromX - startX > 0;
		boolean retreatUpwards = retreatFromY - startY > 0;
		
		int anchorX = anchorTile % LENGTH;
		int anchorY = anchorTile / LENGTH;
		
		int retreatX = anchorX + (radius * (retreatToRight ? -1 : 1));
		int retreatY = anchorY + (radius * (retreatUpwards ? -1 : 1));
		
		return (retreatY * LENGTH) + retreatX;
	}
	
	public static boolean tileIsValid(int floor, int tileId) {
		if (!nodesByFloor.containsKey(floor))
			return false;
		
		return nodesByFloor.get(floor).containsKey(tileId);
	}
}
