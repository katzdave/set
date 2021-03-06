/*
 *                    SetServer
 *                    /   |   \
 *       ServerMessenger  |   ServerLogic (done by SetServer's functions)
 *                 ConnectionAcceptor
 *                        |
 * spawns multiple threads (one for each client connection)
 *
 * users - hashMap keying users by their client ID, 
           managed by SetServer functions
         - it is always a 1-1 mapping of clientID to a User because when a user
           logs out, the user removed from the map
 * gameRooms - hashMap mapping room numbers to actual rooms
 * sockets - maps clientIDs to their sockets
 *
 */

//SQL STUFF:
//users 
//username, password, rating
//db username: root password:cooperee
           
package SetServer;
//package src.main.java.SetServer;

import connectionManager.*;
import gamebackend.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;
import java.util.List;

public class SetServerProtocol extends Protocol {
  
  Connection dbConnection = null;
  final Map<Integer, User> users;
  final Map<Integer, GameRoom> gameRooms;
  int numRooms;
  Message incomingMessage;
  Sql sql;
  
  public SetServerProtocol() {
    super();
    isrunning = true;
    users = new HashMap<>();
    gameRooms = new HashMap<>();
    numRooms = 0;
    sql = new Sql();
  }

  //returns true to accept a connection
  @Override
  public boolean processAcceptorMessages(int numConnections, 
                                         BufferedReader incomingStream, 
                                         Socket cSocket) {
    return true;
  }
  
  @Override
  public void connect() {
    //intentionally left empty
  }
  

  //need to be able to send message to all
  @Override
  public void sendMessage(int connectedID, String message) {
    System.out.println("sending message: " + message);
    if (connectedID == -1) {
      Set<Integer> userIds = sockets.keySet();
      for (Integer userId : userIds) {
        try {
          outgoingMessages.put(new Message(userId, message));
        } catch (InterruptedException e) {
          System.err.println(
                  "Unable to send message: " + message + " to" + userId);
        }
      }
    } else {
      try {
        outgoingMessages.put(new Message(connectedID, message));
      } catch (InterruptedException e) {
        System.err.println(
                "Unable to send message: " + message + " to" + connectedID);
      }
    }
  } 

  public void handleDisconnection(int connectedID) {
    try {
      incomingMessages.put(new Message(connectedID, "D"));
    } catch (InterruptedException e) {
      System.err.println("Interrupted sending disconnect message");
    }
  }
  
  /**
   * L~username~password          :Login
   * R~username~password          :Registration
   * D                            :Disconnection
   * N~[room name]~maxNumPlayers  :Create Game
   * J~[room number]              :Join Game
   * G                            :Start Game (ready button pressed)
   * S~card1~card2~card3          :Set request
   * E                            :Exit Game
   * C~Message                    :Lobby Chat
   * T~Message                    :Game Chat
   * @author Harrison
   */
  @Override
  public void processManagerMessages(Message message) {
    System.out.println("received msg: " + message.message);
    String [] messagePieces;
    messagePieces = message.message.split("~");
    switch(messagePieces[0].charAt(0)) {
      case 'L': 
        pLogin(message.connectedID, messagePieces); 
        System.out.println("process login completed");
        break;
      case 'R':
        pRegistration(message.connectedID, messagePieces);
        System.out.println("process registration completed");
        break;
      case 'D':
        pDisconnection(message.connectedID, messagePieces);
        System.out.println("process disconnection completed");
        break;
      case 'N':
        pCreateGame(message.connectedID, messagePieces);
        System.out.println("processing CreateGame: complete");
        break;
      case 'J':
        pJoinGame(message.connectedID, messagePieces);
        System.out.println("processing JoinGame: complete");
        break;
      case 'G':
        pStartGame(message.connectedID, messagePieces);
        System.out.println("processing StartGame: complete");
        break;
      case 'S':
        pSetRequest(message.connectedID, messagePieces);
        System.out.println("processing SetRequest: complete");
        break;
      case 'E':
        pExitGame(message.connectedID, messagePieces);
        System.out.println("processing ExitGame: completed");
        break;
      case 'C':
        pLobbyChat(message.connectedID, messagePieces);
        System.out.println("processing LobbyChat: complete");
        break;
      case 'T':
        pGameChat(message.connectedID, messagePieces);
        System.out.println("processing GameChat: complete");
        break;
      case 'M':
        pGameMessage(message.connectedID, messagePieces);
        System.out.println("processing GameChat: complete");
        break;
      default:
        System.out.println("Invalid Message!: " + message.message);
    }
  }

  void sendUpdatedInfo(int clientID) {
    Collection<User> userObjs = users.values();
    String username = users.get(clientID).username;
    for (User user : userObjs) {
      if (username != user.username)
        sendMessage(clientID, "P~A~" + user.username);
    }
    Set<Integer> gameRoomIds = gameRooms.keySet();
    GameRoom room = null;
    String status;
    for (Integer gameRoomId : gameRoomIds) {
      room = gameRooms.get(gameRoomId);
      if (!room.isRemoved()) {
        if (room.isPlaying())
          status = "Playing";
        else
          status = "Inactive";
        sendMessage(clientID, "U~A~"+gameRoomId+"~"
            +room.getName()+"~"+room.getNumPlayers()
            +"~"+room.getMaxNumPlayers()+"~"+status);
      }
    }
            
  }

  /**
   * 
   * @param clientID
   * @param messagePieces
   */
  //First three are functions that require SQL connection
  //have to connect and close after each query because of connection timeout
  //accepts message: L~username~password
  //sends either an error to client (X~[message])
  //or a request to update everyone's lobby tables (P~A~[logged in user])
  void pLogin (int clientID, String [] messagePieces) {
    
    if (messagePieces.length != 3) {
      System.err.println("Message length error!");
      return;
    }
    User usr = sql.getUserFromUsername(messagePieces[1]);
      
    if (usr.id == -1) {
      sendMessage(clientID, "X~Username does not exist");
      System.out.println("Username not found!");
    } else {
      
      //check if user is already online; 
      //if user is send error message to client
      for (User current : users.values()) {
        if (messagePieces[1].equals(current.username)) {
          sendMessage(clientID, "X~<html><p><center>" +
            "Username is<br>already online</center></p></html>");
          return;
        }
      }

      //if not already online
      //verify the password and add the user to the lobby + list of users
      if (messagePieces[2].equals(usr.password)) {
        users.put(clientID, usr);
        sendMessage(-1, "P~A~" + messagePieces[1]);
        sendUpdatedInfo(clientID);
      } else {
        sendMessage(clientID, "X~Invalid password");
      }
    }
  }
  
  //accepts message: R~Username~Password
  //sends either an error to client (X~[message])
  //or a request to update everyone's lobby tables (P~A~[logged in user])
  void pRegistration(int clientID, String [] messagePieces) {
    if (messagePieces.length != 3) {
      System.err.println("Message error!");
      sendMessage(clientID, "X~<html><p><center>Invalid Name!<br>" +
          "Probably contains '~'<br></center></p></html>");
      return;
    }

    if (messagePieces[1].contains(" ") || messagePieces[1].contains("_")) {
        sendMessage(clientID, "X~<html><p><center>Invalid Name!<br>" +
          "Probably contains ' ' or '_'<br></center></p></html>");    
        return;
    }
    
    if (!sql.addUser(messagePieces[1], messagePieces[2])) {
      sendMessage(clientID, "X~Username already exists!");
      System.out.println("Username already exists!");
    } else {
      //insert user into database
      User usr = sql.getUserFromUsername(messagePieces[1]);
      System.out.println("Created new user: " + messagePieces[1]);
      users.put(clientID, usr);
      sendMessage(-1, "P~A~" + messagePieces[1]);
      sendUpdatedInfo(clientID);
    }
  }
  
  //accepts message: D (generated by server in connection class)
  //which is sent by each UserConnection thread when attempting to read
  //sends out P~R~[username] to remove client from lobby table of users
  //
  //sends out T~[username] disconnected to display who disconnected in room chat
  //
  //sends out G~U~[user1]~[user1score]~[user2]~[user2score]~...
  //where ... denotes possibly more users and scores
  //the message updates users in gameroom and associated scores
  //
  //sends out G~R if gameroom number of ready players is reset
  //gameroom should allow players to click on ready button again
  void pDisconnection(int clientID, String [] messagePieces) {
    if (messagePieces.length != 1) {
      System.err.println("Disconnection message length error!");
      return;
    }
    User disconnected = users.get(clientID);
    
    //check if disconnected client was in a GameRoom or not
    //if the client was, message GameRoom and remove the corresponding player
    //(update username string)
    if (disconnected != null) {
      sendMessage(-1, "P~R~" + disconnected.username);
      
      if (disconnected.currentGameRoom >= 0) {
        GameRoom currentRm = gameRooms.get(disconnected.currentGameRoom);
        
        if (currentRm != null) {
          currentRm.removePlayer(clientID);
          if (currentRm.isInactive())
            sendMessage(-1, "U~Y~" + disconnected.currentGameRoom);
          if (currentRm.getNumPlayers() > 0) {
            messageGameRoom(currentRm, "T~" + disconnected.username 
                    + " disconnected");
            //update users in game room + their scores
            messageGameRoom(currentRm, currentRm.encodeNamesToString());
            
            //if game is in progress; force it to complete
            //lower disconnected player's score
            if (currentRm.isPlaying()) {
              disconnected.rating -= 10;
              if(!sql.updateUser(disconnected)){
                System.err.println("User not in database??!?!?!");
              }
              
              //if there's only one player left it's game over
              if (currentRm.getNumPlayers() == 1) {
                currentRm.setCompleted();
                handleGameOver(currentRm);
                //sendMessage(-1, "U~I~"+disconnected.currentGameRoom);
              }
            } else {
              //the game has not started yet
              messageGameRoom(currentRm, "T~" + disconnected.username
                      + "disconnected! "
                      + "Ready players have been reset, press ready again!");
              currentRm.resetNumReady();
              messageGameRoom(currentRm, "G~R");
            }
            
          } else {
            if (currentRm.isInactive()) {
              currentRm.setRemoved();
              sendMessage(-1, "U~R~"+disconnected.currentGameRoom);
            }
            gameRooms.remove(disconnected.currentGameRoom);
          }
          
        } else {
          System.err.println("Bug!");
        }
      }
      
      users.remove(clientID);
    }
  }
  
  //accepts message: N~[room name]~maxNumPlayers
  //
  //sends message: A if already in a game room
  //
  //or U~A~[room number]~[room name]~[current numPlayers]~[max players]~[status]
  //which adds game room info to a list of gamerooms in lobby
  //
  //sends out C~[username] created a game: [new room name]
  void pCreateGame(int clientID, String [] messagePieces) {
    if (messagePieces.length != 3) {
      sendMessage(clientID, "X~invalid game name");
      System.err.println("Create game message length error!");
      return;
    }
    User rmCreator = users.get(clientID);
    if (rmCreator.currentGameRoom >= 0) {
      sendMessage(clientID, "A");
      return;
    }
    rmCreator.currentGameRoom = numRooms;
    GameRoom newRm = new GameRoom(
        messagePieces[1], Integer.parseInt(messagePieces[2]));
    newRm.addPlayer(clientID, rmCreator.username);
    //update gameroom window
    sendMessage(clientID, newRm.encodeNamesToString());
    //send an update of list of tables to all clients of new table
    sendMessage(-1, 
            "U~A~"+numRooms+"~"
            +messagePieces[1]+"~"+newRm.getNumPlayers()
            +"~"+newRm.getMaxNumPlayers()+"~Inactive");
    sendMessage(-1, 
            "C~"+rmCreator.username+" created a game with");
    sendMessage(-1,
            "C~\t id: "+numRooms);
    sendMessage(-1,
            "C~\t name: " + messagePieces[1]);
    gameRooms.put(numRooms, newRm);
    ++numRooms;
  }
  
  //accepts a message: J~[room number]
  //sends out "J~I" if game in progress or "J~F" if it's full

  //sends out U~X~[room number] : increase current number players display for gameroom
  //updates the board's names + scores otherwise
  void pJoinGame(int clientID, String [] messagePieces) {
    if (messagePieces.length != 2) {
      System.err.println("Join game message length error!");
      return;
    }
    User joining = users.get(clientID);
    if (joining.currentGameRoom >= 0) {
      sendMessage(clientID, "A");
      return;
    }
    joining.currentGameRoom = Integer.parseInt(messagePieces[1]);
    GameRoom room = gameRooms.get(joining.currentGameRoom);
    if (room == null) {
      System.err.println("Room does not exist, possible bug!");
      joining.currentGameRoom = -1;
    } else {
      if (room.getNumPlayers() < room.getMaxNumPlayers()) {
        if (room.isPlaying()) {
          joining.currentGameRoom = -1;
          sendMessage(clientID, "J~I");
        } else {
          room.addPlayer(clientID, joining.username);
          sendMessage(clientID, "J~J");
          messageGameRoom(room, room.encodeNamesToString());
          sendMessage(-1, 
                  "C~"+joining.username
                  +" joined game room: "+ messagePieces[1] 
                  +" " + room.getName());
          //sends out message to update tables in lobby
          sendMessage(-1, "U~X~" + joining.currentGameRoom);
          messageGameRoom(room, "T~"+joining.username + " joined");
        }
      } else {
        joining.currentGameRoom = -1;
        sendMessage(clientID, "J~F");
      }
    }
  }
  
  //accepts message: G
  //which is sent when a player presses the ready button
  //Requires both players to be in room and ready
  //the clients will already be in the room
  //if not everyone is ready, game will not start but rather increment numready
  //sends out messages of T~[username]~is ready!
  //and a message with G~S~board~scores when everyone is ready
  void pStartGame(int clientID, String [] messagePieces) {
    if (messagePieces.length != 1)
      System.err.println("Start game message length error!");
    User starter = users.get(clientID);
    GameRoom room = gameRooms.get(starter.currentGameRoom);
    if (room == null) {
      System.err.println("Bug!");
    } else {
      room.incNumReady();
      messageGameRoom(room, "T~" + users.get(clientID).username + " is ready!");
      if (room.getNumPlayers() == room.getNumReady()) {
        room.setPlaying();
        messageGameRoom(room, "T~All users are ready. Game start!");
        messageGameRoom(room, room.InitializeGame());
        room.setRemoved();
        sendMessage(-1, "U~R~" + starter.currentGameRoom);
        //sendMessage(-1, "U~P~" + starter.currentGameRoom);
      }
    }
  }
  
  //accepts message: S~card1~card2~card3
  //sends a message of form G~flag~board~scores
  void pSetRequest(int clientID, String [] messagePieces) {
    if (messagePieces.length != 4) {
      System.err.println("Set message length error!");
      return;
    }
    User sender = users.get(clientID);
    GameRoom room = gameRooms.get(sender.currentGameRoom);
    if (room.isBlockSets())
      return;
    String updateMessage = room.CheckSetAndUpdate(clientID,
            messagePieces[1], messagePieces[2], messagePieces[3]);
    if(updateMessage != null)
      messageGameRoom(room, updateMessage);
    
    //check if game's over
    if (room.isCompleted()) {
      //sendMessage(-1, "U~I~"+sender.currentGameRoom);
      handleGameOver(room);
    }
  }
  
  //Accepts "E"
  //sends out "T~[username]~left the game" to the room
  //removes users from the room and handles game over if necessary
  //sends out U~R~[room removed] to update list of gamerooms
  //
  void pExitGame(int clientID, String [] messagePieces) {
    if (messagePieces.length != 1) {
      System.err.println("Leave game message length error!");
      return;
    }
    User user = users.get(clientID);
    int currentRoom = user.currentGameRoom;
    if (currentRoom < 0) {
      System.out.println("error1");
      System.err.println("leave room bug!!!");
      return;
    }
    GameRoom room = gameRooms.get(currentRoom);
    sendMessage(clientID, "E");
    if (room == null) {
      System.out.println("error2");
      System.err.println("leave room bug!!!");
    } else {
      room.removePlayer(clientID);
      if (room.isRoomEmpty()) {
        if (room.isInactive()) {
          room.setRemoved();
          sendMessage(-1, "U~R~"+currentRoom);
        }
        gameRooms.remove(currentRoom);
      } else {
        messageGameRoom(room, "T~" + user.username + " left the game");
        messageGameRoom(room, room.encodeNamesToString());
        if (room.isInactive())
          sendMessage(-1, "U~Y~" + currentRoom);
        if (room.isPlaying()) {
          //lower the score of the player who forfeited
          user.rating -= 10;
          if(!sql.updateUser(user)){
            System.err.println("User not in database??!?!?!");
          }
          
          //handle game over if there's only 1 player left
          if (room.getNumPlayers() == 1) {
            room.setCompleted();
            //sendMessage(-1, "U~I~"+currentRoom);
            handleGameOver(room);
          }
        }

      }
    }
    user.currentGameRoom = -1;
  }
  
  //accepts mesage C~message
  //sends out message C~[sender username]~message
  void pLobbyChat(int clientID, String [] messagePieces) {
    if (messagePieces.length != 2) {
      System.err.println("Lobby chat message length error!");
      return;
    }
    User sender = users.get(clientID);
    sendMessage(-1, "C~" + sender.username + '~' +
            messagePieces[1]);
  }  
  
  //accepts message T~message
  //sends out message T~[sender username]~message
  void pGameChat(int clientID, String [] messagePieces) {
    if (messagePieces.length != 2) {
      System.err.println("Game chat message length error!");
      return;
    }
    User sender = users.get(clientID);
    GameRoom current = gameRooms.get(sender.currentGameRoom);
    messageGameRoom(current, "T~" + sender.username + '~' + messagePieces[1]);
  }

  void pGameMessage(int clientID, String [] messagePieces){
    if (messagePieces.length != 2) {
      System.err.println("Game chat message length error!");
      return;
    }
    User sender = users.get(clientID);
    GameRoom current = gameRooms.get(sender.currentGameRoom);
    messageGameRoom(current, "T~" + messagePieces[1]);
  }
  
  void messageGameRoom(GameRoom room, String message) {
    List<Integer> pids = room.getPlayerIds();
    for (Integer pid : pids)
      sendMessage(pid, message);
  }
  
  //handle cases for only 1 player left (everyone else left)
  //handle cases for actually completed
  // Game is over (players received F in their messages so they know)
  // Send results to database
  // Decide what to do with game room
  //handle what to do upon game over
  // sends out G~R
  void handleGameOver(GameRoom room) {
    System.out.println("handling GameOver");
    if (room.isCompleted() == false)
      System.err.println("Bug!");
    messageGameRoom(room, "T~The game is over. Ratings updating...");
    List<Integer> winners = new ArrayList<>();
    List<Integer> losers = new ArrayList<>();
    room.getWinners(winners, losers);
    int loserSz = losers.size();
    if (loserSz == 0)
      ++loserSz;
    int addedScore=((room.getNumPlayers()-winners.size())*10)/winners.size();
    //for if only 1 player and everyone disconnected
    if (room.getNumPlayers() == winners.size())
      addedScore = 10;
    int subtractedScore=((room.getNumPlayers()-losers.size())*10)/loserSz;
    double updatedScore;
    for (int i = 0; i != winners.size(); ++i) {
      User current = users.get(winners.get(i));
      updatedScore = current.rating + addedScore;
      messageGameRoom(room, "T~" + current.username + "'s rating: " +
              current.rating + " -> " + updatedScore);
      messageGameRoom(room, "T~Press Exit Game to return to lobby");
      current.rating = updatedScore;
      if(!sql.updateUser(current)){
        System.err.println("Error: user not in database");
      }
    }
    for (int i = 0; i != losers.size(); ++i) {
      User current = users.get(losers.get(i));
      updatedScore = current.rating - subtractedScore;
      messageGameRoom(room, "T~" + current.username + "'s rating: " +
              current.rating + " -> " + updatedScore);
      current.rating = updatedScore;
      if(!sql.updateUser(current)){
        System.err.println("Error: user not in database");
      }
    }
    System.out.println("handling GameOver: complete");
    room.blockSets();  
  }

}
