# Comprehensive Test Cases for Predicate Pushdown Analysis

## Test Case 1: Simple Filter (No if/else)
**Purpose:** Basic filter pushdown without any control flow

```python
import pandas as pd
df = pd.read_csv('data.csv')
df1 = df[df['age'] > 30]
print(df1)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[(age, >, 30)])
```

## Test Case 2: Multiple Simple Filters with AND
**Purpose:** Compound filter with AND operator

```python
import pandas as pd
df = pd.read_csv('data.csv')
df1 = df[(df['age'] > 30) & (df['salary'] > 50000)]
print(df1)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30), (salary, >, 50000)]])
```

## Test Case 3: Filters with OR Operator
**Purpose:** OR operator creates multiple filter groups

```python
import pandas as pd
df = pd.read_csv('data.csv')
df1 = df[(df['age'] > 30) | (df['salary'] > 50000)]
print(df1)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30)], [(salary, >, 50000)]])
```

## Test Case 4: Mixed AND/OR Operators
**Purpose:** Complex boolean expression

```python
import pandas as pd
df = pd.read_csv('data.csv')
df1 = df[((df['age'] > 30) & (df['status'] == 'active')) | (df['salary'] > 50000)]
print(df1)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30), (status, ==, 'active')], [(salary, >, 50000)]])
```

## Test Case 5: Single if/else (Different Filters)
**Purpose:** Basic if/else with different filters in each branch

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2

if x > 0:
    df1 = df[df['age'] > 30]
    print(df1)
else:
    df2 = df[df['salary'] > 50000]
    print(df2)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30)], [(salary, >, 50000)]])
```

## Test Case 6: Single if/else (Same Filter Both Branches)
**Purpose:** Common predicates should not create OR groups

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2

if x > 0:
    df1 = df[df['age'] > 30]
    print(df1)
else:
    df2 = df[df['age'] > 30]
    print(df2)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[(age, >, 30)])
```

## Test Case 7: if/else with Compound Filters
**Purpose:** Each branch has multiple filters

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2

if x > 0:
    df1 = df[(df['age'] > 30) & (df['status'] == 'active')]
    print(df1)
else:
    df2 = df[(df['age'] > 30) & (df['dept'] == 'HR')]
    print(df2)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30), (status, ==, 'active')], [(age, >, 30), (dept, ==, 'HR')]])
```

## Test Case 8: Multiple Sequential if/else (Cartesian Product)
**Purpose:** Two separate if/else statements - should create 4 groups

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2

if x > 0:
    df1 = df[(df['age'] > 30) & (df['status'] == 'active')]
    print(df1)
else:
    df2 = df[(df['age'] > 30) & (df['dept'] == 'HR')]
    print(df2)

if x > 5:
    df3 = df[df['salary'] > 40000]
    print(df3)
else:
    df4 = df[df['age'] > 30]
    print(df4)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[
    [(age, >, 30), (status, ==, 'active'), (salary, >, 40000)],
    [(age, >, 30), (status, ==, 'active')],
    [(age, >, 30), (dept, ==, 'HR'), (salary, >, 40000)],
    [(age, >, 30), (dept, ==, 'HR')]
])
```

## Test Case 9: Simple Filter + if/else
**Purpose:** Simple predicates should appear in ALL OR groups

```python
import pandas as pd
df = pd.read_csv('data.csv')
df_filtered = df[df['active'] == True]
x = 2

if x > 0:
    df1 = df[df['age'] > 30]
    print(df1)
else:
    df2 = df[df['salary'] > 50000]
    print(df2)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(active, ==, True), (age, >, 30)], [(active, ==, True), (salary, >, 50000)]])
```

## Test Case 10: Nested if/else (Problem: rewriting issue)
**Purpose:** True nested control flow

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2
y = 3

if x > 0:
    if y > 0:
        df1 = df[df['age'] > 30]
        print(df1)
    else:
        df2 = df[df['salary'] > 50000]
        print(df2)
else:
    df3 = df[df['dept'] == 'HR']
    print(df3)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30)], [(salary, >, 50000)], [(dept, ==, 'HR')]])
```

**Actual Output:**
```python
import pandas as pd
df = pd.read_csv('data.csv',filters=[[(dept, ==, 'HR'), (age, >, 30)], [(dept, ==, 'HR'), (salary, >, 50000)]])
x = 2
y = 3
if (x > 0): // If else are changing
   df3 = df[(df['dept'] == 'HR')]
   print(df3)
else:
   if (y > 0):
      df1 = df[(df['age'] > 30)]
      print(df1)
   else:
      df2 = df[(df['salary'] > 50000)]
      print(df2)
```

## Test Case 11: Different Filter After if/else
**Purpose:** Filter applied after branches should merge into all groups

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2

if x > 0:
    df1 = df[df['age'] > 30]
else:
    df2 = df[df['salary'] > 50000]
if x > 0:
    df_final = df1[df1['status'] == 'active']
else:
    df_final = df2[df2['status'] == 'active']
print(df_final)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[[(age, >, 30)], [(salary, >, 50000)]])
```

## Test Case 12: Multiple DataFrames
**Purpose:** Ensure filters only apply to correct dataframe

```python
import pandas as pd
df1 = pd.read_csv('data1.csv')
df2 = pd.read_csv('data2.csv')

df1_filtered = df1[df1['age'] > 30]
df2_filtered = df2[df2['salary'] > 50000]

print(df1_filtered)
print(df2_filtered)
```

**Expected Output:**
```python
df1 = pd.read_csv('data1.csv', filters=[(age, >, 30)])
df2 = pd.read_csv('data2.csv', filters=[(salary, >, 50000)])
```

## Test Case 13: DataFrame Alias
**Purpose:** Aliases should track back to original source

```python
import pandas as pd
df = pd.read_csv('data.csv')
df_alias = df
df_filtered = df_alias[df_alias['age'] > 30]
print(df_filtered)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[(age, >, 30)])
```

## Test Case 14: Non-Row-Preserving Operation
**Purpose:** Operations like groupby should kill predicates

```python
import pandas as pd
df = pd.read_csv('data.csv')
df_grouped = df.groupby('dept').sum()
df_filtered = df[df['age'] > 30]
print(df_grouped)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=None)
```

## Test Case 15: Column Modification
**Purpose:** Modifying a column should kill predicates on that column

```python
import pandas as pd
df = pd.read_csv('data.csv')
df['age'] = df['age'] + 1
df_filtered = df[df['age'] > 30]
print(df_filtered)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=None)
```

## Test Case 16: isin Operator
**Purpose:** isin should be handled correctly

```python
import pandas as pd
df = pd.read_csv('data.csv')
df_filtered = df[df['dept'].isin(['HR', 'Sales'])]
print(df_filtered)
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[(dept, isin, ['HR', 'Sales'])])
```

## Test Case 17: Sequential if/else (Larger Cartesian Product)  (Not working, scenario is simple predicate in if else before last if else)
**Purpose:** 2 × 2 = 4 groups

```python
import pandas as pd
df = pd.read_csv('data.csv')
x = 2

if x > 0:
    df1 = df[df['age'] > 30]
else:
    df2 = df[df['salary'] > 50000]

if x > 5:
    df3 = df[df['dept'] == 'HR']
else:
    df4 = df[df['status'] == 'active']
```

**Expected Output:**
```python
df = pd.read_csv('data.csv', filters=[
    [(age, >, 30), (dept, ==, 'HR')],
    [(age, >, 30), (status, ==, 'active')],
    [(salary, >, 50000), (dept, ==, 'HR')],
    [(salary, >, 50000), (status, ==, 'active')]
])
```
**Actual Output coming:**
```python
df = pd.read_csv('data.csv',filters=[
     [(dept, ==, 'HR'), (salary, >, 50000), (age, >, 30)],
     [(status, ==, 'active'), (salary, >, 50000), (age, >, 30)]
])
```
## Pending: Unary operators not working