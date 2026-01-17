package com.ptn.strategy.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ptn.strategy.entity.StrategyGuide;
import com.ptn.strategy.mapper.StrategyGuideMapper;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.util.ArrayList;
import java.util.List;

@Service
public class BilibiliCrawlerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StrategyGuideMapper strategyGuideMapper;
    private final StringRedisTemplate redisTemplate;
    private final SqsTemplate sqsTemplate;
    private final S3Client s3Client;
    HttpHeaders headers;
    HttpEntity<String> entity;

    // Redis 中存储已抓取 ID 的 Key 名
    private static final String REDIS_KEY = "bilibili:processed:articles";
    private static final String SQS_QUEUE = "strategy-process-queue";
    private static final String BUCKET_Name = "ptn-strategy-raw-data";

    public BilibiliCrawlerService(StrategyGuideMapper strategyGuideMapper, 
                                  StringRedisTemplate redisTemplate, 
                                  SqsTemplate sqsTemplate,
                                  RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  S3Client s3Client) {
        this.strategyGuideMapper = strategyGuideMapper;
        this.redisTemplate = redisTemplate;
        this.sqsTemplate = sqsTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.s3Client = s3Client;

        // 创建 HttpHeaders 对象并填充从浏览器复制的信息
        headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Cookie", ""); // 替换为你复制的真实 Cookie
        headers.set("Referer", "https://search.bilibili.com/"); // 增加 Referer 模拟是从 B 站站内发起的搜索

        // 将 headers 包装进 HttpEntity
        entity = new HttpEntity<>(headers);
    }

    /**
     * 根据关键词搜索 B 站专栏 ID
     * @param keyword 搜索关键词，例如 "无期迷途 攻略"
     */
    public void searchArticleIds(String keyword) {
        List<String> articleIds = new ArrayList<>();

        // B 站 API 地址
        String url = String.format(
            "https://api.bilibili.com/x/web-interface/wbi/search/type?search_type=article&keyword=%s", 
            keyword
        );

        try {
            // 使用 exchange 发起请求
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );

            String response = responseEntity.getBody();
            
            // 解析 JSON 
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();

            if (code == 0) {
                JsonNode resultList = root.path("data").path("result");
                if (resultList.isArray()) {
                    for (JsonNode node : resultList) {
                        // 提取专栏 id
                        String id = node.path("id").asText();
                        System.out.println("爬取专栏ID: " + id);
                        if (!id.isEmpty()) {
                            articleIds.add(id);
                        }
                    }
                }
            } else {
                System.err.println("B 站 API 返回错误: " + root.path("message").asText());
            }
        } catch (Exception e) {
            System.err.println("搜索 B 站专栏失败: " + e.getMessage());
        }

        processFetchedIds(articleIds);
    }

    /**
     * 处理抓取到的 B 站 ID 列表
     */
    public void processFetchedIds(List<String> articleIds) {
        for (String articleId : articleIds) {
            // 1. Redis 去重检查
            // sadd 命令：如果元素已存在则返回 0，成功插入则返回 1
            Long addedCount = redisTemplate.opsForSet().add(REDIS_KEY, articleId);

            if (addedCount != null && addedCount > 0) {
                // 2. MySQL 持久化 (账本记录)
                StrategyGuide guide = new StrategyGuide();
                guide.setSourceId(articleId);
                guide.setTitleCn("B站攻略-" + articleId); // 初始占位标题
                guide.setSourceUrl("https://www.bilibili.com/opus/" + articleId);
                
                strategyGuideMapper.insert(guide);
                System.out.println("新攻略入库: " + articleId);

                // 3. 发送任务到 AWS SQS
                // 发送的是 ID，下游消费者会根据 ID 去爬取正文并调用 LLM
                sqsTemplate.send(to -> to.queue(SQS_QUEUE).payload(articleId));
                System.out.println("任务已派发至 SQS 队列: " + articleId);

            } else {
                // 已经在 Redis 中，说明之前处理过
                System.out.println("跳过已存在的攻略: " + articleId);
            }
        }
    } 

    /**
     * 根据专栏 ID 获取 JSON 并存入 S3
     */
    public void fetchAndSaveToS3(String articleId) {
        String url = "https://api.bilibili.com/x/article/view?id=" + articleId;
        
        try {
            // 1. 获取原始 JSON 字符串
            // 使用 exchange 发起请求
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );

            String jsonResponse = responseEntity.getBody();

            if (responseEntity != null) {
                // 2. 定义 S3 中的存储路径（Key）
                // 建议按日期或类型分类，例如：raw/articles/cv12345.json
                String s3Key = "raw/articles/cv" + articleId + ".json";

                // 3. 上传到 S3
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(BUCKET_Name)
                        .key(s3Key)
                        .contentType("application/json")
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonResponse));
                
                System.out.println("成功将专栏数据存入 S3: " + s3Key);
            }
        } catch (Exception e) {
            System.err.println("爬取或上传 S3 失败, ID: " + articleId + ", 错误: " + e.getMessage());
        }
    }
}
