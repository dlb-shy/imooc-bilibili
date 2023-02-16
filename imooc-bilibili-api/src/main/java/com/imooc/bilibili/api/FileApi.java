package com.imooc.bilibili.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileApi {



    @Autowired
    private FileService fileService;

    @PostMapping("/md5files")
    public JsonResponse<String> getFileMD5(MultipartFile file) throws Exception {
        String fileMD5 = fileService.getFileMD5(file);
        return new JsonResponse<>(fileMD5);
    }

    @PutMapping("/file-slices")
    public JsonResponse<String> uploadFileBySlices(MultipartFile slice,
                                                   String fileMd5,
                                                   Integer sliceNo,
                                                   Integer totalSliceNo) {
        String filePath = null;
        try {
            filePath = fileService.uploadFileBySlices(slice, fileMd5, sliceNo, totalSliceNo);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return new JsonResponse<>(filePath);
    }

}
