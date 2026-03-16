
import pandas as pd
SO_columns = ["Country"]
SO_c_d_t = {"Country":"str"}
df = pd.read_csv("/home/bhushan/Downloads/5m-Sales-Records/5mSalesRecords.csv",usecols=SO_columns,dtype=SO_c_d_t)
print(df.info())
list_of_countries = df.groupby(["Country"]).size().reset_index(name="Count")
print(list_of_countries.sort_values(by="Count",ascending=False).head(10))
print(("Total rows: " + str(sum(list_of_countries["Count"]))))
