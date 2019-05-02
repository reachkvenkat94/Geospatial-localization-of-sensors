
import os
from gps import *
from time import *
import time
import threading
from sense_hat import SenseHat
import json
from mqttClientConnector import mqttClientConnector
import uuid
import board
import busio
import adafruit_si7021


//Initializing I2C bus
i2c = busio.I2C(board.SCL, board.SDA)
sensor = adafruit_si7021.SI7021(i2c)

gpsd = None #seting the global variable
uid = str(uuid.uuid4())
 

class SensorData(threading.Thread):
	def __init__():
		global sensor
		global uid
		self.temperature = sensor.temperature
		self.longitude = 0
		self.latitude = 0
		self.id = uid
		threading.Thread.__init__(self)
		global gpsd #bring it in scope
		gpsd = gps(mode=WATCH_ENABLE) #starting the stream of info
		self.current_value = None
		self.running = True #setting the thread running to true
 
	def run(self):
		global gpsd
		while gpsp.running:
			gpsd.next() #this will continue to loop and grab EACH set of gpsd info to clear the buffer
	
	def toJsonfromSensor(self):
		dict = {}
		dict['id'] = self.id
		dict['temperature'] = self.temperature
		dict['latitude'] = self.latitude
		dict['longitude'] = self.longitude
		jsonf = json.dumps(dict)
		return jsonf
		
			
	if __name__ == '__main__':
		gpsp = SensorData() # create the thread 
		mqtt_client = MqttClientConnector('geotemperature')
		host = 'test.mosquitto.org'
		topic = 'geotemperature'
		global sensor
		try:
			gpsp.start() # start it up
			while True:
			#It may take a second or two to get good data
			#print gpsd.fix.latitude,', ',gpsd.fix.longitude,'  Time: ',gpsd.utc
			os.system('clear')
			sensor = adafruit_si7021.SI7021(i2c)
			gpsp.latitude = gpsd.fix.latitude
			gpsp.longitude = gpsd.fix.longitude
			gpsp.temperature = sensor.temperature
			json_data = gpsp.toJsonfromSensor()
			mqtt_client.publish(topic,json_data,host)
						
			print
			print ' GPS reading'
			print '----------------------------------------'
			print 'latitudeitude    ' , gpsd.fix.latitudeitude
			print 'longitudeitude   ' , gpsd.fix.longitudeitude
			print 'time utc    ' , gpsd.utc,' + ', gpsd.fix.time
			print 'altitude (m)' , gpsd.fix.altitude

			time.sleep(10) #set to whatever

		except (KeyboardInterrupt, SystemExit): #when you press ctrl+c
		print "\nKilling Thread..."
		gpsp.running = False
		gpsp.join() # wait for the thread to finish what it's doing
		print "Done.\nExiting."
			
			
