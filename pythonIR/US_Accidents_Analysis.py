import pandas as pd
df = pd.read_csv("/home/mudra/Media/Downloads/US_accident_data/US_Accidents_June20.csv")
print(df.Start_Lat,df.Start_Lng)
print(df.Source,df.TMC,df.Severity)
print(df.Start_Time, df.End_Time)
print(df.Number,df.Street,df.Side)
print(df.Weather_Timestamp,df.Zipcode)

