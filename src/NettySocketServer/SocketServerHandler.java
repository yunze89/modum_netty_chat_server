package NettySocketServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

//클라이언트가 서버에 접속하여 서버측 소켓 채널에서 발생하는 이벤트// 
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

	private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	/*
		이벤트 헨들러 순서
		1. 채널 등록 (channelRegistred)
   		2. 채널 활성화 (channelActive)
   		3. 데이터 읽기 (channelRead)
   		4. 데이터 읽기 완료 (channelReacComplete)
   		5. 채널 비활성화 (channelInActive)
   		6. 채널 등록 해제  (channelUnregistred)
	*/
	
	//채널 활성화
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("사용자 접속");
	}


	//서버가 받는 제이슨 데이터에서 헤더로 무슨 데이터인지 파악
	String HEADER;
	/*
	HEADER
	cliententer, livelist(방송 리스트 요청), createroom, enterroom, exitroom-방 구성원들에게 메세지 전송함
	livechatmsg, chatmsg
	
	HEADER정보에 따른 내부 데이터 내용
	
	cliententer : CLIENTID, CLIENTNM
	createroom : ROOMNAME, MEMBERID
	chatmsg : ROOMNAME, CHATMSG, SENDERID
	
	*CHATMSG에 readok(채팅방에서 읽음 확인 메세지), exit(방나감)
	
	 
	*/
	
	//접속한 클라이언트이 정보
	String CLIENTID, CLIENTNM;
	//스트리밍 소켓 서버
	String STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME;
	//방 생성의 경우
	String ROOMTYPE;	//live, chat
	String MEMBERID;
	String[] MEMBERID_ARRAY;	
	//채팅인 경우
	String UNIQUENUMBER, CHATMSG, ROOMTITLE, ROOMNAME, SENDERID;
	//CHATMSG : "일반메세지", "exitroom", "enterroom"
	//라이브 방송
	String DONATION_ANI, DONATION_AMOUNT; //도네이션 메시지, 효과 애니메이션 보내는 경우 애니메이션의 이름?, 번호? 보냄 
										  //도네이션 금액
	
	//영상통화시 사용하는 변수, 상수
	String TAG_VIDEOCALL = "videocall";
	String TAG_REJECTVIDEOCALL = "reject_videocall";
	String TAG_STOPVIDEOCALL = "stop_videocall";
	String PARTICIPANTNMS, PARTICIPANTIDS, VIDEOROOMID;
	private String TAG_VIDEOROOMID = "VIDEOROOM_ID";
    private String TAG_PARTICIPANTNMS = "PARTICIPANTS_NMS";
    private String TAG_PARTICIPANTIDS = "PARTICIPANTS_IDS";
    /**
    "HEADER","videocall"
    "PARTICIPANTIDS", ParticipantId/ParticipantId/ParticipantId/
    "PARTICIPANTNMS", ParticipantNm/ParticipantNm/ParticipantNm/
    "ROOMID", RoomId
    */
    
	//라이브 방송 목록 저장할 어레이리스트
	private static ArrayList<LiveRoomData> LiveList = new ArrayList<>();
	//클라이언트 맵 : 클라이언트아이디, 클라리언트정보(닉네임, 채널)  
	private static Map<String, ClientInfo> ClientList = Collections.synchronizedMap(new HashMap<String, ClientInfo>());
	//채팅방 맵 : 방이름, 방정보(멤버 아이디, 멤버 채널) 
	private static Map<String, ChatRoomData> ChatRoomList = Collections.synchronizedMap(new HashMap<String, ChatRoomData>());
	//라이브방 맵 : 방이름, 방정보
	private static Map<String, LiveRoomData> LiveRoomList = Collections.synchronizedMap(new HashMap<String, LiveRoomData>());
	
	//데이터 읽기
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {		
		//ctx는 현재 서버에 메세지를 보낸 클라이언트
		
		//채널이 클라이언트에게 받은 메세지 
		String message = (String) msg;
			
		System.out.println("[ChannelRead] 서버가 받은 메세지 : "+ message);
		
		//받은 메세지 제이슨 형식으로 변화해서 저장할 객체 선언 
		JSONObject RecieveJsonMsg = null;
		
		try{
			//받은 스트링 메세지를 제이슨 형식으로 변환하여 저장 후 파싱
			RecieveJsonMsg = new JSONObject(message);
			
			//--------클라이언트에게 받은 메시지 파악하는 부분----------//
			//헤더 파악
			HEADER = RecieveJsonMsg.getString("HEADER");
			//처음 접속하면 서버에 클라이언트 자신의 아이디 송신한다 : CLIENTID, CLIENTNM
			if(HEADER.equals("cliententer")){
				CLIENTID = RecieveJsonMsg.getString("CLIENTID");
				CLIENTNM = RecieveJsonMsg.getString("CLIENTNM");
				ClientInfo clientinfo = new ClientInfo(CLIENTNM,ctx.channel());	//클라이언트의 닉네임과 채널 저장
				ClientList.put(CLIENTID, clientinfo);
				System.out.println(CLIENTID+"/"+CLIENTNM+"----현재 접속인원"+ClientList.size());
				//clientinfo.getClientChannel().writeAndFlush("서버에서 접속성공 알림");
			}
			//방생성하는 경우 : ROOMTYPE, ROOMNAME
			else if(HEADER.equals("createroom")){
				System.out.println("방생성");
				ROOMTYPE = RecieveJsonMsg.getString("ROOMTYPE");	//채팅방 혹은 라이브방의 공유 이름
				ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");
				//SENDERID = RecieveJsonMsg.getString("SEDERID");
				
				System.out.println(ROOMTYPE);
				//채팅방 생성하는 경우 : ROOMNAME, MEMBERID("/")
				if(ROOMTYPE.equals("chat")){
					//ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");	//채팅방의 고유 이름
					SENDERID = RecieveJsonMsg.getString("SENDERID");
					MEMBERID = RecieveJsonMsg.getString("MEMBERID");	//자신의 아이디도 포함되어있어야함.
					MEMBERID_ARRAY = MEMBERID.split("/");				//채팅방에 들어갈 멤버들의 아이디 "/"구분자로 저장되어있음. -> 각 아이디를 구분자로 스플릿해서 배열에 저장
					ROOMTITLE = RecieveJsonMsg.getString("ROOMTITLE");
					
					ChatRoomData ChatRoom=new ChatRoomData(ROOMNAME, MEMBERID, ROOMTITLE);
					ChatRoomList.put(ROOMNAME,ChatRoom);	//만든 채팅방을 방 리스트에 넣음
					
					for(int i = 0; i< MEMBERID_ARRAY.length ; i++){
						ChatRoom.enterRoom( MEMBERID_ARRAY[i], ClientList.get(MEMBERID_ARRAY[i]) );
					}
					//방 멤버들에게 방정보를 전송하는 부분 필요
					ChatRoom.notifyCreatedRoom(SENDERID);
					
				}
				//라이브 방 생성하는 경우 : ROOMNAME, STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME
				else {
					System.out.println("라이브 방 생성");
					//ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");	//라이브 채팅 방의 고유 이름
					STREAM_TITLE = RecieveJsonMsg.getString("STREAM_TITLE");
					STREAM_PUBLISHERID = RecieveJsonMsg.getString("STREAM_PUBLISHERID");
					ClientInfo STREAM_PUBLISHER = ClientList.get(STREAM_PUBLISHERID);
					STREAM_FILENAME = RecieveJsonMsg.getString("STREAM_FILENAME");
					STREAM_DATETIME = RecieveJsonMsg.getString("STREAM_DATETIME");
					
					//라이브 방 생성 (채팅방개념포함)
					LiveRoomData LiveRoom = new LiveRoomData(ROOMNAME, STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME, STREAM_PUBLISHER);
					LiveList.add(LiveRoom);
					LiveRoomList.put(ROOMNAME, LiveRoom);
					System.out.println(LiveList.size()+" "+LiveRoomList.size());
				}
				
			}
			//라이브 방송 종료 필요한 키 값 : HEADER, ROOMTYPE, ROOMNAME, SENDERID
			else if(HEADER.equals("deleteroom")){
				System.out.println("방종료");
				ROOMTYPE = RecieveJsonMsg.getString("ROOMTYPE");	//채팅방 혹은 라이브방의 공유 이름
				ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");
				SENDERID = RecieveJsonMsg.getString("SENDERID");
				
				if(ROOMTYPE.equals("chat")){
					
				}
				else{
					LiveRoomData roomdata = LiveRoomList.get(ROOMNAME);
					roomdata.endStreamingMsg(SENDERID);
					LiveList.remove(roomdata);		//라이브 방 리스트에서 삭제
					LiveRoomList.remove(ROOMNAME);	//라이브 방 정보 담은 맵에서 삭제
					System.out.println(LiveList.size()+" "+LiveRoomList.size());	//현재 방 리스트 수 출력
				}
			}
			//라이브 방송, 채팅 방에 사용자 입장 : HEADER, SENDERID, ROOMNAME, ROOMTYPE
			else if(HEADER.equals("enterroom")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMTYPE=RecieveJsonMsg.getString("ROOMTYPE");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				
				//라이브방에 입장
				if(ROOMTYPE.equals("live")){
					//ROOMNAME으로 방을 찾아 그 방에 입장 시킴
					LiveRoomData EnterRoomData= LiveRoomList.get(ROOMNAME);
					EnterRoomData.enterRoom(SENDERID, ClientList.get(SENDERID));
					EnterRoomData.transferMsg(SENDERID, "enterroom");
				}
				else{
					ChatRoomData EnterRoomData = ChatRoomList.get(ROOMNAME);
					EnterRoomData.enterRoom(SENDERID, ClientList.get(SENDERID));
					EnterRoomData.transferMsg(SENDERID, "enterroom");
				}
				
			}
			//방 나가는 경우  : ROOMNAME, ROOMTYPE, SENDERID
			else if(HEADER.equals("exitroom")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMTYPE=RecieveJsonMsg.getString("ROOMTYPE");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				
				//라이브방에 입장
				if(ROOMTYPE.equals("live")){
					//ROOMNAME으로 방을 찾아 그 방에 입장 시킴
					LiveRoomData ExitRoomData= LiveRoomList.get(ROOMNAME);
					ExitRoomData.transferMsg(SENDERID, "exitroom");
					ExitRoomData.exitRoom(SENDERID, ClientList.get(SENDERID));
				}
				else{
					ChatRoomData ExitRoomData = ChatRoomList.get(ROOMNAME);
					ExitRoomData.transferMsg(SENDERID, "exitroom");
					ExitRoomData.exitRoom(SENDERID, ClientList.get(SENDERID));
				}					
			}
			//헤더가 라이브 채팅메시지인 경우 : SENDERID, ROOMNAME, CHATMSG
			else if(HEADER.equals("livechatmsg")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				CHATMSG=RecieveJsonMsg.getString("CHATMSG");
				
				//방정보 가져와 해당 방에 메세지 전달
				LiveRoomData LiveRoomData = LiveRoomList.get(ROOMNAME);				
				LiveRoomData.transferMsg(SENDERID, CHATMSG);
			}
			
			//18.06.07 추가 : 라이브 방송에서 시청자가 도네이션 애니메이션과 메시지 보내는 부분 
			//헤더가 라이브 도네이션메시지인 경우 : SENDERID, ROOMNAME, DONATION_ANI, CHATMSG
			else if(HEADER.equals("livedonationmsg")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				DONATION_ANI=RecieveJsonMsg.getString("DONATION_ANI");
				DONATION_AMOUNT=RecieveJsonMsg.getString("DONATION_AMOUNT");
				CHATMSG=RecieveJsonMsg.getString("CHATMSG");
				
				//방정보 가져와 해당 방에 메세지 전달
				LiveRoomData LiveRoomData = LiveRoomList.get(ROOMNAME);				
				LiveRoomData.transferDonationMsg(SENDERID, CHATMSG, DONATION_ANI, DONATION_AMOUNT);
			}
			
			//헤더가 일반 채팅메시지인 경우 : SENDERID, ROOMNAME, CHATMSG
			else if(HEADER.equals("chatmsg")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				CHATMSG=RecieveJsonMsg.getString("CHATMSG");
				UNIQUENUMBER=RecieveJsonMsg.getString("UNIQUENUMBER");
				
				//방정보 가져와 해당 방에 메세지 전달 
				ChatRoomData ChatRoomData = ChatRoomList.get(ROOMNAME);
				ChatRoomData.transferMsg(SENDERID, CHATMSG, UNIQUENUMBER);
			}
			//스트리밍 방송 리스트 요청하는 경우 : SENDERID
			else if (HEADER.equals("livelist")){
				//임시출력
				System.out.println("라이브 리스트 요청 받음"+LiveList.size());
				//요청한 사용자 아이디 파싱
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ClientInfo clientinfo = ClientList.get(SENDERID);
				
				//사용자에게 보낼 JSON메세지 제작
				JSONObject LiveRoomJson = new JSONObject();
				LiveRoomJson.put("HEADER", "livelist");
				
				JSONArray LiveRoomList = new JSONArray();
				
				//라이브 목록이 존재하는 경우
				if(LiveList.size()!=0){
					for(int i = LiveList.size()-1 ; i>=0 ; i--){
						JSONObject LiveRoomItem = new JSONObject();	//하나의 방정보 넣을 제이슨 오브젝트 선언
						LiveRoomData roomdata = LiveList.get(i);	//방정보 가져옴
						//LiveRoomItem.put(roomdata.getLiveRoomData());	//방정보를 제이슨 오브젝트에 추가
						LiveRoomList.put(roomdata.getLiveRoomData());	//하나의 방정보 담은 제이슨 오브젝트를 제이슨 어레이에 추가
						//System.out.println(roomdata.RoomName+"/"+roomdata.getLiveRoomData());
					}
					LiveRoomJson.put("LIST", LiveRoomList);
					
					System.out.println(LiveRoomJson);
					
					//제이슨을 문자열이 아닌 그대로 보내니 전송이 안되는 문제가 있었음
					clientinfo.getClientChannel().writeAndFlush(LiveRoomJson.toString());
				}
				//라이브 방송목록이 빈 경우
				else{
					LiveRoomJson.put("LIST", "null");
					clientinfo.getClientChannel().writeAndFlush(LiveRoomJson.toString());
				}
			}
			
			//영상통화 콜 참여자에게 보내기 
			else if(HEADER.equals(TAG_VIDEOCALL)){
				String CallPartIds, CallPartNms, RoomId, CallSenderId;
				CallPartIds=RecieveJsonMsg.getString(TAG_PARTICIPANTIDS);
				CallPartNms=RecieveJsonMsg.getString(TAG_PARTICIPANTNMS);
				RoomId = RecieveJsonMsg.getString(TAG_VIDEOROOMID);
				CallSenderId = RecieveJsonMsg.getString("SENDERID");
				
				System.out.println(CallSenderId);
				System.out.println(CallPartIds);
				System.out.println(CallPartNms);
				
				String[] CallPartIdArray= CallPartIds.split("/");
				
				for(int i = 0 ; i < CallPartIdArray.length ; i ++){
					
					//영상통화 건 본인은 영상통화콜링 메세지를 받지 않음
					if(CallPartIdArray[i].equals(CallSenderId))
						continue;
					
					JSONObject CallMsg = new JSONObject();
					CallMsg.put("HEADER", TAG_VIDEOCALL);
					CallMsg.put(TAG_PARTICIPANTIDS, CallPartIds);
					CallMsg.put(TAG_PARTICIPANTNMS, CallPartNms);
					CallMsg.put(TAG_VIDEOROOMID, RoomId);
					CallMsg.put("SENDERID", CallSenderId);
					System.out.println("----------"+CallPartIdArray[i]+"----------");
					ClientInfo clientinfo = ClientList.get(CallPartIdArray[i]);
					
					clientinfo.getClientChannel().writeAndFlush(CallMsg.toString());
				}
			}
			//영상통화 수신자가 받지않고 종료하는 경우
			else if(HEADER.equals(TAG_REJECTVIDEOCALL)){
				String CallPartIds=RecieveJsonMsg.getString(TAG_PARTICIPANTIDS);
				String RejectUserNm = RecieveJsonMsg.getString("RejectUserNm");
				String[] CallPartIdArray= CallPartIds.split("/");
				
				for(int i = 0 ; i < CallPartIdArray.length ; i ++){
					
					System.out.println("---통화거절---"+CallPartIdArray[i]+"----------");
					
					JSONObject CallMsg = new JSONObject();
					CallMsg.put("HEADER", TAG_REJECTVIDEOCALL);
					CallMsg.put("RejectUserNm", RejectUserNm);
					ClientInfo clientinfo = ClientList.get(CallPartIdArray[i]);
					clientinfo.getClientChannel().writeAndFlush(CallMsg.toString());
				}
			}
			//영상통화 발신자가 걸다가 다시 통화를 종료하는 경우
			else if(HEADER.equals(TAG_STOPVIDEOCALL)){
				String CallPartIds=RecieveJsonMsg.getString(TAG_PARTICIPANTIDS);
				String[] CallPartIdArray= CallPartIds.split("/");
				
				for(int i = 0 ; i < CallPartIdArray.length ; i ++){
					System.out.println("----콜 중지---"+CallPartIdArray[i]+"----------");
					
					JSONObject CallMsg = new JSONObject();
					CallMsg.put("HEADER", TAG_STOPVIDEOCALL);
					ClientInfo clientinfo = ClientList.get(CallPartIdArray[i]);
					clientinfo.getClientChannel().writeAndFlush(CallMsg.toString());
				}
			}
			//서버와 클라이언트 연결 계속 유지하기 위한 KeepAlive메시지
			else if(HEADER.equals("KEEPALIVE")){
				String ConnectedId;
				ConnectedId = RecieveJsonMsg.getString("ConnectedId");
				
				JSONObject AckMsg = new JSONObject();
				AckMsg.put("HEADER", "KEEPALIVE_ACK");
				
				ClientInfo clientinfo = ClientList.get(ConnectedId);
				clientinfo.getClientChannel().writeAndFlush(AckMsg.toString());
			}
			
		}
		catch(JSONException e){
			
		}
		
		
		/*Channel incoming = ctx.channel();
		
		//--------사용자들에게 메시지 전달하는 부분----------//
		for (Channel channel : channelGroup) {
            if (channel != incoming) {
                channel.writeAndFlush("[" + incoming.remoteAddress() + "]" + message + "\n");
            }
        }
		
		//나감
		if ("exit".equals(message.toLowerCase())) {
            ctx.close();
        }*/
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	//사용자 추가 되는 경우
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		System.out.println("사용자 추가됨 [서버]");
		Channel incomming = ctx.channel();
		
		/*for(Channel channel : channelGroup){
			//사용자 추가되었을 때 기존 사용자에게 알림
			channel.write("[서버] - "+incomming.remoteAddress()+"가 입장!\n");
		}*/
		channelGroup.add(incomming);
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		System.out.println("사용자 나감 [서버]");
		Channel outgoing = ctx.channel();
		/*for(Channel channel : channelGroup){
			//사용자 나갔을 때 기존 사용자에게 알림
			channel.write("[서버] - "+outgoing.remoteAddress()+"가 나감!\n");
		}*/
		channelGroup.remove(outgoing);
	}	
}
