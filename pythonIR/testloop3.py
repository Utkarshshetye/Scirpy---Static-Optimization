import pandas as pd
df = pd.read_csv('./input/survey.scaled.csv')
df.info()
to_drop = ['Timestamp']
df.Gender.unique()
df.Gender.value_counts()
df.Gender = df.Gender.str.lower()
df.Gender = df.Gender = df.Gender.replace('m', 'male')
df.Gender = df.Gender.replace('f', 'female')
df['HasMale'] = df.Gender.str.contains('male|man|guy|maile|malr|androgyne|male|mal|make|msle')
df['HasFemale'] = df.Gender.str.contains('female|woman|femail|androgyne|femake')
df['HasNB'] = df.Gender.str.contains('non-binary|enby|queer|all|fluid|agender|neuter|p')
# That's gender cleaned up.
to_drop.append('Gender')
# Moving on.
df.describe(include=['O'])
df.Country.unique()

for country in sorted(list(df.Country.unique())):
    df['Country_'+str(country)] = (df.Country == country).astype(int)
to_drop.append('Country')

df.groupby('Country')['state'].apply(lambda x: x.isnull().mean())

for st in list(df.state.unique()):
    df['state_'+str(st)] = (df.state == st).astype(int)
to_drop.append('state')

# all the columns which are binary in nature, let's make them 01 based.
df.self_employed.fillna(df.self_employed.mode()[0], inplace=True)
for col in df.select_dtypes(include=['object']):
    u_count = len(df[col].unique())
    if u_count < 2:
        to_drop.append(col)
        print('adding ', col, 'to drop list as no variation')
    elif u_count == 2:
        first = list(df[col].unique())[-1]
        df[col] = (df[col] == first).astype(int)
        print('converted', col)

df.drop(to_drop, axis=1).info()

df.work_interfere.unique()

# There is another pattern here. We take advantage of that:
option_map = {'Yes': 1, 'No': -1, "Don't know": 0,
              'Not sure': 0, 'Maybe': 0, 'Some of them': 0}
ynns = {'Yes': 1, 'No': -1, 'Not sure': 0}
for col in df.select_dtypes(include=['object']):
    uniques = set(df[col].unique())
#     if (uniques == {'Yes', 'No', "Don't know"} or
#         uniques == {'Yes', 'No', 'Not sure'} or
#         uniques == {'Yes', 'No', 'Maybe'} or
#         uniques == {'Yes', 'No', 'Some of them'}):
#         print('encoding', col, 'To -1, 0, 1')
#         df[col] = df[col].map(option_map)

df.describe(include=['O'])

df.leave.unique()

to_drop.append('comments')
df.info()

print(to_drop)
data = df.drop(to_drop, axis=1)
data.info()
