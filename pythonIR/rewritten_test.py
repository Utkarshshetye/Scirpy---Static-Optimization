import pandas as pd
x,y,z=1,2,3
columns = ["Address","Phone"]
c_d_t = {"Name":"str","Age":"category","Address":"str","Phone":"int64"," Email":"str"," URL":"category"}
df = pd.read_csv("data.csv",usecols=columns,dtype=c_d_t)
if x>y:
    columns = ["Name","Age"]
    c_d_t = {"Name":"str","Age":"int64","Designation":"category"," Address":"str"," Pincode":"int64"}
    df = pd.read_csv("large.csv",usecols=columns,dtype=c_d_t)
    print (df.Name[1:10],df.Age[1:10])
    print (df.Address,df.Phone)
