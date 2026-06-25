package com.neusoft.demo.controller;

import com.neusoft.demo.entity.Notice;
import com.neusoft.demo.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /** 发布公告 */
    @PostMapping("/add")
    public String add(@RequestBody Notice notice){

        noticeService.addNotice(notice);

        return "发布成功";

    }

    /** 查询公告 */
    @GetMapping("/list")
    public List<Notice> list(){

        return noticeService.listNotice();

    }

    /** 删除公告 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id){

        noticeService.deleteNotice(id);

        return "删除成功";

    }

}