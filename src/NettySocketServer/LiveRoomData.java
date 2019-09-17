package NettySocketServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import io.netty.channel.Channel;

public class LiveRoomData {
	
	//��� ���� ���� �̸�
	String RoomName;
	//��� �ϴ� ȣ��Ʈ ���̵�
	String HostId;
	//����� ����
	String STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME;
	
	ArrayList<Channel> ClientChannelListInRoom=new ArrayList<>();
	ArrayList<String> ClientIdListInRoom=new ArrayList<>();
	Map<String,String> ClientIdNmMap=Collections.synchronizedMap(new HashMap<String, String>());
	
	public LiveRoomData(String ROOMNAME, String STREAM_TITLE, String STREAM_PUBLISHERID, String STREAM_FILENAME, String STREAM_DATETIME, ClientInfo STREAM_PUBLISHER){
		this.RoomName=ROOMNAME;
		this.STREAM_TITLE=STREAM_TITLE;
		this.STREAM_PUBLISHERID = STREAM_PUBLISHERID;
		this.STREAM_FILENAME = STREAM_FILENAME;
		this.STREAM_DATETIME = STREAM_DATETIME;
		this.HostId=STREAM_PUBLISHERID;
		//���� ������ ������� ���� �߰�
		ClientChannelListInRoom.add(STREAM_PUBLISHER.getClientChannel());
		ClientIdListInRoom.add(STREAM_PUBLISHERID);
		ClientIdNmMap.put(STREAM_PUBLISHERID,STREAM_PUBLISHER.getClientNm());
	}
	
	public String getHostId(){
		return this.HostId;
	}
	
	public JSONObject getLiveRoomData(){
		JSONObject JsonRoomData = new JSONObject();
		try {
			JsonRoomData.put("ROOMNAME", RoomName);
			JsonRoomData.put("STREAM_TITLE", STREAM_TITLE);
			JsonRoomData.put("STREAM_PUBLISHERNM", ClientIdNmMap.get(STREAM_PUBLISHERID));
			JsonRoomData.put("STREAM_FILENAME", STREAM_FILENAME);
			JsonRoomData.put("STREAM_DATETIME", STREAM_DATETIME);
			JsonRoomData.put("VIEW_NUM", ClientIdListInRoom.size());
			JsonRoomData.put("HOSTID", HostId);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return JsonRoomData;		
	}
	
	//������
	public void enterRoom(String SenderId, ClientInfo ClientInfo){
		ClientIdListInRoom.add(SenderId);
		ClientChannelListInRoom.add(ClientInfo.getClientChannel());
		ClientIdNmMap.put(SenderId,ClientInfo.getClientNm());
	}
	
	public void exitRoom(String SenderId, ClientInfo clientinfo){
		int removeindex=ClientIdListInRoom.indexOf(SenderId);
		ClientIdListInRoom.remove(SenderId);
		ClientChannelListInRoom.remove(removeindex);
		ClientIdNmMap.remove(SenderId);
	}
	
	//����鿡�� ä�� �޼��� ����
	public void transferMsg(String SenderId, String ChatMessage){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "livechatmsg");
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
	
	//18.06.07 �߰�
	//�����̼� ȿ��, �޽��� ������ �޼��� : HEADER, SENDERNM, DONATION_ANI, CHATMSG
	public void transferDonationMsg(String SenderId, String ChatMessage, String DonationAni, String DonationAmount){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "livedonationmsg");
			EntireMessageJson.put("SENDERNM",ClientIdNmMap.get(SenderId));
			EntireMessageJson.put("DONATION_ANI", DonationAni);
			EntireMessageJson.put("CHATMSG", ChatMessage);
			EntireMessageJson.put("DONATION_AMOUNT", DonationAmount);
			
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
	
	
	//������� �޼���
	public void endStreamingMsg(String SenderId){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "liveend");
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
