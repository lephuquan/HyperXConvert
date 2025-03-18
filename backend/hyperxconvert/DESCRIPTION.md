Kịch bản phát triển Backend cho dự án HyperXConvert
1️⃣ Cấu trúc dự án và công nghệ sử dụng
Công nghệ chính

Framework backend: Java Spring Boot
Database: PostgreSQL
File Storage: AWS S3
Queue system: RabbitMQ
Authentication: JWT, OAuth2
Email Service: SendGrid (với fallback Gmail SMTP)
Payment Integration: Stripe, MoMo, VNPay
CI/CD: Docker, GitHub Actions
Monitoring: Telegram Bot (thông báo lỗi)

Cấu trúc project
com.hyperxconvert.api
  ├─ config/           # Cấu hình hệ thống
  ├─ controller/       # REST API endpoints
  ├─ service/          # Business logic
  ├─ repository/       # Data access
  ├─ model/            # Entities & DTOs
  ├─ queue/            # RabbitMQ processors
  ├─ converter/        # File conversion handlers
  ├─ security/         # JWT & authentication
  ├─ exception/        # Exception handling
  ├─ util/             # Helper classes
  └─ notification/     # Email & Telegram services

2️⃣ Thiết kế cơ sở dữ liệu
Các bảng chính

users

id, email, password_hash, full_name, created_at, updated_at
role (ADMIN, USER), status (ACTIVE, INACTIVE)
login_provider (EMAIL, GOOGLE, FACEBOOK)


subscriptions

id, user_id, plan_type (BASIC, PRO, ULTIMATE)
start_date, end_date, status (ACTIVE, CANCELLED, EXPIRED)
payment_id, auto_renew (boolean)


credits

id, user_id, amount, created_at
transaction_id, status (ACTIVE, USED, EXPIRED)


conversions

id, user_id, original_file_name, original_file_size
original_format, target_format, created_at
status (PENDING, PROCESSING, COMPLETED, FAILED)
s3_original_path, s3_converted_path, expiry_date
credits_used, error_message (nếu có)


payments

id, user_id, amount, currency
payment_method (STRIPE, MOMO, VNPAY, PAYPAL)
status (PENDING, COMPLETED, FAILED, REFUNDED)
transaction_id, created_at, invoice_url


api_keys

id, user_id, api_key, created_at
last_used_at, status (ACTIVE, REVOKED)
rate_limit_daily



3️⃣ RESTful API Endpoints
Authentication API

POST /api/v1/auth/register - Đăng ký tài khoản
POST /api/v1/auth/login - Đăng nhập
POST /api/v1/auth/oauth/{provider} - Đăng nhập OAuth (Google/Facebook)
POST /api/v1/auth/refresh-token - Làm mới token
POST /api/v1/auth/forgot-password - Quên mật khẩu
POST /api/v1/auth/reset-password - Đặt lại mật khẩu

User API

GET /api/v1/users/me - Thông tin người dùng
PUT /api/v1/users/me - Cập nhật thông tin
GET /api/v1/users/me/conversions - Lịch sử chuyển đổi
GET /api/v1/users/me/payments - Lịch sử thanh toán
GET /api/v1/users/me/credits - Thông tin credits

Conversion API

POST /api/v1/conversions - Tạo conversion mới
GET /api/v1/conversions/{id} - Kiểm tra trạng thái
GET /api/v1/conversions/{id}/download - Tải xuống file đã chuyển đổi
DELETE /api/v1/conversions/{id} - Xóa lịch sử chuyển đổi

Payment API

POST /api/v1/payments/checkout - Tạo checkout session
POST /api/v1/payments/webhook - Webhook từ cổng thanh toán
GET /api/v1/payments/plans - Danh sách gói dịch vụ
POST /api/v1/payments/buy-credits - Mua credits

Subscription API

POST /api/v1/subscriptions - Đăng ký gói dịch vụ
PUT /api/v1/subscriptions/{id} - Cập nhật gói dịch vụ
DELETE /api/v1/subscriptions/{id} - Hủy gói dịch vụ

Admin API

GET /api/v1/admin/users - Danh sách người dùng
GET /api/v1/admin/conversions - Danh sách chuyển đổi
GET /api/v1/admin/payments - Danh sách thanh toán
POST /api/v1/admin/notifications - Gửi thông báo

Developer API

POST /api/v1/developer/api-keys - Tạo API key
GET /api/v1/developer/api-keys - Liệt kê API keys
DELETE /api/v1/developer/api-keys/{id} - Xóa API key

4️⃣ Xử lý chuyển đổi file
Luồng xử lý file

1. Upload & Validation
// FileUploadService.java
public FileUploadResponse uploadFile(MultipartFile file, String targetFormat, User user) {
    // Kiểm tra giới hạn dung lượng và định dạng file
    validateFileSize(file, user.getSubscription());
    validateFileFormat(file, targetFormat);
    
    // Upload file lên S3
    String s3Path = s3Service.uploadFile(file);
    
    // Tạo conversion request
    Conversion conversion = conversionRepository.save(new Conversion(
        user, file.getOriginalFilename(), file.getSize(),
        getFileExtension(file), targetFormat,
        s3Path, null, ConversionStatus.PENDING));
    
    // Gửi vào RabbitMQ queue
    conversionQueueService.enqueueConversion(conversion);
    
    return new FileUploadResponse(conversion.getId(), ConversionStatus.PENDING);
}

2. Queue Processing
// ConversionConsumer.java
@RabbitListener(queues = "conversion-queue")
public void processConversion(ConversionMessage message) {
    try {
        // Cập nhật trạng thái
        conversionService.updateStatus(message.getId(), ConversionStatus.PROCESSING);
        
        // Tải file từ S3
        File sourceFile = s3Service.downloadToTemp(message.getS3OriginalPath());
        
        // Chuyển đổi file
        File convertedFile = fileConverterFactory
            .getConverter(message.getOriginalFormat(), message.getTargetFormat())
            .convert(sourceFile);
        
        // Upload kết quả lên S3
        String convertedS3Path = s3Service.uploadFile(convertedFile);
        
        // Cập nhật thông tin
        conversionService.completeConversion(message.getId(), convertedS3Path);
        
        // Gửi thông báo hoàn tất
        notificationService.sendConversionCompleteNotification(message.getUserId(), message.getId());
    } catch (Exception e) {
        // Xử lý lỗi và gửi thông báo
        conversionService.failConversion(message.getId(), e.getMessage());
        telegramNotificationService.sendErrorAlert("Conversion failed", e);
    }
}
Factory Pattern cho File Converters
javaCopy// FileConverterFactory.java
public interface FileConverter {
    File convert(File source) throws ConversionException;
}

public class PdfToDocxConverter implements FileConverter {
    @Override
    public File convert(File source) throws ConversionException {
        // Xử lý chuyển đổi từ PDF sang DOCX
    }
}

public class FileConverterFactory {
    private Map<String, FileConverter> converters = new HashMap<>();
    
    public FileConverter getConverter(String sourceFormat, String targetFormat) {
        String key = sourceFormat + "-to-" + targetFormat;
        return converters.get(key);
    }
}


5️⃣ Bảo mật và Rate Limiting
JWT Authentication
javaCopy// JwtTokenProvider.java
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
Rate Limiting với Bucket4j
javaCopy// RateLimitingFilter.java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final UserService userService;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        User user = userService.findByApiKey(apiKey);
        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        
        Bucket bucket = buckets.computeIfAbsent(apiKey, k -> createNewBucket(user));
        
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
        }
    }
    
    private Bucket createNewBucket(User user) {
        long rateLimit = user.getSubscription().getRateLimit();
        Bandwidth limit = Bandwidth.classic(rateLimit, Refill.intervally(rateLimit, Duration.ofDays(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
6️⃣ Thanh toán và Gói dịch vụ
Stripe Payment Integration
javaCopy// StripePaymentService.java
@Service
public class StripePaymentService implements PaymentService {
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @Override
    public PaymentResponse createCheckoutSession(PaymentRequest request) {
        try {
            Stripe.apiKey = stripeApiKey;
            
            List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
            
            lineItems.add(
                SessionCreateParams.LineItem.builder()
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(request.getAmount() * 100L)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(request.getDescription())
                                    .build())
                            .build())
                    .setQuantity(1L)
                    .build());
            
            SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.getSuccessUrl())
                .setCancelUrl(request.getCancelUrl())
                .addAllLineItem(lineItems)
                .build();
            
            Session session = Session.create(params);
            
            // Lưu thông tin payment vào database
            Payment payment = paymentRepository.save(new Payment(
                request.getUserId(), request.getAmount(), "USD",
                PaymentMethod.STRIPE, PaymentStatus.PENDING,
                session.getId(), new Date()));
            
            return new PaymentResponse(payment.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PaymentException("Failed to create payment session", e);
        }
    }
    
    @Override
    public void handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().get();
                
                // Cập nhật trạng thái payment
                Payment payment = paymentRepository.findByTransactionId(session.getId());
                payment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);
                
                // Cập nhật credits hoặc subscription
                userService.processPaymentCompletion(payment);
            }
        } catch (Exception e) {
            telegramNotificationService.sendErrorAlert("Stripe webhook error", e);
            throw new PaymentException("Webhook processing failed", e);
        }
    }
}
MoMo Payment Integration
javaCopy// MomoPaymentService.java
@Service
public class MomoPaymentService implements PaymentService {
    @Value("${momo.partner.code}")
    private String partnerCode;
    
    @Value("${momo.access.key}")
    private String accessKey;
    
    @Value("${momo.secret.key}")
    private String secretKey;
    
    @Override
    public PaymentResponse createCheckoutSession(PaymentRequest request) {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderId = UUID.randomUUID().toString();
            
            String redirectUrl = request.getSuccessUrl();
            String ipnUrl = request.getWebhookUrl();
            String amount = String.valueOf(request.getAmount());
            String orderInfo = request.getDescription();
            
            // Tạo signature
            String rawHash = "partnerCode=" + partnerCode +
                "&accessKey=" + accessKey +
                "&requestId=" + requestId +
                "&amount=" + amount +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&redirectUrl=" + redirectUrl +
                "&ipnUrl=" + ipnUrl;
            
            String signature = HashUtils.hmacSHA256(rawHash, secretKey);
            
            // Gửi request đến MoMo API
            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", redirectUrl);
            requestBody.put("ipnUrl", ipnUrl);
            requestBody.put("requestType", "captureWallet");
            requestBody.put("signature", signature);
            
            // Gọi API MoMo và parse response
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                    .uri(URI.create("https://test-payment.momo.vn/gw_payment/transactionProcessor"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build(), 
                HttpResponse.BodyHandlers.ofString());
            
            JSONObject jsonResponse = new JSONObject(response.body());
            
            // Lưu thông tin payment vào database
            Payment payment = paymentRepository.save(new Payment(
                request.getUserId(), request.getAmount(), "VND",
                PaymentMethod.MOMO, PaymentStatus.PENDING,
                orderId, new Date()));
            
            return new PaymentResponse(payment.getId(), jsonResponse.getString("payUrl"));
        } catch (Exception e) {
            throw new PaymentException("Failed to create MoMo payment", e);
        }
    }
}
7️⃣ Xử lý Email và Thông báo
Email Service với Fallback
javaCopy// EmailService.java
@Service
public class EmailService {
    private final JavaMailSender primaryMailSender; // SendGrid
    private final JavaMailSender fallbackMailSender; // Gmail SMTP
    private final AtomicBoolean useFallback = new AtomicBoolean(false);
    
    public void sendEmail(String to, String subject, String content) {
        if (useFallback.get()) {
            try {
                sendWithFallback(to, subject, content);
            } catch (Exception e) {
                telegramNotificationService.sendErrorAlert("Email fallback failed", e);
                throw new EmailException("Failed to send email", e);
            }
        } else {
            try {
                sendWithPrimary(to, subject, content);
            } catch (Exception e) {
                // Switch to fallback
                useFallback.set(true);
                telegramNotificationService.sendErrorAlert("Primary email service failed, switching to fallback", e);
                sendEmail(to, subject, content); // Retry with fallback
            }
        }
    }
    
    private void sendWithPrimary(String to, String subject, String content) {
        MimeMessage message = primaryMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true for HTML
        primaryMailSender.send(message);
    }
    
    private void sendWithFallback(String to, String subject, String content) {
        MimeMessage message = fallbackMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        fallbackMailSender.send(message);
    }
}
Telegram Notification Service
javaCopy// TelegramNotificationService.java
@Service
public class TelegramNotificationService {
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.chat.id}")
    private String chatId;
    
    public void sendErrorAlert(String errorTitle, Exception e) {
        String stackTrace = ExceptionUtils.getStackTrace(e);
        String shortStackTrace = stackTrace.length() > 500 
            ? stackTrace.substring(0, 500) + "..." 
            : stackTrace;
        
        String message = String.format(
            "🚨 [ERROR] - %s\n" +
            "📅 Time: %s\n" +
            "🖥 Server: %s\n" +
            "⚠ Chi tiết: %s\n\n" +
            "```\n%s\n```",
            errorTitle,
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss").format(LocalDateTime.now()),
            getHostName(),
            e.getMessage(),
            shortStackTrace
        );
        
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            String urlString = String.format(
                "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                botToken, chatId, encodedMessage);
            
            HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .GET()
                    .build(), 
                HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            log.error("Failed to send Telegram notification", ex);
        }
    }
    
    public void sendUserAlert(String title, String details) {
        // Similar implementation for user-related alerts
    }
    
    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
8️⃣ AWS S3 Storage & Lifecycle Management
S3 Service
javaCopy// S3Service.java
@Service
public class S3Service {
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    private final AmazonS3 s3Client;
    
    public S3Service(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }
    
    public String uploadFile(File file, String key) {
        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, key, file);
            s3Client.putObject(request);
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to upload file to S3", e);
        }
    }
    
    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            File tempFile = File.createTempFile("temp-", fileName);
            file.transferTo(tempFile);
            
            String key = "uploads/" + fileName;
            uploadFile(tempFile, key);
            
            tempFile.delete();
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to upload multipart file to S3", e);
        }
    }
    
    public File downloadToTemp(String key) {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key);
            
            // Xử lý download
            String fileName = key.substring(key.lastIndexOf('/') + 1);
            File tempFile = File.createTempFile("download-", fileName);
            
            try (S3ObjectInputStream inputStream = s3Object.getObjectContent();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            return tempFile;
        } catch (Exception e) {
            throw new StorageException("Failed to download file from S3", e);
        }
    }
    
    public void deleteFile(String key) {
        try {
            s3Client.deleteObject(bucketName, key);
        } catch (Exception e) {
            throw new StorageException("Failed to delete file from S3", e);
        }
    }
    
    // Cấu hình lifecycle policy
    public void setupLifecyclePolicy() {
        BucketLifecycleConfiguration.Rule standardUserRule = new BucketLifecycleConfiguration.Rule()
            .withId("DeleteAfter7Days")
            .withFilter(new LifecycleFilter(
                new LifecyclePrefixPredicate("uploads/standard/")))
            .withExpirationInDays(7)
            .withStatus(BucketLifecycleConfiguration.ENABLED);
        
        BucketLifecycleConfiguration.Rule premiumUserRule = new BucketLifecycleConfiguration.Rule()
            .withId("DeleteAfter30Days")
            .withFilter(new LifecycleFilter(
                new LifecyclePrefixPredicate("uploads/premium/")))
            .withExpirationInDays(30)
            .withStatus(BucketLifecycleConfiguration.ENABLED);
        
        BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration()
            .withRules(Arrays.asList(standardUserRule, premiumUserRule));
        
        s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
    }
}
9️⃣ WebSocket cho cập nhật trạng thái real-time
WebSocket Configuration
javaCopy// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}
Conversion Status Update
javaCopy// ConversionService.java
@Service
public class ConversionService {
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversionRepository conversionRepository;
    
    public void completeConversion(Long id, String convertedS3Path) {
        Conversion conversion = conversionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conversion not found"));
        
        conversion.setStatus(ConversionStatus.COMPLETED);
        conversion.setS3ConvertedPath(convertedS3Path);
        conversion.setExpiryDate(calculateExpiryDate(conversion.getUser()));
        
        conversionRepository.save(conversion);
        
        // Gửi thông báo qua WebSocket
        messagingTemplate.convertAndSend(
            "/topic/conversions/" + conversion.getUser().getId(),
            new ConversionStatusUpdate(id, ConversionStatus.COMPLETED)
        );
    }
    
    private Date calculateExpiryDate(User user) {
        // Tính thời gian hết hạn dựa trên loại gói
        int retentionDays = user.getSubscription().getPlan() == SubscriptionPlan.ULTIMATE ? 30 : 7;
        
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, retentionDays);
        return calendar.getTime();
    }
}
🔟 Scheduled Tasks
Scheduled File Cleanup
javaCopy// ScheduledTasks.java
@Component
public class ScheduledTasks {
    private final ConversionRepository conversionRepository;
    private final S3Service s3Service;
    
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM every day
    public void cleanupExpiredFiles() {
        List<Conversion> expiredConversions = conversionRepository.findByExpiryDateBeforeAndStatus(
            new Date(), ConversionStatus.COMPLETED);
        
        for (Conversion conversion : expiredConversions) {
            try {
                // Xóa file từ S3 nếu cần (bổ sung cho lifecycle policy)
                if (conversion.getS3ConvertedPath() != null) {
                    s3Service.deleteFile(conversion.getS3ConvertedPath());
                }
                
                // Cập nhật trạng thái
                conversion.setStatus(ConversionStatus.EXPIRED);
                conversionRepository.save(conversion);
            } catch (Exception e) {
                log.error("Failed to cleanup expired file: " + conversion.getId(), e);
            }
        }
    }
    
    @Scheduled(cron = "0 0 1 * * ?") // 1 AM every day
    public void checkSubscriptions() {
        List<Subscription> expiringSubscriptions = subscriptionRepository.findByEndDateBetween(
            new Date(), 
            DateUtils.addDays(new Date(), 3));
        
        for (Subscription subscription : expiringSubscriptions) {
            try {
                // Gửi email nhắc nhở
                User user = subscription.getUser();
                emailService.sendSubscriptionExpiringEmail(user.getEmail(), subscription);
            } catch (Exception e) {
                log.error("Failed to send subscription expiry notification", e);
            }
        }
    }
}
Application Configuration
application.yml
yamlCopyserver:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hyperxconvert
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: ${RABBITMQ_PASSWORD}
  mail:
    primary:
      host: api.sendgrid.com
      port: 587
      username: apikey
      password: ${SENDGRID_API_KEY}
    fallback:
      host: smtp.gmail.com
      port: 587
      username: ${GMAIL_USERNAME}
      password: ${GMAIL_PASSWORD}
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

aws:
  s3:
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
    region: ap-southeast-1
    bucket: hyperxconvert-files

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000 # 24 hours

payment:
  stripe:
    api-key: ${STRIPE_API_KEY}
    webhook-secret: ${STRIPE_WEBHOOK_SECRET}
  momo:
    partner-code: ${MOMO_PARTNER_CODE}
    access-key: ${MOMO_ACCESS_KEY}
    secret-key: ${MOMO_SECRET_KEY}
  vnpay:
    tmnCode