package com.example.uasiot_satria_billy;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String MQTT_SERVER = "192.168.41.54";
    private static final String DHT_TOPIC = "dht";
    private static final String POT_TOPIC = "potensio";

    private TextView tempTextView;
    private TextView rpmTextView;
    private TextView itemTextView;
    private TextView lightTextView;

    private MqttClient mqttClient;
    private Handler handler;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tempTextView = findViewById(R.id.textView);
        rpmTextView = findViewById(R.id.textView2);
        itemTextView = findViewById(R.id.textView3);
        lightTextView = findViewById(R.id.textView4);

        connectToMqttBroker();

        handler = new Handler();
        random = new Random();
        generateRandomData();
    }

    private void connectToMqttBroker() {
        String clientId = MqttClient.generateClientId();
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            mqttClient = new MqttClient("tcp://" + MQTT_SERVER, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String data = new String(message.getPayload());
                    if (topic.equals(DHT_TOPIC)) {
                        updateTempTextView(data);
                    } else if (topic.equals(POT_TOPIC)) {
                        updateRpmTextView(data);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used in this example
                }
            });
            mqttClient.subscribe(DHT_TOPIC);
            mqttClient.subscribe(POT_TOPIC);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect to MQTT broker", e);
        }
    }

    private void updateTempTextView(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tempTextView.setText(data);
            }
        });
    }

    private void updateRpmTextView(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rpmTextView.setText(data);
            }
        });
    }

    private void generateRandomData() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int randomItem = random.nextInt(10) + 1;
                int randomLightStatus = random.nextInt(2);
                updateItemTextView(randomItem);
                updateLightTextView(randomLightStatus);
                generateRandomData();
            }
        }, 3000); // Change the delay here to set the interval between updates (in milliseconds)
    }

    private void updateItemTextView(final int data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemTextView.setText(String.valueOf(data));
            }
        });
    }

    private void updateLightTextView(final int data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status = (data == 1) ? "ON" : "OFF";
                lightTextView.setText(status);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mqttClient.disconnect();
            mqttClient.close();
            handler.removeCallbacksAndMessages(null);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to disconnect from MQTT broker", e);
        }
    }
}
