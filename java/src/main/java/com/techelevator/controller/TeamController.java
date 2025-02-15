package com.techelevator.controller;

import com.techelevator.dao.GameDao;
import com.techelevator.dao.JdbcMemberDao;
import com.techelevator.dao.TeamDao;
import com.techelevator.exception.DaoException;
import com.techelevator.model.AcceptRejectTeamDto;
import com.techelevator.model.Team;
import com.techelevator.model.TeamDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.List;

@RestController
@CrossOrigin
public class TeamController {

    @Autowired
    private TeamDao teamDao;


    //CREATE new teams
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(path = "/teams", method = RequestMethod.POST)
    public void makeTeams(@RequestBody Team newTeam, Principal principal) {
        try {
            if (teamDao.getTeamByTeamName(newTeam.getTeamName()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team already exists.");
            } else {
                teamDao.createTeam(newTeam, principal);
            }
        } catch (DaoException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Team registration error.");
        }

    }

    //GET all teams Dto
    @RequestMapping(path = "/teams", method = RequestMethod.GET)
    public List<TeamDto> getAllTeams() {
        return teamDao.getAllTeams();
    }

    //GET teams by ID
    @RequestMapping(path = "/teams/{teamId}", method = RequestMethod.GET)
    public Team getTeam(@PathVariable int teamId) {
        Team team = teamDao.getTeamById(teamId);
        if(team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found.");
        } else {
            return team;
        }
    }

    //PUT teams by ID
    @RequestMapping(path = "/teams/{teamId}", method = RequestMethod.PUT)
    public Team updateTeam(@RequestBody Team team, @PathVariable int teamId) {
        team.setTeamId(teamId);
        try {
            Team updatedTeam = teamDao.updateTeam(team);
            return updatedTeam;
        } catch (DaoException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found.");
        }
    }


    //DELETE teams by ID
    @RequestMapping(path = "/teams/{teamId}", method = RequestMethod.DELETE)
    public void deleteTeam(@PathVariable int teamId, Principal principal) {

        try {
            boolean deleted = teamDao.deleteTeamById(teamId);
            if (!deleted) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found.");
            }
        } catch (DaoException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Team deletion error.");
        }
    }

    //GET teams by game ID
    @RequestMapping(path = "/teams/game/{gameId}", method = RequestMethod.GET)
    public Team getTeamByGameId(@PathVariable int gameId) {
        Team team = teamDao.getTeamByGameId(gameId);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found.");
        } else {
            return team;
        }
    }

    //GET teams by captain ID
    @RequestMapping(path = "/teams/captain/{captainId}", method = RequestMethod.GET)
    public Team getTeamByCaptainId(@PathVariable int captainId) {
        Team team = teamDao.getTeamById(captainId);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found.");
        } else {
            return team;
        }
    }

    //Request to Join
    @RequestMapping(path="/teams/join/{teamId}", method=RequestMethod.POST)
    public boolean requestTeamJoin(@PathVariable int teamId, Principal principal){
            return teamDao.requestTeamJoin(principal, teamId);
    }

    //PATCH accept/reject team members
    @RequestMapping(path="/accept-teammate/", method = RequestMethod.PUT)
    public void acceptTeammate(@RequestBody AcceptRejectTeamDto acceptReject){
        teamDao.acceptRequest(acceptReject);
    }

    @RequestMapping(path="/reject-teammate/", method = RequestMethod.PUT)
    public void rejectTeammate(@RequestBody AcceptRejectTeamDto acceptReject){
        teamDao.rejectRequest(acceptReject);
    }

    @RequestMapping(path="/teams-im-captain/", method= RequestMethod.GET)
    public List<TeamDto> teamsImCaptain(Principal principal){
        return teamDao.teamsIAmCaptainOn(principal);
    }
}

