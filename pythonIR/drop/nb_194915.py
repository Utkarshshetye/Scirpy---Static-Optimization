#!/usr/bin/env python
# coding: utf-8

# In[ ]:


import glob
import re
import csv
import json
import os
path = "C:\\Shamal\\NEU\\Python\\MidTerm\\StackData\\"
os.chdir(path)
list = []
D=[]
FinalList = []

def cleanhtml(raw_html):
  cleanr = re.compile('<.*?>')
  cleantext = re.sub(cleanr, '', raw_html)
  return cleantext

listmy = (glob.glob("*.json"))

files = []
for a in os.listdir(path):
    if os.path.isfile(os.path.join(path,a)) and 'question' in a:
        files.append(a)
print (files)
        
userfiles = []
for m in os.listdir(path):
    if os.path.isfile(os.path.join(path,m)) and 'user' in m:
        userfiles.append(m)
print (userfiles)
       
for file in files:
    with open(file,'r') as json_data:
        a = json.load(json_data)
        data = a['items']
        for i in data:
            if (i['tags'] == 'python', 'pandas'):
                owner = i['owner']
                #print (owner)
                if (owner['user_type'] == 'registered'):
                    usrID = owner['user_id']
                    clean = cleanhtml(i['body'])
                    D.append([usrID, clean])
print ("//////////")
print (D)
print ("//////////")
def calculatebadge(Gcount, Scount, Bcount):
    score = 0
    Tscore = score + Gcount*1000+ Scount*400+ Bcount*100
    return Tscore


for l in D:                
    for f in userfiles:
        with open(f,'r') as json_userdata:
            b = json.load(json_userdata)
            for x in b:
                if(b['items'][0]['user_id'] == l[0]):
                    print (b['items'][0]['user_id'])
                    badge_count = b['items'][0]['badge_counts']
                    Scount = badge_count['silver']
                    Gcount = badge_count['gold']
                    Bcount = badge_count['bronze']
                    Total = calculatebadge(Gcount, Scount, Bcount)
                    FinalList.append([b['items'][0]['user_id'],l[1],Total])
                    break
                    
print ("...............................")
print (FinalList)
print ("...............................")

def getitem(item):
    return item[0]

testList =[]
testList = sorted(FinalList, key=getitem)
print (testList)

with open('output5.csv', 'w') as myfile:
    wr = csv.writer(myfile,delimiter=',', quoting=csv.QUOTE_ALL)
    for itm in testList:
        wr.writerow([itm[0],itm[1], itm[2]])


# In[ ]:




