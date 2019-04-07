class SensorData():
   __instance = None
   def getInstance():
      """ Static access method. """
      if SensorData.__instance == None:
         SensorData()
      return SensorData.__instance
   def __init__(self,name,value):
      if SensorData.__instance != None:
         raise Exception("This class is a SensorData!")
      else:
         SensorData.__instance = self
		 self.name = name
		 self.value = value