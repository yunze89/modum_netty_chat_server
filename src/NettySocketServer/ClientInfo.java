package NettySocketServer;

import io.netty.channel.Channel;

public class ClientInfo {
	String ClientNm;
	Channel ClientChannel;
	public ClientInfo(String ClientNm, Channel ClientChannel){
		this.ClientNm=ClientNm;
		this.ClientChannel=ClientChannel;
	}
	
	public Channel getClientChannel(){
		return this.ClientChannel;
	}
	
	public String getClientNm(){
		return this.ClientNm;
	}
}
