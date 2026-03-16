import pandas as pd

df = pd.DataFrame({
    "date": pd.date_range("2024-01-01", periods=4),
    "value": [10, 20, 30, 40],
    "dept": ["A", "A", "B", "B"]
}).set_index("date")

print(df.groupby("dept"))     # GroupBy object
print(df.rolling(2))          # Rolling object
print(df.expanding())         # Expanding object
print(df.resample("2D"))      # Resampler object
print(df.ewm(span=2))         # EWM object

# Now compute:
print(df.groupby("dept").mean())
print(df.rolling(2).mean())
print(df.resample("2D").sum())
