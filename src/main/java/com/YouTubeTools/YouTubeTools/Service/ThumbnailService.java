package com.YouTubeTools.YouTubeTools.Service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ThumbnailService {

    public String extractVideo(String url) {

        // जर direct YouTube Video ID दिली असेल
        if (url.matches("^[a-zA-Z0-9_-]{11}$")) {
            return url;
        }

        String[] patterns = {
                "(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})",
                "(?:https?://)?youtu\\.be/([a-zA-Z0-9_-]{11})",
                "(?:https?://)?(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]{11})",
                "(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"
        };

        for (String pattern : patterns) {

            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(url);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new IllegalArgumentException("Invalid YouTube URL");
    }
}