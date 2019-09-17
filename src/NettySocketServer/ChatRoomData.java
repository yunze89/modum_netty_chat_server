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
	
	//�۽��� ���̵�� �޼����� �Ķ���ͷ� ���޹޾� �ش� ���̵��� �г������� �޼����� �� ����鿡�� ����
	public void transferMsg(String SenderId, String ChatMessage, String UniqueNumber){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "chatmsg");
			EntireMessageJson.put("ROOMNAME", RoomName);
			EntireMessageJson.put("UNIQUENUMBER", UniqueNumber);
			EntireMessageJson.put("SENDERID", SenderId);
			EntireMessageJson.put("SENDERNM",ClientIdNmMap.get(SenderId));
			EntireMessageJson.put("CHATMSG", ChatMessage);
			
			//Ŭ���̾�Ʈ�鿡�� �޼����� �Ѹ��� �κ�
			for(int i=0 ; i<ClientChannelListInRoom.size() ; i++){
				//�ڽ��� ������ Ŭ���̾�Ʈ�鿡�Ը� �޼��� ����
				if(!ClientIdListInRoom.get(i).equals(SenderId))
					ClientChannelListInRoom.get(i).writeAndFlush(EntireMessageJson.toString());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//�۽��� ���̵�� �޼����� �Ķ���ͷ� ���޹޾� �ش� ���̵��� �г������� �޼����� �� ����鿡�� ����
		public void transferMsg(String SenderId, String ChatMessage){
			JSONObject EntireMessageJson = new JSONObject();
			try {
				EntireMessageJson.put("HEADER", "chatmsg");
				EntireMessageJson.put("ROOMNAME", RoomName);
				EntireMessageJson.put("SENDERID", SenderId);
				EntireMessageJson.put("SENDERNM",ClientIdNmMap.get(SenderId));
				EntireMessageJson.put("CHATMSG", ChatMessage);
				
				//Ŭ���̾�Ʈ�鿡�� �޼����� �Ѹ��� �κ�
				for(int i=0 ; i<ClientChannelListInRoom.size() ; i++){
					//�ڽ��� ������ Ŭ���̾�Ʈ�鿡�Ը� �޼��� ����
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
				//���̵�� �г��� ������ ������ ","�� ���� ���ڿ� ���� : ���̽����� �����ڿ��� ������ ����
				for(int i=0; i<ClientIdListInRoom.size(); i++){
					MemberNm+=ClientIdNmMap.get(ClientIdListInRoom.get(i))+",";
				}
				MemberNm = MemberNm.substring(0, MemberNm.length()-1);	//������ "," ����
			
				EntireMessageJson.put("HEADER", "chatroomcreated");
				EntireMessageJson.put("ROOMNAME", RoomName);
				EntireMessageJson.put("ROOMTITLE", RoomTitle);
				EntireMessageJson.put("MEMBERID", MemberId);		
				EntireMessageJson.put("MEMBERNM", MemberNm);
				
				//Ŭ���̾�Ʈ�鿡�� �޼����� �Ѹ��� �κ�
				for(int i=0 ; i<ClientChannelListInRoom.size() ; i++){
					//�ڽ��� ������ Ŭ���̾�Ʈ�鿡�Ը� �޼��� ����
					if(!ClientIdListInRoom.get(i).equals(SenderId))
						ClientChannelListInRoom.get(i).writeAndFlush(EntireMessageJson.toString());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	
}
