package main.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Stack;

import lombok.Getter;
import lombok.Setter;
import main.database.SceneryDao;

public class PathFinder {
	private static PathFinder instance;
	
	public static final int LENGTH = 250;
	
	private PathNode[] nodes = new PathNode[LENGTH * LENGTH];// always square so no need for separate length/width variables
	
	
	private PathFinder() {
		HashSet<Integer> impassableTileIds = SceneryDao.getImpassableTileIdsByRoomId(1);
		
		for (int i = 0; i < nodes.length; ++i) {
			nodes[i] = new PathNode();
		}
		
		for (int i = 0; i < nodes.length; ++i) {
			PathNode[] siblings = {
				getNode(i, i - LENGTH - 1),
				getNode(i, i - LENGTH),
				getNode(i, i - LENGTH + 1),
				getNode(i, i - 1),
				getNode(i, i + 1),
				getNode(i, i + LENGTH - 1),
				getNode(i, i + LENGTH),
				getNode(i, i + LENGTH + 1)
			};
			
			nodes[i].setId(i);
			nodes[i].setWeight(impassableTileIds.contains(i) ? -1 : 1);
			nodes[i].setSiblings(siblings);
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
			return sibling != null && sibling.getWeight() != -1;
		}
		
		public double getF() {
			return g + h;
		}
	}
	
	private PathNode getNode(int id, int siblingId) {
		if (siblingId < 0 || siblingId >= LENGTH * LENGTH) 
			return null;// above first row or below last row
		
		if (id % LENGTH == 0 && siblingId == id - 1)
			return null;// left of leftmost
		
		if (id % LENGTH == LENGTH - 1 && siblingId == id + 1)
			return null;// right of rightmost
		
		return nodes[siblingId];
	}
	
	public static Stack<Integer> findPath(int from, int to, boolean includeToTile) {
		// TODO A* algorithm
		Stack<Integer> output = new Stack<>();
		
		PathNode[] nodes = PathFinder.get().nodes;
		if (from < 0 || from >= nodes.length || to < 0 || to >= nodes.length)
			return output;
		
		// cannot move to an impassable tile.
		// TODO move to the nearest passable tile.
		if (nodes[to].getWeight() == -1)
			return output;
		
		ArrayList<PathNode> open = new ArrayList<>();
		ArrayList<PathNode> closed = new ArrayList<>();
		
		nodes[from].setParent(null);
		open.add(nodes[from]);
		
		while (!open.isEmpty() ) {
			// get lowest F node from open list
			PathNode q = Collections.min(open, Comparator.comparing(s -> s.getF()));

			open.remove(q);
			closed.add(q);
			
			//q.setAsParent();
			
			for (int i = 0; i < q.getSiblings().length; ++i) {
				PathNode successor = q.getSibling(i);
				if (successor == null || successor.getWeight() == -1)// corner and edge nodes have some null siblings
					continue;
				
				if (closed.contains(successor))
					continue;

				// 0   1   2
				// 3   q   4
				// 5   6   7
				
				// don't cut corners
				if (i == 0 && (!q.isSiblingPassable(1) || !q.isSiblingPassable(3))) {
					continue;
				} 
				if (i == 2 && (!q.isSiblingPassable(1) || !q.isSiblingPassable(4))) {
					continue;
				} 
				if (i == 5 && (!q.isSiblingPassable(3) || !q.isSiblingPassable(6))) {
					continue;
				} 
				if (i == 7 && (!q.isSiblingPassable(4) || !q.isSiblingPassable(6))) {
					continue;
				}
				
				boolean isDiagonal = i == 0 || i == 2 || i == 5 || i == 7;
				
				double newG = q.getG() + (isDiagonal ? 1.414 : 1.0);
				double newH = calculateManhattan(successor.getId(), to);
				double newF = newG + newH;

				if (newF < successor.getF() || !open.contains(successor)) {
					successor.setG(newG);
					successor.setH(newH);
					successor.setParent(q);
					
					if (!open.contains(successor))
						open.add(successor);
				}
				
				if (successor == nodes[to]) {
					// found it
					if (!includeToTile)
						successor = successor.getParent();
					
					while (successor.getParent() != null) {
						output.push(successor.getId());
						successor = successor.getParent();
					}
					return output;
				}
				
//				if (open.contains(successor)) {
//					if (successor.getG() < newG)
//						continue;
//					open.remove(successor);
//				}
//				
//				successor.setParent(q);
//				successor.setG(newG);
//				successor.setH(newH);
//				open.add(successor);
			}
		}
		
		return output;
	}
	
	private static double calculateManhattan(int src, int dest) {
		return Math.abs(src % LENGTH - dest % LENGTH) + Math.abs(src / LENGTH - dest / LENGTH);
	}
	
	public static boolean isNextTo(int srcTile, int destTile) {
		// returns true if srcTile and destTile are touching horizontally or vertically (or are the same tile)
		return  destTile == srcTile - 1 || // left
				destTile == srcTile + 1 || // right
				destTile == srcTile - LENGTH || // above
				destTile == srcTile + LENGTH || // below
				destTile == srcTile;// same tile
	}
}
