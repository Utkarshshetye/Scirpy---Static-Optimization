import pandas as pd
import numpy as np

df = pd.DataFrame({'c1': [10, 11, 12], 'c2': [100, 110, 120]})
print(df)
for index, row in df.iterrows():
    print(row)
