/*
 *      Copyright (c) 2004-2015 Matthew Altman & Stuart Boston
 *
 *      This file is part of TheTVDB API.
 *
 *      TheTVDB API is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      TheTVDB API is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with TheTVDB API.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.omertron.thetvdbapi;

import com.omertron.thetvdbapi.model.Actor;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Episode;
import com.omertron.thetvdbapi.model.Series;
import com.omertron.thetvdbapi.model.TVDBUpdates;
import com.omertron.thetvdbapi.tools.DOMHelper;
import com.omertron.thetvdbapi.tools.TvdbParser;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.SimpleHttpClientBuilder;

/**
 * @author altman.matthew
 * @author stuart.boston
 */
public class TheTVDBApi {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDBApi.class);
    private String apiKey = null;
    private HttpClient httpClient;
    private static final String BASE_URL = "http://thetvdb.com/api/";
    private static final String XML_EXTENSION = ".xml";
    private static final String SERIES_URL = "/series/";
    private static final String ALL_URL = "/all/";
    private static final String WEEKLY_UPDATES_URL = "/updates/updates_week.xml";
    private static final String URL = "URL: {}";

    /**
     * Create an API object with the passed API Key
     *
     * @param apiKey Must be valid
     */
    public TheTVDBApi(String apiKey) {
        // No HttpClient passed, so use a default
        this(apiKey, new SimpleHttpClientBuilder().build());
    }

    /**
     * Create an API object with the passed API key and using the supplied HttpClient
     *
     * @param apiKey Must not be null or empty
     * @param httpClient
     */
    public TheTVDBApi(String apiKey, HttpClient httpClient) {
        if (StringUtils.isBlank(apiKey)) {
            return;
        }

        this.apiKey = apiKey;
        this.httpClient = httpClient;
        DOMHelper.setHttpClient(this.httpClient);
    }

    /**
     * Get the series information
     *
     * @param id
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public Series getSeries(String id, String language) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(SERIES_URL);
        urlBuilder.append(id);
        urlBuilder.append("/");
        if (StringUtils.isNotBlank(language)) {
            urlBuilder.append(language).append(XML_EXTENSION);
        }

        LOG.trace(URL, urlBuilder.toString());
        List<Series> seriesList = TvdbParser.getSeriesList(urlBuilder.toString());
        if (seriesList.isEmpty()) {
            return null;
        } else {
            return seriesList.get(0);
        }
    }

    /**
     * Get all the episodes for a series. Note: This could be a lot of records
     *
     * @param id
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public List<Episode> getAllEpisodes(String id, String language) throws TvDbException {
        List<Episode> episodeList = Collections.emptyList();

        if (isValidNumber(id)) {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(BASE_URL);
            urlBuilder.append(apiKey);
            urlBuilder.append(SERIES_URL);
            urlBuilder.append(id);
            urlBuilder.append(ALL_URL);
            if (StringUtils.isNotBlank(language)) {
                urlBuilder.append(language).append(XML_EXTENSION);
            }

            LOG.trace(URL, urlBuilder.toString());
            episodeList = TvdbParser.getAllEpisodes(urlBuilder.toString(), -1);
        }
        return episodeList;
    }

    /**
     * Get all the episodes from a specific season for a series. Note: This could be a lot of records
     *
     * @param id
     * @param season
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public List<Episode> getSeasonEpisodes(String id, int season, String language) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(SERIES_URL);
        urlBuilder.append(id);
        urlBuilder.append(ALL_URL);
        if (StringUtils.isNotBlank(language)) {
            urlBuilder.append(language).append(XML_EXTENSION);
        }

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getAllEpisodes(urlBuilder.toString(), season);
    }

    /**
     * Get a specific episode's information
     *
     * @param seriesId
     * @param seasonNbr
     * @param episodeNbr
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public Episode getEpisode(String seriesId, int seasonNbr, int episodeNbr, String language) throws TvDbException {
        return getTVEpisode(seriesId, seasonNbr, episodeNbr, language, "/default/");
    }

    /**
     * Get a specific DVD episode's information
     *
     * @param seriesId
     * @param seasonNbr
     * @param episodeNbr
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public Episode getDVDEpisode(String seriesId, int seasonNbr, int episodeNbr, String language) throws TvDbException {
        return getTVEpisode(seriesId, seasonNbr, episodeNbr, language, "/dvd/");
    }

    /**
     * Generic function to get either the standard TV episode list or the DVD list
     *
     * @param seriesId
     * @param seasonNbr
     * @param episodeNbr
     * @param language
     * @param episodeType
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    private Episode getTVEpisode(String seriesId, int seasonNbr, int episodeNbr, String language, String episodeType) throws TvDbException {
        if (!isValidNumber(seriesId) || !isValidNumber(seasonNbr) || !isValidNumber(episodeNbr)) {
            // Invalid number passed
            return new Episode();
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(SERIES_URL);
        urlBuilder.append(seriesId);
        urlBuilder.append(episodeType);
        urlBuilder.append(seasonNbr);
        urlBuilder.append("/");
        urlBuilder.append(episodeNbr);
        urlBuilder.append("/");
        if (StringUtils.isNotBlank(language)) {
            urlBuilder.append(language).append(XML_EXTENSION);
        }

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getEpisode(urlBuilder.toString());
    }

    /**
     * Get a specific absolute episode's information
     *
     * @param seriesId
     * @param episodeNbr
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public Episode getAbsoluteEpisode(String seriesId, int episodeNbr, String language) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(SERIES_URL);
        urlBuilder.append(seriesId);
        urlBuilder.append("/absolute/");
        urlBuilder.append(episodeNbr);
        urlBuilder.append("/");
        if (StringUtils.isNotBlank(language)) {
            urlBuilder.append(language).append(XML_EXTENSION);
        }

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getEpisode(urlBuilder.toString());
    }

    /**
     * Get a list of banners for the series id
     *
     * @param id
     * @param seasonNbr
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public String getSeasonYear(String id, int seasonNbr, String language) throws TvDbException {
        String year = null;

        Episode episode = getEpisode(id, seasonNbr, 1, language);
        if (episode != null && StringUtils.isNotBlank(episode.getFirstAired())) {
            Date date;

            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                date = dateFormat.parse(episode.getFirstAired());
            } catch (ParseException ex) {
                LOG.trace("Failed to transform date: {}", episode.getFirstAired(), ex);
                date = null;
            }

            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                year = String.valueOf(cal.get(Calendar.YEAR));
            }
        }

        return year;
    }

    /**
     * Get a list of banners for the series id
     *
     * @param seriesId
     * @return
     * @throws TvDbException
     */
    public Banners getBanners(String seriesId) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(SERIES_URL);
        urlBuilder.append(seriesId);
        urlBuilder.append("/banners.xml");

        LOG.trace(URL, urlBuilder.toString());
        Banners b = TvdbParser.getBanners(urlBuilder.toString());

        if (b != null) {
            b.setSeriesId(NumberUtils.toInt(seriesId));
        }

        return b;
    }

    /**
     * Get a list of actors from the series id
     *
     * @param seriesId
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public List<Actor> getActors(String seriesId) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(SERIES_URL);
        urlBuilder.append(seriesId);
        urlBuilder.append("/actors.xml");

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getActors(urlBuilder.toString());
    }

    /**
     * Get a list of series using a title and language
     *
     * @param title
     * @param language
     * @return
     * @throws TvDbException
     */
    public List<Series> searchSeries(String title, String language) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();

        try {
            urlBuilder.append(BASE_URL);
            urlBuilder.append("GetSeries.php?seriesname=");
            urlBuilder.append(URLEncoder.encode(title, "UTF-8"));
            if (StringUtils.isNotBlank(language)) {
                urlBuilder.append("&language=").append(language);
            }
        } catch (UnsupportedEncodingException ex) {
            LOG.trace("Failed to encode title: {}", title, ex);
            // Try and use the raw title
            urlBuilder.append(title);
        }

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getSeriesList(urlBuilder.toString());
    }

    /**
     * Get information for a specific episode
     *
     * @param episodeId
     * @param language
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public Episode getEpisodeById(String episodeId, String language) throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append("/episodes/");
        urlBuilder.append(episodeId);
        urlBuilder.append("/");
        if (StringUtils.isNotBlank(language)) {
            urlBuilder.append(language);
            urlBuilder.append(XML_EXTENSION);
        }

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getEpisode(urlBuilder.toString());
    }

    /**
     * Get the weekly updates
     *
     * @return
     * @throws com.omertron.thetvdbapi.TvDbException
     */
    public TVDBUpdates getWeeklyUpdates() throws TvDbException {
        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append(BASE_URL);
        urlBuilder.append(apiKey);
        urlBuilder.append(WEEKLY_UPDATES_URL);

        LOG.trace(URL, urlBuilder.toString());
        return TvdbParser.getUpdates(urlBuilder.toString());
    }

    /**
     * Convert a string to a number and then validate it
     *
     * @param number
     * @return
     */
    private boolean isValidNumber(String number) {
        return isValidNumber(NumberUtils.toInt(number, 0));
    }

    /**
     * Validate the number, i.e. ensure it is greater than zero
     *
     * @param number
     * @return
     */
    private boolean isValidNumber(int number) {
        return number >= 0;
    }
}
