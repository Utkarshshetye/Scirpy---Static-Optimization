def soc_loop(leaguedf,TEAM,):
    leaguedf['Draws'] = 99999
    for row in range(0, len(leaguedf)):
        if ((leaguedf['HomeTeam'].iloc[row] == TEAM) & (leaguedf['FTR'].iloc[row] == 'D')) | \
                ((leaguedf['AwayTeam'].iloc[row] == TEAM) & (leaguedf['FTR'].iloc[row] == 'D')):
            leaguedf['Draws'].iloc[row] = 'Draw'
        elif ((leaguedf['HomeTeam'].iloc[row] == TEAM) & (leaguedf['FTR'].iloc[row] != 'D')) | \
                ((leaguedf['AwayTeam'].iloc[row] == TEAM) & (leaguedf['FTR'].iloc[row] != 'D')):
            leaguedf['Draws'].iloc[row] = 'No_Draw'
        else:
            leaguedf['Draws'].\
                iloc[row] = 'No_Game'
            print("heloo")



soc_loop(df, 'arsenal')