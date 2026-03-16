#https://medium.com/@lsriniv/reading-large-csv-files-using-pandas-7659baed6c27
import pandas as pd
df=pd.read_csv('/home/bhushan/Downloads/5m-Sales-Records/5mSalesRecords.csv')
print(df.info())
list_of_countries = df.groupby(["Country"]).size().reset_index(name="Count")
print(list_of_countries.sort_values(by='Count', ascending = False).head(10))
print("Total rows: " + str(sum(list_of_countries['Count'])))