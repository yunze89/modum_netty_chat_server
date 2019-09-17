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

//Ŭ���̾�Ʈ�� ������ �����Ͽ� ������ ���� ä�ο��� �߻��ϴ� �̺�Ʈ// 
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

	private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	/*
		�̺�Ʈ ��鷯 ����
		1. ä�� ��� (channelRegistred)
   		2. ä�� Ȱ��ȭ (channelActive)
   		3. ������ �б� (channelRead)
   		4. ������ �б� �Ϸ� (channelReacComplete)
   		5. ä�� ��Ȱ��ȭ (channelInActive)
   		6. ä�� ��� ����  (channelUnregistred)
	*/
	
	//ä�� Ȱ��ȭ
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("����� ����");
	}


	//������ �޴� ���̽� �����Ϳ��� ����� ���� ���������� �ľ�
	String HEADER;
	/*
	HEADER
	cliententer, livelist(��� ����Ʈ ��û), createroom, enterroom, exitroom-�� �������鿡�� �޼��� ������
	livechatmsg, chatmsg
	
	HEADER������ ���� ���� ������ ����
	
	cliententer : CLIENTID, CLIENTNM
	createroom : ROOMNAME, MEMBERID
	chatmsg : ROOMNAME, CHATMSG, SENDERID
	
	*CHATMSG�� readok(ä�ù濡�� ���� Ȯ�� �޼���), exit(�泪��)
	
	 
	*/
	
	//������ Ŭ���̾�Ʈ�� ����
	String CLIENTID, CLIENTNM;
	//��Ʈ���� ���� ����
	String STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME;
	//�� ������ ���
	String ROOMTYPE;	//live, chat
	String MEMBERID;
	String[] MEMBERID_ARRAY;	
	//ä���� ���
	String UNIQUENUMBER, CHATMSG, ROOMTITLE, ROOMNAME, SENDERID;
	//CHATMSG : "�Ϲݸ޼���", "exitroom", "enterroom"
	//���̺� ���
	String DONATION_ANI, DONATION_AMOUNT; //�����̼� �޽���, ȿ�� �ִϸ��̼� ������ ��� �ִϸ��̼��� �̸�?, ��ȣ? ���� 
										  //�����̼� �ݾ�
	
	//������ȭ�� ����ϴ� ����, ���
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
    
	//���̺� ��� ��� ������ ��̸���Ʈ
	private static ArrayList<LiveRoomData> LiveList = new ArrayList<>();
	//Ŭ���̾�Ʈ �� : Ŭ���̾�Ʈ���̵�, Ŭ�󸮾�Ʈ����(�г���, ä��)  
	private static Map<String, ClientInfo> ClientList = Collections.synchronizedMap(new HashMap<String, ClientInfo>());
	//ä�ù� �� : ���̸�, ������(��� ���̵�, ��� ä��) 
	private static Map<String, ChatRoomData> ChatRoomList = Collections.synchronizedMap(new HashMap<String, ChatRoomData>());
	//���̺�� �� : ���̸�, ������
	private static Map<String, LiveRoomData> LiveRoomList = Collections.synchronizedMap(new HashMap<String, LiveRoomData>());
	
	//������ �б�
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {		
		//ctx�� ���� ������ �޼����� ���� Ŭ���̾�Ʈ
		
		//ä���� Ŭ���̾�Ʈ���� ���� �޼��� 
		String message = (String) msg;
			
		System.out.println("[ChannelRead] ������ ���� �޼��� : "+ message);
		
		//���� �޼��� ���̽� �������� ��ȭ�ؼ� ������ ��ü ���� 
		JSONObject RecieveJsonMsg = null;
		
		try{
			//���� ��Ʈ�� �޼����� ���̽� �������� ��ȯ�Ͽ� ���� �� �Ľ�
			RecieveJsonMsg = new JSONObject(message);
			
			//--------Ŭ���̾�Ʈ���� ���� �޽��� �ľ��ϴ� �κ�----------//
			//��� �ľ�
			HEADER = RecieveJsonMsg.getString("HEADER");
			//ó�� �����ϸ� ������ Ŭ���̾�Ʈ �ڽ��� ���̵� �۽��Ѵ� : CLIENTID, CLIENTNM
			if(HEADER.equals("cliententer")){
				CLIENTID = RecieveJsonMsg.getString("CLIENTID");
				CLIENTNM = RecieveJsonMsg.getString("CLIENTNM");
				ClientInfo clientinfo = new ClientInfo(CLIENTNM,ctx.channel());	//Ŭ���̾�Ʈ�� �г��Ӱ� ä�� ����
				ClientList.put(CLIENTID, clientinfo);
				System.out.println(CLIENTID+"/"+CLIENTNM+"----���� �����ο�"+ClientList.size());
				//clientinfo.getClientChannel().writeAndFlush("�������� ���Ӽ��� �˸�");
			}
			//������ϴ� ��� : ROOMTYPE, ROOMNAME
			else if(HEADER.equals("createroom")){
				System.out.println("�����");
				ROOMTYPE = RecieveJsonMsg.getString("ROOMTYPE");	//ä�ù� Ȥ�� ���̺���� ���� �̸�
				ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");
				//SENDERID = RecieveJsonMsg.getString("SEDERID");
				
				System.out.println(ROOMTYPE);
				//ä�ù� �����ϴ� ��� : ROOMNAME, MEMBERID("/")
				if(ROOMTYPE.equals("chat")){
					//ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");	//ä�ù��� ���� �̸�
					SENDERID = RecieveJsonMsg.getString("SENDERID");
					MEMBERID = RecieveJsonMsg.getString("MEMBERID");	//�ڽ��� ���̵� ���ԵǾ��־����.
					MEMBERID_ARRAY = MEMBERID.split("/");				//ä�ù濡 �� ������� ���̵� "/"�����ڷ� ����Ǿ�����. -> �� ���̵� �����ڷ� ���ø��ؼ� �迭�� ����
					ROOMTITLE = RecieveJsonMsg.getString("ROOMTITLE");
					
					ChatRoomData ChatRoom=new ChatRoomData(ROOMNAME, MEMBERID, ROOMTITLE);
					ChatRoomList.put(ROOMNAME,ChatRoom);	//���� ä�ù��� �� ����Ʈ�� ����
					
					for(int i = 0; i< MEMBERID_ARRAY.length ; i++){
						ChatRoom.enterRoom( MEMBERID_ARRAY[i], ClientList.get(MEMBERID_ARRAY[i]) );
					}
					//�� ����鿡�� �������� �����ϴ� �κ� �ʿ�
					ChatRoom.notifyCreatedRoom(SENDERID);
					
				}
				//���̺� �� �����ϴ� ��� : ROOMNAME, STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME
				else {
					System.out.println("���̺� �� ����");
					//ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");	//���̺� ä�� ���� ���� �̸�
					STREAM_TITLE = RecieveJsonMsg.getString("STREAM_TITLE");
					STREAM_PUBLISHERID = RecieveJsonMsg.getString("STREAM_PUBLISHERID");
					ClientInfo STREAM_PUBLISHER = ClientList.get(STREAM_PUBLISHERID);
					STREAM_FILENAME = RecieveJsonMsg.getString("STREAM_FILENAME");
					STREAM_DATETIME = RecieveJsonMsg.getString("STREAM_DATETIME");
					
					//���̺� �� ���� (ä�ù氳������)
					LiveRoomData LiveRoom = new LiveRoomData(ROOMNAME, STREAM_TITLE, STREAM_PUBLISHERID, STREAM_FILENAME, STREAM_DATETIME, STREAM_PUBLISHER);
					LiveList.add(LiveRoom);
					LiveRoomList.put(ROOMNAME, LiveRoom);
					System.out.println(LiveList.size()+" "+LiveRoomList.size());
				}
				
			}
			//���̺� ��� ���� �ʿ��� Ű �� : HEADER, ROOMTYPE, ROOMNAME, SENDERID
			else if(HEADER.equals("deleteroom")){
				System.out.println("������");
				ROOMTYPE = RecieveJsonMsg.getString("ROOMTYPE");	//ä�ù� Ȥ�� ���̺���� ���� �̸�
				ROOMNAME = RecieveJsonMsg.getString("ROOMNAME");
				SENDERID = RecieveJsonMsg.getString("SENDERID");
				
				if(ROOMTYPE.equals("chat")){
					
				}
				else{
					LiveRoomData roomdata = LiveRoomList.get(ROOMNAME);
					roomdata.endStreamingMsg(SENDERID);
					LiveList.remove(roomdata);		//���̺� �� ����Ʈ���� ����
					LiveRoomList.remove(ROOMNAME);	//���̺� �� ���� ���� �ʿ��� ����
					System.out.println(LiveList.size()+" "+LiveRoomList.size());	//���� �� ����Ʈ �� ���
				}
			}
			//���̺� ���, ä�� �濡 ����� ���� : HEADER, SENDERID, ROOMNAME, ROOMTYPE
			else if(HEADER.equals("enterroom")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMTYPE=RecieveJsonMsg.getString("ROOMTYPE");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				
				//���̺�濡 ����
				if(ROOMTYPE.equals("live")){
					//ROOMNAME���� ���� ã�� �� �濡 ���� ��Ŵ
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
			//�� ������ ���  : ROOMNAME, ROOMTYPE, SENDERID
			else if(HEADER.equals("exitroom")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMTYPE=RecieveJsonMsg.getString("ROOMTYPE");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				
				//���̺�濡 ����
				if(ROOMTYPE.equals("live")){
					//ROOMNAME���� ���� ã�� �� �濡 ���� ��Ŵ
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
			//����� ���̺� ä�ø޽����� ��� : SENDERID, ROOMNAME, CHATMSG
			else if(HEADER.equals("livechatmsg")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				CHATMSG=RecieveJsonMsg.getString("CHATMSG");
				
				//������ ������ �ش� �濡 �޼��� ����
				LiveRoomData LiveRoomData = LiveRoomList.get(ROOMNAME);				
				LiveRoomData.transferMsg(SENDERID, CHATMSG);
			}
			
			//18.06.07 �߰� : ���̺� ��ۿ��� ��û�ڰ� �����̼� �ִϸ��̼ǰ� �޽��� ������ �κ� 
			//����� ���̺� �����̼Ǹ޽����� ��� : SENDERID, ROOMNAME, DONATION_ANI, CHATMSG
			else if(HEADER.equals("livedonationmsg")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				DONATION_ANI=RecieveJsonMsg.getString("DONATION_ANI");
				DONATION_AMOUNT=RecieveJsonMsg.getString("DONATION_AMOUNT");
				CHATMSG=RecieveJsonMsg.getString("CHATMSG");
				
				//������ ������ �ش� �濡 �޼��� ����
				LiveRoomData LiveRoomData = LiveRoomList.get(ROOMNAME);				
				LiveRoomData.transferDonationMsg(SENDERID, CHATMSG, DONATION_ANI, DONATION_AMOUNT);
			}
			
			//����� �Ϲ� ä�ø޽����� ��� : SENDERID, ROOMNAME, CHATMSG
			else if(HEADER.equals("chatmsg")){
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ROOMNAME=RecieveJsonMsg.getString("ROOMNAME");
				CHATMSG=RecieveJsonMsg.getString("CHATMSG");
				UNIQUENUMBER=RecieveJsonMsg.getString("UNIQUENUMBER");
				
				//������ ������ �ش� �濡 �޼��� ���� 
				ChatRoomData ChatRoomData = ChatRoomList.get(ROOMNAME);
				ChatRoomData.transferMsg(SENDERID, CHATMSG, UNIQUENUMBER);
			}
			//��Ʈ���� ��� ����Ʈ ��û�ϴ� ��� : SENDERID
			else if (HEADER.equals("livelist")){
				//�ӽ����
				System.out.println("���̺� ����Ʈ ��û ����"+LiveList.size());
				//��û�� ����� ���̵� �Ľ�
				SENDERID=RecieveJsonMsg.getString("SENDERID");
				ClientInfo clientinfo = ClientList.get(SENDERID);
				
				//����ڿ��� ���� JSON�޼��� ����
				JSONObject LiveRoomJson = new JSONObject();
				LiveRoomJson.put("HEADER", "livelist");
				
				JSONArray LiveRoomList = new JSONArray();
				
				//���̺� ����� �����ϴ� ���
				if(LiveList.size()!=0){
					for(int i = LiveList.size()-1 ; i>=0 ; i--){
						JSONObject LiveRoomItem = new JSONObject();	//�ϳ��� ������ ���� ���̽� ������Ʈ ����
						LiveRoomData roomdata = LiveList.get(i);	//������ ������
						//LiveRoomItem.put(roomdata.getLiveRoomData());	//�������� ���̽� ������Ʈ�� �߰�
						LiveRoomList.put(roomdata.getLiveRoomData());	//�ϳ��� ������ ���� ���̽� ������Ʈ�� ���̽� ��̿� �߰�
						//System.out.println(roomdata.RoomName+"/"+roomdata.getLiveRoomData());
					}
					LiveRoomJson.put("LIST", LiveRoomList);
					
					System.out.println(LiveRoomJson);
					
					//���̽��� ���ڿ��� �ƴ� �״�� ������ ������ �ȵǴ� ������ �־���
					clientinfo.getClientChannel().writeAndFlush(LiveRoomJson.toString());
				}
				//���̺� ��۸���� �� ���
				else{
					LiveRoomJson.put("LIST", "null");
					clientinfo.getClientChannel().writeAndFlush(LiveRoomJson.toString());
				}
			}
			
			//������ȭ �� �����ڿ��� ������ 
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
					
					//������ȭ �� ������ ������ȭ�ݸ� �޼����� ���� ����
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
			//������ȭ �����ڰ� �����ʰ� �����ϴ� ���
			else if(HEADER.equals(TAG_REJECTVIDEOCALL)){
				String CallPartIds=RecieveJsonMsg.getString(TAG_PARTICIPANTIDS);
				String RejectUserNm = RecieveJsonMsg.getString("RejectUserNm");
				String[] CallPartIdArray= CallPartIds.split("/");
				
				for(int i = 0 ; i < CallPartIdArray.length ; i ++){
					
					System.out.println("---��ȭ����---"+CallPartIdArray[i]+"----------");
					
					JSONObject CallMsg = new JSONObject();
					CallMsg.put("HEADER", TAG_REJECTVIDEOCALL);
					CallMsg.put("RejectUserNm", RejectUserNm);
					ClientInfo clientinfo = ClientList.get(CallPartIdArray[i]);
					clientinfo.getClientChannel().writeAndFlush(CallMsg.toString());
				}
			}
			//������ȭ �߽��ڰ� �ɴٰ� �ٽ� ��ȭ�� �����ϴ� ���
			else if(HEADER.equals(TAG_STOPVIDEOCALL)){
				String CallPartIds=RecieveJsonMsg.getString(TAG_PARTICIPANTIDS);
				String[] CallPartIdArray= CallPartIds.split("/");
				
				for(int i = 0 ; i < CallPartIdArray.length ; i ++){
					System.out.println("----�� ����---"+CallPartIdArray[i]+"----------");
					
					JSONObject CallMsg = new JSONObject();
					CallMsg.put("HEADER", TAG_STOPVIDEOCALL);
					ClientInfo clientinfo = ClientList.get(CallPartIdArray[i]);
					clientinfo.getClientChannel().writeAndFlush(CallMsg.toString());
				}
			}
			//������ Ŭ���̾�Ʈ ���� ��� �����ϱ� ���� KeepAlive�޽���
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
		
		//--------����ڵ鿡�� �޽��� �����ϴ� �κ�----------//
		for (Channel channel : channelGroup) {
            if (channel != incoming) {
                channel.writeAndFlush("[" + incoming.remoteAddress() + "]" + message + "\n");
            }
        }
		
		//����
		if ("exit".equals(message.toLowerCase())) {
            ctx.close();
        }*/
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	//����� �߰� �Ǵ� ���
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		System.out.println("����� �߰��� [����]");
		Channel incomming = ctx.channel();
		
		/*for(Channel channel : channelGroup){
			//����� �߰��Ǿ��� �� ���� ����ڿ��� �˸�
			channel.write("[����] - "+incomming.remoteAddress()+"�� ����!\n");
		}*/
		channelGroup.add(incomming);
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		System.out.println("����� ���� [����]");
		Channel outgoing = ctx.channel();
		/*for(Channel channel : channelGroup){
			//����� ������ �� ���� ����ڿ��� �˸�
			channel.write("[����] - "+outgoing.remoteAddress()+"�� ����!\n");
		}*/
		channelGroup.remove(outgoing);
	}	
}
