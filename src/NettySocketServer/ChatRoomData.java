package NettySocketServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import io.netty.channel.Channel;

public class ChatRoomData {
	ArrayList<Channel> ClientChannelListInRoom=new ArrayList<>();
	ArrayList<String> ClientIdListInRoom=new ArrayList<>();
	Map<String,String> ClientIdNmMap= Collections.synchronizedMap(new HashMap<String, String>());
	private String RoomName;
	private String MemberId;
	private String RoomTitle;
	
	public ChatRoomData(String RoomName, String MemberId, String RoomTitle){
		this.RoomName = RoomName;
		this.MemberId = MemberId;
		this.RoomTitle = RoomTitle;
	}
	
	public void enterRoom(String ClientId, ClientInfo clientinfo){
		ClientIdListInRoom.add(ClientId);
		ClientIdNmMap.put(ClientId, clientinfo.getClientNm());
		ClientChannelListInRoom.add(clientinfo.getClientChannel());
	}
	public void exitRoom(String SenderId, ClientInfo clientinfo){
		int removeindex=ClientIdListInRoom.indexOf(SenderId);
		ClientIdListInRoom.remove(SenderId);
		ClientChannelListInRoom.remove(removeindex);
		ClientIdNmMap.remove(SenderId);
	}
	
	//송신자 아이디와 메세지를 파라미터로 전달받아 해당 아이디의 닉네임으로 메세지를 방 멤버들에게 전달
	public void transferMsg(String SenderId, String ChatMessage, String UniqueNumber){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "chatmsg");
			EntireMessageJson.put("ROOMNAME", RoomName);
			EntireMessageJson.put("UNIQUENUMBER", UniqueNumber);
			EntireMessageJson.put("SENDERID", SenderId);
			EntireMessageJson.put("SENDERNM",ClientIdNmMap.get(SenderId));
			EntireMessageJson.put("CHATMSG", ChatMessage);
			
			//클라이언트들에게 메세지를 뿌리는 부분
			for(int i=0 ; i<ClientChannelListInRoom.size() ; i++){
				//자신을 제외한 클라이언트들에게만 메세지 전달
				if(!ClientIdListInRoom.get(i).equals(SenderId))
					ClientChannelListInRoom.get(i).writeAndFlush(EntireMessageJson.toString());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//송신자 아이디와 메세지를 파라미터로 전달받아 해당 아이디의 닉네임으로 메세지를 방 멤버들에게 전달
		public void transferMsg(String SenderId, String ChatMessage){
			JSONObject EntireMessageJson = new JSONObject();
			try {
				EntireMessageJson.put("HEADER", "chatmsg");
				EntireMessageJson.put("ROOMNAME", RoomName);
				EntireMessageJson.put("SENDERID", SenderId);
				EntireMessageJson.put("SENDERNM",ClientIdNmMap.get(SenderId));
				EntireMessageJson.put("CHATMSG", ChatMessage);
				
				//클라이언트들에게 메세지를 뿌리는 부분
				for(int i=0 ; i<ClientChannelListInRoom.size() ; i++){
					//자신을 제외한 클라이언트들에게만 메세지 전달
					if(!ClientIdListInRoom.get(i).equals(SenderId))
						ClientChannelListInRoom.get(i).writeAndFlush(EntireMessageJson.toString());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void notifyCreatedRoom(String SenderId){
			JSONObject EntireMessageJson = new JSONObject();
			try {
							
				String MemberNm="";
				//아이디로 닉네임 꺼내서 구분자 ","를 붙인 문자열 생성 : 제이슨으로 참여자에게 보내기 위해
				for(int i=0; i<ClientIdListInRoom.size(); i++){
					MemberNm+=ClientIdNmMap.get(ClientIdListInRoom.get(i))+",";
				}
				MemberNm = MemberNm.substring(0, MemberNm.length()-1);	//마지막 "," 제거
			
				EntireMessageJson.put("HEADER", "chatroomcreated");
				EntireMessageJson.put("ROOMNAME", RoomName);
				EntireMessageJson.put("ROOMTITLE", RoomTitle);
				EntireMessageJson.put("MEMBERID", MemberId);		
				EntireMessageJson.put("MEMBERNM", MemberNm);
				
				//클라이언트들에게 메세지를 뿌리는 부분
				for(int i=0 ; i<ClientChannelListInRoom.size() ; i++){
					//자신을 제외한 클라이언트들에게만 메세지 전달
					if(!ClientIdListInRoom.get(i).equals(SenderId))
						ClientChannelListInRoom.get(i).writeAndFlush(EntireMessageJson.toString());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	
}
