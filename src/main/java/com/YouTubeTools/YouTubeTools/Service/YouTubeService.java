package com.YouTubeTools.YouTubeTools.Service;

import com.YouTubeTools.YouTubeTools.Model.SearchVideo;
import com.YouTubeTools.YouTubeTools.Model.Video;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final WebClient.Builder webClientBuilder;

    @Value("${youtube.api.key}")
    private String apikey;

    @Value("${youtube.api.base.url}")
    private String baseUrl;

    @Value("${youtube.api.max.related.videos}")
    private int maxRelatedVideos;


    // =========================================================
    // EXISTING SEARCH FUNCTION
    // =========================================================

    public SearchVideo searchVideo(String videoTitle) {

        List<String> videoIds = searchForVideoIds(videoTitle);

        if (videoIds.isEmpty()) {
            return SearchVideo.builder()
                    .primaryVideo(null)
                    .relatedVideos(List.of())
                    .build();
        }

        List<Video> videos = getVideoDetails(videoIds);

        if (videos.isEmpty()) {
            return SearchVideo.builder()
                    .primaryVideo(null)
                    .relatedVideos(List.of())
                    .build();
        }

        Video primaryVideo = videos.get(0);

        List<Video> relatedVideos = videos.size() > 1
                ? videos.subList(1, videos.size())
                : List.of();

        return SearchVideo.builder()
                .primaryVideo(primaryVideo)
                .relatedVideos(relatedVideos)
                .build();
    }


    // =========================================================
    // SEARCH VIDEO IDS
    // =========================================================

    private List<String> searchForVideoIds(String videoTitle) {

        SearchApiResponse response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", videoTitle)
                        .queryParam("type", "video")
                        .queryParam("maxResults", maxRelatedVideos + 1)
                        .queryParam("key", apikey)
                        .build())
                .retrieve()
                .bodyToMono(SearchApiResponse.class)
                .block();

        if (response == null || response.getItems() == null) {
            return List.of();
        }

        return response.getItems()
                .stream()
                .filter(item ->
                        item.getId() != null &&
                                item.getId().getVideoId() != null
                )
                .map(item -> item.getId().getVideoId())
                .toList();
    }


    // =========================================================
    // GET DETAILS FOR SEARCHED VIDEOS
    // =========================================================

    private List<Video> getVideoDetails(List<String> videoIds) {

        String ids = String.join(",", videoIds);

        VideoApiResponse response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet")
                        .queryParam("id", ids)
                        .queryParam("key", apikey)
                        .build())
                .retrieve()
                .bodyToMono(VideoApiResponse.class)
                .block();

        if (response == null || response.getItems() == null) {
            return List.of();
        }

        return response.getItems()
                .stream()
                .map(this::convertToVideo)
                .toList();
    }


    // =========================================================
    // CONVERT SEARCH VIDEO RESPONSE
    // =========================================================

    private Video convertToVideo(VideoItem item) {

        Snippet snippet = item.getSnippet();

        if (snippet == null) {
            return Video.builder()
                    .id(item.getId())
                    .build();
        }

        return Video.builder()
                .id(item.getId())
                .channelTitle(snippet.getChannelTitle())
                .title(snippet.getTitle())
                .description(snippet.getDescription())
                .publishedAt(snippet.getPublishedAt())
                .tags(
                        snippet.getTags() != null
                                ? snippet.getTags()
                                : List.of()
                )
                .build();
    }


    // =========================================================
    // NEW FUNCTION
    // GET VIDEO DETAILS BY URL OR VIDEO ID
    // =========================================================

    public Video getVideoDetailsById(String videoUrlOrId) {

        String videoId = extractVideoId(videoUrlOrId);

        VideoApiResponse response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet")
                        .queryParam("id", videoId)
                        .queryParam("key", apikey)
                        .build())
                .retrieve()
                .bodyToMono(VideoApiResponse.class)
                .block();

        if (response == null ||
                response.getItems() == null ||
                response.getItems().isEmpty()) {

            throw new RuntimeException("Video not found");
        }

        return convertToVideoDetails(
                response.getItems().get(0)
        );
    }


    // =========================================================
    // CONVERT VIDEO DETAILS
    // =========================================================

    private Video convertToVideoDetails(VideoItem item) {

        Snippet snippet = item.getSnippet();

        if (snippet == null) {
            throw new RuntimeException("Video information not available");
        }

        String thumbnailUrl = null;

        if (snippet.getThumbnails() != null) {

            if (snippet.getThumbnails().getHigh() != null) {

                thumbnailUrl = snippet.getThumbnails()
                        .getHigh()
                        .getUrl();

            } else if (snippet.getThumbnails().getMedium() != null) {

                thumbnailUrl = snippet.getThumbnails()
                        .getMedium()
                        .getUrl();

            } else if (snippet.getThumbnails().getDefaultThumbnail() != null) {

                thumbnailUrl = snippet.getThumbnails()
                        .getDefaultThumbnail()
                        .getUrl();
            }
        }

        return Video.builder()
                .id(item.getId())
                .channelTitle(snippet.getChannelTitle())
                .title(snippet.getTitle())
                .description(snippet.getDescription())
                .publishedAt(snippet.getPublishedAt())
                .thumbnailUrl(thumbnailUrl)
                .tags(
                        snippet.getTags() != null
                                ? snippet.getTags()
                                : List.of()
                )
                .build();
    }


    // =========================================================
    // EXTRACT VIDEO ID FROM URL
    // =========================================================

    private String extractVideoId(String videoUrlOrId) {

        if (videoUrlOrId == null || videoUrlOrId.isBlank()) {
            throw new IllegalArgumentException(
                    "Video URL or ID is required"
            );
        }

        videoUrlOrId = videoUrlOrId.trim();


        // Direct Video ID
        if (!videoUrlOrId.contains("youtube.com")
                && !videoUrlOrId.contains("youtu.be")) {

            return videoUrlOrId;
        }


        // https://www.youtube.com/watch?v=VIDEO_ID

        if (videoUrlOrId.contains("v=")) {

            String videoId = videoUrlOrId.substring(
                    videoUrlOrId.indexOf("v=") + 2
            );

            if (videoId.contains("&")) {
                videoId = videoId.substring(
                        0,
                        videoId.indexOf("&")
                );
            }

            return videoId;
        }


        // https://youtu.be/VIDEO_ID

        if (videoUrlOrId.contains("youtu.be/")) {

            String videoId = videoUrlOrId.substring(
                    videoUrlOrId.indexOf("youtu.be/") + 9
            );

            if (videoId.contains("?")) {
                videoId = videoId.substring(
                        0,
                        videoId.indexOf("?")
                );
            }

            return videoId;
        }


        throw new IllegalArgumentException(
                "Invalid YouTube URL or ID"
        );
    }


    // =========================================================
    // API RESPONSE CLASSES
    // =========================================================

    @Data
    static class SearchApiResponse {

        private List<SearchItem> items;
    }


    @Data
    static class SearchItem {

        private Id id;
    }


    @Data
    static class Id {

        private String videoId;
    }


    @Data
    static class VideoApiResponse {

        private List<VideoItem> items;
    }


    @Data
    static class VideoItem {

        private String id;

        private Snippet snippet;
    }


    @Data
    static class Snippet {

        private String title;

        private String description;

        private String channelTitle;

        private String publishedAt;

        private List<String> tags;

        private Thumbnails thumbnails;
    }


    // =========================================================
    // THUMBNAILS
    // =========================================================

    @Data
    static class Thumbnails {

        private Thumbnail maxres;

        private Thumbnail high;

        private Thumbnail medium;

        private Thumbnail standard;

        private Thumbnail defaultThumbnail;
    }


    @Data
    static class Thumbnail {

        private String url;

        private int width;

        private int height;
    }
}