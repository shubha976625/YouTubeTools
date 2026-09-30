package com.YouTubeTools.YouTubeTools.Controller;

import com.YouTubeTools.YouTubeTools.Model.Video;
import com.YouTubeTools.YouTubeTools.Service.YouTubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/youtube")
public class YouTubeVideoController {

    private final YouTubeService youtubeService;


    // =========================================================
    // OPEN VIDEO DETAILS PAGE
    // =========================================================

    @GetMapping("/video-details")
    public String videoDetailsPage() {

        return "video-details";
    }


    // =========================================================
    // FETCH VIDEO DETAILS
    // =========================================================

    @PostMapping("/video-details")
    public String getVideoDetails(
            @RequestParam String videoUrlOrId,
            Model model) {

        try {

            Video videoDetails =
                    youtubeService.getVideoDetailsById(videoUrlOrId);

            model.addAttribute(
                    "videoDetails",
                    videoDetails
            );

            model.addAttribute(
                    "videoUrlOrId",
                    videoUrlOrId
            );

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "Unable to fetch video details. Please check the YouTube URL or ID."
            );

            model.addAttribute(
                    "videoUrlOrId",
                    videoUrlOrId
            );
        }

        return "video-details";
    }
}