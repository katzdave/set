package Set_GUI;

import java.io.*;
import java.util.HashMap;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import SetServer.*;
import gamebackend.*;

// NEED TO MAKE SURE LOGGING OUT AND THEN BACK IN AGAIN DOESN'T CAUSE DUPLICATION OF THE PAGE!

@SuppressWarnings("serial")
public class Lobby extends JPanel {
  
  // master panel containing everything this screen contains
  private JPanel panel = new JPanel(new BorderLayout()); 
  
  // all the components of this screen
  private JPanel top = new JPanel();
  private JPanel left = new JPanel();
  private JPanel right = new JPanel();
  private JPanel center = new JPanel();
  
  // subcomponents of the above
  //private JPanel headertext = new JPanel();
  
  // reference to the Login class
  private Login login_Frame;
  
  // The player's username
  private String username;
  
  // welcome message to the user
  private JLabel welcome;
  
  // calling Client object
  private SetClientProtocol callingObj;
  
  // chat box
  private String CHAT_HEADER = "Lobby Group Chat";
  private JLabel chatHeader;
  private JTextArea chatLog;
  private JTextField messageInput;
  private JButton sendMessage;
  
  // active user list and open game list
  public DefaultListModel<String> currentUsers, currentGames;
  private JList<String> userList, gameList;
  
  // challenge other players
  private JPopupMenu challengeMenu;
  private JMenuItem menuChallenge;
  
  // join other games
  private JPopupMenu joinMenu;
  private JMenuItem menuJoin;
  
  // flag for empty game list
  private String emptyGameList = "There are no open games";
  
  // hash table for game rooms
  private HashMap<Integer,GameRoomData> gameRoomList;

  // stuff for creating a game
  private JPopupMenu gameCreate; 
  
  private JLabel gameName;
  private JLabel maxPlayers;
  
  private final JTextField gameNameField = new JTextField(10);
  private final JTextField maxPlayerField = new JTextField(2);

  private JPanel namePanel;
  private JPanel playerPanel;
  
  private JButton submit;

  public Lobby(Login login_Frame) {
    this.login_Frame = login_Frame;

  }
  
  /**
   * Enters the lobby with your specified username.
   * <p>
   * Sets the default button for the lobby window and adds the user's username to the server's list of active users. Also sets the welcome
   * text
   * <p>
   * @param username Identifies the name of the user using this instance of the game lobby.
   */
  public void enterLobby (String username, SetClientProtocol callingObj) {
    this.username = username;
    this.callingObj = callingObj;
    
    // probably won't need this in the end.
    //currentUsers.addElement(username);
    
    // or send the new username to the server to update the list (probably that)
    
    welcome.setText("Welcome " + username);
    this.getRootPane().setDefaultButton(sendMessage);
  }

  /**
   * Creates the GUI for the Lobby page
   * <p>
   * Uses a Border Layout Manager to place all components in separate parts of the screen. 
   * 
   */
  public final void createGUI() {
    makeTop();
    makeLeft();
    makeCenter();
    makeRight();

    panel.add(top, BorderLayout.NORTH);
    panel.add(left, BorderLayout.WEST);
    panel.add(center, BorderLayout.CENTER);
    panel.add(right, BorderLayout.EAST);
    
    // challenge popup menu
    challengeMenu = new JPopupMenu();
    menuChallenge = new JMenuItem("Challenge");
    challengeMenu.add(menuChallenge);
    menuChallenge.addActionListener(new IssueChallengeListener());
    
    // join game popup menu
    joinMenu = new JPopupMenu();
    menuJoin = new JMenuItem("Join Game");
    joinMenu.add(menuJoin);
    menuJoin.addActionListener(new JoinGameListener());
    add(panel);
    
    // create game popup menu
    gameCreate = new JPopupMenu();
    gameCreate.setLayout(new BoxLayout(gameCreate, BoxLayout.Y_AXIS));
    
    gameName = new JLabel("Game Name: ");
    maxPlayers = new JLabel("Max Number of Players: ");
    
    namePanel = new JPanel();
    playerPanel = new JPanel();
    
    namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
    playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.X_AXIS));
    
    namePanel.add(gameName);
    namePanel.add(gameNameField);
    
    playerPanel.add(maxPlayers);
    playerPanel.add(maxPlayerField);
    // change this to use JOptionPane & JDialog
    // popup menu is giving me issues
    //http://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html#input
    gameNameField.setEditable(true);
    maxPlayerField.setEditable(true);
    
    submit = new JButton("OK");
    submit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        String newGameName = gameNameField.getText();
        String newPlayerMax = maxPlayerField.getText();
        callingObj.sendMessageToServer("N~"+newGameName+"~"+newPlayerMax);
      }
    });
    
    gameCreate.add(namePanel);
    gameCreate.add(playerPanel);
    gameCreate.add(submit);
  }

  /**
   *  The heading for the lobby page
   */
  public void makeTop() {
    
    //image for header
    //BufferedImage header = null;
	BufferedImage header;
	Boolean image_succeed = true;
	JLabel headerLabel;
    try {
      // Here's code for getting the current working directory, but i'm not sure
      // if we need that. the hard coded one works fine for now. It may be an issue
      // later though depending on how we're running the client code.
      //String dirtest = System.getProperty("user.dir");
      //System.out.println("Current working directory = " + dirtest);
      header = ImageIO.read(new File("src/main/resources/set_card.png"));
    }
    catch (IOException ex) {
      // handle exception
      image_succeed = false;
      header = null;
    }
    if(!image_succeed) {
      headerLabel = new JLabel("Image Not Found");
    }
    else {
      headerLabel = new JLabel(new ImageIcon(header));
      headerLabel.setAlignmentX(CENTER_ALIGNMENT);
    }
    
    welcome = new JLabel();
    welcome.setAlignmentX(LEFT_ALIGNMENT);

    JButton Logout = new JButton("Logout");
    Logout.setAlignmentX(RIGHT_ALIGNMENT);
    Logout.addActionListener(new ActionListener(){
      /**
       * Calls the logout method of the Login class.
       */
      public void actionPerformed(ActionEvent evt) {
        login_Frame.logout();
      }
    });
    
    JPanel headerText = new JPanel();
    headerText.add(welcome);
    headerText.add(Box.createRigidArea(new Dimension(400,0)));
    headerText.add(Logout);
    headerText.setVisible(true);
    
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    
    //top.setAlignmentY(Component.CENTER_ALIGNMENT);
    top.add(headerLabel);
    //top.add(Box.createRigidArea(new Dimension(0,10)));
    top.add(headerText);
    headerText.setVisible(true);
    //top.add(welcome);
    //top.add(Box.createRigidArea(new Dimension(200,0)));
    //top.add(Logout);
  }
  
  /**
   * Creates a list of players logged in.
   */
  public void makeLeft() {
    currentUsers = new DefaultListModel<String>();
    userList = new JList<String>(currentUsers);
    
    JScrollPane userPane = new JScrollPane(userList);
    userPane.setAlignmentX(LEFT_ALIGNMENT);
    
    //userList.addListSelectionListener(new ChallengeListener2());
    userList.addMouseListener(new ChallengeListener());
    userList.setPreferredSize(new Dimension(100, 100));

    JLabel titlePanel = new JLabel("Users in Lobby");
    JPanel subPanel = new JPanel();
    subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
    subPanel.add(titlePanel);
    subPanel.add(userPane);
    
    left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
    left.add(subPanel);
    left.add(Box.createRigidArea(new Dimension(50,0)));
  }

  // The lobby chat 
  public void makeRight() {
    right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
    
    // the header for the chat area
    chatHeader = new JLabel(CHAT_HEADER);
    chatHeader.setAlignmentX(CENTER_ALIGNMENT);
    
    // creating and organizing the component that stores the chatlog
    chatLog = new JTextArea(15,30);
    chatLog.setEditable(false);
    chatLog.setLineWrap(true);
    
    // the field where you type your messages to the chat
    messageInput = new JTextField(15);
    
    // submit button. Invisible since enter activates it
    sendMessage = new JButton("Send");
    sendMessage.addActionListener(new ChatButtonListener());
    sendMessage.setVisible(false);
    
    // Adding all the components to the right side of the screen
    right.add(chatHeader);
    right.add(chatLog);
    right.add(messageInput);
    right.add(sendMessage);
  }

  /** 
   *  Creates the center portion of the game window
   *  Has a create game button as well as a list of all active games.
   *  Clicking on an active game will open a pop-up menu giving the option to 
   *  join that game. 
   *  As far as I know, games should be removed from the list once they begin.
   *  
   */
  public void makeCenter() {
    //gameCreate.setLayout(new BoxLayout(gameCreate, BoxLayout.Y_AXIS));

    JButton game_Request = new JButton("Join Game");
    game_Request.setVisible(false);
    JButton create_game = new JButton("Create Game");
    
    create_game.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        gameCreate.show((Component) gameList, 0,0);
      }
    });
      //new CreationListener());
    
    currentGames = new DefaultListModel<String>();
    
    gameList = new JList<String>(currentGames);
    
    JScrollPane gamePane = new JScrollPane(gameList);
    gamePane.setAlignmentX(CENTER_ALIGNMENT);
    
    // temporary game room for testing
    currentGames.addElement("1024: Artificial_Game 1/4 open");
    
    gameList.addMouseListener(new JoinListener());
    gameList.setPreferredSize(new Dimension(200, 300));
    
    JPanel subPanel = new JPanel();
    subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
    subPanel.add(create_game);
    subPanel.add(Box.createRigidArea(new Dimension(0,5)));
    subPanel.add(gamePane);
    
    center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
    //center.add(game_Request); // need message "N~[room name]~maxNumPlayers" around here
    center.add(subPanel);
    center.add(Box.createRigidArea(new Dimension(50,0)));
    
    //namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
    //playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.X_AXIS));
    
    //namePanel.add(gameName);
    //namePanel.add(gameNameField);
    
    //playerPanel.add(maxPlayers);
    //playerPanel.add(maxPlayerField);
  }

  /* mouse event listener for list
   * 
   */
  private class ChallengeListener extends MouseAdapter {
   public void mouseClicked(MouseEvent evt) {
    String challenged = userList.getSelectedValue();
    String challenger = username;
    if(challenged != " " && challenged != username) {
      //int userIndex = userList.getSelectedIndex();
      challengeMenu.show((Component) userList,0,0);
    }
   }
  }
  
  private class IssueChallengeListener implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      // send challenge request to the server which will handle it.
    }
  }

  /**
   * blahblahblah
   * @author alejandro
   *
   */
  //public class CreationListener implements ActionListener {
  //  public void actionPerformed(ActionEvent evt) {
          
  //    gameCreate.show((Component) gameList, 0,0);
      
  //  }
  //}

  private class JoinListener extends MouseAdapter {
    public void mouseClicked(MouseEvent evt) {
      String joined = gameList.getSelectedValue();
      if(!joined.equals(emptyGameList)) {
        // set up the code for what to do
        joinMenu.show((Component) gameList,0,0);
      }
    }
  }

  private class JoinGameListener implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      String roomInfo = gameList.getSelectedValue();

      // parse roomInfo to grab room number
      String [] roombits = roomInfo.split(" ");

      // removing colon in room number
      int roomNumber = Integer.parseInt
          (roombits[0].substring(0,roombits[0].length()-1));

      callingObj.sendMessageToServer("J~"+roomNumber); // join the game. Server will presumeably send back U~X~[room number]
      login_Frame.enterGame();
    }
  }

  private class ChatButtonListener implements ActionListener {    
    public void actionPerformed(ActionEvent event) {
      if(messageInput.isFocusOwner()) {
        //System.out.println("test button press\n");
        String message = messageInput.getText();
        if(!message.equals("")) { 
          // this line is temporary
          //chatLog.append(username + ": " + message + "\n");
          
          messageInput.setText("");
          callingObj.sendMessageToServer("C~"+message);
        }
      }
      else { }; // do nothing
    }
  }

  // contains the game room data.
  private class GameRoomData {
    String roomName;
    int currentNumPlayers;
    int maxNumPlayers;
    boolean playingStatus; // true if playing
    boolean full; // true if game room is full
    
    String gameRoomInfo;
    
    public GameRoomData(String roomName, int currentNumPlayers, 
        int maxNumPlayers, boolean status) {
      this.roomName = roomName;
      this.maxNumPlayers = maxNumPlayers;
      this.currentNumPlayers = currentNumPlayers;
      this.playingStatus = status;
      
      this.full = false;
    }
    
    public void setListString(String gameRoomInfo) {
      this.gameRoomInfo = gameRoomInfo;
    }
    
    public String getListString() {
      return gameRoomInfo;
    }
  
    public boolean testFull() {
      //full = currentNumPlayers == maxNumPlayers ? true : false;
      if (currentNumPlayers == maxNumPlayers) {
        full = true;
      }
      else {
        full = false;
      }
      return full;
    }
    
    public void gameStart() {
      playingStatus = true;
    }
    
    public void setInactive() {
      playingStatus = false;
    }
    
    public void setPlaying() {
      playingStatus = true;
    }
    
    public void addPlayer() {
      if(!full) {
        ++currentNumPlayers;
      }
    }
    
    public void removePlayer() {
      if(currentNumPlayers > 0) {
        --currentNumPlayers;
      }
    }
    
  }

  /**
   * Creates a new game room from the parameters specified in the game protocol
   */
  public void addGameRoom(int roomNumber, String roomName, 
      int curNumPlayer, int maxNumPlayer, boolean status) {
    GameRoomData newRoom = new GameRoomData(roomName, curNumPlayer, 
        maxNumPlayer, status);
    gameRoomList.put(roomNumber, newRoom);
    String statusWord = status == true ? "Playing" : "Open";
    String gameRoomInfo = roomName + " " + roomNumber + " " + 
        curNumPlayer + "/" + maxNumPlayer + " " + statusWord;
    currentGames.addElement(gameRoomInfo);
    newRoom.setListString(gameRoomInfo);
  }
  
  /**
   * Removes the specified game from the window and removes it from the master 
   * hash table of games.
   */
  public void removeGameRoom(int roomNumber) {
    GameRoomData deadRoom = gameRoomList.get(roomNumber);
    String gameRoomInfo = deadRoom.getListString();
    currentGames.removeElement(gameRoomInfo);
    gameRoomList.remove(roomNumber);
  }
  
  public void setInactive(int roomNum) {
    GameRoomData gameRoom = gameRoomList.get(roomNum);
    gameRoom.setInactive();
  }

  public void setPlaying(int roomNum) {
    GameRoomData gameRoom = gameRoomList.get(roomNum);
    gameRoom.setPlaying();
  }
  
  public void increasePlayers(int roomNum) {
    GameRoomData gameRoom = gameRoomList.get(roomNum);
    gameRoom.addPlayer();
  }
  
  public void decreasePlayers(int roomNum) {
    GameRoomData gameRoom = gameRoomList.get(roomNum);
    gameRoom.removePlayer();
  }
  
  /**
   * Will update the chat log as messages are sent
   * @param username: the username of the user who sent the message 
   * @param message: the message sent
   */
  public void updateChat(String username, String message) {
    chatLog.append(username + ": " + message + "\n");
  }

  /**
   *  Will update the userlist
   * @param mode: the mode of the change. "A" represents adding to the userlist.
   * "R" represents removing from the userlist.
   * @param username: The username of the user who sent the message
   */
  public void updateUserList(String mode, String username) {
    if (mode.equals("A")) {
      currentUsers.addElement(username);
    }
    else if (mode.equals("R")) {
      currentUsers.removeElement(username);
    }
    else {
      // shouldn't run
      System.err.println("Error. That is an invalid userlist update command");
    }
  }
}
