package org.visallo.wikipedia;

import org.vertexium.Vertex;
import org.visallo.core.model.properties.types.DateVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

public class WikipediaConstants {
    public static final String WIKIPEDIA_QUEUE = "wikipedia";
    public static final String CONFIG_FLUSH = "flush";
    public static final String WIKIPEDIA_PAGE_CONCEPT_URI = "http://visallo.org/wikipedia#wikipediaPage";
    public static final String WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI = "http://visallo.org/wikipedia#wikipediaPageInternalLinkWikipediaPage";
    public static final String WIKIPEDIA_SOURCE = "Wikipedia";
    public static final String WIKIPEDIA_ID_PREFIX = "WIKIPEDIA_";
    public static final String WIKIPEDIA_LINK_ID_PREFIX = "WIKIPEDIA_LINK_";

    public static final DateVisalloProperty PUBLISHED_DATE = new DateVisalloProperty("http://visallo.org#publishedDate");
    public static final StringVisalloProperty PAGE_TITLE = new StringVisalloProperty("http://visallo.org/wikipedia#pageTitle");

    public static String getWikipediaPageVertexId(String pageTitle) {
        return WIKIPEDIA_ID_PREFIX + pageTitle.trim().toLowerCase();
    }

    public static String getWikipediaPageToPageEdgeId(Vertex pageVertex, Vertex linkedPageVertex) {
        return WIKIPEDIA_LINK_ID_PREFIX + getWikipediaPageTitleFromId(pageVertex.getId()) + "_" + getWikipediaPageTitleFromId(linkedPageVertex.getId());
    }

    public static String getWikipediaPageTitleFromId(Object id) {
        return id.toString().substring(WIKIPEDIA_ID_PREFIX.length());
    }
}
