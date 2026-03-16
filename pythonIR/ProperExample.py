import pandas as pd
df = pd.read_csv("large.csv")
if 'Name' in df:
    print(df.Name)
else:
    if 'Address' in df:
        print(df.Address)
    else:
        print("No name or address column")

