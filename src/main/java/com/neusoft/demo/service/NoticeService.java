package com.neusoft.demo.service;

import com.neusoft.demo.entity.Notice;

import java.util.List;

public interface NoticeService {


    /** 发布公告 */
    void addNotice(Notice notice);

    /** 查询公告列表 */
    List<Notice> listNotice();

    /** 删除公告 */
    void deleteNotice(Long id);

}
