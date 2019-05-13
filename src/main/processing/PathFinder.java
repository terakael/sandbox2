package main.processing;

import java.util.ArrayList;
import java.util.Stack;

import lombok.Getter;
import lombok.Setter;

public class PathFinder {
	private static PathFinder instance;
	
	public static final int LENGTH = 250;
	private static final int HEIGHT = 250;
	
	private PathNode[] nodes = new PathNode[LENGTH * HEIGHT];
	
	
	private PathFinder() {
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
			nodes[i].setWeight(1);
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
		
		public double getF() {
			return g + h;
		}
	}
	
	private PathNode getNode(int id, int siblingId) {
		if (siblingId < 0 || siblingId >= LENGTH * HEIGHT) 
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
		
		ArrayList<PathNode> open = new ArrayList<>();
		ArrayList<PathNode> closed = new ArrayList<>();
		
		nodes[from].setParent(null);
		open.add(nodes[from]);
		
		while (!open.isEmpty() ) {
			PathNode q = null;
			for (PathNode node : open) {
				if (q == null) {
					q = node;
				} else {
					if (node.getF() < q.getF()) {
						q = node;
					}
				}
			}
			open.remove(q);
			closed.add(q);
			
			q.setAsParent();
			
			for (int i = 0; i < q.getSiblings().length; ++i) {
				PathNode successor = q.getSiblings()[i];
				if (successor == null)// corner and edge nodes have some null siblings
					continue;
				
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
				
				if (closed.contains(successor))
					continue;
				
				boolean isDiagonal = i == 0 || i == 2 || i == 5 || i == 7;
				
				successor.setG(q.getG() + (isDiagonal ? 1.414 : 1));
				successor.h = calculateManhattan(successor.getId(), to);
				
				if (open.contains(successor)) {
					//if (successor.getG())
				} else {
					open.add(successor);
				}
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
