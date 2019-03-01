import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Objects;
import java.awt.event.*;

public class Player extends JFrame {
	
	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	private int width;
	private int height;
	private Container gamePane;
	private JTextArea message;
	private JButton[] cells;
	private String address;
	private int port;
	private int playerID;
	private int otherPlayer; // ID of enemy player.
	private int[] boardStatus;
	private int[] boardValues;
	private int maxTurns;
	private int turnsMade;
	private int myPoints;
	private int enemyPoints;
	private JLabel playerInfoLabel; // Basic info on the player.
	private boolean buttonsEnabled;
	JRadioButton normalMoveButton;
	JRadioButton replaceJokerButton;
	JRadioButton doubleMoveJokerButton;
	JRadioButton freedomJokerButton;
	JRadioButton randomJokerButton;
	private JLabel picturePane;
	private ImageIcon moveIcon;
	private ImageIcon replaceIcon;
	private ImageIcon doubleMoveIcon;
	private ImageIcon freedomIcon;
	private ImageIcon randomIcon;
	private String normalMove;
	private String jokerReplace;
	private String jokerDoubleMove;
	private String jokerFreedom;
	private String jokerRandom;
	private JButton idleMoveButton;
	private JButton resignButton;
	private boolean isBoardFull;
	
	// Instantiated in the connectToServer() method.
	private ClientSideConnection csc;
	
	// Player Constructor.
	public Player(int w, int h) {
		
		System.out.println(" --- Player (client) --- ");
		width = w;
		height = h;
		gamePane = this.getContentPane();
		message = new JTextArea();
		cells = new JButton[60];
		boardStatus = new int[60];
		boardValues = new int[60];
		address = "localhost";
		port = 52525;
		turnsMade = 0;
		myPoints = 0;
		enemyPoints = 0;
		isBoardFull = false;
		playerInfoLabel = new JLabel();
		
		normalMove = new String ("Normal Move");
		jokerReplace = new String("Replace");
		jokerDoubleMove = new String("Double Move");
		jokerFreedom = new String("Freedom");
		jokerRandom = new String("Random");
		
		moveIcon = new ImageIcon("res/move.png");
		replaceIcon = new ImageIcon("res/replace.png");
		doubleMoveIcon = new ImageIcon("res/doublemove.png");
		freedomIcon = new ImageIcon("res/freedom.png");
		randomIcon = new ImageIcon("res/random.png");
		
		picturePane = new JLabel(moveIcon);
	}
	
	public void setUpGUI () {
		
		this.setSize(width, height);
		this.setTitle("Connected as player " + playerID + ". Turn-based multiplayer game in Java.");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		int cellSize = 50;
		int rowCount = 0;
		int columnCount = 0;
		
		//gamePane.setLayout(new GridLayout(1,5));
		gamePane.setLayout(null);
		gamePane.add(message);
		message.setText("A turn-based game in Java.");
		message.setWrapStyleWord(true);
		message.setLineWrap(true);
		message.setEditable(false);
		message.setBounds(5, 308, 504, 77);
		
		gamePane.add(playerInfoLabel);
		playerInfoLabel.setText("<html>Player 1: <font color='blue'>BLUE</font>\nPlayer 2: <font color='green'>GREEN</font></html>");
		playerInfoLabel.setBounds(525, 0, 224, 24);
		
		for (int i = 0; i < cells.length; i++) {
	        cells[i] = new JButton("" + i);
		}
		
		for (int i = 0; i < cells.length; i++) {
	        gamePane.add(cells[i]);
	        cells[i].setBackground(Color.white);
	        
	        if ((i != 0) && (i % 10 == 0)) {rowCount++;}
	        if (i % 10 != 0)  {columnCount++;} else {columnCount = 0;}
	        
	        cells[i].setBounds(columnCount*51,  (rowCount*51),
	        		cellSize, cellSize);
		}
		
		if (playerID == 1) {
			message.setText(">> You are player 1. Your move!\n>> Click on a cell to take control over it or use a Joker when it is unlocked.\n>> The Joker is a special card that you can only use once per match.\n>> Hint: Try to capture the corners and the center. These value more.");
			otherPlayer = 2;
			buttonsEnabled = true;
		} else {
			message.setText(">> You are player 2. Wait for your turn.\n>> Click on a cell to take control over it or use a Joker when it is unlocked.\n>> The Joker is a special card that you can only use once per match.\n>> Hint: Try to capture the corners and the center. These value more.");
			otherPlayer = 1;
			buttonsEnabled = false;
			
			// A new thread that calls the updateTurn() method, which in turn calls csc.receiveButtonVals() so the players receives the button that the other player has clicked.
			// Running network code in a new thread so it doesn't delay GUI updates.
			Thread t = new Thread (new Runnable() {
				public void run() {
					
					updateTurn();
					
				}
			});
			t.start();
		}
		
		// Colors the table cells on GUI startup.
		checkOwnership();
		
		toggleButtons();
		toggleIdleButton();
		toggleResignButton();
		
		this.setVisible(true);
		this.setResizable(false);
		
	}
	
	public void setUpIdler() {
		
		idleMoveButton = new JButton("Idle move");
		gamePane.add(idleMoveButton);
		idleMoveButton.setBounds(520, 308, 227, 77);
		idleMoveButton.setVisible(true);
		
		ActionListener idleMoveAL = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				// TODO Auto-generated method stub
				
				System.out.println("Idling this move!");
				
				message.setText(">> You idled this move!\n>> You've got " + myPoints + " points, while the enemy has " + enemyPoints + " points.\n>> Now wait for player number " + otherPlayer + ".");
				turnsMade++;
				System.out.println("Turns made: " + turnsMade);
				
				buttonsEnabled = false;
				toggleButtons();
				toggleJokers();
				toggleIdleButton();
				toggleResignButton();
				
				System.out.println("My points: " + myPoints);
				picturePane.setIcon(moveIcon);
				// Sends the number of the cell of every click.
				csc.sendButtonVal("Idle");
				
				// Colors the table cells.
				checkOwnership();
				
				// Victory condition for player 2. If player 2 made the last move, then we can check for a winner.
				if ((playerID == 2 && turnsMade == maxTurns) || (playerID == 2 && isBoardFull)) {
					checkWinner();
				} else {
					Thread t = new Thread (new Runnable() {
						public void run() {
							
							updateTurn();
							
						}
					});
					t.start();
				}
				
			}
				
		};
		
		idleMoveButton.addActionListener(idleMoveAL);
		
	}
	
	public void setUpResigning() {
		
		resignButton = new JButton("Resign");
		gamePane.add(resignButton);
		resignButton.setBounds(633, 270, 114, 35);
		resignButton.setBackground(Color.red);
		resignButton.setVisible(true);
		
		ActionListener resignMoveAL = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				System.out.println("Player gave up!");
				
				message.setText(">> Resigning ... \n>> You didn't even try.\n>> Hope to see you again soon! Closing the connection to the server ... \n>> Bye GameServer!");
				turnsMade++;
				System.out.println("Turns made: " + turnsMade);
				
				buttonsEnabled = false;
				toggleButtons();
				toggleJokers();
				toggleIdleButton();
				toggleResignButton();
				// Sends the number of the cell of every click.
				csc.sendButtonVal("Resign");
				
				
				myPoints = 0;
				checkWinner();
				updateTurn();
			}
		};
		
		resignButton.addActionListener(resignMoveAL);
	
	}
	
	public void setUpMoveTypes() {
		
		gamePane.add(picturePane);
		picturePane.setBounds(575, 25, 100, 100);
		
		JLabel jokerInfo = new JLabel("<html><font color='red'>Careful! </font>Jokers exhaust on use !!!</html>");
		gamePane.add(jokerInfo);
		jokerInfo.setBounds(530, 115, 200, 50);
		
		normalMoveButton = new JRadioButton(normalMove);
		normalMoveButton.setMnemonic(KeyEvent.VK_B);
		normalMoveButton.setActionCommand(normalMove);
		normalMoveButton.setSelected(true);
		//normalMoveButton.isVisible();
        
        replaceJokerButton = new JRadioButton(jokerReplace);
        replaceJokerButton.setMnemonic(KeyEvent.VK_B);
        replaceJokerButton.setActionCommand(normalMove);
        replaceJokerButton.setSelected(true);
        
        doubleMoveJokerButton = new JRadioButton(jokerDoubleMove);
        doubleMoveJokerButton.setMnemonic(KeyEvent.VK_B);
        doubleMoveJokerButton.setActionCommand(normalMove);
        doubleMoveJokerButton.setSelected(true);
        
        freedomJokerButton = new JRadioButton(jokerFreedom);
        freedomJokerButton.setMnemonic(KeyEvent.VK_B);
        freedomJokerButton.setActionCommand(normalMove);
        freedomJokerButton.setSelected(true);
        
        randomJokerButton = new JRadioButton(jokerRandom);
        randomJokerButton.setMnemonic(KeyEvent.VK_B);
        randomJokerButton.setActionCommand(normalMove);
        randomJokerButton.setSelected(true);
        
        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(normalMoveButton);
        group.add(replaceJokerButton);
        group.add(doubleMoveJokerButton);
        group.add(freedomJokerButton);
        group.add(randomJokerButton);
        
        normalMoveButton.setBounds(525, 170, 100, 20);
        replaceJokerButton.setBounds(525, 210, 100, 20);
        doubleMoveJokerButton.setBounds(625, 170, 100, 20);
        freedomJokerButton.setBounds(625, 210, 100, 20);
        randomJokerButton.setBounds(525, 250, 100, 20);
        
        normalMoveButton.setEnabled(false);
		freedomJokerButton.setEnabled(false);
		doubleMoveJokerButton.setEnabled(false);
		replaceJokerButton.setEnabled(false);
		randomJokerButton.setEnabled(false);
        
        ActionListener alNormalMove = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				picturePane.setIcon(moveIcon);
				System.out.println("Working!");
				
			}
		};
		
		ActionListener alReplaceJoker = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				picturePane.setIcon(replaceIcon);
				freedomJokerButton.setEnabled(false);
		    	doubleMoveJokerButton.setEnabled(false);
		    	replaceJokerButton.setEnabled(false);
		    	randomJokerButton.setEnabled(false);
		    	idleMoveButton.setEnabled(false);
		    	resignButton.setEnabled(false);
		    	
				replaceMove();
				
				replaceJokerButton.setVisible(false);
				normalMoveButton.setSelected(true);
				System.out.println("Working!");
				
			}
		};
		
		ActionListener alDoubleMove = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				picturePane.setIcon(doubleMoveIcon);
				freedomJokerButton.setEnabled(false);
		    	doubleMoveJokerButton.setEnabled(false);
		    	replaceJokerButton.setEnabled(false);
		    	randomJokerButton.setEnabled(false);
		    	idleMoveButton.setEnabled(false);
		    	resignButton.setEnabled(false);
		    	
		    	
		    	
				doubleMoveJokerButton.setVisible(false);
				normalMoveButton.setSelected(true);
				System.out.println("Working!");
				
			}
		};
		
		ActionListener alFreedom = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				picturePane.setIcon(freedomIcon);
		    	freedomJokerButton.setEnabled(false);
		    	doubleMoveJokerButton.setEnabled(false);
		    	replaceJokerButton.setEnabled(false);
		    	randomJokerButton.setEnabled(false);
		    	idleMoveButton.setEnabled(false);
		    	resignButton.setEnabled(false);
		    	
		    	freedomMove();
		    	
		    	freedomJokerButton.setVisible(false);
		    	normalMoveButton.setSelected(true);
				System.out.println("Working!");
				
			}
		};
		
		ActionListener alRandom = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				picturePane.setIcon(randomIcon);
				freedomJokerButton.setEnabled(false);
		    	doubleMoveJokerButton.setEnabled(false);
		    	replaceJokerButton.setEnabled(false);
		    	randomJokerButton.setEnabled(false);
		    	idleMoveButton.setEnabled(false);
		    	resignButton.setEnabled(false);
		    	
		    	randomMove();
		    	
				randomJokerButton.setVisible(false);
				normalMoveButton.setSelected(true);
				System.out.println("Working!");
				
			}
		};
		
		//Register a listener for the radio buttons.
        normalMoveButton.addActionListener(alNormalMove);
        replaceJokerButton.addActionListener(alReplaceJoker);
        doubleMoveJokerButton.addActionListener(alDoubleMove);
        freedomJokerButton.addActionListener(alFreedom);
        randomJokerButton.addActionListener(alRandom);
		
        gamePane.add(normalMoveButton);
        gamePane.add(replaceJokerButton);
        gamePane.add(doubleMoveJokerButton);
        gamePane.add(freedomJokerButton);
        gamePane.add(randomJokerButton);
        
	}
	
	// Replaces a random cell on the board.
	// There's a chance to replace an enemy cell as well.
	public int pickRandomCell() {
		
		int randomCell = (int) Math.ceil(Math.random() * 60);
		
		if (boardStatus[randomCell] == playerID) {
			pickRandomCell();
			return randomCell;
		} else  {
			return randomCell;
		}
		
	}
	
	public void randomMove() {
		
		int randomCell = pickRandomCell();
		
		turnsMade++;
		System.out.println("Turns made: " + turnsMade);
		
		buttonsEnabled = false;
		toggleButtons();
		toggleJokers();
		toggleIdleButton();
		toggleResignButton();
		
		myPoints += boardValues[randomCell];
		boardStatus[randomCell] = playerID;
		message.setText(">> You used the random Joker on cell " + randomCell + ".\n>> You've accumulated " + myPoints + " points.\n>> Now wait for player number " + otherPlayer + " to make a move.");
		
		System.out.println("My points: " + myPoints);
		picturePane.setIcon(moveIcon);
		// Sends the number of the cell of every click.
		csc.sendButtonVal("" + randomCell);
		
		// Colors the table cells.
		checkOwnership();
		
		isBoardFull = checkBoardAvailability();
		
		// Victory condition for player 2. If player 2 made the last move, then we can check for a winner.
		if ((playerID == 2 && turnsMade == maxTurns) || (playerID == 2 && isBoardFull)) {
			checkWinner();
		} else {
			Thread t = new Thread (new Runnable() {
				public void run() {
					
					updateTurn();
					
				}
			});
			t.start();
		}
		
	}
	
	// Instantiate the ClientSideConnection
	public void connectToServer() {
		
		csc = new ClientSideConnection();
		
	}
	
	public void setUpButtons() {
		
		ActionListener al = new ActionListener() { // Anonymous class.
			// Identifies which buttons the players clicked.
			public void actionPerformed(ActionEvent ae) {
				
				JButton b = (JButton) ae.getSource(); 
				String bNum = b.getText();
				int m;
				
				turnsMade++;
				System.out.println("Turns made: " + turnsMade);
				
				buttonsEnabled = false;
				toggleButtons();
				toggleJokers();
				toggleIdleButton();
				toggleResignButton();
				
				if (isInteger(bNum)) {
					m = Integer.parseInt(bNum);
					myPoints += boardValues[m];
					boardStatus[m] = playerID;
					message.setText(">> You clicked on cell " + m + ".\n>> You've accumulated " + myPoints + " points.\n>> Now wait for player number " + otherPlayer + " to make a move.");
				} else if (Objects.equals(bNum, "Idle")) {
					message.setText(">> You idled this move!\n>> You have " + myPoints + " points, while the enemy has " + enemyPoints + "points.\n>> Now wait for player number " + otherPlayer + ".");
				} else {
					message.setText("You are not supposed to see this! Please refer to the \"setUpButtons\" method in the Player class.");
				}
				
				System.out.println("My points: " + myPoints);
				picturePane.setIcon(moveIcon);
				// Sends the number of the cell of every click.
				csc.sendButtonVal(bNum);
				
				// Colors the table cells.
				checkOwnership();
				
				isBoardFull = checkBoardAvailability();
				
				// Victory condition for player 2. If player 2 made the last move, then we can check for a winner.
				if ((playerID == 2 && turnsMade == maxTurns) || (playerID == 2 && isBoardFull)) {
					checkWinner();
				} else {
					Thread t = new Thread (new Runnable() {
						public void run() {
							
							updateTurn();
							
						}
					});
					t.start();
				}
				
			}
		};
		
		for (int i=0; i < cells.length; i++) {
			cells[i].addActionListener(al);
		}
		
	}
	
	// Method that toggles buttons.
	public void toggleButtons() {
		
		for (int i=0; i < cells.length; i++) {
			cells[i].setEnabled(false);
		}
		
		validateMoves();
		
	}
	
	// Toggles Jokers depending on the current player status.
	public void toggleJokers() {
		
		if (turnsMade > 1) {
			normalMoveButton.setEnabled(buttonsEnabled);
			replaceJokerButton.setEnabled(buttonsEnabled);
			doubleMoveJokerButton.setEnabled(buttonsEnabled);
			freedomJokerButton.setEnabled(buttonsEnabled);
			randomJokerButton.setEnabled(buttonsEnabled);
		} else {
			normalMoveButton.setEnabled(false);
			freedomJokerButton.setEnabled(false);
			doubleMoveJokerButton.setEnabled(false);
			replaceJokerButton.setEnabled(false);
			randomJokerButton.setEnabled(false);
		}
		
	}
	
	// Toggles the Idle Current Move button.
	public void toggleIdleButton() {
		
		if (turnsMade > 0) {
			idleMoveButton.setEnabled(buttonsEnabled);
		} else {
			idleMoveButton.setEnabled(false);
		}
		
	}
	
	public void toggleResignButton() {
		
		if (turnsMade > 0) {
			resignButton.setEnabled(buttonsEnabled);
		} else {
			resignButton.setEnabled(false);
		}
		
	}
	
	// Determines if a string is an integer or not
	// Used to determine if the button value is a cell from the board or any other button type (like the "Idle Move" button).
	public static boolean isInteger(String s) {
	    return isInteger(s,10);
	}

	public static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}
	
	// You can make a move only after receiving the cell that was clicked by the other player.
	public void updateTurn() {
		
		String n = csc.receiveButtonVal();
		int m;
		if (isInteger(n)) {
			m = Integer.parseInt(n);
			enemyPoints += boardValues[m];
			boardStatus[m] = otherPlayer;
			message.setText(">> Your enemy clicked on cell " + n + ".\n>> The enemy accumulated " + enemyPoints + " points.\n>> Your turn to make a move.");
		} else if (Objects.equals(n, "Idle")) {
			message.setText(">> Your enemy idled the move!\n>> Enemy has " + enemyPoints + " points, while you have " + myPoints + " points.\n>> You can make a move now.");
		} else if (Objects.equals(n, "Resign")) {
			enemyPoints = 0;
			checkWinner();
			message.setText(">> The other player resigned! You are the winner!\n>> Good luck in finding a better opponent next time!\n>> Hope to see you again soon! Closing the connection to the server ... \n>> Bye GameServer!");
		}
		
		picturePane.setIcon(moveIcon);
		System.out.println("Your enemy has " + enemyPoints + " points.");
		// Colors the table cells.
		checkOwnership();
		// Check if the board is full.
		isBoardFull = checkBoardAvailability();
		// Checking victory conditions for player 1. The victory condition for player 2 is in the setUpButtons() method.
		if ((playerID == 1 && turnsMade == maxTurns) || (playerID == 1 && isBoardFull)) {
			checkWinner();
		} else {
			buttonsEnabled = true;
		}
		toggleButtons();
		toggleJokers();
		toggleIdleButton();
		toggleResignButton();
		
	}
	
	private void checkWinner() {
		
		buttonsEnabled = false;
		// Displaying the scores.
		if (myPoints > enemyPoints) {
			message.setText(">> Match won!\n>> I scored: " + myPoints + ".\n>> Enemy scored: " + enemyPoints + ".");
		} else if (myPoints < enemyPoints){
			message.setText(">> Match lost!\n>> I scored: " + myPoints + ".\n>> Enemy scored: " + enemyPoints + ".");
		} else {
			message.setText(">> Match draw!\n>> You both scored " + myPoints + " points.");
		}
		
		message.append("\n>> Hope to see you again soon! Closing the connection to the server ... \n>> Bye GameServer!");
		
		csc.closeConnection();
		
	}
	
	// Colors the table cells.
	public void checkOwnership() {
		
		for (int i = 0; i < boardStatus.length; i++) {
			if (boardStatus[i] == 0) {
				cells[i].setBackground(Color.WHITE);
			} else if (boardStatus[i] == 1) {
				cells[i].setBackground(Color.BLUE);
			} else if (boardStatus[i] == 2) {
				cells[i].setBackground(Color.GREEN);
			}
		}
		System.out.println("Making changes to the game table.");
		
	}
	
	// Checks where the players can make valid moves. Invoked in toggleButtons().
	public void validateMoves() {
		
		for (int i = 0; i < boardStatus.length; i++) {
			if (boardStatus[i] == 0) {
				if (i % 10 > 0) {
					if (boardStatus[i-1] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i % 10 < 9) {
					if (boardStatus[i+1] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 > 0 && i % 10 > 0) {
					if (boardStatus[i-11] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 > 0) {
					if (boardStatus[i-10] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 > 0 && i % 10 < 9) {
					if (boardStatus[i-9] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 < 5 && i % 10 > 0) {
					if (boardStatus[i+9] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 < 5) {
					if (boardStatus[i+10] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 < 5 && i % 10 < 9) {
					if (boardStatus[i+11] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
			}
		}
		
	}
	
	// Checks where the players can make valid moves. Invoked in toggleButtons().
	public void replaceMove() {
		
		for (int i = 0; i < boardStatus.length; i++) {
			if (boardStatus[i] == otherPlayer) {
				if (i % 10 > 0) {
					if (boardStatus[i-1] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i % 10 < 9) {
					if (boardStatus[i+1] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 > 0 && i % 10 > 0) {
					if (boardStatus[i-11] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 > 0) {
					if (boardStatus[i-10] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 > 0 && i % 10 < 9) {
					if (boardStatus[i-9] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 < 5 && i % 10 > 0) {
					if (boardStatus[i+9] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 < 5) {
					if (boardStatus[i+10] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
				if (i / 10 < 5 && i % 10 < 9) {
					if (boardStatus[i+11] == playerID) cells[i].setEnabled(buttonsEnabled);
				}
			} else {cells[i].setEnabled(false);}
		}
		
	}
	
	public void freedomMove() {
		
		for (int i = 0; i < cells.length; i++) {
    		if(boardStatus[i] == 0) cells[i].setEnabled(true);
    	}
		
	}
	
	// Checks if the table is full and returns true or false.
	// Used to check if game should continue or end.
	public boolean checkBoardAvailability() {
		
		boolean filledCells = true;
		
		for (int i = 0; i < cells.length; i++) {
    		if(boardStatus[i] == 0) {
    			filledCells = false;
    			break;
    		}
    	}
		
		return filledCells;
		
	}

	// Client Connection.
	private class ClientSideConnection {
		
		private Socket socket;
		private DataInputStream dataIn;
		private DataOutputStream dataOut;
		
		public ClientSideConnection() {
			
			System.out.println(" --- Client --- ");
			try {
				socket = new Socket(address, port);
				dataIn = new DataInputStream(socket.getInputStream());
				dataOut = new DataOutputStream(socket.getOutputStream());
				playerID = dataIn.readInt();
				System.out.println("Connected to server as Player number " + playerID + ".");
				
				maxTurns = dataIn.readInt() / 2; // Server maxTurns divided by server maxPlayers.
				System.out.println("maxTurns" + maxTurns);
				
				// Receive the starting positions generated by the server.
				for (int i = 0; i < boardStatus.length; i++) {
					boardStatus[i] = dataIn.readInt();
				}
				for (int i = 0; i < boardValues.length; i++) {
					boardValues[i] = dataIn.readInt();
				}
				for (int i = 0; i < boardStatus.length; i++) {
					System.out.println("Status of cell " + i  + " is: " + boardStatus[i]);
				}
				for (int i = 0; i < boardValues.length; i++) {
					System.out.println("Value of cell " + i  + " is: " + boardValues[i]);
				}
			} catch (IOException e) {
				System.out.println("IOException from ClientSideConnection Constructor!");
			}
			
		}
		
		// Method that allows to send the button number.
		// Called in the ActionListener for the buttons.
		public void sendButtonVal(String n) {
			
			try {
				dataOut.writeUTF(n);
				dataOut.flush();
			} catch (IOException e) {
				System.out.println("IOException from \"sendButtonVal()\" in ClientSideConnection!");
			}
			
		}
		
		public String receiveButtonVal() {
			
			String n = ""; // Will be replaced by a cell number.
			try {
				n = dataIn.readUTF();
				System.out.println("Player " + otherPlayer + " clicked on cell " + n);
			} catch (IOException e) {
				System.out.println("IOException from \"receiveButtonVal()\" in ClientSideConnection!");
			}
			return n;
			
		}
		
		// Closes connection on the client's side.
		public void closeConnection() {
			
			try {
				socket.close();
				System.out.println("Bye Server!");
				System.out.println(" --- Connection terminated! --- ");
			} catch (IOException e) {
				System.out.println("IOException from \"closeConnection()\" in ClientSideConnection!");
			}
			
		}
		
	}
	
	public static void main (String[] args) {
		
		Player p = new Player(759, 420);
		// Connect to server, then start the player GUI.
		p.connectToServer();
		p.setUpIdler();
		p.setUpResigning();
		p.setUpGUI();
		p.setUpButtons();
		p.setUpMoveTypes();
		
		
	}
	
}