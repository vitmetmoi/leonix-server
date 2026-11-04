package com.example.myapp.features.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:${mail.username:}}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String fullName) {
        log.info("Preparing welcome email for recipient: {}", toEmail);
        try {
            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@jencastudio.com";

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(sender, "Jenca Studio / LeoNix");
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn gia nhập LeoNix / Jenca Studio!");

            String displayName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e5e1d7; border-radius: 12px; background-color: #f9f5ec; color: #102a32;">
                    <h2 style="color: #025066; margin-bottom: 16px;">Chào mừng %s đến với Studio!</h2>
                    <p style="font-size: 15px; line-height: 1.6; color: #495c62;">
                        Chúc mừng bạn đã tạo tài khoản thành công tại <strong>LeoNix / Jenca Studio</strong> — Xưởng sáng tạo video AI & hình ảnh cinematic hàng đầu.
                    </p>
                    <div style="margin: 24px 0; padding: 16px; background-color: #ffffff; border-radius: 8px; border: 1px solid #d3cdc1;">
                        <p style="margin: 0; font-size: 14px; color: #102a32;"><strong>Email đăng nhập:</strong> %s</p>
                        <p style="margin: 8px 0 0; font-size: 14px; color: #102a32;"><strong>Tài khoản:</strong> Thành viên tiêu chuẩn (Đã tặng credit khởi động)</p>
                    </div>
                    <p style="font-size: 14px; line-height: 1.6; color: #495c62;">
                        Hãy bắt đầu trải nghiệm dựng video prompt 5 lớp, tạo ảnh AI và quản lý workspace dự án ngay bây giờ.
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="http://localhost:3000/login" style="background-color: #025066; color: #ffffff; padding: 12px 28px; text-decoration: none; font-weight: bold; border-radius: 8px; display: inline-block;">
                            Bắt đầu sáng tạo ngay
                        </a>
                    </div>
                    <hr style="border: none; border-top: 1px solid #e5e1d7; margin: 24px 0;" />
                    <p style="font-size: 12px; color: #5a696e; text-align: center;">
                        © 2026 Jenca Studio · LeoNix Studio. Email này được gửi tự động, vui lòng không phản hồi trực tiếp.
                    </p>
                </div>
            """.formatted(displayName, toEmail);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Welcome email sent successfully to '{}'", toEmail);
        } catch (Exception e) {
            log.warn("Could not dispatch welcome email to '{}' (SMTP credentials may not be configured): {}", toEmail, e.getMessage());
        }
    }
}
