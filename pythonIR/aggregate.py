import pandas as pd
SO_columns = ["clan_location"]
df = pd.read_csv('./input/coc_clans_dataset.scaled.csv',index_col=0)
df.head()
df.info()
_ = df.isnull()
missing_count = df['clan_location'].isnull().sum()
non_missing_count = (df['clan_location'] > missing_count)