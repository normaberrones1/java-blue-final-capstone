package com.techelevator.dao;

import com.techelevator.model.BracketDto;
import com.techelevator.model.LoadingBracketData;
import com.techelevator.model.MatchDto;
import com.techelevator.model.ScoreDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcTeamBracketDao implements TeamBracketDao{

    private JdbcTemplate template;

    private JdbcTeamDao teamDao;

    public JdbcTeamBracketDao(JdbcTemplate template, JdbcTeamDao teamDao){
        this.template = template;
        this.teamDao = teamDao;
    }
    public void saveFirstRound(int tourneyId, List<String> bracketData){
        String sql = "INSERT INTO tourney_matches (tourney_id, team_1_id, team_2_id, team_1_points, team_2_points, round, seat) " +
                "VALUES (?,?,?,0,0,1,?)";
        int matchId = 1;

        while(bracketData.size() > 1){
            String team1 = bracketData.get(0);
            bracketData.remove(0);
            String team2 = bracketData.get(0);
            bracketData.remove(0);
            template.update(sql, tourneyId, teamDao.getTeamByTeamName(team1).getTeamId(),
                    teamDao.getTeamByTeamName(team2).getTeamId(), matchId);
            matchId++;
        }

        if(bracketData.size() == 1){
            String byeRound = bracketData.get(0);
            bracketData.remove(0);
            template.update(sql, tourneyId, teamDao.getTeamByTeamName(byeRound).getTeamId(),
                    teamDao.getTeamByTeamName(byeRound).getTeamId(), matchId);
        }
    }

    public List<MatchDto> advanceNextRound(int tourneyId){
        int round = 0;
        int setMatchId = 1;
        int findMatchId = 1;
        int totalMatches = 0;

        String sqlForFindRound = "SELECT MAX(round) FROM tourney_matches WHERE tourney_id = ?";
        SqlRowSet maxRowSet = template.queryForRowSet(sqlForFindRound, tourneyId);
        if(maxRowSet.next()){
            round = maxRowSet.getInt("max");
        }

        String getTotalMatches = "SELECT count(*) FROM tourney_matches WHERE tourney_id = ? AND round = ?";
        SqlRowSet countRowSet = template.queryForRowSet(getTotalMatches, tourneyId, round);
        if(countRowSet.next()){
            totalMatches = countRowSet.getInt("count");
        }

        while(totalMatches > 1) {
            String nextRoundSql = "INSERT INTO tourney_matches (tourney_id, team_1_id, team_2_id, team_1_points, team_2_points, round, seat) " +
                    "VALUES (?,?,?,0,0,?,?)";
            template.update(nextRoundSql, tourneyId, getWinnerId(findMatchId++, round, tourneyId),
                    getWinnerId(findMatchId++, round, tourneyId),
                    round+1,setMatchId++);
            totalMatches = totalMatches - 2;
        }

        System.out.println(round);
        System.out.println(setMatchId);
        if(totalMatches == 1){
            int byeTeamId = getWinnerId(findMatchId, round, tourneyId);
            String nextRoundSql = "INSERT INTO tourney_matches (tourney_id, team_1_id, team_2_id, team_1_points, team_2_points, round, seat) " +
                    "VALUES (?,?,?,0,0,?,?);";
            template.update(nextRoundSql, tourneyId, byeTeamId, byeTeamId, round+1, setMatchId++);
        }

        List<MatchDto> matchupList = new ArrayList<>();
        String getNewMatchupsSql = "SELECT team_1_id, team_2_id, seat, round FROM tourney_matches " +
                "WHERE tourney_id = ? AND round = ?";
        SqlRowSet resultRowSet = template.queryForRowSet(getNewMatchupsSql, tourneyId, round+1);
        while(resultRowSet.next()){
            matchupList.add(mapRowToMatchDto(resultRowSet));
        }

        String updateTourney = "UPDATE tournament SET round = ? WHERE tourney_id = ?";
        template.update(updateTourney, round+1, tourneyId);

        return matchupList;
    }

    @Override
    public boolean saveScore(ScoreDto scores, int tourneyId) {
        String sql = "UPDATE tourney_matches SET team_1_points = ?, team_2_points = ?, set_winner = ? " +
                "WHERE tourney_id = ? AND team_1_id = ? AND team_2_id = ?";
        String loserSql = "UPDATE team_tourney SET eliminated = TRUE WHERE team_id = ?";
        if(scores.getTeam1Score() > scores.getTeam2Score()) {
            template.update(sql, scores.getTeam1Score(), scores.getTeam2Score(),
                    teamDao.getTeamByTeamName(scores.getTeam1Name()).getTeamId(),
                    tourneyId, teamDao.getTeamByTeamName(scores.getTeam1Name()).getTeamId(),
                    teamDao.getTeamByTeamName(scores.getTeam2Name()).getTeamId());
            template.update(loserSql, teamDao.getTeamByTeamName(scores.getTeam2Name()).getTeamId());
        }else{
            template.update(sql, scores.getTeam1Score(), scores.getTeam2Score(),
                    teamDao.getTeamByTeamName(scores.getTeam2Name()).getTeamId(),
                    tourneyId, teamDao.getTeamByTeamName(scores.getTeam1Name()).getTeamId(),
                    teamDao.getTeamByTeamName(scores.getTeam2Name()).getTeamId());
            template.update(loserSql, teamDao.getTeamByTeamName(scores.getTeam1Name()).getTeamId());
        }
        return true;
    }


    public List<LoadingBracketData> getTourneyMatches(int tourneyId){
        List<LoadingBracketData> brackets = new ArrayList<>();
        String sql = "SELECT * " +
                "FROM tourney_matches WHERE tourney_id = ? " +
                "ORDER BY round, seat";
        SqlRowSet rowSet = template.queryForRowSet(sql,tourneyId);
        while(rowSet.next()){
            brackets.add(mapToBracketData(rowSet));
        }
        return brackets;
    }

    private LoadingBracketData mapToBracketData(SqlRowSet rowSet){
        LoadingBracketData data = new LoadingBracketData();
        data.setTeam1Name(teamDao.getTeamById(rowSet.getInt("team_1_id")).getTeamName());
        data.setTeam2Name(teamDao.getTeamById(rowSet.getInt("team_2_id")).getTeamName());
        data.setTeam1Score(rowSet.getInt("team_1_points"));
        data.setTeam2Score(rowSet.getInt("team_2_points"));
        data.setRound(rowSet.getInt("round"));
        data.setSeat(rowSet.getInt("seat"));
        return data;
    }

    private int getWinnerId(int matchNumber, int roundNumber, int tourneyId){
        int winnerId= 0;
        String sql = "SELECT set_winner FROM tourney_matches WHERE tourney_id = ? AND " +
                "round = ? AND seat = ?";
        SqlRowSet rowSet = template.queryForRowSet(sql, tourneyId, roundNumber, matchNumber);
        if(rowSet.next() && (Integer.valueOf(rowSet.getInt("set_winner")) != null)){
            winnerId= rowSet.getInt("set_winner");
        }
        if(winnerId==0){
            String sql2 = "SELECT team_1_id FROM tourney_matches WHERE tourney_id = ? " +
                    "AND round = ? AND seat = ?";
            SqlRowSet rowSet1 = template.queryForRowSet(sql2, tourneyId, roundNumber, matchNumber);
            if(rowSet1.next()){
                winnerId = rowSet1.getInt("team_1_id");
            }
        }
            return winnerId;
    }

    private MatchDto mapRowToMatchDto(SqlRowSet rowSet){
        MatchDto match = new MatchDto();
        match.setTeam1Name(teamDao.getTeamById(rowSet.getInt("team_1_id")).getTeamName());
        match.setTeam2Name(teamDao.getTeamById(rowSet.getInt("team_2_id")).getTeamName());
        match.setMatchNumber(rowSet.getInt("seat"));
        match.setRound(rowSet.getInt("round"));
        return match;
    }
}
