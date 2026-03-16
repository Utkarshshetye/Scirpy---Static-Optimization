
import pandas as pd
SO_columns = ["Date","Police_Force","Weather_Conditions","Number_of_Vehicles","Day_of_Week"]
data = pd.read_csv("/home/bhushan/intellijprojects/scirpy_benchmarks/examples/data/Accidents7904.csv",low_memory=False,usecols=SO_columns)
print(data.info())
print("Total rows: {0}".format(len(data)))
print(list(data))
print("Accidents")
print("-----------")
accidents_sunday = data[(data.Day_of_Week == 1)]
print("Accidents which happened on a Sunday: {0}".format(len(accidents_sunday)))
accidents_sunday_twenty_cars = data[((data.Day_of_Week == 1) & (data.Number_of_Vehicles > 20))]
print("Accidents which happened on a Sunday involving > 20 cars: {0}".format(len(accidents_sunday_twenty_cars)))
accidents_sunday_twenty_cars_rain = data[(((data.Day_of_Week == 1) & (data.Number_of_Vehicles > 20)) & (data.Weather_Conditions == 2))]
print("Accidents which happened on a Sunday involving > 20 cars in the rain: {0}".format(len(accidents_sunday_twenty_cars_rain)))
london_data = data[(data["Police_Force"] == (1 & (data.Day_of_Week == 1)))]
print("Accidents in London from 1979-2004 on a Sunday: {0}".format(len(london_data)))
london_data_2000 = london_data[((pd.to_datetime(london_data["Date"],errors="coerce") > pd.to_datetime("2000-01-01",errors="coerce")) & (pd.to_datetime(london_data["Date"],errors="coerce") < pd.to_datetime("2000-12-31",errors="coerce")))]
print("Accidents in London in the year 2000 on a Sunday: {0}".format(len(london_data_2000)))
