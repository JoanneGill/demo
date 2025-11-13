package com.example.demo.Config;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@Service
public class File {

    @Value("${file.uploadUrlQR}")
    private String  fileUrlQR;
    @Value("${file.uploadUrlEC}")
    private String  fileUrlEC;
    @Value("${file.uploadUrlScreenImg}")
    private String  fileUrlScreenImg;

    public String addEC(MultipartFile file) throws Exception{
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println("localhost: " + localHost);
        System.out.println("getHostAddress:  " + localHost.getHostAddress());
        System.out.println("getHostName:  " + localHost.getHostName());

        String originalFilename = file.getOriginalFilename(); //获取文件名
        String type = FileUtil.extName(originalFilename);//
        long size = file.getSize();//大小

        String uuid = IdUtil.fastSimpleUUID();
        String fileUUID = uuid + StrUtil.DOT + type; //41b1076684904f9cb4a503fb028db94b.jpg
        Path uploadPath = Paths.get(fileUrlEC,  fileUUID);
        // 5. 创建父目录
        Files.createDirectories(uploadPath.getParent());
        try (ReadableByteChannel inputChannel = Channels.newChannel(file.getInputStream());
             FileChannel outputChannel = FileChannel.open(uploadPath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            outputChannel.transferFrom(inputChannel, 0, file.getSize());
        }
        //获取文件的md5
        //使用m5 避免重复上传相同的文件
//        String  md5 = SecureUtil.md5(uploadFile);

        return  "/EC/"+fileUUID;
    }

    public String addUserQRImg(MultipartFile file) throws Exception{
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println("localhost: " + localHost);
        System.out.println("getHostAddress:  " + localHost.getHostAddress());
        System.out.println("getHostName:  " + localHost.getHostName());



        String originalFilename = file.getOriginalFilename(); //获取文件名
        String type = FileUtil.extName(originalFilename);//
        long size = file.getSize();//大小



        String uuid = IdUtil.fastSimpleUUID();
        String fileUUID = uuid + StrUtil.DOT + type; //41b1076684904f9cb4a503fb028db94b.jpg
        Path uploadPath = Paths.get(fileUrlQR,  fileUUID);
        // 5. 创建父目录
        Files.createDirectories(uploadPath.getParent());
        try (ReadableByteChannel inputChannel = Channels.newChannel(file.getInputStream());
             FileChannel outputChannel = FileChannel.open(uploadPath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            outputChannel.transferFrom(inputChannel, 0, file.getSize());
        }
        //获取文件的md5
        //使用m5 避免重复上传相同的文件
//        String  md5 = SecureUtil.md5(uploadFile);

        return "/user/qrImg/"+fileUUID;
    }



//    public String adddeviceScreenImg(MultipartFile file, String roomId, String videoName, String deviceId) {
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("上传文件不能为空");
//        }
//        if (StrUtil.isBlank(roomId) || StrUtil.isBlank(videoName) || StrUtil.isBlank(deviceId)) {
//            throw new IllegalArgumentException("roomId、videoName、deviceId不能为空");
//        }
//
//        try {
//            // 1. 清理 videoName（去掉非法字符）
//            String safeVideoName = videoName.replaceAll("[\\\\/:*?\"<>|\\s]", "");
//
//            // 2. 获取文件后缀名（例如 jpg）
//            String originalFilename = file.getOriginalFilename();
//            String fileExt = FileUtil.extName(originalFilename);
//
//            // 3. 生成唯一文件名
//            String uuid = IdUtil.fastSimpleUUID();
//            String newFileName = deviceId + "kkkkkk" + uuid + "." + fileExt;
//
//            // 4. 构建存储路径
//            Path uploadPath = Paths.get(fileUrlScreenImg, roomId + safeVideoName, newFileName);
//
//            // 5. 创建父目录
//            Files.createDirectories(uploadPath.getParent());
//
//            // 6. 使用 NIO 快速保存文件
//            try (ReadableByteChannel inputChannel = Channels.newChannel(file.getInputStream());
//                 FileChannel outputChannel = FileChannel.open(uploadPath,
//                         StandardOpenOption.CREATE,
//                         StandardOpenOption.WRITE,
//                         StandardOpenOption.TRUNCATE_EXISTING)) {
//
//                outputChannel.transferFrom(inputChannel, 0, file.getSize());
//            }
//            // 7. 返回对外访问的路径
//            return "/screen/" + roomId + safeVideoName + "/" + newFileName;
//
//        } catch (IOException e) {
//            log.error("保存文件失败", e);
//            throw new RuntimeException("文件保存失败", e);
//        }
//    }

public String adddeviceScreenImg(MultipartFile file, String roomId, String videoName, String deviceId) {
    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("上传文件不能为空");
    }
    if (StrUtil.isBlank(roomId) || StrUtil.isBlank(videoName) || StrUtil.isBlank(deviceId)) {
        throw new IllegalArgumentException("roomId、videoName、deviceId不能为空");
    }

    try {
        // 1. 清理 videoName（去掉非法字符）
        String safeVideoName = videoName.replaceAll("[\\\\/:*?\"<>|\\s]", "");

        // 2. 获取文件后缀名（例如 jpg）
        String originalFilename = file.getOriginalFilename();
        String fileExt = FileUtil.extName(originalFilename);

        // 3. 生成唯一文件名
        String uuid = IdUtil.fastSimpleUUID();
        String newFileName = deviceId + "kkkkkk" + uuid + "." + fileExt;

        // 4. 构建存储路径
        Path uploadPath = Paths.get(fileUrlScreenImg, roomId + safeVideoName, newFileName);

        // 5. 创建父目录
        Files.createDirectories(uploadPath.getParent());

        // 6. 使用 transferTo 方法保存文件（推荐方式）
        file.transferTo(uploadPath.toFile());

        // 7. 返回对外访问的路径
        return "/screen/" + roomId + safeVideoName + "/" + newFileName;

    } catch (IOException e) {
        log.error("保存文件失败", e);
        throw new RuntimeException("文件保存失败", e);
    }
}




    @Async("fileTaskExecutor")  // 指定线程池
    public CompletableFuture<String> adddeviceScreenImgAsync(MultipartFile file,
                                                             String roomId,
                                                             String videoName,
                                                             String deviceId) {
        try {
            // 保留原文件处理逻辑
            String originalFilename = file.getOriginalFilename();
            String type = FileUtil.extName(originalFilename);
            videoName = videoName.replaceAll("[\\s\\\\/:*?\"<>|]", "");

            String uuid = IdUtil.fastSimpleUUID();
            String fileUUID = deviceId + "kkkkkk" + uuid + StrUtil.DOT + type;

            Path targetPath = Paths.get(fileUrlScreenImg, roomId, videoName, fileUUID);
            Files.createDirectories(targetPath.getParent());

            // 使用NIO异步写入
            try (InputStream is = file.getInputStream();
                 FileChannel channel = FileChannel.open(targetPath,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.WRITE)) {

                ByteBuffer buffer = ByteBuffer.allocateDirect(8192);  // 直接内存
                while (is.read(buffer.array()) > 0) {
                    buffer.flip();
                    channel.write(buffer);
                    buffer.clear();
                }
            }

            return CompletableFuture.completedFuture("/screen/"+roomId+videoName+"/"+ fileUUID);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
