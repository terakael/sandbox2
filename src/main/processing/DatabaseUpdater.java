package main.processing;

import java.util.LinkedList;
import java.util.Queue;

import main.database.entity.UpdateableEntity;
import main.database.entity.other.KeepAlive;

public class DatabaseUpdater implements Runnable {
	private Thread thread;
	private static Queue<UpdateableEntity> queue = new LinkedList<>();
	private static KeepAlive keepAlive = new KeepAlive();
	
	public void start() {
		if (thread == null) {
			thread = new Thread(this, "databaseUpdater");
			thread.start();
		}
	}
	
	public static void enqueue(UpdateableEntity entity) {
		queue.add(entity);
	}
	
	public static void keepAlive() {
		enqueue(keepAlive);
	}
	
	@Override
	public void run() {
		while (true) {
			if (queue.isEmpty()) {
				try { Thread.sleep(10); } catch (InterruptedException e) {}
				continue;
			}
			
			UpdateableEntity entity = queue.poll();
			if (entity != null) {
				entity.process();
			}
		}
	}
}
