package com.YouTubeTools.YouTubeTools.Controller;

import com.YouTubeTools.YouTubeTools.Model.SearchVideo;
import com.YouTubeTools.YouTubeTools.Service.YouTubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/youtube")
public class YouTubeTagsController {

    @Autowired
    private YouTubeService youTubeService;

    @Value("${youtube.api.key}")
    private String apiKey;

    private boolean isApiKeyConfigured(){
        if(apiKey==null || apiKey.isEmpty()){
            return false;
        }
        return true;
    }

    @PostMapping("/search")
    public String videoTags(@RequestParam ("videoTitle") String videoTitle, Model model){

        if(!isApiKeyConfigured()){
            model.addAttribute("error" , "Api key isn't configured");
        }

        if(videoTitle == null || videoTitle.isEmpty() ){
            model.addAttribute("error","video tittle is Required");
        }

        try{
            SearchVideo result= youTubeService.searchVideo(videoTitle);
            model.addAttribute("primaryVideo",result.getPrimaryVideo());
            model.addAttribute("relatedVideos",result.getRelatedVideos());
            return "home";


        }catch(Exception e ){
            model.addAttribute("error", e.getMessage());
            return "home";
        }




    }

}