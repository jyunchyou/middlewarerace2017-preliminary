
package io.openmessaging.demo;


import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import io.openmessaging.Message;
import io.openmessaging.MessageFactory;
import io.openmessaging.MessageHeader;
import io.openmessaging.Producer;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class DefaultProducer implements Producer {
    private MessageFactory messageFactory = new DefaultMessageFactory();
    private MessageStore messageStore = MessageStore.getInstance();
    private KeyValue properties;
    private HashMap hashMap=new HashMap();
    public DefaultProducer(KeyValue properties) {

        this.properties = properties;
    }


    @Override public BytesMessage createBytesMessageToTopic(String topic, byte[] body) {
        if(!hashMap.containsKey(topic)){
            File fileChild = new File(properties.getString("STORE_PATH") +"/"+MessageHeader.TOPIC+"/"+topic);
            try {
                fileChild.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        DefaultBytesMessage defaultBytesMessage= (DefaultBytesMessage) messageFactory.createBytesMessageToTopic(topic, body);
        String key= (String) properties.keySet().toArray()[0];

       defaultBytesMessage.putProperties(key,properties.getString(key));
        return defaultBytesMessage;

    }

    @Override public BytesMessage createBytesMessageToQueue(String queue, byte[] body) {
        if(!hashMap.containsKey(queue)){
            File fileChild = new File(properties.getString("STORE_PATH") +"/"+MessageHeader.QUEUE+"/"+queue);
            try {
                fileChild.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       DefaultBytesMessage defaultBytesMessage= (DefaultBytesMessage) messageFactory.createBytesMessageToQueue(queue, body);

        String key= (String) properties.keySet().toArray()[0];

        defaultBytesMessage.putProperties(key,properties.getString(key));
        return defaultBytesMessage;
    }


    @Override public KeyValue properties() {
        return properties;
    }

    @Override public void send(Message message) {

        if (message == null) return ;
        String topic = message.headers().getString(MessageHeader.TOPIC);
        String queue = message.headers().getString(MessageHeader.QUEUE);
        if ((topic == null && queue == null) || (topic != null && queue != null)) {

         return ;
        }

        try {
            messageStore.putMessage(topic != null ? topic : queue,message,properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override public void send(Message message, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }



    @Override public void sendOneway(Message message) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override public void sendOneway(Message message, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

}