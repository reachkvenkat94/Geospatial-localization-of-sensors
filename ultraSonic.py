from sensorData import SensorData

class UltraSonic(SensorData):
   __instance = None
   @staticmethod 
   def getInstance():
      """ Static access method. """
      if UltraSonic.__instance == None:
         UltraSonic()
      return UltraSonic.__instance
   def __init__(self,name,value):
      if UltraSonic.__instance != None:
         raise Exception("This class is a UltraSonic!")
      else:
         UltraSonic.__instance = self
		 self.name = "temperature"
		 self.value = value