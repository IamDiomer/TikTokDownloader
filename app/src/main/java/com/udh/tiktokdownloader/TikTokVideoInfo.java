package com.udh.tiktokdownloader;

public class TikTokVideoInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String title;
    private String coverUrl;
    private String videoNoWmUrl;
    private String videoWmUrl;
    private String audioUrl;

    public String getAudioUrl() { return audioUrl == null ? "" : audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    private String authorNickname;
    private String authorUniqueId;
    private String authorAvatar;
    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private long viewsCount;

    public TikTokVideoInfo() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getVideoNoWmUrl() {
        return videoNoWmUrl;
    }

    public void setVideoNoWmUrl(String videoNoWmUrl) {
        this.videoNoWmUrl = videoNoWmUrl;
    }

    public String getVideoWmUrl() {
        return videoWmUrl;
    }

    public void setVideoWmUrl(String videoWmUrl) {
        this.videoWmUrl = videoWmUrl;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorUniqueId() {
        return authorUniqueId;
    }

    public void setAuthorUniqueId(String authorUniqueId) {
        this.authorUniqueId = authorUniqueId;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public long getSharesCount() {
        return sharesCount;
    }

    public void setSharesCount(long sharesCount) {
        this.sharesCount = sharesCount;
    }

    public long getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(long viewsCount) {
        this.viewsCount = viewsCount;
    }
}
