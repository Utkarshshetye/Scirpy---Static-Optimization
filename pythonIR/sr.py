import pandas as pd
a=[1,2,3]
b=(1,2,3)
o=1
p=6
while o <= p:
    print("o:", o)
    o = 7
o=6
df = pd.read_csv("large.csv")
print (df.Name[1:10] , df.Age[1:10],df.Age1[1:10])
if o>p:
    print("o is greater")
    if o<p:
        print("o is smaller")
    else:
        print("o is greater")
    df = pd.read_csv("large.csv")
    print (df.Naaaa[1:10] , df.Ageaaa[1:10])
else:
    print (df.Name[1:10] , df.Age[1:10],df.Age2[1:10])
if o>p:
    print("Hi")
print (df.N000[1:10] , df.Ag00[1:10])
ab=["aa","bb",2,3]
df = pd.read_csv("large.csv")
print (df.Name[1:10] , df.Age[1:10])
print("Test string")
df = pd.read_csv("large.csv")
print (df.Address[1:10] , df.Age[1:10])
print(df)
df1= pd.read_csv("large.csv")
print(df1.Address1[1:10], df.Age[1:10])
print(df1.Age1[1:10], df.Name[1:10])











#toy example
import pandas as pd
x=3
y=2
if x>y:
    df = pd.read_csv("large.csv")
    print(df.Agee)
else:
    df = pd.read_csv("large.csv")
    print(df.Ag)
df = pd.read_csv("large.csv")
print (df.Name[1:10] , df.Age[1:10],df.Ageee[1:10])
print(df.Agee)
df1 = pd.read_csv("large1.csv")
print (df1.Name1[1:10] , df1.Age1[1:10],df1.Age1[1:10])
print(df.AgeAfter)
df = pd.read_csv("large.csv")
print(df.Age,df1.Age1)
print(df.Age,"sfsa")
print(df.Age1)



import pandas as pd
x=3
y=2
if x>y:
    df = pd.read_csv("large.csv")
    print(df.iff)
    if x>y:
        df = pd.read_csv("large.csv")
        print(df.iff_iff)
        if x>y:
            df = pd.read_csv("large.csv")
            print(df.iff_iff_iff)
        else:
            print(df.iff_iff_else)
        print(df.iff_iff)
    print(df.iff)
else:
    df = pd.read_csv("large.csv")
    print(df.elss)
    if x>y:
        df = pd.read_csv("large.csv")
        print(df.elssif)

