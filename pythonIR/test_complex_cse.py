import pandas as pd
import numpy as np
import time

# Mocking a large dataset
train = pd.DataFrame({
    'card1': np.random.randint(0, 100, 1000),
    'TransactionAmt': np.random.rand(1000) * 100,
    'id_02': np.random.rand(1000) * 1000,
    'D15': np.random.rand(1000) * 500
})

# Example 1: Complex Financial Formula (Black-Scholes-like redundancy)
# Imagine calculating a complex metric multiple times with slight variations
# (a * b) / (c + d) is repeated
a = train['TransactionAmt']
b = train['id_02']
c = train['D15']
d = train['card1']

train['metric_1'] = (a * b) / (c + d) + (a * b).mean()
train['metric_2'] = (a * b) / (c + d) - (a * b).std()
train['metric_3'] = ((a * b) / (c + d)) * 1.5

# Example 2: Pre-processing Chains
# String operations or complex casting repeated
train['id_str'] = train['id_02'].astype(str).str.zfill(10)
train['id_prefix'] = train['id_02'].astype(str).str.zfill(10).str[:3]
train['id_suffix'] = train['id_02'].astype(str).str.zfill(10).str[-3:]

# Example 3: Nested Analytics with overlapping windows (Rolling/Expanding)
# Note: groupby is skipped, but the resulting series operations might be redundant
# cse_temp should catch the heavy lifting if the chain is identical
train['roll_mean_7'] = train['TransactionAmt'].rolling(window=7).mean()
train['roll_mean_7_norm'] = train['TransactionAmt'] / train['TransactionAmt'].rolling(window=7).mean()
train['roll_mean_7_diff'] = train['TransactionAmt'] - train['TransactionAmt'].rolling(window=7).mean()

# Example 4: Compound Boolean Logic (Expensive filters)
# (condition1 & condition2) is repeated
heavy_filter = (train['TransactionAmt'] > 50) & (train['id_02'] < 500)
train['flag_heavy'] = heavy_filter.map({True: 1, False: 0})
train['heavy_amt'] = train.loc[heavy_filter, 'TransactionAmt']
train['heavy_d15'] = train.loc[heavy_filter, 'D15']

print(train.head())
