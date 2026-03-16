...
fbi = pd.read_csv('fbi_reports.csv')
us_pop = pd.read_csv('US_Population.csv')
fbi.drop(['Record Source'],inplace=True,axis=1)
fbi.drop(['Agency Code'],inplace=True,axis=1)
fbi.drop(['Agency Name'],inplace=True,axis=1)
fbi.drop(['Agency Type'],inplace=True,axis=1)
fbi.loc[:, ['Incident', 'Victim Age', 'Perpetrator Age', 'Victim Count', 'Perpetrator Count']].describe()
fbi.loc[:, ['Incident', 'Victim Age', 'Perpetrator Age', 'Victim Count', 'Perpetrator Count']].corr()
fbi['Crime Type'].value_counts().plot(kind='bar')
...
(fbi[fbi['Crime Type'] == 'Murder or Manslaughter'].count()['Incident'] / fbi['Crime Type'].count()) * 100
fbi['Year'].value_counts().sort_index(ascending=True).plot()
...
incidents_by_year = fbi.groupby('Year').count()['Incident']
total_population = us_pop['US_Population']
...
incidents_per_population = [i/j * 100000 for i,j in zip(incidents_by_year, total_population)]
...
fbi['Crime Solved'].value_counts().plot(kind='bar')
...
total_crimes = fbi['Crime Solved'].count()
fbi_crimes_solved_y = fbi[fbi['Crime Solved'] == 'Yes']
fbi_crimes_solved_n = fbi[fbi['Crime Solved'] == 'No']
total_crimes_solved = fbi_crimes_solved_y['Crime Solved'].count()
...
crimes_solved_per_year = fbi_crimes_solved_y.groupby('Year').count()['Crime Solved']
total_crimes_solved = fbi.groupby('Year').count()['Crime Solved']
...
crimes_not_solved = fbi_crimes_solved_n.groupby('Year').count()['Crime Solved']
crimes_solved = fbi_crimes_solved_y.groupby('Year').count()['Crime Solved']
y = fbi.groupby('Year').count()['Crime Solved']
...
fbi_crimes_solved_y = fbi[fbi['Crime Solved'] == 'Yes']
fbi_crimes_solved_n = fbi[fbi['Crime Solved'] == 'No']
x = fbi_crimes_solved_y.groupby('Year').count()['Crime Solved']
y = us_pop['US_Population']
...
a = fbi_crimes_solved_n.groupby('Year').count()['Crime Solved']
b = us_pop['US_Population']
fbi_handgun = fbi[fbi['Weapon'] == 'Handgun']
fbi_crimes_solved_n = fbi[fbi['Crime Solved'] == 'No']
x = fbi_handgun.groupby('Year').count()['Weapon']
y = fbi.groupby('Year').count()['Crime Solved']
...
x = fbi_handgun.groupby('Year').count()['Weapon']
y = us_pop['US_Population']
...



