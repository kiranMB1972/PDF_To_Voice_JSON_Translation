package com.org.games.model;

/**
 * One game. "name"/"character"/"difficulty"/"type" keep the field names of the attached Hotstuff project's API.
 * group: the heading inside the Entertainment Games drop-down.
 */
public record GameInfo(String id, String name, String group, String character, String type, String difficulty,
                       String icon, String file, String blurb, String controls) { }
