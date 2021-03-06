package Set_GUI;

// how should leaving the lobby be handled?
/**
* Protocol for SetClient that uses the ConnectionManager class
* @author Harrison
* @author Alejandro Acosta
*/
import connectionManager.Connection;
import connectionManager.Message;
import connectionManager.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import javax.swing.text.*;
import java.awt.color.*;
import java.awt.*;
import javax.swing.*;

import javax.swing.SwingUtilities;

public class SetClientProtocol extends Protocol {

  final int serverId;
  String serverIp;
  int serverPort;
  
  /*
   * References to the game windows so that we can call their member functions
   */
  Lobby lobRef;
  Login logRef;
  Game gameRef;

  Style good = gameRef.textDoc.addStyle("good style", null);
  Style bad = gameRef.textDoc.addStyle("bad style", null);
  Style normal = gameRef.textDoc.addStyle("default style", null);
  
  /**
   * Constructor, modify arguments passed to it in SetClientMain
   * @param serverIp
   * @param serverPort 
   */
  public SetClientProtocol(String serverIp, int serverPort) {
    super();
    serverId = 0;
    this.serverIp = serverIp;
    this.serverPort = serverPort;
  }
  
  /**
   * attempts to connect to set server, will be called automatically
   * after constructors finish
   * also calls the showInterface function (not sure if supposed to be here)
   * @author Harrison
   */
  @Override
  public void connect() {
    System.out.println("Attempting to connect to set server");
    Socket masterSocket;
    try {
      masterSocket = new Socket(serverIp, serverPort);
      BufferedReader masterStream = new BufferedReader(
              new InputStreamReader(masterSocket.getInputStream()));
      sockets.put(serverId, masterSocket);
      Connection connection= new Connection(serverId,
                                            isrunning,
                                            incomingMessages,
                                            masterStream,
                                            sockets,
                                            this);
      connection.start();
    } catch (IOException ex) {
      System.err.println("Couldn't connect to master!");
      System.exit(1);
    }
    
    showInterface();
  }
  
  /**
   * if server disconnects, shutdown everything
   * @param connectedID 
   */
  @Override
  public void handleDisconnection(int connectedID) {
    System.err.println("The server went offline! exiting...");
    isrunning = false;
  }
  
  /**
   * Sends message to set server
   * automatically appends a newline to end of message
   * @param message 
   */
  public void sendMessageToServer(String message) {
    // temp
    System.out.println("Message sent is: " + message);
    sendMessage(serverId, message);
  }
  
  /**
   * @author Alejandro Acosta
   */
  public void showInterface() {
    /*
     * Opening the Login Screen
     */
    
    final SetClientProtocol runObj = this;
    
    SwingUtilities.invokeLater(new Runnable() {
      
      @Override
      public void run() {
        Login log = new Login(runObj);
        log.setVisible(true);
      }
    });
  }
  
  /**
   * the processing function for message that the client receives from server
   * @param message 
   */
  @Override
  public void processManagerMessages(Message message) {

    StyleConstants.setForeground(good, Color.BLUE);
    StyleConstants.setForeground(bad, Color.RED);
    StyleConstants.setForeground(normal, Color.BLACK);
    System.out.println("Received message: " + message.message);
    String [] messagePieces = message.message.split("~");
    switch(messagePieces[0].charAt(0)) {
      case 'X':
        // parse (errorMSG): Login/Register error
        String errorText = messagePieces[1];
        logRef.setErrorText(errorText);
        break;
      case 'G':
    	  gameRef.displayBoard(messagePieces);

/*        G~S start?
            G~Y yes set made
            G~F game over
            G~N no set wasn't made
            G~R reset ready button (shouldn't be able to press if already pressed)
            G~U~[game room userlist string] update names+scores
                 whenever a name is added or removed
               :Update GameRoom in game*/
/*
               G~FLAG~BOARD~SCORES

eg...

G~Y~1 20 32 22 23 2 6 7 9 3 70 72~9 6

FLAG:
  Y: Someone got a set scores need updating
  N: Someone failed a set scores need updating
  F: Game is over, do game over behavior
  B: Board state, no changes to scores
  S: start game

Special flags
  U: will be G~U~[user1]~[user1score]~... where ... means possibly multiple lists users+scores
  R: resets room and ready button
  */
    	  
        break;
      case 'E':
        // exited GameRoom
        logRef.exitGame();
        break;
      case 'J':
        /* J~I :Could not join, game in progress
        J~F :Game Room is full
        J~J :Join is successful */
          lobRef.handleJoin(messagePieces[1].charAt(0));
        break;
      case 'C':
        //C~[username]~[message] : chat username messaged lobbying 
        System.out.println("Received message:" + message.message);
        if(messagePieces.length == 3) {
          String username = messagePieces[1];
          String chatMessage = messagePieces[2];
          lobRef.updateChat(username, chatMessage);
        }
        // C~[message] : lobby chat announcement
        else {
          String chatMessage = messagePieces[1];
          lobRef.updateChat("", chatMessage);
        }
        break;
      case 'T':
    	  System.out.println("Got a chat message:");
    	  if (messagePieces.length == 2) {
      		String[] content =  messagePieces[1].split(" ");
      		if (content.length == 4) {
      			try{
      				gameRef.textDoc.insertString(gameRef.textDoc.getLength(), messagePieces[1] + "\n", good);
              JScrollBar vertical = gameRef.scrollPane.getVerticalScrollBar();
              vertical.setValue(vertical.getMaximum());
      			} catch(BadLocationException ble){System.out.println("Text didn't work for some reason");}
      		} else {
      			try{
      				gameRef.textDoc.insertString(gameRef.textDoc.getLength(), messagePieces[1] + "\n", bad);
              JScrollBar vertical = gameRef.scrollPane.getVerticalScrollBar();
              vertical.setValue(vertical.getMaximum());
      			} catch(BadLocationException ble){System.out.println("Text didn't work for some reason");}
      		}
    	  } else {
    		try{
    			gameRef.textDoc.insertString(gameRef.textDoc.getLength(), messagePieces[1] + ": " + messagePieces[2] + "\n", normal);
          JScrollBar vertical = gameRef.scrollPane.getVerticalScrollBar();
          vertical.setValue(vertical.getMaximum());
    		} catch(BadLocationException ble){System.out.println("Text didn't work for some reason");}
    	  }
        	  
          /*
    T~[message] : send message to game room
    T~[username]~[message] : sends out message to gameroom from [username]
             */
        break;
      case 'P':
        /* P~A~name :update players in lobby table of users
     P~R~name: removes name from lobby table of users*/
        String mode = messagePieces[1];
        String senderUsername = messagePieces[2];
        switch(mode) {
        case "A":
          System.out.println("Logged in value is " + logRef.isLoggedIn);
          System.out.println("My username is " + logRef.myUsername + " Login username is " + senderUsername);

          if(logRef.myUsername != null) {
            if(!logRef.isLoggedIn && logRef.myUsername.equals(senderUsername)) {
              logRef.login(senderUsername);
            }
            lobRef.updateUserList("A", senderUsername);
          }

          // need code to populate userlist for new players
          break;
        case "R":
          if(logRef.isLoggedIn && logRef.myUsername.equals(senderUsername)) {
            logRef.logout();
          }
          lobRef.updateUserList("R", senderUsername);
          break;
        default:
          System.err.println("Error with P~ message");
            
        }
        break;
      case 'U':
        /*
         * U~A~[room number]~[rm name]~[curr numPlayers]~[max player]~[status]: adds to list of gamerooms
         * U~R~[room number removed] : to update list of gamerooms, removes room with that number id
         * U~I~[room number] : Set to not playing
         * U~P~[room number] : currently playing
         * U~X~[room number] : increase current number players display for gameroom
         * U~Y~[room number] : decrease current number players display for gameroom
         */
        int roomNum, curNumPlayers, maxNumPlayers;
        String roomName, statusString;
        boolean status;
        
        roomNum = Integer.parseInt(messagePieces[2]);
        
        switch(messagePieces[1].charAt(0)) {
          /* U~A~[room number]~[rm name]~[curr numPlayers]~[max player]~[status]: 
           * adds to list of gamerooms
           */
          case 'A': 
            roomName = messagePieces[3];
            curNumPlayers = Integer.parseInt(messagePieces[4]);
            maxNumPlayers = Integer.parseInt(messagePieces[5]);
            statusString = messagePieces[6];
            status = statusString.equals("Playing");

            lobRef.addGameRoom(roomNum, roomName, curNumPlayers, 
                maxNumPlayers, status);
            break;
            
          /* U~R~[room number removed] : 
           * to update list of gamerooms, removes room with that number id
           */
          case 'R':          
            lobRef.removeGameRoom(roomNum);
            break;
            
          /* U~I~[room number] : 
           * Set to not playing
           */
          case 'I':
            lobRef.setInactive(roomNum);
            break;
            
          /* U~P~[room number] : 
           * currently playing
           */
          //case 'P':
            //lobRef.setPlaying(roomNum);
            //break;
            
          /* U~X~[room number] : 
           * increase current number players display for gameroom
           */
          case 'X':
            lobRef.increasePlayers(roomNum);
            break;
            
          /* U~Y~[room number] : 
           * decrease current number players display for gameroom
           */
          case 'Y':
            lobRef.decreasePlayers(roomNum);
            break;
          default:
            System.err.println("Error with gameList update message");
          }
        break;
    }
  }

  /**
   * @author Alejandro Acosta
   * 
   * sends the message to the server
   * SEE USE SEND MESSAGETOSERVER FUNCTION INSTEAD OF CREATING AN OUTPUTSTREAM
   */
  public void sendMessage(String message) {
    /*
     * L~username~password          :Login
     * R~username~password          :Registration
     * D                            :Disconnection
     * N~[room name]~maxNumPlayers  :Create Game
     * J~[room number]              :Join Game
     * G                            :Start Game (ready button pressed)
     * S~card1~card2~card3          :Set request
     * E                            :Exit Game
     * C~Message                    :Lobby Chat
     * T~Message                    :Game Chatindex
     *
     */
    
    System.out.println("testing");
    sendMessageToServer(message);
    System.out.println("success, sent:" + message);    
  }
  
  public void grabPanels(Login log, Lobby lob, Game game) {
    this.logRef = log;
    this.lobRef = lob;
    this.gameRef = game;
  }

  /** 
  * Deletes the existing interface and creates a new one 
  */
  public void refreshInterface() {
    lobRef = null;
    logRef = null;
    gameRef = null;

    showInterface();
  }
}
