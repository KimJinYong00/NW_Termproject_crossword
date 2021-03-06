package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.ListSelectionModel;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import info.*;

public class Client {
	public final static int PORT = 9000;
	private BufferedReader in;
	private PrintWriter out;
	private ObjectOutputStream oout;
	private ObjectInputStream oin;
	private HashMap<Integer, String> roomList;
	private String[] roomNames;
	private String userID, pw;
	private LoginFrame loginFrame;
	private LobbyFrame lobbyFrame;
	private RoomFrame roomFrame;
	private int roomNum = -1;

	public Client() {
		userID = "";
		pw = "";
		loginFrame = new LoginFrame();
		loginFrame.setVisible(true);
		loginFrame.loginBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				userID = loginFrame.getId();
				pw = new String(loginFrame.getPassword());

				JSONObject loginJSON = new JSONObject();
				loginJSON.put("type", "LOGIN");
				loginJSON.put("userID", userID);
				loginJSON.put("pw", pw);
				out.println(loginJSON.toString());
			}
		});
	}

	public void run() {
		String serverAddress = "127.0.0.1";
		JSONParser parser = new JSONParser();
		JSONObject inJSON;
		int stringIndex = 0;

		try {
			Socket socket = new Socket(serverAddress, PORT);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			oin = new ObjectInputStream(socket.getInputStream());
			//oout = new ObjectOutputStream(socket.getOutputStream());
			

			while(true) {
				String input = in.readLine();
				stringIndex  = input.indexOf("{");
				inJSON = (JSONObject) parser.parse(input.substring(stringIndex));
				String type = inJSON.get("type").toString();
				System.out.println(type);
				JSONObject outJSON = new JSONObject();
				
				switch(type) {
				case "LOGIN":
					String loginState = inJSON.get("state").toString();
					
					if(loginState.equals("SUCCESS")) {
						System.out.println("login success");
						loginSuccess();
						setList(oin.readObject());
					}
					else if(loginState.equals("FAIL"))
						System.out.println("login fail");
					break;
					
				case "CREATEROOM":
					String createState = inJSON.get("state").toString();
					
					if(createState.equals("SUCCESS")) {
						lobbyFrame.setVisible(false);
						roomFrame = new RoomFrame(inJSON.get("roomNum").toString(), userID);
						roomFrame.setVisible(true);
					}
					else if(createState.equals("FAIL"))
						System.out.println("Fail create room");
					break;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void loginSuccess() {
		loginFrame.setVisible(false);
		loginFrame.dispose();
		
		lobbyFrame = new LobbyFrame();
		lobbyFrame.setVisible(true);
		
		lobbyFrame.createRoomBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final OptionFrame optionFrame = new OptionFrame();
				optionFrame.setVisible(true);
				
				optionFrame.btnCreateRoom.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String roomName = optionFrame.roomNameText.getText();
						String mode = optionFrame.getMode();
						int peopleNum = Integer.parseInt(optionFrame.peopleNumText.getText());
						if(roomName.isEmpty() || mode.isEmpty() || peopleNum == 0)
							System.out.println("Wrong Input");
						else {
							JSONObject json = new JSONObject();
							json.put("type", "CREATEROOM");
							json.put("mode", mode);
							json.put("roomName", roomName);
							json.put("peopleNum", peopleNum);
							json.put("userID", userID);
							out.println(json.toString());
							optionFrame.dispose();
						}
					}
				});
			}
		});
	}
	
	private void setList(Object o) {
		roomList = (HashMap<Integer, String>) o;
		roomNames = new String[roomList.size()];
		
		Iterator<Integer> it = roomList.keySet().iterator();
		int i = 0;
		while(it.hasNext()) {
			roomNames[i++] = roomList.get(it.next());
		}
		
		lobbyFrame.list.setListData(roomNames);		
	}
	
	public static void main(String[] args) {
		Client client = new Client();

		try {
			System.out.println("Login");
			client.run();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
