import pandas as pd
# Name,Age,Address,Phone, Email, URL

df = pd.read_csv("data.csv")
print(df)
df1 = df[df['Name'] ==  'Ahu' ]
temp = df1['Name'].count()
print(temp)

