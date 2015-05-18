package org.visallo.themoviedb;

import org.visallo.core.model.properties.types.DateVisalloProperty;
import org.visallo.core.model.properties.types.DoubleVisalloProperty;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

public class TheMovieDbOntology {
    public static final String EDGE_LABEL_PLAYED = "http://visallo.org/themoviedb#played";
    public static final String EDGE_LABEL_HAS_ROLE = "http://visallo.org/themoviedb#hasRole";
    public static final String EDGE_LABEL_PRODUCED = "http://visallo.org/themoviedb#produced";
    public static final String EDGE_LABEL_HAS_PROFILE_IMAGE = "http://visallo.org/themoviedb#hasProfileImage";
    public static final String EDGE_LABEL_HAS_POSTER_IMAGE = "http://visallo.org/themoviedb#hasPosterImage";
    public static final String EDGE_LABEL_STARRED_IN = "http://visallo.org/themoviedb#starredin";
    public static final String EDGE_LABEL_HAS_LOGO = "http://visallo.org/themoviedb#hasLogo";

    public static final String CONCEPT_TYPE_THE_MOVIE_DB = "http://visallo.org/themoviedb#the-movie-db-root";
    public static final String CONCEPT_TYPE_PERSON = "http://visallo.org/themoviedb#person";
    public static final String CONCEPT_TYPE_PROFILE_IMAGE = "http://visallo.org/themoviedb#profileimage";
    public static final String CONCEPT_TYPE_MOVIE = "http://visallo.org/themoviedb#movie";
    public static final String CONCEPT_TYPE_LOGO = "http://visallo.org/themoviedb#logo";
    public static final String CONCEPT_TYPE_ROLE = "http://visallo.org/themoviedb#role";
    public static final String CONCEPT_TYPE_PRODUCTION_COMPANY = "http://visallo.org/themoviedb#productionCompany";
    public static final String CONCEPT_TYPE_POSTER_IMAGE = "http://visallo.org/themoviedb#posterImage";

    public static final DoubleVisalloProperty RUNTIME = new DoubleVisalloProperty("http://visallo.org/themoviedb#runtime");
    public static final IntegerVisalloProperty REVENUE = new IntegerVisalloProperty("http://visallo.org/themoviedb#revenue");
    public static final StringVisalloProperty GENRE = new StringVisalloProperty("http://visallo.org/themoviedb#genre");
    public static final DateVisalloProperty BIRTHDATE = new DateVisalloProperty("http://visallo.org/themoviedb#birthdate");
    public static final StringVisalloProperty TAG_LINE = new StringVisalloProperty("http://visallo.org/themoviedb#tagLine");
    public static final DateVisalloProperty DEATH_DATE = new DateVisalloProperty("http://visallo.org/themoviedb#deathdate");
    public static final IntegerVisalloProperty BUDGET = new IntegerVisalloProperty("http://visallo.org/themoviedb#budge");
    public static final StringVisalloProperty ALSO_KNOWN_AS = new StringVisalloProperty("http://visallo.org/themoviedb#aka");
    public static final DateVisalloProperty RELEASE_DATE = new DateVisalloProperty("http://visallo.org/themoviedb#releaseDate");

    public static String getImageVertexId(String imagePath) {
        return "MOVIEDB_IMAGE_" + imagePath;
    }

    public static String getStarredInEdgeId(int personId, int movieId) {
        return "MOVIEDB_STARRED_" + personId + "_" + movieId;
    }

    public static String getMovieVertexId(int movieId) {
        return "MOVIEDB_MOVIE_" + movieId;
    }

    public static String getPersonVertexId(int personId) {
        return "MOVIEDB_PERSON_" + personId;
    }

    public static String getProductionCompanyVertexId(int productionCompanyId) {
        return "MOVIEDB_PRODCO_" + productionCompanyId;
    }

    public static String getProductionCompanyProducedEdgeId(int productionCompanyId, int movieId) {
        return "MOVIEDB_PRODCO_PRODUCED_" + productionCompanyId + "_" + movieId;
    }

    public static String getHasImageEdgeId(int id, String imagePath) {
        return "MOVIEDB_HAS_IMAGE_" + id + "_" + imagePath;
    }

    public static String getRoleId(int personId, int movieId) {
        return personId + "-" + movieId;
    }

    public static String getRoleVertexId(String roleId) {
        return "MOVIEDB_ROLE_" + roleId;
    }

    public static String getPlayedEdgeId(int personId, String roleId) {
        return "MOVIEDB_PLAYED_" + personId + "_" + roleId;
    }

    public static String getHasRoleEdgeId(int movieId, String roleId) {
        return "MOVIEDB_HAS_ROLE_" + movieId + "_" + roleId;
    }
}
