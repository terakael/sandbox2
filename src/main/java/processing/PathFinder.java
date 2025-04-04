package processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import database.dao.DoorDao;
import database.dao.GroundTextureDao;
import database.dao.SceneryDao;
import lombok.Getter;
import lombok.Setter;
import processing.managers.WallManager;
import types.ImpassableTypes;
import utils.Stopwatch;
import utils.Utils;

public class PathFinder {
	private static PathFinder instance;

	public static final int LENGTH = 46325; // the biggest number divisible by minimap segments whose square fits in a
											// max int (albeit signed) TODO look unto unsigned int
	private static Map<Integer, Map<Integer, PathNode>> nodesByFloor = new HashMap<>(); // floor, <tileId, node> (the
																						// tileId map is so we can
																						// quickly retrieve by tileId)
	private static Map<Integer, Map<Integer, PathNode>> sailableNodesByFloor = new HashMap<>();

	private PathFinder() {
		Set<Integer> distinctFloors = GroundTextureDao.getDistinctFloors();
		for (int floor : distinctFloors) {
			loadNodesByFloor(floor, nodesByFloor, GroundTextureDao.getAllWalkableTileIdsByFloor(floor));
			loadNodesByFloor(floor, sailableNodesByFloor, GroundTextureDao.getAllSailableTileIdsByFloor(floor));
		}
	}

	private void loadNodesByFloor(int floor, Map<Integer, Map<Integer, PathNode>> nodes, Set<Integer> tileIds) {
		if (tileIds.isEmpty())
			return;

		nodes.put(floor, new HashMap<>());

		// we want any tileId that has scenery and/or wall on it.
		// if it has both, we want the combined impassable of the two.
		final Set<Integer> sceneryTileIds = SceneryDao.getAllSceneryInstancesByFloor(floor).values().stream()
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
		sceneryTileIds.addAll(WallManager.getWallTileIdsByFloor(floor));

		final Map<Integer, Integer> tileIdImpassability = sceneryTileIds.stream()
				.collect(Collectors.toMap(Function.identity(), tileId -> {
					return SceneryDao.getImpassableTypeByFloorAndTileId(floor, tileId) |
							WallManager.getImpassableTypeByFloorAndTileId(floor, tileId);
				}));

		for (int tileId : tileIds) {
			PathNode node = new PathNode();
			node.setId(tileId);
			node.setImpassableTypes(tileIdImpassability.get(tileId) == null ? 0 : tileIdImpassability.get(tileId));
			nodes.get(floor).put(tileId, node);
		}

		for (int tileId : tileIds) {
			int[] tileIdsToCheck = {
					tileId - LENGTH - 1, // top left
					tileId - LENGTH, // top
					tileId - LENGTH + 1, // top right
					tileId - 1, // left
					tileId + 1, // right
					tileId + LENGTH - 1, // bottom left
					tileId + LENGTH, // bottom
					tileId + LENGTH + 1 // bottom right
			};

			PathNode currentNode = nodes.get(floor).get(tileId);
			PathNode[] siblings = new PathNode[tileIdsToCheck.length];
			for (int i = 0; i < tileIdsToCheck.length; ++i) {
				siblings[i] = nodes.get(floor).get(tileIdsToCheck[i]);
			}

			currentNode.setSiblings(siblings);
		}
	}

	public static PathFinder get() {
		if (instance == null)
			instance = new PathFinder();
		return instance;
	}

	@Setter
	@Getter
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

		private boolean isSiblingPassable(int elementId, int floor, boolean ignoreDoors) { // floor is needed for door
																							// checks
			PathNode sibling = getSibling(elementId);
			if (sibling == null)
				return false;// if it doesn't exist then it's obviously not passable

			switch (elementId) {
				case 0: {
					// sibling is upper-left
					int doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id);
					if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.LEFT, impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId());
					if (ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
							sibling.impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
									sibling.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id - LENGTH); // avoid
																													// null
																													// check
																													// for
																													// upper;
																													// if
																													// it
																													// doesn't
																													// exist
																													// in
																													// the
																													// array
																													// then
																													// it's
																													// 0
																													// (i.e.
																													// passable)
					PathNode upper = getSibling(1);
					if (upper == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
									upper.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
									upper.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id - 1); // avoid
																												// null
																												// check
																												// for
																												// left;
																												// if it
																												// doesn't
																												// exist
																												// in
																												// the
																												// array
																												// then
																												// it's
																												// 0
																												// (i.e.
																												// passable)
					PathNode left = getSibling(3);
					if (left == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.TOP,
									left.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									left.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					return true;
				}
				case 1: {
					// sibling is above
					return !ImpassableTypes.isImpassable(ImpassableTypes.TOP,
							impassableTypes | (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id))) &&
							!ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, sibling.impassableTypes
									| (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId())));
				}
				case 2: {
					// sibling is upper-right
					int doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id);
					if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId());
					if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
							sibling.impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
									sibling.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id - LENGTH);
					PathNode upper = getSibling(1);
					if (upper == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									upper.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
									upper.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id + 1);
					PathNode right = getSibling(4);
					if (right == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.TOP,
									right.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
									right.impassableTypes | doorImpassableTypes)) {
						return false;
					}
					return true;
				}
				case 3: {
					// sibling is to the left
					return !ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
							impassableTypes | (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id))) &&
							!ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, sibling.impassableTypes
									| (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId())));
				}
				case 4: {
					// sibling is to the right
					return !ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
							impassableTypes | (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id))) &&
							!ImpassableTypes.isImpassable(ImpassableTypes.LEFT, sibling.impassableTypes
									| (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId())));
				}
				case 5: {
					// sibling is lower-left
					int doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id);
					if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.LEFT, impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId());
					if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, sibling.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									sibling.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id + LENGTH);
					PathNode lower = getSibling(6);
					if (lower == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
									lower.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.TOP,
									lower.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id - 1);
					PathNode left = getSibling(3);
					if (left == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
									left.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									left.impassableTypes | doorImpassableTypes)) {
						return false;
					}
					return true;
				}
				case 6: {
					// siblings is below:
					return !ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
							impassableTypes | (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id))) &&
							!ImpassableTypes.isImpassable(ImpassableTypes.TOP, sibling.impassableTypes
									| (ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId())));
				}
				case 7: {
					// sibling is lower-right
					int doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id);
					if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, sibling.getId());
					if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
							sibling.impassableTypes | doorImpassableTypes) ||
							ImpassableTypes.isImpassable(ImpassableTypes.TOP,
									sibling.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id + LENGTH);
					PathNode lower = getSibling(6);
					if (lower == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.RIGHT,
									lower.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.TOP,
									lower.impassableTypes | doorImpassableTypes)) {
						return false;
					}

					doorImpassableTypes = ignoreDoors ? 0 : DoorDao.getDoorImpassableByTileId(floor, id + 1);
					PathNode right = getSibling(4);
					if (right == null ||
							ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM,
									right.impassableTypes | doorImpassableTypes)
							||
							ImpassableTypes.isImpassable(ImpassableTypes.LEFT,
									right.impassableTypes | doorImpassableTypes)) {
						return false;
					}
					return true;
				}
				default:
					return false;
			}
		}

		public double getF() {
			return g + h;
		}
	}

	public static Stack<Integer> findPath(int floor, int from, int to, boolean includeToTile) {
		// first we try to find a path that doesn't go through closed doors.
		// if that's not possible, find a path that includes closed doors.
		Stack<Integer> ints = findPathInternal(floor, from, to, includeToTile, false, 0, false);
		if (ints.isEmpty())
			ints = findPathInternal(floor, from, to, includeToTile, false, 0, true);
		return ints;
	}

	public static Stack<Integer> findPath(int floor, int from, int to, boolean includeToTile, boolean ignoreDoors) {
		return findPathInternal(floor, from, to, includeToTile, false, 0, ignoreDoors);
	}

	// Used for things such as casting spells (over walls etc)
	public static Stack<Integer> findPathInRange(int floor, int from, int to, int range) {
		return findPathInternal(floor, from, to, true, false, range, false);
	}

	public static Stack<Integer> findPathToDoor(int floor, int from, int to) {
		return findPathInternal(floor, from, to, true, true, 0, false);
	}

	// public static Stack<Integer> findPath(int floor, int from, int to, boolean
	// includeToTile, int spawnTileId, int maxRadius) {
	// return findPathInternal(floor, from, to, includeToTile, spawnTileId,
	// maxRadius, false, 0);
	// }

	private static Stack<Integer> findPathInternal(int floor, int from, int to, boolean includeToTile, boolean toDoor,
			int withinRange, boolean ignoreDoors) {
		Stopwatch.start("find path");
		Stack<Integer> output = new Stack<>();
		if (from == to) {
			if (includeToTile) {
				return output;
			} else {
				// we start on the final tile, but we don't want to be on the final tile.
				// example is a pet when dropped.
				// choose a random non-diagonal tile and move to that.
				List<Integer> nonDiagonalTiles = Arrays.asList(
						to - LENGTH,
						to - 1,
						to + 1,
						to + LENGTH);
				Collections.shuffle(nonDiagonalTiles);

				for (Integer tileId : nonDiagonalTiles) {
					if (tileIsWalkable(floor, tileId) && isNextTo(floor, tileId, to)) {
						output.push(tileId);
						return output;
					}
				}
			}
		}

		// chooses land- or water-based nodes based on the "from" tile
		Map<Integer, PathNode> nodes = getNodeMapByFloorAndTileId(floor, from);
		if (nodes == null)
			return output;

		if (!nodes.containsKey(to)) {
			// find the closest walkable tile from the "to" tile closest to the "from"
			int fromX = from % LENGTH;
			int fromY = from / LENGTH;

			int toX = to % LENGTH;
			int toY = to / LENGTH;

			// get that ratio: if fromX and toX are the same X (i.e. diffY is zero) then
			// only modify X to find the closest.
			// if diffX and diffY are equal then alternate: modify X, modify Y, modify X,
			// modify Y
			// if diffX is twice diffY then alternate: modifyX, modifyX, modifyY, modifyX,
			// modifyX, modifyY etc
			int diffX = fromX - toX;
			int diffY = fromY - toY;

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
			} while (!(diffX == 0 && diffY == 0));

			if (to == from || !nodes.containsKey(to)) // we've ended up on the same tile or the start tile was invalid
				return output;
		}

		// cannot move to an impassable tile.
		if ((nodes.get(to).getImpassableTypes() & 15) == 15) // TODO inaccurate
			includeToTile = false;

		ArrayList<PathNode> open = new ArrayList<>();
		ArrayList<PathNode> closed = new ArrayList<>();

		nodes.get(from).setParent(null);
		open.add(nodes.get(from));

		while (!open.isEmpty()) {
			// get lowest F node from open list
			PathNode q = Collections.min(open, Comparator.comparing(s -> s.getF()));

			open.remove(q);
			closed.add(q);
			if (closed.size() > 500) {
				// if we hit 500 checked tiles then bail, thats way too many and its probably an
				// impossible path
				// System.out.println(String.format("500+ closed: ms=%d, open=%d, closed=%d,
				// from=%d, to=%d", Stopwatch.getMs("find path"), open.size(), closed.size(),
				// from, to));
				return output;
			}

			for (int i = 0; i < q.getSiblings().length; ++i) {
				// sometimes the sibling isn't passable, but the sibling happens to be the
				// destination node.
				// we obviously want to find the destination node so we cannot "continue" in
				// this case.
				if (!q.isSiblingPassable(i, floor, ignoreDoors) && q.getSibling(i) != nodes.get(to))
					continue;

				PathNode successor = q.getSibling(i);
				if (successor == null || (successor.getWeight() == -1 && successor != nodes.get(to)))// corner and edge
																										// nodes have
																										// some null
																										// siblings
					continue;

				// we're at the final step - if the final step is completely impassable we want
				// to keep processing
				// but if there is some passable way and we're not actually next to it then
				// continue.
				if (successor == nodes.get(to) && !isNextTo(floor, q.id, successor.id, false, true))
					continue;

				if (closed.contains(successor))
					continue;

				boolean isDiagonal = i == 0 || i == 2 || i == 5 || i == 7;

				double newG = q.getG() + (isDiagonal ? 1.414 : 1.0);
				double newH = calculateManhattan(successor.getId(), to);
				double newF = newG + newH;

				if ((newF < successor.getF() || !open.contains(successor))
						&& (!(successor == nodes.get(to) && isDiagonal))) {
					successor.setG(newG);
					successor.setH(newH);
					successor.setParent(q);

					if (!open.contains(successor))
						open.add(successor);
				}

				// for things like magic, we just need to be within a certain range of the
				// target.
				// if we're within range and there's nothing blocking the distance between the
				// current tile and the target (walls etc),
				// then this tile will do, return the path from here.
				if (withinRange > 0) {
					// Bresenham's algorithm?
					// cast a ray from this tile to the target tile, and note all tiles it passes
					// through.
					// for each of the tiles it passes through, check if the ray passes through an
					// impassable.
					// if it doesn't pass through any impassables, then this can be counted as the
					// destination.
					if (lineOfSightIsClear(floor, successor.getId(), to, withinRange)) {
						while (successor.getParent() != null) {
							output.push(successor.getId());
							successor = successor.getParent();
						}

						Stopwatch.end("find path");
						return output;
					}
				}

				if (successor == nodes.get(to)) {
					if (isDiagonal)
						continue;

					// found it
					if (!includeToTile || (toDoor && !isNextTo(floor, q.id, successor.id, true, true)))
						successor = successor.getParent();

					while (successor.getParent() != null) {
						output.push(successor.getId());
						successor = successor.getParent();
					}

					Stopwatch.end("find path");
					if (Stopwatch.getMs("find path") > 100) {
						System.out.println(String.format(
								"WEIRD PATHFIND: ms=%d, open=%d, closed=%d, from=%d, to=%d, getLocalPathNodes=%d",
								Stopwatch.getMs("find path"), open.size(), closed.size(), from, to,
								Stopwatch.getMs("getLocalPathNodes")));

						Stopwatch.dump();
					}
					return output;
				}
			}
		}

		Stopwatch.end("find path");
		if (Stopwatch.getMs("find path") > 100) {
			System.out.println(String.format("BAD PATHFIND: ms=%d, open=%d, closed=%d, from=%d, to=%d",
					Stopwatch.getMs("find path"), open.size(), closed.size(), from, to));
		}
		return output;
	}

	private static double calculateManhattan(int src, int dest) {
		return Math.abs(src % LENGTH - dest % LENGTH) + Math.abs(src / LENGTH - dest / LENGTH);
	}

	public static double calculateDistance(int src, int dest) {
		final int x0 = src % LENGTH;
		final int y0 = src / LENGTH;

		final int x1 = dest % LENGTH;
		final int y1 = dest / LENGTH;

		return Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));
	}

	public static boolean lineOfSightIsClear(int floor, int srcTileId, int destTileId, int range) {
		final int x0 = srcTileId % LENGTH;
		final int y0 = srcTileId / LENGTH;

		final int x1 = destTileId % LENGTH;
		final int y1 = destTileId / LENGTH;

		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);

		if (dx + dy > range)
			return false;

		int x = x0;
		int y = y0;
		int n = 1 + dx + dy;
		int x_inc = (x1 > x0) ? 1 : -1;
		int y_inc = (y1 > y0) ? 1 : -1;
		int error = dx - dy;
		dx *= 2;
		dy *= 2;

		int prevTileId = srcTileId;
		for (; n > 0; --n) {
			// check if tile has impassable that intersects the line, returning false if
			// true
			final int checkTileId = (y * LENGTH) + x;

			if (!isNextTo(floor, prevTileId, checkTileId, true, false)) {
				// despite the two tiles being next to eachother, they are not "next to"
				// eachother,
				// meaning there is a barrier in the way.
				return false;
			}
			prevTileId = checkTileId;

			if (error > 0) {
				x += x_inc;
				error -= dy;
			} else {
				y += y_inc;
				error += dx;
			}
		}

		return true;
	}

	// standard behaviour is to include door impassables; only opening/closing doors
	// changes this behaviour
	public static boolean isNextTo(int floor, int srcTile, int destTile) {
		return isNextTo(floor, srcTile, destTile, true, true);
	}

	public static boolean isNextTo(int floor, int srcTile, int destTile, boolean includeDoors,
			boolean includeLowWalls) {
		// returns true if srcTile and destTile are touching horizontally or vertically
		// (or are the same tile)
		// if the tiles are next to eachother but there's a barrier between them
		// (non-zero impassableType)
		// then they are not technically next to eachother (i.e. there's a wall between
		// the tiles).
		// the exception to this is if the dest tile is completely impassable i.e.
		// impassableType 15 like a rock etc
		// in this case we are next to it, as there's no way to get any closer.

		// early fail if the tiles aren't adjacent
		if (!isAdjacent(srcTile, destTile) && srcTile != destTile)
			return false;

		// ok we're adjacent, are there any impassables between us?
		int srcImpassableType = getImpassableByTileId(floor, srcTile);

		// for the destination, let's ignore any impassable types of 15; could be a
		// rock/tree/ladder etc
		int destImpassableType = getImpassableByTileId(floor, destTile);
		if ((destImpassableType & 15) == 15)
			destImpassableType -= 15;

		if (includeDoors) {
			srcImpassableType |= DoorDao.getDoorImpassableByTileId(floor, srcTile);
			destImpassableType |= DoorDao.getDoorImpassableByTileId(floor, destTile);
		}

		if (!includeLowWalls) {
			// if we're not including low walls, then clear the impassable type of walls
			// that are flagged as low
			if (ImpassableTypes.isImpassable(ImpassableTypes.TOP_IS_LOW, srcImpassableType))
				srcImpassableType &= ~ImpassableTypes.TOP.getValue();

			if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT_IS_LOW, srcImpassableType))
				srcImpassableType &= ~ImpassableTypes.LEFT.getValue();

			if (ImpassableTypes.isImpassable(ImpassableTypes.RIGHT_IS_LOW, srcImpassableType))
				srcImpassableType &= ~ImpassableTypes.RIGHT.getValue();

			if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM_IS_LOW, srcImpassableType))
				srcImpassableType &= ~ImpassableTypes.BOTTOM.getValue();

			// also do dest
			if (ImpassableTypes.isImpassable(ImpassableTypes.TOP_IS_LOW, destImpassableType))
				destImpassableType &= ~ImpassableTypes.TOP.getValue();

			if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT_IS_LOW, destImpassableType))
				destImpassableType &= ~ImpassableTypes.LEFT.getValue();

			if (ImpassableTypes.isImpassable(ImpassableTypes.RIGHT_IS_LOW, destImpassableType))
				destImpassableType &= ~ImpassableTypes.RIGHT.getValue();

			if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM_IS_LOW, destImpassableType))
				destImpassableType &= ~ImpassableTypes.BOTTOM.getValue();
		}

		if (destTile == srcTile - 1) {
			if (ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, destImpassableType))
				return false;

			if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT, srcImpassableType))
				return false;
		} else if (destTile == srcTile + 1) {
			if (ImpassableTypes.isImpassable(ImpassableTypes.LEFT, destImpassableType))
				return false;

			if (ImpassableTypes.isImpassable(ImpassableTypes.RIGHT, srcImpassableType))
				return false;
		} else if (destTile == srcTile - LENGTH) {
			if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, destImpassableType))
				return false;

			if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, srcImpassableType))
				return false;
		} else if (destTile == srcTile + LENGTH) {
			if (ImpassableTypes.isImpassable(ImpassableTypes.TOP, destImpassableType))
				return false;

			if (ImpassableTypes.isImpassable(ImpassableTypes.BOTTOM, srcImpassableType))
				return false;
		}
		return true;
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
		// TODO might be better to precalc each npcs available tiles and choose from
		// that list
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

	public static boolean tileIsWalkable(int floor, int tileId) {
		if (!nodesByFloor.containsKey(floor))
			return false;

		return nodesByFloor.get(floor).containsKey(tileId);
	}

	public static boolean tileIsSailable(int floor, int tileId) {
		if (!sailableNodesByFloor.containsKey(floor))
			return false;

		return sailableNodesByFloor.get(floor).containsKey(tileId);
	}

	public static boolean isNextToWater(int floor, int tileId) {
		// we're on the land, but one of our adjacent tiles is water
		return tileIsWalkable(floor, tileId) &&
				(tileIsSailable(floor, tileId - 1) ||
						tileIsSailable(floor, tileId + 1) ||
						tileIsSailable(floor, tileId - LENGTH) ||
						tileIsSailable(floor, tileId + LENGTH));
	}

	public static int getClosestWalkableTile(int floor, int tileId) {
		return getClosestWalkableTile(floor, tileId, true);
	}

	public static int getClosestWalkableTile(int floor, int tileId, boolean requireClearPath) {
		// assuming we're standing on tileId, branch out from there
		// i.e. tileId, then the eight surrounding tiles, then the 15 outside that
		for (int i = 0; i <= LENGTH; ++i) {
			int topLeft = tileId - i - (i * LENGTH);
			int bottomRight = tileId + i + (i * LENGTH);
			Set<Integer> checkTiles = new HashSet<>();
			for (int j = 0; j < (i * 2) + 1; ++j) {
				checkTiles.add(topLeft + j);
				checkTiles.add(topLeft + (j * LENGTH));

				checkTiles.add(bottomRight - j);
				checkTiles.add(bottomRight - (j * LENGTH));
			}

			int closestCheckTile = -1;

			checkTiles.removeIf(checkTileId -> !tileIsWalkable(floor, checkTileId)
					|| ((PathFinder.getImpassableByTileId(floor, checkTileId) & 15) == 15));
			if (!checkTiles.isEmpty()) {
				// we have sailable tiles; return the closest one with a valid path
				for (int checkTileId : checkTiles) {
					// if we're already next to it then that's the closest
					if (PathFinder.isAdjacent(checkTileId, tileId)) {
						// can't get much closer than next-to
						return checkTileId;
					}

					if (closestCheckTile == -1 || getCloserTile(tileId, checkTileId, closestCheckTile) == checkTileId) {
						final Stack<Integer> path = PathFinder.findPath(floor, tileId, checkTileId, true);
						if (!requireClearPath || !path.isEmpty())
							closestCheckTile = checkTileId;
					}
				}
			}

			if (closestCheckTile != -1)
				return closestCheckTile;
		}

		return -1;
	}

	public static int getClosestSailableTile(int floor, int tileId) {
		// assuming we're standing on tileId, branch out from there
		// i.e. tileId, then the eight surrounding tiles, then the 15 outside that

		// check a radius of 12 tiles (that's a 24x24 square)
		for (int i = 0; i <= 12; ++i) {
			int topLeft = tileId - i - (i * PathFinder.LENGTH);
			int bottomRight = tileId + i + (i * PathFinder.LENGTH);
			Set<Integer> checkTiles = new HashSet<>();
			for (int j = 0; j < (i * 2) + 1; ++j) {
				checkTiles.add(topLeft + j);
				checkTiles.add(topLeft + (j * LENGTH));

				checkTiles.add(bottomRight - j);
				checkTiles.add(bottomRight - (j * LENGTH));
			}

			int closestCheckTile = -1;

			checkTiles.removeIf(checkTileId -> !tileIsSailable(floor, checkTileId));
			if (!checkTiles.isEmpty()) {
				// we have sailable tiles; return the closest one with a valid path
				for (int checkTileId : checkTiles) {
					// if we're already next to it then that's the closest
					if (PathFinder.isAdjacent(checkTileId, tileId)) {
						// can't get much closer than next-to
						return checkTileId;
					}

					if (closestCheckTile == -1 || getCloserTile(tileId, checkTileId, closestCheckTile) == checkTileId) {
						final Stack<Integer> path = PathFinder.findPath(floor, tileId, checkTileId, true);
						if (!path.isEmpty())
							closestCheckTile = checkTileId;
					}
				}
			}

			if (closestCheckTile != -1)
				return closestCheckTile;
		}

		return -1;
	}

	public static Map<Integer, PathNode> getNodeMapByFloorAndTileId(int floor, int tileId) {
		if (tileIsWalkable(floor, tileId))
			return nodesByFloor.get(floor);

		if (tileIsSailable(floor, tileId))
			return sailableNodesByFloor.get(floor);

		return null;
	}

	public static int getImpassableByTileId(int floor, int tileId) {
		final Map<Integer, PathNode> nodes = getNodeMapByFloorAndTileId(floor, tileId);
		if (nodes == null)
			return 0;
		return nodes.get(tileId).getImpassableTypes();
	}

	public static String getDirection(int srcTileId, int destTileId) {
		String direction = "";
		if (srcTileId + 1 == destTileId)
			direction = "right";
		else if (srcTileId - 1 == destTileId)
			direction = "left";
		else if (srcTileId + LENGTH == destTileId)
			direction = "down";
		else if (srcTileId - LENGTH == destTileId)
			direction = "up";

		return direction;
	}

	public static boolean isDiagonal(int src, int dest) {
		return src == (dest - LENGTH - 1) // top-left
				|| src == (dest - LENGTH + 1) // top-right
				|| src == (dest + LENGTH - 1) // bottom-left
				|| src == (dest + LENGTH + 1); // bottom-right
	}

	public static boolean isAdjacent(int src, int dest) {
		return src == dest - 1 ||
				src == dest + 1 ||
				src == dest - LENGTH ||
				src == dest + LENGTH;
	}

	public static void setImpassabilityOnTileId(int floor, int tileId, int impassability) {
		// this is used for the undead army ents; they're impassable trees until a
		// certain wave where they become npcs.
		if (!nodesByFloor.containsKey(floor))
			return;

		if (!nodesByFloor.get(floor).containsKey(tileId))
			return;

		nodesByFloor.get(floor).get(tileId).setImpassableTypes(impassability);
	}

	public static List<Integer> calculateWalkableTiles(int floor, int tileId, int radius) {
		// things to consider
		// if there's a wall/lake going through the tiles within the radius,
		// we don't want to walk outside of our walkable tiles, around the wall/lake,
		// to get to the walkable tiles on the other side.

		final List<Integer> walkableTiles = new ArrayList<>();

		final List<Integer> localTiles = Utils.getLocalTiles(tileId, radius).stream().collect(Collectors.toList());
		for (int localTileId : localTiles) {
			if (!PathFinder.tileIsWalkable(floor, localTileId))
				continue;

			if ((PathFinder.getImpassableByTileId(floor, localTileId) & 15) == 15) // there's something impassable on it
				continue;

			final Stack<Integer> path = PathFinder.findPath(floor, tileId, localTileId, true);
			while (!path.isEmpty()) {
				final int currentTileId = path.pop();
				if (!localTiles.contains(currentTileId)) {
					// we need to walk outside our radius to get to this tile discount it.
					break;
				}
			}

			if (!path.isEmpty()) // we broke early, meaning we walked outside of the local tiles
				continue;

			walkableTiles.add(localTileId);
		}

		return walkableTiles;
	}

	public static int calculateThroughTileId(int tileId, int impassable) {
		// we assume the impassable is a single side i.e. 1/2/4/8.
		// if it's multiple sides then who knows what the through tileId is...

		switch (impassable) {
			case 0: // no sides, return the input tileId
				return tileId;

			case 1:
				return tileId - PathFinder.LENGTH;

			case 2:
				return tileId - 1;

			case 4:
				return tileId + 1;

			case 8:
				return tileId + PathFinder.LENGTH;

			default: // if there are multiple sides then fuck it basically
				return -1;
		}
	}

	public static int getCloserTile(int fromTileId, int firstTileId, int secondTileId) {
		final double firstTileDist = PathFinder.calculateManhattan(fromTileId, firstTileId);
		final double secondTileDist = PathFinder.calculateManhattan(fromTileId, secondTileId);
		return firstTileDist < secondTileDist ? firstTileId : secondTileId;
	}
}
