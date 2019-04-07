'''
Created on Feb 25, 2019
This Class is used to connect to a MQTT broker for publishing and subscription

@author: Venkat Prasad Krishnamurthy
'''

import paho.mqtt.client as mqtt    #import client library
import paho.mqtt.publish as publish
from asyncio.tasks import sleep
import time

class MqttClientConnector():
    host = "test.mosquitto.org"
    json_data = "Hello"
    
    '''
    Callback function: This function will execute once the client connects
    with MQTT Broker
    '''
    def on_connect(self,client,userdata,flags,rc):
        print("Connected with Client: "+str(rc))
        client.subscribe(self.topic,2)
    
    '''
    Callback function: This function will execute once the client receives message
    from MQTT Broker
    ''' 
    def on_message(self,client,userdata,msg):
        global json_data
        json_data = str(msg.payload.decode("utf-8"))
        client.loop_stop()
    
    '''
    Constructor
    @param topic:topic of the message published or subscribed 
    ''' 
    def __init__(self,topic=None):
        self.topic = topic;
        
    '''
    Function is used to publish the message
    @param topic: Topic of the message
    @param message: Message to be sent
    @param host: address of MQTT broker   
    '''
    def publish(self,topic,message,host):
        client = mqtt.Client();
        client.connect(host,1883)
        client.publish(topic,message)
        
    '''
    Function is used to subscribe to a topic
    @param host: Address of MQTT broker 
    '''
    def subscribe(self,host):
        client = mqtt.Client()
        client.on_connect = self.on_connect
        client.on_message = self.on_message
        client.connect(host,1883,60)
        client.loop_start()
        time.sleep(10)
    
    '''
    Function is used to store the data received from MQTT Broker
    @return: Data received from MQTT Broker
    '''
    def message(self):
        global json_data
        return json_data
        
        
        