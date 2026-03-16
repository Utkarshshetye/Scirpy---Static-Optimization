import numpy as np
import pandas as pd
#userId,movieId,rating,timestamp
ratings_data = pd.read_csv("data/ratings.csv")
ratings_data.head()
#movieId,title,genres
movie_names = pd.read_csv("data/movies.csv")
movie_names.head()
movie_data = pd.merge(ratings_data, movie_names, on='movieId')
movie_data.head()
movie_data.groupby('title')['rating'].mean().head()
movie_data.groupby('title')['rating'].mean().sort_values(ascending=False).head()
movie_data.groupby('title')['rating'].count().sort_values(ascending=False).head()
ratings_mean_count = pd.DataFrame(movie_data.groupby('title')['rating'].mean())
ratings_mean_count['rating_counts'] = pd.DataFrame(movie_data.groupby('title')['rating'].count())
ratings_mean_count.head()
user_movie_rating = movie_data.pivot_table(index='userId', columns='title', values='rating')
forrest_gump_ratings = user_movie_rating['Forrest Gump (1994)']
movies_like_forest_gump = user_movie_rating.corrwith(forrest_gump_ratings)

corr_forrest_gump = pd.DataFrame(movies_like_forest_gump, columns=['Correlation'])
corr_forrest_gump.dropna(inplace=True)
corr_forrest_gump.head()
corr_forrest_gump.sort_values('Correlation', ascending=False).head(10)
corr_forrest_gump = corr_forrest_gump.join(ratings_mean_count['rating_counts'])
corr_forrest_gump.head()