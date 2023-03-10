package com.imooc.service;

import com.imooc.bilibili.dao.repository.UserInfoRepository;
import com.imooc.bilibili.dao.repository.VideoRepository;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.Video;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchSortValues;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class ElasticSearchService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public void addUserInfo(UserInfo userInfo){
        userInfoRepository.save(userInfo);
    }

    public void addVideo(Video video){
        videoRepository.save(video);
    }

    public List<Map<String, Object>> getContents(String keyword,
                                                 Integer pageNo,
                                                 Integer pageSize) throws IOException {
        String[] indices = {"videos", "user-infos"};
        SearchRequest searchRequest = new SearchRequest(indices);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //
        sourceBuilder.from(pageNo - 1);
        sourceBuilder.size(pageSize);

        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "nick", "description");
        sourceBuilder.query(matchQueryBuilder);
        searchRequest.source(sourceBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        //????????????
        String[] array = {"title", "nick", "description"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for (String key : array) {
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false); //??????????????????????????????????????????false
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);

        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(searchResponse.toString());
        List<Map<String, Object>> arrayList = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            //??????????????????
            Map<String, HighlightField> highLightBuilderFields = hit.getHighlightFields();
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            for(String key : array){
                HighlightField field = highLightBuilderFields.get(key);
                if(field != null){
                    Text[] fragments = field.fragments();
                    String str = Arrays.toString(fragments);
                    str = str.substring(1, str.length()-1);
                    sourceMap.put(key, str);
                }
            }
            arrayList.add(sourceMap);
        }
        return arrayList;
    }

//    public List<Map<String, Object>> getContents(String keyword,
//                                                 Integer pageNo,
//                                                 Integer pageSize) throws IOException {
//        String[] indices = {"videos", "user-infos"};
//        SearchRequest searchRequest = new SearchRequest(indices);
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        //??????
//        sourceBuilder.from(pageNo - 1);
//        sourceBuilder.size(pageSize);
//        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "nick", "description");
//        sourceBuilder.query(matchQueryBuilder);
//        searchRequest.source(sourceBuilder);
//        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//        //????????????
//        String[] array = {"title", "nick", "description"};
//        HighlightBuilder highlightBuilder = new HighlightBuilder();
//        for(String key : array){
//            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
//        }
//        highlightBuilder.requireFieldMatch(false); //??????????????????????????????????????????false
//        highlightBuilder.preTags("<span style=\"color:red\">");
//        highlightBuilder.postTags("</span>");
//        sourceBuilder.highlighter(highlightBuilder);
//        //????????????
//        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//        List<Map<String, Object>> arrayList = new ArrayList<>();
//        for(SearchHit hit : searchResponse.getHits()){
//            //??????????????????
//            Map<String, HighlightField> highLightBuilderFields = hit.getHighlightFields();
//            Map<String, Object> sourceMap = hit.getSourceAsMap();
//            for(String key : array){
//                HighlightField field = highLightBuilderFields.get(key);
//                if(field != null){
//                    Text[] fragments = field.fragments();
//                    String str = Arrays.toString(fragments);
//                    str = str.substring(1, str.length()-1);
//                    sourceMap.put(key, str);
//                }
//            }
//            arrayList.add(sourceMap);
//        }
//        return arrayList;
//    }

    public Video getVideos(String keyword){
        return videoRepository.findByTitleLike(keyword);
    }

    public void deleteAllVideos(){
        videoRepository.deleteAll();
    }
}
