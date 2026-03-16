import pandas as pd
job_data = pd.read_csv("./input/Uncleaned_DS_jobs.scaled.csv")

job_data['min_salary_K$'], job_data['salary_estimate'] = zip(*job_data['salary_estimate'].apply(extract_values))

job_data['max_salary_K$'], job_data['salary_estimate'] = zip(*job_data['salary_estimate'].apply(extract_values))

job_data['avg_salary_estimate'] = (np.round((job_data['min_salary_K$'] + job_data['max_salary_K$'])/2,decimals=0)).astype(int)

job_data.head()

print(job_data)