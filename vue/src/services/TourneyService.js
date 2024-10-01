import axios from 'axios';

export default {
    getCurrentTournaments(){
        return axios.get(`/tournaments`);
    },

    getTournamentsHistory(){
        return axios.get(`/tournaments/history`)
    },

    getTournamentById(id){
        return axios.get(`/tournaments/${id}`);
    },

    getTournamentByTeamId(id){
        return axios.get(``);
    },

    getTournamentByDirector(directorName){
        return axios.get(``);
    },
    
    getTournamentByDate(date){
        return axios.get(``);
    },

    createTournament(tournament){
        return axios.post(``);
    },

    updateTournament(tournament){
        return axios.put(``);
    },

    

}