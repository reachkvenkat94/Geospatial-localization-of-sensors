import os
from gps import *
from time import *
import time
import threading
from sense_hat import SenseHat

gpsd = None #seting the global variable
 

class SensorData(threading.Thread):
	def __init__():
		self.temp = 0
		self.long 0
		self.lat = 0
		threading.Thread.__init__(self)
		global gpsd #bring it in scope
		gpsd = gps(mode=WATCH_ENABLE) #starting the stream of info
		self.current_value = None
		self.running = True #setting the thread running to true
 
	def run(self):
		global gpsd
		while gpsp.running:
			gpsd.next() #this will continue to loop and grab EACH set of gpsd info to clear the buffer
			
	if __name__ == '__main__':
		gpsp = SensorData() # create the thread
		sense = SenseHat() 
		try:
			gpsp.start() # start it up
			while True:
			#It may take a second or two to get good data
			#print gpsd.fix.latitude,', ',gpsd.fix.longitude,'  Time: ',gpsd.utc
			os.system('clear')
			self.lat = gpsd.fix.latitude
			self.long = gpsd.fix.longitude
 
			print
			print ' GPS reading'
			print '----------------------------------------'
			print 'latitude    ' , gpsd.fix.latitude
			print 'longitude   ' , gpsd.fix.longitude
			print 'time utc    ' , gpsd.utc,' + ', gpsd.fix.time
			print 'altitude (m)' , gpsd.fix.altitude

			time.sleep(5) #set to whatever

		except (KeyboardInterrupt, SystemExit): #when you press ctrl+c
		print "\nKilling Thread..."
		gpsp.running = False
		gpsp.join() # wait for the thread to finish what it's doing
		print "Done.\nExiting."
			
			
