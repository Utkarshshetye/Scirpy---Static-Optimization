import pandas as pd
columns = ["Weather_Timestamp","Zipcode","Number","Street","Side","Start_Time","End_Time","Source","TMC","Severity","Start_Lat","Start_Lng"]
c_d_t = {"ID":"str","Source":"category","TMC":"category","Severity":"category","Start_Time":"str","End_Time":"str","Start_Lat":"float32","Start_Lng":"float32","End_Lat":"category","End_Lng":"category","Distance(mi)":"category","Description":"str","Number":"category","Street":"str","Side":"category","City":"str","County":"str","State":"category","Zipcode":"str","Country":"category","Timezone":"category","Airport_Code":"str","Weather_Timestamp":"str","Temperature(F)":"float32","Wind_Chill(F)":"str","Humidity(%)":"float32","Pressure(in)":"float32","Visibility(mi)":"float32","Wind_Direction":"category","Wind_Speed(mph)":"str","Precipitation(in)":"str","Weather_Condition":"str","Amenity":"category","Bump":"category","Crossing":"category","Give_Way":"category","Junction":"category","No_Exit":"category","Railway":"category","Roundabout":"category","Station":"category","Stop":"category","Traffic_Calming":"category","Traffic_Signal":"category","Turning_Loop":"category","Sunrise_Sunset":"category","Civil_Twilight":"category","Nautical_Twilight":"category","Astronomical_Twilight":"category"}
columns = ["Start_Lng","Start_Lat","Severity","TMC","Source","End_Time","Start_Time","Side","Street"]
df = pd.read_csv("/home/mudra/Media/Downloads/US_accident_data/US_Accidents_June20.csv",usecols=columns,dtype=c_d_t)
print(df.Start_Lat,df.Start_Lng)
df.drop(["Start_Lat","Start_Lng"],axis=1,inplace=True)
print(df.Source,df.TMC,df.Severity)
df.drop(["Source","TMC","Severity"],axis=1,inplace=True)
print(df.Start_Time,df.End_Time)
df.drop(["Start_Time","End_Time"],axis=1,inplace=True)
columns = ["Number","Zipcode","Weather_Timestamp"]
_d_f_0 = pd.read_csv("/home/mudra/Media/Downloads/US_accident_data/US_Accidents_June20.csv",usecols=columns,dtype=c_d_t)
pd.concat([df,_d_f_0],axis=1)
print(df.Number,df.Street,df.Side)
df.drop(["Number","Street","Side"],axis=1,inplace=True)
print(df.Weather_Timestamp,df.Zipcode)
df.drop(["Weather_Timestamp","Zipcode"],axis=1,inplace=True)
