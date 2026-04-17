package com.spytube.app.models;

import java.util.List;

public class CreditsResponse {
    public List<CastMember> cast;

    public static class CastMember {
        public int id;
        public String name;
        public String character;
        public String profile_path;
    }
}
