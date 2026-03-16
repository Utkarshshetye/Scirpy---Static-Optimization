import pandas as pd
a=[1,2,3]
b=(1,2,3)


def norm(df, df2, df3):
    result = df.copy()
    for feature in df.columns:
        max_v = df[feature].max()
        min_v = df[feature].min()
        if max_v!=min_v:
            result[feature] = (df[feature] - min_v) / (max_v - min_v)
    return result
o=1
p=6
while o <= p:
    print("o:", o)
    o = 7

def finalFeature(var):
    result = var
    return result

cols=[a,b,c]
df = pd.read_csv('large.csv',usecol=cols)
print (df.Name[1:10] , df.Age[1:10],df.Age1[1:10])


#if o>p>0 or o!=0 and o!=1 or 0==5:
if o>p:
    print("o is greater")
    if o<p:
        print("o is smaller")
    else:
        print("o is greater")
    df = pd.read_csv('large.csv')
    print (df.Naaaa[1:10] , df.Ageaaa[1:10])

else:
    print (df.Name[1:10] , df.Age[1:10],df.Age2[1:10])


if o>p:
    print("Hi")
print (df.N000[1:10] , df.Ag00[1:10])

ab=['aa','bb',2,3]
df = pd.read_csv('large.csv')
print (df.Name[1:10] , df.Age[1:10])
print("Test string")
df = pd.read_csv('large.csv')
print (df.Address[1:10] , df.Age[1:10])
print(df)
df1= pd.read_csv('large.csv')
print(df1.Address1[1:10], df.Age[1:10])
print(df1.Age1[1:10], df.Name[1:10])
#print(df1.Ag, df.xy, df.za)









