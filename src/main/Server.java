package main;

import java.util.Scanner;

import javax.websocket.DeploymentException;

import main.processing.PathFinder;
import main.processing.WorldProcessor;

public class Server {

	public static void main(String[] args) {
		org.glassfish.tyrus.server.Server server = new org.glassfish.tyrus.server.Server("localhost", 45555, "/ws", null, Endpoint.class);
		
		try {
			server.start();
			
			PathFinder.get();// init the path nodes and relationships
			
			WorldProcessor processor = new WorldProcessor();
			processor.start();
			
			System.out.println("press any key to quit");
			new Scanner(System.in).nextLine();
		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			server.stop();
		}
	}
}
