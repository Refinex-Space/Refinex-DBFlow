package com.refinex.dbflow.admin.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Locale;
import java.util.Set;

/**
 * React 管理端 SPA 路由控制器。
 *
 * @author refinex
 */
@Controller
public class AdminSpaController {

    /**
     * React 管理端入口转发地址。
     */
    private static final String ADMIN_INDEX_FORWARD = "forward:/admin/index.html";

    /**
     * React 管理端静态资源 classpath 根目录。
     */
    private static final String ADMIN_RESOURCE_LOCATION = "static/admin";

    /**
     * 不应回退到 SPA 入口的静态资源扩展名。
     */
    private static final Set<String> STATIC_RESOURCE_EXTENSIONS = Set.of(
            "css", "eot", "gif", "html", "ico", "jpeg", "jpg", "js", "json", "map", "png", "svg", "ttf",
            "txt", "webp", "woff", "woff2");

    /**
     * 将 React 管理端根路径转发到 SPA 入口。
     *
     * @return SPA 入口转发地址
     */
    @GetMapping({"/admin", "/admin/"})
    public String forwardRoot() {
        return ADMIN_INDEX_FORWARD;
    }

    /**
     * 处理 React 管理端子路由和静态资源路径。
     *
     * @param path `/admin` 之后的相对路径
     * @return SPA 入口转发地址或静态资源响应
     */
    @GetMapping("/admin/{*path}")
    public Object forwardRoute(@PathVariable("path") String path) {
        if (isAdminApiPath(path)) {
            return ResponseEntity.notFound().build();
        }
        if (isStaticResourcePath(path)) {
            return staticResource(path);
        }
        return ADMIN_INDEX_FORWARD;
    }

    /**
     * 读取 React 管理端静态资源。
     *
     * @param path `/admin` 之后的相对路径
     * @return 静态资源响应，找不到时返回 404
     */
    private ResponseEntity<Resource> staticResource(String path) {
        String normalizedPath = normalizeResourcePath(path);
        if (normalizedPath == null) {
            return ResponseEntity.notFound().build();
        }
        ClassPathResource resource = new ClassPathResource(ADMIN_RESOURCE_LOCATION + normalizedPath);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    /**
     * 判断路径是否是静态资源路径。
     *
     * @param path `/admin` 之后的相对路径
     * @return 如果路径扩展名属于静态资源则返回 true
     */
    private boolean isStaticResourcePath(String path) {
        String normalizedPath = normalizeResourcePath(path);
        if (normalizedPath == null) {
            return false;
        }
        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        int dotIndex = normalizedPath.lastIndexOf('.');
        if (dotIndex <= lastSlashIndex || dotIndex == normalizedPath.length() - 1) {
            return false;
        }
        String extension = normalizedPath.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return STATIC_RESOURCE_EXTENSIONS.contains(extension);
    }

    /**
     * 规范化资源路径，阻止路径穿越。
     *
     * 判断路径是否命中管理端 JSON API。
     *
     * @param path `/admin` 之后的相对路径
     * @return 如果是管理端 API 路径则返回 true
     */
    private boolean isAdminApiPath(String path) {
        String normalizedPath = normalizeResourcePath(path);
        return normalizedPath != null && normalizedPath.startsWith("/api/");
    }

    /**
     * 规范化资源路径，阻止路径穿越。
     *
     * @param path `/admin` 之后的相对路径
     * @return 以 `/` 开头的规范路径，非法路径返回 null
     */
    private String normalizeResourcePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (normalizedPath.contains("..")) {
            return null;
        }
        return normalizedPath;
    }
}
