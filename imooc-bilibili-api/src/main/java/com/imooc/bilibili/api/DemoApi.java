package com.imooc.bilibili.api;

import com.imooc.service.DemoService;
import com.imooc.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DemoApi {
    @Autowired
    private DemoService demoService;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @GetMapping("/query")
    public Long query(Long id){
        return demoService.query(id);
    }

    @GetMapping("/slices")
    public void slice(MultipartFile file) throws Exception {
        fastDFSUtil.convertFileToSlices(file);
    }



}