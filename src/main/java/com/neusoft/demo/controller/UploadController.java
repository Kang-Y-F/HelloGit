package com.neusoft.demo.controller;

import com.neusoft.demo.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    // 头像存储目录（相对路径，可根据需要修改为绝对路径）
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatar/";

    // 图片最大大小（5MB）
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 允许的文件类型
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    /**
     * 上传患者头像
     * POST /upload/avatar
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 校验文件是否为空
            if (file == null || file.isEmpty()) {
                return Result.fail("文件不能为空");
            }

            // 2. 校验文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                return Result.fail("文件大小不能超过5MB");
            }

            // 3. 校验文件类型
            String contentType = file.getContentType();
            boolean isAllowedType = false;
            for (String allowedType : ALLOWED_TYPES) {
                if (allowedType.equals(contentType)) {
                    isAllowedType = true;
                    break;
                }
            }
            if (!isAllowedType) {
                return Result.fail("只支持 JPG、PNG、GIF、WEBP 格式的图片");
            }

            // 4. 创建上传目录
            File uploadDir = new File(AVATAR_UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 5. 生成唯一文件名（UUID + 原文件扩展名）
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // 6. 保存文件
            Path filePath = Paths.get(AVATAR_UPLOAD_DIR, fileName);
            Files.write(filePath, file.getBytes());

            // 7. 返回访问URL（根据实际部署环境调整）
            String accessUrl = "/uploads/avatar/" + fileName;

            return Result.success(accessUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("上传异常：" + e.getMessage());
        }
    }

    /**
     * 上传通用文件（预留扩展）
     * POST /upload/file
     */
    @PostMapping("/file")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return uploadAvatar(file); // 暂时复用头像上传逻辑
    }
}
