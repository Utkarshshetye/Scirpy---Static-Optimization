import pandas as pd
import numpy as np
import matplotlib
import sys
import matplotlib.pyplot as plt
np.warnings.filterwarnings('ignore')

def norm(df):
    result = df.copy()
    for feature in df.columns:
        max_v = df[feature].max()
        min_v = df[feature].min()
        if max_v!=min_v:
            result[feature] = (df[feature] - min_v) / (max_v - min_v)
    return result

def squ(df):
    result = df.copy()
    for feature in df.columns:
        result[feature]=df[feature]**2
    return result

def cube(df):
    result = df.copy()
    for feature in df.columns:
        result[feature]=df[feature]**3
    return result


def squroot(df):
    result = df.copy()
    for feature in df.columns:
        result[feature] = df[feature] ** 1/2
    return result

def log(df):
    result = df.copy()
    for feature in df.columns:
        result[feature] = np.log(df[feature])
    return result

def finalFeature(df):
    result = df.filter(['F6', 'base_time', 'pd_bd'], axis=1)
    #result=df[['F6','base_time','pd_bd']].copy

    return result

def sigmoid(df):

    result = df.copy()
    for feature in df.columns:
        feat=np.exp(-df[feature])+1
        result[feature] = 1/feat
    #print (result)
    return result

def modsigmoid(df):

    result = df.copy()
    for feature in df.columns:
        feat=np.exp(-df[feature])+1
        result[feature] = (1/feat)**2
    #print (result)
    return result

def modsigmoidsqrt(df):

    result = df.copy()
    for feature in df.columns:
        feat=np.exp(-df[feature])+1
        result[feature] = (1/feat)**1/2
    #print (result)
    return result

def daytodate (row):
   pd=0
   bd=0
   if row['post_day'] == 'monday' :
       pd=1
   if row['post_day'] == 'tuesday':
       pd = 2
   if row['post_day'] == 'wednesday':
       pd = 3
   if row['post_day'] == 'thursday':
       pd=4
   if row['post_day'] == 'friday':
       pd = 5
   if row['post_day'] == 'saturday':
       pd = 6
   if row['post_day'] == 'sunday':
       pd=7
   if row['basetime_day'] == 'monday':
       bd = 1
   if row['basetime_day'] == 'tuesday':
       bd = 2
   if row['basetime_day'] == 'wednesday':
       bd = 3
   if row['basetime_day'] == 'thursday':
       bd = 4
   if row['basetime_day'] == 'friday':
       bd = 5
   if row['basetime_day'] == 'saturday':
       bd = 6
   if row['basetime_day'] == 'sunday':
       bd = 7
   if bd>pd:
       basevspost=bd-pd
   else:
       basevspost=(bd-pd+7)%7

   return basevspost

def get_features(file_path):
    df = pd.read_csv(file_path,
                     usecols=["page_likes", "page_checkin", "daily_crowd", "page_category", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
                              "F8", "c1", "c2", "c3", "c4", "c5", "base_time", "post_length", "share_count",
                              "promotion", "h_target", "post_day", "basetime_day", ])
    df['pd_bd'] = df.apply(lambda row: daytodate(row), axis=1)
    del df['basetime_day']
    del df['post_day']
    r,c=df.shape
    df['bias'] = np.ones(r)
    return df

def compute_error(df, y, w):
    hypo = np.dot(df, w)
    #print("hypo",hypo)
    error=y-hypo
    erT=error.transpose()
    m,n= rows,columns= df.shape
    cost = np.dot(erT, error)/m
    rms=np.sqrt(cost)
    return rms,error

def compute_grad(error, df, m):
    dfT=df.transpose()
    grad=np.dot(dfT,error)
    return grad/m

def task1(df,df1, y,w,la,ita,task):
    rows, columns = df.shape

    rms, error = compute_error(df, y, w)
    rmso = rms + 1

    while rmso - rms >= 0.0005:
        rmso = rms
        grad = compute_grad(error, df, rows)
        ww = ita * (grad) + w * (1 - (ita * la))
        #ww = ita * (grad) + w - dnormw(w, 2) * ((ita * la) / rows)
        w = ww
        rms, error = compute_error(df, y, w)
        """print("gradient", grad.sum())
        print("rms:", rms)
        print("rmso:", rmso)"""
    hyp_test = np.dot(df1, w)
    hyp_test = hyp_test.clip(min=0)
    if task=='unreg':
        np.savetxt("Question1_unreg.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d", header="Id,Target",comments='')
    if task == 'reg':
        np.savetxt("Question1_reg.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d",
                       header="Id,Target",comments='')
    if task=='sq':
        np.savetxt("Question4sq.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d", header="Id,Target",comments='')
    if task=='sqrt':
        np.savetxt("Question4sqrt.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d", header="Id,Target",comments='')
    if task == 'cube':
        np.savetxt("Question4cube.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d",header="Id,Target",comments='')
    if task == 'si':
        np.savetxt("Question4sigmoid.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d",header="Id,Target",comments='')
    if task == 'modsi':
        np.savetxt("Question4modsigmoid.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d",header="Id,Target",comments='')
    if task == 'final':
        np.savetxt("Question5final.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d",header="Id,Target",comments='')

    print("rms:",task, rms)
    return w
def task2(df,y):
    cov=[]
    fea=[]
    co=np.asarray(cov)

    for feature in df.columns:
        Covariance = np.cov(df[feature], y, bias=True)[0][1]
        cov.append(abs(Covariance))
        fea.append(feature)
    co = np.asarray(cov)
    ind = np.argpartition(co, -3)[-3:]
    for i in ind:
        print(fea[int(i)])

def dnormw(w,n):
    ww=np.power(w, n)
    wwd=np.power(w, n-1)
    sum=np.sum(ww)
    div=sum**(1/n-1)
    div=div*1/n
    wwdf=np.multiply(div,wwd)
    #print(wwdf)
    return wwdf


def task3a(df,df1, y,w,la,ita):
    rows, columns = df.shape
    rms, error = compute_error(df, y, w)
    rmso = rms + 1
    while rmso - rms >= 0.0005:
        rmso = rms
        grad = compute_grad(error, df, rows)
        ww = ita * (grad) + w  - dnormw(w,4)*((ita * la))
        w = ww
        rms, error = compute_error(df, y, w)
        """print("gradient4", grad.sum())
        print("rms4:", rms)
        print("rmso4:", rmso)
        print("hypo;",w)"""
    hyp_test = np.dot(df1, w)
    hyp_test = hyp_test.clip(min=0)
    np.savetxt("Question3_L4.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d", header="Id,Target",comments='')
    print("rms4:", rms)
    return w

def task3b(df,df1, y,w,la,ita,question):
    rows, columns = df.shape
    rms, error = compute_error(df, y, w)
    rmso = rms + 1
    while rmso - rms >= 0.0005:
        rmso = rms
        grad = compute_grad(error, df, rows)
        ww = ita * (grad) + w  - dnormw(w,6)*((ita * la) / rows)
        w = ww
        rms, error = compute_error(df, y, w)
        """print("gradient6", grad.sum())
        print("rms6:", rms)
        print("rmso6:", rmso)"""
    hyp_test = np.dot(df1, w)
    hyp_test = hyp_test.clip(min=0)
    np.savetxt("Question3_L6.csv", np.dstack((np.arange(0, hyp_test.size), hyp_test))[0], "%d,%d", header="Id,Target",comments='')
    print("rms6:", rms)
    return w

def task4(df,df1, y,w,la,ita,a):
    dfsq=squ(df)
    df1sq=squ(df1)
    w=task1(dfsq,df1sq,y,w,la,ita,"sq")
    #np.savetxt("wsq.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    print("squarecomple")


    w=a
    dfsqrt = squroot(df)
    df1sqrt = squroot(df1)
    w=task1(dfsqrt, df1sqrt, y, w, la, ita,"sqrt")
    #np.savetxt("wsqrt.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    print("squarerootcomple")

    w = a
    dfcu = cube(df)
    df1cu = cube(df1)
    w = task1(dfcu, df1cu, y, w, la, ita, "cube")
    #np.savetxt("wcube.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    print("cubecompleted")


def task4sig(dfR,df1R,y,w,la,ita,a):
    # sigmoid test
    dfsi = sigmoid(dfR)
    df1si = sigmoid(df1R)
    w = task1(dfsi, df1si, y, w, la, ita, "si")
    #np.savetxt("wsig.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    print("sicompleted")
    # sigmoid test end

def task4modsig(dfR,df1R,y,w,la,ita,a):
    # sigmoid test
    dfsi = modsigmoid(dfR)
    df1si = modsigmoid(df1R)
    w = task1(dfsi, df1si, y, w, la, ita, "modsi")
    #np.savetxt("wmodsig.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    print("modsicompleted")

def task5(df,df1,y,w,la,ita,a):
    # sigmoid test
    dfx = sigmoid(df)
    df1x = sigmoid(df1)
    a.fill(1)
    w = a
    w = task1(dfx, df1x, y, w, la, ita, "final")
    #np.savetxt("wfinal.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    print("finalcompleted")

def main():
    train_file=sys.argv[1]
    dfR=get_features(train_file)
    df = norm(dfR)
    y = pd.read_csv(train_file, sep=',', usecols=['target'], squeeze=True)
    test_file=sys.argv[2]
    df1R = get_features(test_file)
    df1=norm(df1R)
    r,c=df.shape
    a = np.empty(c);
    a.fill(10)
    w = a
    la = 0000
    ita = 0.005#0.01
    #QUESTION 1 This is first call to unregularized
    w=task1(df, df1, y, w, la, ita, "unreg")
    #np.savetxt("wunreg.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    a.fill(10)
    w = a
    la = 7
    #QUESTION 1  This is for for regularized lambda set to 7
    w=task1(df, df1, y,w,la,ita,"reg")
    # QUESTION 2  This is for finding three features with amximum covariance with Y
    task2(df, y)
    #np.savetxt("wreg.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')

    a.fill(1)

    w = a
    # QUESTION 3  This is for l4 norm
    w=task3a(df, df1, y, w, la, ita)

    #np.savetxt("w4.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')

    # QUESTION 3 This is for l6 norm
    w = a
    w=task3b(df, df1, y, w, la, ita,"3b")
    #np.savetxt("w6.csv", np.dstack((np.arange(0, w.size), w))[0], "%d,%d", header="sl,ita",comments='')
    a.fill(0)
    # QUESTION 4 different methods

    ita=0.01
    la=1
    w = a
    task4(df, df1, y, w, la, ita, a)
    w = a
    task4sig(dfR, df1R, y, w, la, ita, a)
    w = a
    task4modsig(dfR, df1R, y, w, la, ita, a)
    w = a
    task5(dfR, df1R, y, w, la, ita, a)

main()



