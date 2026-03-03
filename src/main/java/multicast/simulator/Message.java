package multicast.simulator;

import java.io.Serializable;

public class Message implements Serializable{
    private static final long serialVersionUID = 1L;

    private MessageType type_msg;
    private int senderID;
    private int messageID;

    public Message(MessageType type, int send_id, int msg_id){
        this.type_msg = type;
        this.senderID = send_id;
        this.messageID = msg_id;
    }

    public MessageType getType(){
        return type_msg;
    }

    public int getSenderID(){
        return senderID;
    }

    public int getMessageID(){
        return messageID;
    }

    public void setMessageType(MessageType type){
        this.type_msg = type;
    }

    public void setSenderID(int id){
        this.senderID = id;
    }

    public void setMessageID(int id){
        this.messageID = id;
    }

    @Override
    public String toString(){
        return "Message{" +
                "type= " + type_msg +
                " sender_id= " + senderID +
                " message_id= " + messageID +
                "}";
    }
}
