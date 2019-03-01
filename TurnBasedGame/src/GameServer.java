import java.io.*;
import java.net.*;

public class GameServer {
	
	private ServerSocket ss;
	private int numPlayers;
	private int maxPlayers;
	private ServerSideConnection player1; // Allows us to call the appropriate sendButtonVal() method.
	private ServerSideConnection player2;
	private String address;
	private int port;
	private int turnsMade;
	private int maxTurns;
	private int[] boardStatus; // Randomly generated positions for each player.
	private int[] boardValues;
	private String player1ButtonNum; // Stores the button number that player 1 clicked on (which gets sent to the other player).
	private String player2ButtonNum; // Stores the button number that player 2 clicked on (which gets sent to the other player).
	
	// GameServer Constructor.
	public GameServer() {
		
		System.out.println(" --- Game Server --- ");
		numPlayers = 0;
		maxPlayers = 2; // Limit the number of connections.
		address = "localhost";
		port = 52525;
		turnsMade = 0;
		maxTurns = 60; // Even number!
		boardStatus = new int[60];
		boardValues = new int[60];
		
		randomPos();
		for (int i = 0; i < boardStatus.length; i++) {
			System.out.println("Status of cell " + i + " is " + boardStatus[i] + ".");
		}
		
		setBoardPoints();
		for (int i = 0; i < boardValues.length; i++) {
			System.out.println("Value of cell " + i + " is " + boardValues[i] + ".");
		}
		
		try {
			ss=new ServerSocket(port);
		} catch (IOException e) {
			// Some feedback on the cause of the exception.
			System.out.println("IOException from GameServer Constructor!");
		}
		
	}
	
	// The starting positions for each player.
	public void randomPos() {
		
		int initialPos1 = (int) Math.ceil(Math.random() * 60);
		int initialPos2 = (int) Math.ceil(Math.random() * 60);
		
		for (int i = 0; i < boardStatus.length; i++) {
			boardStatus[i] = 0;
		}
		
		if (initialPos1 == initialPos2) {
			randomPos();
		} else {
			boardStatus[initialPos1] = 1;
			boardStatus[initialPos2] = 2;
		}
		
	}
	
	public void setBoardPoints() {
		
		for (int i = 0; i < boardValues.length; i++) {
			boardValues[i] = 10;
		}
		
		for (int i = 0; i < boardValues.length; i=i+10) {
			boardValues[i] = 25;
		}
		
		for (int i = 9; i < boardValues.length; i=i+10) {
			boardValues[i] = 25;
		}
		
		for (int i = 0; i < boardValues.length/6; i++) {
			boardValues[i] = 25;
		}
		
		for (int i = 50; i < boardValues.length; i++) {
			boardValues[i] = 25;
		}
		
		boardValues[0] += 25;
		boardValues[9] += 25;
		boardValues[50] += 25;
		boardValues[59] += 25;
		boardValues[23] = 25;
		boardValues[33] = 25;
		boardValues[26] = 25;
		boardValues[36] = 25;
		boardValues[24] = 100;
		boardValues[25] = 100;
		boardValues[34] = 100;
		boardValues[35] = 100;
		
		System.out.println("Adjusting the table values ... ");
		
	}
	
	public void acceptConnections() {
		
		try {
			System.out.println("Waiting for players to connect ... ");
			while(numPlayers < maxPlayers) {
				// Tells the server to begin accepting connections.
				Socket s = ss.accept();
				// The first player that connects gets on ID of 1.
				// Every time a player connects, the numPlayers increments.
				numPlayers++;
				System.out.println("Player #" + numPlayers + " has connected to the server!");
				// Creates a new ServerSideConnection.
				ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
				// Assigns the correct ServerSideConnection runnable to the corresponding field.
				if (numPlayers == 1) { // Player is #1.
					player1 = ssc;
				} else {
					player2 = ssc;
				}
				// Creating the threads.
				// Whatever is in the ServerSideConnection run method will run in a new thread.
				Thread t = new Thread(ssc);
				t.start();
			}
			// After maxPlayers have connected, the server should stop receiving connections.
			if (numPlayers < maxPlayers) { 
				System.out.println("Server is accepting more players!"); 
			} else { 
					System.out.println("We now have " + maxPlayers + " players! No longer accepting connections!"); 
			}
		} catch (IOException e) {
			System.out.println("IOException from \"acceptConnection()\"!");
		}
		
	}
	
	// Creating runnable objects for each of the players. 
	// Creating one thread per player.
	private class ServerSideConnection implements Runnable {
		
		private Socket socket;
		private DataInputStream dataIn;
		private DataOutputStream dataOut;
		private int playerID; // Differentiate between the ServerSideConnection for different players.
		
		// ServerSideConnection Constructor
		public ServerSideConnection(Socket s, int id) {
			// Everything that gets sent to the ServerSideConnection will be assigned to these fields.
			socket = s;
			playerID = id;
			try {
				dataIn = new DataInputStream(socket.getInputStream());
				dataOut = new DataOutputStream(socket.getOutputStream());
			} catch(IOException e) {
				System.out.println("IOException from ServerSideConnection Constructor!");
			}
		}
		
		// Instructions that should run on the new thread.
		public void run() {
			try {
				// When a player connects, the server assigns an ID to that player.
				dataOut.writeInt(playerID); // Sends the ID to the player.
				dataOut.writeInt(maxTurns); // Players should know how many turn they have.
				for (int i = 0; i < boardStatus.length; i++) {
					dataOut.writeInt(boardStatus[i]);
				}
				for (int i = 0; i < boardValues.length; i++) {
					dataOut.writeInt(boardValues[i]);
				}
				
				dataOut.flush();
				
				while(true){
					// Allows the server to send and receive more data.
					if (playerID == 1) {
						player1ButtonNum = dataIn.readUTF();
						System.out.println("Player 1 clicked on cell " + player1ButtonNum + ".");
						// Sends the number of the button of player 2 from player 1.
						player2.sendButtonVal(player1ButtonNum);
					} else {
						player2ButtonNum = dataIn.readUTF();
						System.out.println("Player 2 clicked on cell " + player2ButtonNum + ".");
						player1.sendButtonVal(player2ButtonNum);
					}
					turnsMade++;
					if (turnsMade == maxTurns) {
						System.out.println("The maximum number of turns was reached!");
						// Breaking up from the loop and run() method and terminating the thread when maxTurns was reached.
						break;
					}
				}
				
				// After player 2 moves and turnsMade is equal to maxTurns, the connection is terminated.
				player1.closeConnection();
				player2.closeConnection();
				
			} catch (IOException e) {
				System.out.println("IOException from \"run()\" in the ServerSideConnection!");
			}
		}
		
		public void sendButtonVal(String n) {
			try {
				dataOut.writeUTF(n);
				dataOut.flush();
			} catch(IOException e) {
				System.out.println("IOException from \"sendButonNum()\" in the ServerSideConnection!");
			}
		}
		
		// Closes connection on the server's side.
		public void closeConnection() {
			
			try {
				socket.close();
				System.out.println("Bye Client!");
				System.out.println(" --- Connection terminated! --- ");
			} catch (IOException e) {
				System.out.println("IOException from \"closeConnection()\" in the ServerSideConnection class!");
			}
			
		}
		
	}
	
	public static void main(String[] args) {
		
		GameServer gs = new GameServer();
		gs.acceptConnections();
		
	}
	
}