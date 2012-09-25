package cs5223.maze.client;



import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.cs5223.maze.ChangeCoordinates;
import com.cs5223.maze.Notify;
import com.cs5223.maze.PolicyFileLocator;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MovePlayer extends JFrame  {
	private static ChangeCoordinates changecord;
	private static String myKey;
	private Board board;
	
	public MovePlayer(){
		myKey=new ClientKeygen().genKey();
	}

	public void doCustomRmiHandling() {
		Registry registry;
		try {
			//Set system properties
			System.setProperty("java.security.policy", PolicyFileLocator.getLocationOfPolicyFile());
			registry = LocateRegistry.getRegistry("127.0.0.1",9000);
			changecord= (ChangeCoordinates)registry.lookup(ChangeCoordinates.SERVICE_NAME);
			 
			HashMap<String, Object> connect=changecord.connectToServer(myKey);
			if(connect==null){
				System.out.println("The game has already started please try again later...");
				System.exit(0);
				
			}
			
			//Once connection has been successfully established, start sending periodic heart beats to the server
			Thread heartbeat=new Thread(new HeartBeatSender());   
			heartbeat.setDaemon(true);
			heartbeat.start();
		    
			

		    //Print the current state 
			
			System.out.println("_________________________");
			System.out.println("MY STATE"+"==>"+connect.get(myKey));
			System.out.println("NO_OF_PLAYERS"+"==>"+connect.get("NO_OF_PLAYERS"));
			System.out.println("GRID WITH TREASURES");
			
			AtomicInteger[][] grid=(AtomicInteger[][]) connect.get("GRID");
			for(int i=0;i<10;i++){
				for(int j=0;j<10;j++){
					System.out.print(grid[i][j]+" ");
				}
				System.out.println("");
			}
		
			
			System.out.println("_________________________");
			
			//Wait for a second before executing the moves
			Thread.sleep(1000);
			HashMap<String,Integer> myinfo=(HashMap<String, Integer>) connect.get(myKey);			
			board = new Board(10*75,myinfo.get("XCORD"),myinfo.get("YCORD"),grid);
	        add(board);
	        setTitle("Skeleton");
	        setDefaultCloseOperation(EXIT_ON_CLOSE);
	        setSize(10*77, 10*78);
	        setLocationRelativeTo(null);
	        setVisible(true);
	        setResizable(false);
						 
			System.out.println("Please start moving. Enter L/l for LEFT, R/r for RIGHT, U/u for UP and any key for DOWN ");
			
						            	
		           
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		
	}
	public void move(String move) throws InterruptedException {
		
		HashMap<String, Object> afterMove = null;
		AtomicInteger[][] gridAfterMove = null;
		try {
			afterMove = changecord.moveToLocation(move,myKey);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		
 		if(afterMove==null){
 			System.out.println("Game Over...");
 			System.exit(0);
 		}else if(afterMove.get(myKey)=="DISCONNECTED"){
 			System.out.println("Time out error. Game over!!");
 			System.exit(0);
 			
 		}else{
 			
 			System.out.println("________AFTER MOVE__________");
			System.out.println("MY STATE"+"==>"+afterMove.get(myKey));
			System.out.println("NO_OF_PLAYERS"+"==>"+afterMove.get("NO_OF_PLAYERS"));
			System.out.println("GRID WITH TREASURES");
			
			gridAfterMove=(AtomicInteger[][]) afterMove.get("GRID");
			for(int i=0;i<10;i++){
				for(int j=0;j<10;j++){
					System.out.print(gridAfterMove[i][j]+" ");
				}
				System.out.println("");
			}
		
			
			System.out.println("_______END OF AFTER MOVE________");
		
 		}	
 		HashMap<String,Integer> myinfo=(HashMap<String, Integer>) afterMove.get(myKey);
        
 		board.drawAgain(myinfo.get("XCORD"),myinfo.get("YCORD"),gridAfterMove);
 		Thread.sleep(100);
					
	}
	
	public static void main(String[] args) {
		 
		new MovePlayer().doCustomRmiHandling();
	        
	}
	 	 
	 private static class HeartBeatSender extends Thread{
		 @Override
		 public void run(){
			 while(true){
				 try {
					 Notify note=new NotifyImpl();
					 changecord.heartBeat(myKey,note);
					 Thread.sleep(2000);
				 } catch (RemoteException e) {
					 e.printStackTrace(); 
				 } catch (InterruptedException e) {
				
					 e.printStackTrace();
				 }
			 
			 }
		 }
		 
	 }
	 
	 @SuppressWarnings("serial")
	 private static class NotifyImpl extends UnicastRemoteObject implements Notify{
		 protected NotifyImpl() throws RemoteException {
			 super();
		 }

		 @Override
		 public void onSuccess() throws RemoteException {
			 //System.out.println("\n All players alive");				
		 }
		 
		 @Override
		 public void onFailure(String crashedClient) throws RemoteException {
			 System.out.println(crashedClient+" crashed");
			 
		 }
		 
		 
	 }

	 public class Board extends JPanel  {
		 
		 private String location = "/Users/nikilvasudevan/Desktop/DS/AWT/bin/image.png";
		 private int boardSize;
		 private Image image;
		 private int x = 10;
		 private int y = 10;
		 private AtomicInteger [][]grid;
		 
		 public Board(int boardSize,int x, int y, AtomicInteger[][] grid2) {
			 
			 setFocusable(true);
			 addKeyListener(new TAdapter());
			 setDoubleBuffered(true);
			 this.boardSize=boardSize;
			 this.x = x;
			 this.y = y;
			 this.grid = grid2;
			 try {
				 image = ImageIO.read(new File(location));
			 } catch (IOException e) {
				 e.printStackTrace();
			 }


		 }


		 public void paint(Graphics g) {
			 super.paint(g);
			 Graphics2D g2d = (Graphics2D)g;
			 g2d.setColor(Color.BLUE);        
			 g2d.drawImage(image,40+y*75,40+x*75, this);
			 for( int i = 2; i <= boardSize+2; i=i+75 ){
				 g2d.drawLine(2, i, boardSize+2, i);
				 g2d.drawLine(i, 2, i, boardSize+2);
			 }
			 for(int k=0;k<10;k++){
				 for(int j=0;j<10;j++){
					 g.drawString(grid[k][j].toString(),20+j*75,20+k*75);
						
				 }	
			 }

		        
			 Toolkit.getDefaultToolkit().sync();
			 g.dispose();
		 }
		    
		    
		 public void drawAgain(int newX, int newY,AtomicInteger [][] newGrd) {
			 x = newX ;
			 y = newY ;
			 grid = newGrd;
			 repaint();  
		    	
		 }

		 private class TAdapter extends KeyAdapter {
			 
			 public void keyPressed(KeyEvent e) {
				 int key = e.getKeyCode();
				 switch(key) { 
				 	case KeyEvent.VK_RIGHT: 
				 		try {
				 			MovePlayer.this.move("r");
				 		} catch (InterruptedException e1) {
				 			// TODO Auto-generated catch block
				 			e1.printStackTrace();
						} 
				 		break; 

				 	case KeyEvent.VK_LEFT: 
				 		try {
				 			MovePlayer.this.move("l");
				 		} catch (InterruptedException e1) {
				 			// TODO Auto-generated catch block
				 			e1.printStackTrace();
				 		} 
				 		break; 

				 	case KeyEvent.VK_UP: 
				 		try {
				 			MovePlayer.this.move("u");
				 		} catch (InterruptedException e1) {
				 			// TODO Auto-generated catch block
				 			e1.printStackTrace();
				 		} 
				 		break; 

				 	case KeyEvent.VK_DOWN: 
				 		try {
				 			MovePlayer.this.move("d");
				 		} catch (InterruptedException e1) {
				 			// TODO Auto-generated catch block
				 			e1.printStackTrace();
				 		}
				 		break; 
				 }
			 }
			
		 }
		    
	 }
	 	
}
