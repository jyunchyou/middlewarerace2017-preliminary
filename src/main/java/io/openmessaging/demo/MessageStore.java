package io.openmessaging.demo;

import io.openmessaging.KeyValue;
import io.openmessaging.Message;
import io.openmessaging.MessageHeader;
import io.openmessaging.StreamCallBack;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;



public class MessageStore  {

    private static final MessageStore INSTANCE = new MessageStore();

    public static MessageStore getInstance() {
        return INSTANCE;
    }

    //private Map<String, ArrayList<Message>> messageBuckets = new HashMap<>();

   // private Map<String, HashMap<String, Integer>> queueOffsets = new HashMap<>();

   // private Lock lockFile = new ReentrantLock();

    //private Lock lockGet=new ReentrantLock();

    //private Lock lockRead=new ReentrantLock();

    private HashMap<String,FileChannelProxy> queueMap = new HashMap(20);

    private HashMap<String,FileChannelProxy> topicMap = new HashMap(120);

    //private DistributeLock sendLock=new DistributeLock(120);

    //final ThreadLocal threadLocal=new ThreadLocal();

    private HashMap<String,FileChannelProxy> sendMap;




    public synchronized void putMessage( Message message, KeyValue properties) throws IOException {

        if (message == null) return ;

        String fileType = message.headers().containsKey(MessageHeader.TOPIC) ? MessageHeader.TOPIC : MessageHeader.QUEUE;

        String bucket = message.headers().getString(fileType);

        String fileLocal=properties.getString("STORE_PATH") + "/" + fileType + "/" + bucket;

       /* threadLocal.set(fileLocal);

        FileChannelProxy fileChannelProxy=null;

        StreamCallBack callBack=new StreamCallBack() {
            @Override
            public FileChannelProxy callBack() {
                FileOutputStream fileOutputStream= null;
                try {
                    fileOutputStream = new FileOutputStream((String) threadLocal.get(),true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                FileChannel fileChannel=fileOutputStream.getChannel();
                FileChannelProxy fileChannelProxy=new FileChannelProxy();
                fileChannelProxy.setFileChannel(fileChannel);
                fileChannelProxy.setFileOutputStream(fileOutputStream);
                return fileChannelProxy;
            }
        };
        while((fileChannelProxy=sendLock.lock(bucket,callBack))==null);*/
        FileChannelProxy fileChannelProxy=sendMap.get(bucket);


        DefaultBytesMessage defaultBytesMessage = (DefaultBytesMessage) message;
        byte[] body=defaultBytesMessage.getBody();
        int length=body.length;
        //ByteBuffer byteBuffer = ByteBuffer.allocate(length + 2);

        AtomicBoolean atomicBooleanWrite=fileChannelProxy.runThread.getAtomicBooleanWrite();
       /* while(!atomicBooleanWrite.compareAndSet(true,false)){
           System.out.println("正在io。。。");


        }*/
        int num=fileChannelProxy.runThread.getIsFirst()&1;
        try {
            //long start=System.currentTimeMillis();

            fileChannelProxy.runThread.getSemaphoresWrites()[num].acquire();

           // long end=System.currentTimeMillis();
           // System.out.println("等待io cust:"+(start-end));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // fileChannelProxy.runThread.getLock().lock();
//        try {
//            fileChannelProxy.runThread.getSignal().await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
       // System.out.println("io结束");
    //    ByteBuffer byteBuffer=fileChannelProxy.runThread.getByteBuffer();
        ByteBuffer[] byteBuffers=fileChannelProxy.runThread.getByteBuffers();

        ByteBuffer byteBuffer=byteBuffers[num];
        //调整buff大小
        if( fileChannelProxy.runThread.getBuffSizes()[num]!=(length+2)){

            byteBuffer=ByteBuffer.allocateDirect(length+2);
            fileChannelProxy.runThread.getByteBuffers()[num]=(byteBuffer);
            fileChannelProxy.runThread.getBuffSizes()[num]=(length+2);
        }


       // int i=0;//i��ʾ1�ֽڿ��Ա�ʾ�����ִ�С��
        int j=0;//j��ʾ�������ٸ��ֽ�
        byte[] lenFlag=new byte[2];
        if(length>255){
            j=length/255;
        }

        lenFlag[0]= (byte) j;
        lenFlag[1]= (byte) length;


        byteBuffer.clear();
        byteBuffer.put(lenFlag);
        byteBuffer.put(defaultBytesMessage.getBody());

        byteBuffer.flip();

      //  System.out.println("123");
       // AtomicBoolean atomicBooleanRead=fileChannelProxy.runThread.getAtomicBooleanRead();
        //atomicBooleanRead.set(true);
       // fileChannelProxy.runThread.getSignal().signal();
        //fileChannelProxy.getLock().unlock();
        if(fileChannelProxy.runThread.getIsFirst()==0){
            fileChannelProxy.runThread.add();
            return ;

        }
        if(fileChannelProxy.runThread.getIsFirst()==1){
            fileChannelProxy.runThread.add();
            fileChannelProxy.run();
            return;

        }

        fileChannelProxy.runThread.getSemaphoresReads()[num].release();
        fileChannelProxy.runThread.add();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        FileInputStream fileInputStream=new FileInputStream("D:\\race\\test.txt");
        FileChannel fileChannel=fileInputStream.getChannel();

        ByteBuffer byteBuffer=ByteBuffer.allocate(5);

        fileChannel.read(byteBuffer);

        byte[] b=byteBuffer.array();
       // System.out.println(Arrays.toString(b));

        FileChannel fileChannel2=fileInputStream.getChannel();
        ByteBuffer byteBuffer2=ByteBuffer.allocate(5);
        ThreadLocal threadLocal=new ThreadLocal();
        Long l=0l;
        ThreadLocal threadLocal1=new ThreadLocal();
        threadLocal.set(l);
        threadLocal1.set(2);
        System.out.println(threadLocal.get());
        System.out.println(threadLocal1.get());
        fileChannel2.position((Long) threadLocal.get());
        System.out.println(threadLocal.get());
        fileChannel2.read(byteBuffer2);

        byte[] b2=byteBuffer2.array();
        System.out.println(Arrays.toString(b2));
        System.out.println(fileChannel);
        System.out.println(fileChannel2);
    }
    public  MessageProxy pullMessage(String queue, String bucket, KeyValue keyValue) {


        String queueLocal = keyValue.getString("STORE_PATH") + "/" + MessageHeader.QUEUE + "/" + bucket;
        String topicLocal = keyValue.getString("STORE_PATH") + "/" + MessageHeader.TOPIC + "/" + bucket;

        if(queue.equals(bucket)) {
            return readQueue(queueLocal, queue,keyValue);
        }else {

            return readTopic(topicLocal,bucket,keyValue);

        }
    }

    public  MessageProxy readQueue(String fileLocal,String bucket,KeyValue keyValue) {



        FileChannelProxy inputStream = queueMap.get(bucket);
        FileChannel fileChannel = null;

        ByteBuffer preBuff = ByteBuffer.allocate(2);
        try {
            fileChannel=inputStream.getFileChannel();

            if (fileChannel.read(preBuff) == -1) {
             
                fileChannel.close();

                inputStream.getFileInputStream().close();



                return new MessageProxy(true);

            }


            //  System.out.println(Arrays.toString(preBuff.array()));
            preBuff.flip();
            byte[] lenFlag = preBuff.array();
            int len = 0;
            if (lenFlag[0] != 0) {
                int temp = lenFlag[0] * 255;
                len += temp;
            }
            len += lenFlag[1];

            ByteBuffer buff = ByteBuffer.allocate(len);
            //System.out.println("����"+len);

            fileChannel.read(buff);

            DefaultBytesMessage message = new DefaultBytesMessage(buff.array());
            message.putHeaders(MessageHeader.QUEUE, bucket);
            message.putProperties(keyValue);


            return new MessageProxy(message);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }







    public  MessageProxy readTopic(String fileLocal,String bucket,KeyValue keyValue){

        FileChannelProxy fileChannelProxy=topicMap.get(bucket);
         ReentrantLock lock= (ReentrantLock) fileChannelProxy.getLock();

         lock.lock();
       /* AtomicBoolean lock=fileChannelProxy.getLock();
        while(!lock.compareAndSet(true,false));
*/





      /*  if (topicMap.containsKey(bucket)) {//judge and set
            inputStream = topicMap.get(bucket);

        } else {
            try {
                fileInputStream = new FileInputStream(fileLocal);
                inputStream = new FileChannelProxy();
                inputStream.setFileChannel(fileInputStream.getChannel());
                inputStream.setFileInputStream(fileInputStream);
            } catch (FileNotFoundException e) {

                e.printStackTrace();
            }
            topicMap.put(bucket, inputStream);
        }*/

        if(fileChannelProxy.isEnd()){
            MessageProxy messageProxy=new MessageProxy(true);

            return messageProxy;
        }

        ByteBuffer preBuff = ByteBuffer.allocate(2);
        try {

            FileChannel fileChannel = fileChannelProxy.getFileChannel();

            fileChannel.position(fileChannelProxy.getPosition());
            //  System.out.println(fileChannelProxy.getPosition());
            if (fileChannel.read(preBuff) == -1) {




                fileChannel.close();

                fileChannelProxy.getFileInputStream().close();
                fileChannelProxy.setEnd(true);

                //todo remove filechannelproxy on lockmap

                MessageProxy messageProxy=new MessageProxy(true);
                  lock.unlock();
                //lock.set(true);
                return messageProxy;

            }


            //  System.out.println(Arrays.toString(preBuff.array()));
            preBuff.flip();
            byte[] lenFlag = preBuff.array();
            int len = 0;
            if (lenFlag[0] != 0) {
                int temp = lenFlag[0] * 255;
                len += temp;
            }
            len += lenFlag[1];

            ByteBuffer buff = ByteBuffer.allocate(len);
            //System.out.println("����"+len);

            fileChannel.read(buff);

            fileChannelProxy.setPosiLtion(fileChannel.position());

            DefaultBytesMessage message = new DefaultBytesMessage(buff.array());
            message.putHeaders(MessageHeader.TOPIC, bucket);
            message.putProperties(keyValue);

            lock.unlock();
            //lock.set(true);
            return new MessageProxy(message);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    public void attachInitQueue(String queue,KeyValue properties) {

        String fileLocal = properties.getString("STORE_PATH") + "/" + MessageHeader.QUEUE + "/" + queue;
        FileChannelProxy fileChannelProxy=new FileChannelProxy();

        try {
            FileInputStream fileInputStream=new FileInputStream(fileLocal);

            fileChannelProxy.setFileChannel(fileInputStream.getChannel());
            fileChannelProxy.setFileInputStream(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        queueMap.put(queue,fileChannelProxy);


    }
    public void attachInitTopic(Collection<String> collection,KeyValue properties){

        for(String bucket:collection) {

            String fileLocal = properties.getString("STORE_PATH") + "/" + MessageHeader.TOPIC + "/" + bucket;

            try {
                if(topicMap.get(bucket)==null){
                    FileChannelProxy fileChannelProxy=new FileChannelProxy();
                    FileInputStream fileInputStream = new FileInputStream(fileLocal);
                    fileChannelProxy.setFileChannel(fileInputStream.getChannel());
                    fileChannelProxy.setFileInputStream(fileInputStream);
                    Long start=0l;
                    fileChannelProxy.setPosiLtion(start);

                    topicMap.put(bucket,fileChannelProxy);
                }


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
    }

    public HashMap<String, FileChannelProxy> getSendMap() {
        return sendMap;
    }

    public void setSendMap(HashMap<String, FileChannelProxy> sendMap) {
        this.sendMap = sendMap;
    }
 /*   public static void main(String[] args) throws FileNotFoundException, IOException {
      *//*  int m=7;
        int n=90;
         m=m<<1+8;
        System.out.println(m>>2);
        BitSet bitSet=new BitSet();*//*

      RandomAccessFile random=new RandomAccessFile("D:\\race\\Queue\\test.txt","r");
        Mytask task=new Mytask();
        Mytask task2=new Mytask();
        ReentrantLock reen=new ReentrantLock();
        task2.random=random;
        task2.lock=reen;
        task2.i=0;
        task.random=random;
        task.lock=reen;
        task.i=5;
        Thread thread=new Thread(task);
        Thread thread2=new Thread(task2);
        thread.start();
        thread2.start();

      *//*

        FileInputStream input=new FileInputStream("D:\\race\\Queue\\test.txt");

        FileChannel fileChannel=input.getChannel();
        System.out.println(fileChannel.position());
        FileChannel file=fileChannel.position(5);
        ByteBuffer byteBuffer=ByteBuffer.allocate(20);
        file.read(byteBuffer);

        System.out.println(Arrays.toString(byteBuffer.array()));

        FileChannel channel=input.getChannel();
        ByteBuffer b=ByteBuffer.allocate(20);
        channel.position(6);
        channel.read(b);
        System.out.println(Arrays.toString(b.array()));
*//*


        *//*ReentrantLock lock=new ReentrantLock();
        Mytask myTask=new Mytask();
        myTask.input=input;
        myTask.lock=lock;
        Thread thread1=new Thread(myTask);
        Thread thread2=new Thread(myTask);
        thread1.start();
        thread2.start();
*//*

    }


}


class Mytask implements  Runnable{
    FileInputStream input;
    ReentrantLock lock;
    RandomAccessFile random;
    int i;
    public void read() {
        lock.lock();
        try {
            random.seek(i);

            for(int indexNum=0;indexNum<10;indexNum++) {



            }
            random.seek(0);

            } catch (IOException e) {
            e.printStackTrace();
        }
        lock.unlock();
    }
    @Override
    public void run() {
        read();
    }*/
}


