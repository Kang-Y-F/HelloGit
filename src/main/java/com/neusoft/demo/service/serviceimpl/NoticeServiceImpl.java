package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.demo.entity.Notice;
import com.neusoft.demo.mapper.NoticeMapper;
import com.neusoft.demo.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {


    @Autowired
    private NoticeMapper noticeMapper;

    /** 发布公告 */
    @Override
    public void addNotice(Notice notice) {

        noticeMapper.insert(notice);

    }

    /** 查询公告列表 */
    @Override
    public List<Notice> listNotice() {

        QueryWrapper<Notice> wrapper = new QueryWrapper<>();

        wrapper.orderByDesc("create_time");


        return noticeMapper.selectList(wrapper);

    }

    /** 删除公告 */
    @Override
    public void deleteNotice(Long id) {

        noticeMapper.deleteById(id);

    }

    @Override
    public List<Notice> listDoctorNotice(){

        return noticeMapper.selectList(
                new QueryWrapper<Notice>()
                        .in("target_type",1,3)
                        .orderByDesc("create_time")
        );

    }

    @Override
    public List<Notice> listPatientNotice(){

        return noticeMapper.selectList(
                new QueryWrapper<Notice>()
                        .in("target_type",2,3)
                        .orderByDesc("create_time")
        );

    }

}
