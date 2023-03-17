package com.mci.ais;

import org.eclipse.paho.client.mqttv3.*;

import java.util.Arrays;
import java.util.UUID;

public class MQTT_Coordinator implements MqttCallback {

    private MqttClient mqtt;
    private final long totalDarts = 10000;
    private final long dartsProRequest = 1000;
    private long requestedDarts = 0;
    private long resultTotal = 0;
    private long hitsTotal = 0;

    public MQTT_Coordinator(String mqtturl) throws MqttException {

        mqtt = new MqttClient(mqtturl, UUID.randomUUID().toString());

    }

    public void connect() throws MqttException {
        mqtt.connect();
        mqtt.setCallback(this);
        mqtt.subscribe("coordinator/#");
        System.out.println("Coordinator connected");
    }

    public void disconnect() throws MqttException {
        mqtt.disconnect();
        mqtt.close();
        System.out.println("Coordinator disconnected");
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Connection lost to MQTT_Broker!");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String[] topicparts = topic.split("/");
        if (topicparts[0].equals("coordinator")) {
            if (topicparts.length > 2) {
                // New Request for Darts to work with
                if (topicparts[1].equals("request")) {
                    System.out.println("New request for Darts");
                    String msg;
                    if (totalDarts > requestedDarts) {
                        long darts = Math.min(totalDarts - requestedDarts, dartsProRequest);
                        msg = "OK:" + darts;
                        System.out.println("OK: Worker recives: " + darts + " darts");
                    } else {
                        msg = "NOK";
                        System.out.println("NOK: No more darts available");
                    }
                    MqttMessage sendMessage = new MqttMessage(msg.getBytes());
                    mqtt.publish("worker/" + topicparts[2], message);
                    System.out.println("DEBUG: message: " + message + " sent to " + topicparts[2]);
                }
                // New message with results from a Worker for our calculation
            } else if (topicparts[1].equals("result")) {
                System.out.println("New result received from Worker ");
                // message format: topicparts[2]= total:hits
                String[] msgparts = message.toString().split(":");
                if (msgparts.length == 2) {
                    long total = Long.parseLong(msgparts[0]);
                    long hits = Long.parseLong(msgparts[1]);
                    System.out.println("Worker " + topicparts[1] + Arrays.toString(msgparts));
                    resultTotal += total;
                    hitsTotal += hits;
                }else {
                    System.err.println("Unknown message Type!");
                }
            }
        }
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("DeliveryComplete" + token);

    }


    public void work() throws InterruptedException {
        System.out.println("Cordinator listens @localhost:1883 for MQTT messages!");
        while (totalDarts > resultTotal) {
            Thread.sleep(200);
        }
        // Calculate Pi
    }

    public static void main(String[] args) throws MqttException {
        try {
            MQTT_Coordinator coordinator = new MQTT_Coordinator("tcp://localhost:1883");
            System.out.println("New Coordinator!");
            coordinator.connect();
            coordinator.work();
            coordinator.disconnect();


        } catch (InterruptedException e) {
            e.printStackTrace();

        }
    }
}