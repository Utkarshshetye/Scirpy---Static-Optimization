import pandas as pd
df = pd.DataFrame([[1, 2], [4, 5], [7, 8]],
                 index=['cobra', 'viper', 'sidewinder'],
                 columns=['max_speed', 'shield'])
print(df)
#df.loc[pd_series < lower_bound , "cutoff_rate" ] = lower_bound
ser=df['max_speed']
df.loc[ser < 8 , "max_speed" ] = 16
print(df)