package NettySocketServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import io.netty.channel.Channel;

public class LiveRoomData {
	
	//방송 방의 고유 이름
	String RoomName;
	//방송 하는 호스트 아이디
	String HostId;
	//방송의 정보
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
		//방을 개설한 방송자의 정보 추가
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
	
	//방입장
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
	
	//멤버들에게 채팅 메세지 전송
	public void transferMsg(String SenderId, String ChatMessage){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "livechatmsg");
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
	
	//18.06.07 추가
	//도네이션 효과, 메시지 보내는 메서드 : HEADER, SENDERNM, DONATION_ANI, CHATMSG
	public void transferDonationMsg(String SenderId, String ChatMessage, String DonationAni, String DonationAmount){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "livedonationmsg");
			EntireMessageJson.put("SENDERNM",ClientIdNmMap.get(SenderId));
			EntireMessageJson.put("DONATION_ANI", DonationAni);
			EntireMessageJson.put("CHATMSG", ChatMessage);
			EntireMessageJson.put("DONATION_AMOUNT", DonationAmount);
			
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
	
	
	//방송종료 메세지
	public void endStreamingMsg(String SenderId){
		JSONObject EntireMessageJson = new JSONObject();
		try {
			EntireMessageJson.put("HEADER", "liveend");
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
