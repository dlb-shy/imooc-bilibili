package com.imooc.service;

import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.service.util.FastDFSUtil;
import com.imooc.service.util.ImageUtil;
import com.imooc.service.util.IpUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.io.File;

@Service
public class VideoService {

    private static final int FRAME_NO = 16;
    @Autowired
    private VideoDao videoDao;

    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;

    @Autowired
    private UserCoinService userCoinService;

    @Autowired
    private FastDFSUtil fastDFSUtil;
    @Autowired
    private ImageUtil imageUtil;

    @Transactional
    public void addVideos(Video video) {
        video.setCreateTime(new Date());
        videoDao.addVideos(video);
        Date now = new Date();
        Long videoId = video.getId();
        List<VideoTag> videoTagList = video.getVideoTagList();
        videoTagList.forEach(item -> {
            item.setCreateTime(now);
            item.setVideoId(videoId);
        });
        videoDao.batchAddVideoTags(videoTagList);
    }

    public PageResult<Video> pageListVideos(Integer size, Integer no, String area) {
        if (size == null || no == null){
            throw new ConditionException("???????????????");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1) * size);
        params.put("limit", size);
        params.put("area", area);
        List<Video> list = new ArrayList<>();
        Integer total =  videoDao.pageCountVideos(params);
        if (total > 0){
            list = videoDao.pageListVideos(params);
        }
        return new PageResult<>(total, list);
    }

    public void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        fastDFSUtil.viewVideoOnlineBySlices(request, response, url);
    }

    public void addVideoLike(Long videoId, Long userId) {

        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("???????????????");
        }
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        if(videoLike != null){
            throw new ConditionException("???????????????");
        }
        videoLike = new VideoLike();
        videoLike.setVideoId(videoId);
        videoLike.setCreateTime(new Date());
        videoLike.setUserId(userId);
        videoDao.addVideoLike(videoLike);
    }

    public void deleteVideoLike(Long videoId, Long userId) {
        videoDao.deleteVideoLike(videoId, userId);
    }

    public Map<String, Object> getVideoLikes(Long videoId, Long userId) {
        Long count = videoDao.getVideoLikes(videoId);
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        boolean like = videoLike != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    @Transactional
    public void addVideoCollection(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if(videoId == null || groupId == null){
            throw new ConditionException("???????????????");
        }
        Video video = videoDao.getVideoById(videoCollection.getVideoId());
        if(video == null){
            throw new ConditionException("???????????????");
        }
        //????????????????????????
        videoDao.deleteVideoCollection(videoId, userId);
        //????????????????????????
        videoCollection.setUserId(userId);
        videoCollection.setCreateTime(new Date());
        videoDao.addVideoCollection(videoCollection);

    }

    public void deleteVideoCollection(Long videoId, Long userId) {
        videoDao.deleteVideoCollection(videoId, userId);
    }

    public Map<String, Object> getVideoCollections(Long videoId, Long userId) {
        Long count = videoDao.getVideoCollections(videoId);
        VideoCollection videoCollection = videoDao.getVideoCollectionByVideoIdAndUserId(videoId, userId);
        boolean collection = videoCollection != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("collection", collection);
        return result;

    }
    @Transactional
    public void addVideoCoins(VideoCoin videoCoin, Long userId) {
        Long videoId = videoCoin.getVideoId();
        Integer amount = videoCoin.getAmount();
        if(videoId == null || amount == null){
            throw new ConditionException("???????????????");
        }
        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("???????????????");
        }
        // ??????????????????????????????
        //???????????????????????????????????????????????????
        Integer userCoinsAmount = userCoinService.getUserCoinsAmount(userId);
        userCoinsAmount = userCoinsAmount == null ? 0 : userCoinsAmount;
        if(amount > userCoinsAmount){
            throw new ConditionException("?????????????????????");
        }
        //????????????????????????????????????????????????????????????
        VideoCoin dbVideoCoin = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        //??????????????????
        if(dbVideoCoin == null){
            videoCoin.setUserId(userId);
            videoCoin.setCreateTime(new Date());
            videoDao.addVideoCoin(videoCoin);
        }else{
            Integer dbAmount = dbVideoCoin.getAmount();
            dbAmount += amount;
            //??????????????????
            videoCoin.setUserId(userId);
            videoCoin.setAmount(dbAmount);
            videoCoin.setUpdateTime(new Date());
            videoDao.updateVideoCoin(videoCoin);
        }
        //??????????????????????????????
        userCoinService.updateUserCoinsAmount(userId, (userCoinsAmount-amount));
    }

    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        Long count = videoDao.getVideoCoinsAmount(videoId);
        VideoCoin videoCollection = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        boolean like = videoCollection != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    public void addVideoComment(VideoComment videoComment, Long userId) {
        Long videoId = videoComment.getVideoId();
        if(videoId == null){
            throw new ConditionException("???????????????");
        }
        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("???????????????");
        }
        videoComment.setCreateTime(new Date());
        videoComment.setUserId(userId);
        videoDao.addVideoComment(videoComment);
    }

    public PageResult<VideoComment> pageListVideoComments(Integer size, Integer no, Long videoId) {
        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("???????????????");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("videoId", videoId);
        Integer total = videoDao.pageCountVideoComments(params);
        List<VideoComment> list = new ArrayList<>();
        if(total > 0){
            list = videoDao.pageListVideoComments(params);
            //????????????????????????
            List<Long> parentIdList = list.stream().map(VideoComment::getId).collect(Collectors.toList());
            List<VideoComment> childCommentList = videoDao.batchGetVideoCommentsByRootIds(parentIdList);
            //????????????????????????
            Set<Long> userIdList = list.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
            Set<Long> replyUserIdList = childCommentList.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
            Set<Long> childUserIdList = childCommentList.stream().map(VideoComment::getReplyUserId).collect(Collectors.toSet());
            userIdList.addAll(replyUserIdList);
            userIdList.addAll(childUserIdList);
            List<UserInfo> userInfoList = userService.batchGetUserInfoByUserIds(userIdList);
            Map<Long, UserInfo> userInfoMap = userInfoList.stream().collect(Collectors.toMap(UserInfo :: getUserId, userInfo -> userInfo));
            list.forEach(comment -> {
                Long id = comment.getId();
                List<VideoComment> childList = new ArrayList<>();
                childCommentList.forEach(child -> {
                    if(id.equals(child.getRootId())){
                        child.setUserInfo(userInfoMap.get(child.getUserId()));
                        child.setReplyUserInfo(userInfoMap.get(child.getReplyUserId()));
                        childList.add(child);
                    }
                });
                comment.setChildList(childList);
                comment.setUserInfo(userInfoMap.get(comment.getUserId()));
            });
        }
        return new PageResult<>(total, list);
    }

    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video =  videoDao.getVideoDetails(videoId);
        Long userId = video.getUserId();
        User user = userService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();
        Map<String, Object> result = new HashMap<>();
        result.put("video", video);
        result.put("userInfo", userInfo);
        return result;
    }

    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();
        // ??????clientId
        String agent = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
        String clientId = String.valueOf(userAgent.getId());
        String ip = IpUtil.getIP(request);
        Map<String, Object> params = new HashMap<>();
        if (userId != null){
            params.put("userId", userId);
        }else {
            params.put("ip", ip);
            params.put("clientId", clientId);
        }
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        params.put("today", sdf.format(now));
        params.put("videoId", videoId);
        // ??????????????????
        VideoView dbVideoView = videoDao.getVideoView(params);
        if (dbVideoView == null){
            videoView.setIp(ip);
            videoView.setClientId(clientId);
            videoView.setCreateTime(new Date());
            videoDao.addVideoView(videoView);
        }

    }

    public Integer getVideoViewCounts(Long videoId) {
        return videoDao.getVideoViewCounts(videoId);
    }

    /**
     * ???????????????????????????
     * @param userId
     * @return
     * @throws TasteException
     */
    public List<Video> recommond(Long userId) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        // ??????????????????
        DataModel dataModel = this.createDataModel(list);
        // ????????????????????????
        UncenteredCosineSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        System.out.println(similarity.userSimilarity(11, 12));
        // ??????????????????
        NearestNUserNeighborhood userNeighborhood = new NearestNUserNeighborhood(2, similarity, dataModel);
        long[] ar = userNeighborhood.getUserNeighborhood(userId);
        // ???????????????
        GenericUserBasedRecommender recommender = new GenericUserBasedRecommender(dataModel, userNeighborhood, similarity);
        // ????????????
        List<RecommendedItem> recommendedItems = recommender.recommend(userId, 5);
        List<Long> itemIds = recommendedItems.stream().map(RecommendedItem::getItemID).collect(Collectors.toList());
        return videoDao.batchGetVideosByIds(itemIds);

    }
    /**
     * ???????????????????????????
     * @param userId ??????id
     * @param itemId ????????????id?????????????????????????????????????????????
     * @param howMany ?????????????????????
     */
    public List<Video> recommendByItem(Long userId, Long itemId, int howMany) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        //??????????????????
        DataModel dataModel = this.createDataModel(list);
        //????????????????????????
        ItemSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        GenericItemBasedRecommender genericItemBasedRecommender = new GenericItemBasedRecommender(dataModel, similarity);
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????
        List<Long> itemIds = genericItemBasedRecommender.recommendedBecause(userId, itemId, howMany)
                .stream()
                .map(RecommendedItem::getItemID)
                .collect(Collectors.toList());
        //????????????
        return videoDao.batchGetVideosByIds(itemIds);
    }


    private DataModel createDataModel(List<UserPreference> userPreferenceList) {
        FastByIDMap<PreferenceArray> fastByIdMap = new FastByIDMap<>();
        Map<Long, List<UserPreference>> map = userPreferenceList.stream().collect(Collectors.groupingBy(UserPreference::getUserId));
        Collection<List<UserPreference>> list = map.values();
        for(List<UserPreference> userPreferences : list){
            GenericPreference[] array = new GenericPreference[userPreferences.size()];
            for(int i = 0; i < userPreferences.size(); i++){
                UserPreference userPreference = userPreferences.get(i);
                GenericPreference item = new GenericPreference(userPreference.getUserId(), userPreference.getVideoId(), userPreference.getValue());
                array[i] = item;
            }
            fastByIdMap.put(array[0].getUserID(), new GenericUserPreferenceArray(Arrays.asList(array)));
        }
        return new GenericDataModel(fastByIdMap);
    }

    public List<VideoBinaryPicture> convertVideoToImage(Long videoId, String fileMd5) throws Exception{
        com.imooc.bilibili.domain.File file = fileService.getFileByMd5(fileMd5);
        String filePath = "/root/Desktop/env/fastdfs/tempfile/fileForVideoId" + videoId + "." + file.getType();
        fastDFSUtil.downLoadFile(file.getUrl(), filePath);
        FFmpegFrameGrabber fFmpegFrameGrabber = FFmpegFrameGrabber.createDefault(filePath);
        fFmpegFrameGrabber.start();
        int ffLength = fFmpegFrameGrabber.getLengthInFrames();
        Frame frame;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        int count = 1;
        List<VideoBinaryPicture> pictures = new ArrayList<>();
        for(int i=1; i<= ffLength; i ++){
            long timestamp = fFmpegFrameGrabber.getTimestamp();
            frame = fFmpegFrameGrabber.grabImage();
            if(count == i){
                if(frame == null){
                    throw new ConditionException("?????????");
                }
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", os);
                InputStream inputStream = new ByteArrayInputStream(os.toByteArray());
                //????????????????????????
                java.io.File outputFile = java.io.File.createTempFile("convert-" + videoId + "-", ".png");
                BufferedImage binaryImg = imageUtil.getBodyOutline(bufferedImage, inputStream);
                ImageIO.write(binaryImg, "png", outputFile);
                //???????????????????????????????????????????????????????????????????????????????????????????????????
                imageUtil.transferAlpha(outputFile, outputFile);
                //????????????????????????
                String imgUrl = fastDFSUtil.uploadCommonFile((MultipartFile) outputFile);
                VideoBinaryPicture videoBinaryPicture = new VideoBinaryPicture();
                videoBinaryPicture.setFrameNo(i);
                videoBinaryPicture.setUrl(imgUrl);
                videoBinaryPicture.setVideoId(videoId);
                videoBinaryPicture.setVideoTimestamp(timestamp);
                pictures.add(videoBinaryPicture);
                count += FRAME_NO;
                //??????????????????
                outputFile.delete();
            }
        }
        //??????????????????
        File tmpFile = new File(filePath);
        tmpFile.delete();
        //??????????????????????????????
        videoDao.batchAddVideoBinaryPictures(pictures);
        return pictures;
    }
}


