package com.imooc.bilibili.api;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RESTfulApi {


    private final Map<Integer, Map<String, Object>> dataMap;

    public RESTfulApi(){
        dataMap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("id", i);
            data.put("name", "name" + i);
            dataMap.put(i, data);
        }
    }

    @GetMapping("/objects/{id}")
    public Map<String, Object> getData(@PathVariable("id") Integer id){
        return dataMap.get(id);
    }

    @DeleteMapping("/objects/{id}")
    public String delData(@PathVariable("id") Integer id){
        dataMap.remove(id);
        return "del success";
    }

    @PostMapping("/objects")
    public String postData(@RequestBody Map<String, Object> map){
        Integer[] idArray = dataMap.keySet().toArray(new Integer[0]);
        Arrays.sort(idArray);
        int nextId = idArray[idArray.length - 1] + 1;
        dataMap.put(nextId, map);
        return "post success";
    }
    @PutMapping("/objects")
    public String ptData(@RequestBody Map<String, Object> map){

        Integer id = Integer.valueOf(map.get("id").toString());
        Map<String, Object> objectMap = dataMap.get(id);
        if(objectMap == null){
            Integer[] idArray = dataMap.keySet().toArray(new Integer[0]);
            Arrays.sort(idArray);
            int nextId = idArray[idArray.length - 1] + 1;
            dataMap.put(nextId, map);
        }else
        {
            dataMap.put(id, map);
        }
        return "put success";
    }

}
