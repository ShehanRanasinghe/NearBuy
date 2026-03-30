package com.example.nearbuy.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.nearbuy.BuildConfig;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * EmailService – sends transactional emails from the NearBuy Gmail account.
 *
 * Currently used for: OTP verification codes during customer registration.
 *
 * How it works:
 *   1. SMTP credentials come from the .env file, injected into BuildConfig at build time.
 *   2. All network I/O runs on a background executor – never the UI thread.
 *   3. The result callback always fires on the Android main thread so callers can
 *      update the UI directly without runOnUiThread().
 *
 * Gmail setup:
 *   • Generate a Gmail App Password at myaccount.google.com/apppasswords
 *   • Set SMTP_EMAIL and SMTP_PASSWORD in the .env file at the project root
 */
public final class EmailService {

    private static final String TAG = "NearBuy.EmailService";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private static final Handler         mainHandler = new Handler(Looper.getMainLooper());

    private EmailService() {}

    // ── Callback ───────────────────────────────────────────────────────────────

    /** Both methods always called on the Android main thread. */
    public interface EmailCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Sends a 6-digit OTP to the customer's email via Gmail SMTP.
     * Runs on a background thread; callback delivered on the main thread.
     *
     * @param toEmail   recipient email address
     * @param toName    recipient's display name (personalises the email greeting)
     * @param otpCode   the 6-digit code to embed in the message
     * @param callback  result callback – called on the main thread
     */
    public static void sendOtpEmail(String toEmail,
                                    String toName,
                                    String otpCode,
                                    EmailCallback callback) {

        final String fromEmail    = BuildConfig.SMTP_EMAIL;
        final String fromPassword = BuildConfig.SMTP_PASSWORD;

        if (fromEmail == null || fromEmail.isEmpty()
                || fromPassword == null || fromPassword.isEmpty()) {
            Log.w(TAG, "SMTP credentials not configured in .env – skipping email send.");
            if (callback != null)
                mainHandler.post(() -> callback.onError(
                        new IllegalStateException("SMTP_EMAIL / SMTP_PASSWORD not set in .env")));
            return;
        }

        executor.execute(() -> {
            try {
                // ── SMTP connection settings (Gmail TLS port 587) ──────────────
                Properties props = new Properties();
                props.put("mail.smtp.auth",                "true");
                props.put("mail.smtp.starttls.enable",     "true");
                props.put("mail.smtp.host",                SMTP_HOST);
                props.put("mail.smtp.port",                SMTP_PORT);
                props.put("mail.smtp.ssl.protocols",       "TLSv1.2");
                props.put("mail.smtp.ssl.trust",           SMTP_HOST);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromEmail, fromPassword);
                    }
                });

                // ── Build HTML message ─────────────────────────────────────────
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail, "NearBuy"));
                message.addRecipient(Message.RecipientType.TO,
                        new InternetAddress(toEmail, toName != null ? toName : "Customer"));
                message.setSubject("NearBuy – Your Verification Code: " + otpCode);

                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(buildEmailHtml(toName, otpCode), "text/html; charset=utf-8");

                MimeMultipart multipart = new MimeMultipart("alternative");
                multipart.addBodyPart(htmlPart);
                message.setContent(multipart);

                Transport.send(message);
                Log.d(TAG, "OTP email delivered to: " + toEmail);

                if (callback != null) mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e(TAG, "Failed to send OTP email to: " + toEmail, e);
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    // ── HTML Template ──────────────────────────────────────────────────────────

    /**
     * Builds a professional HTML email for the OTP code.
     * Colours match the NearBuy customer app brand: teal (#0097A7) + orange (#FF6D00).
     *
     * @param name    customer's display name (personalises the greeting)
     * @param otp     the 6-digit verification code
     * @return        full HTML string ready to set as email body
     */
    private static String buildEmailHtml(String name, String otp) {
        String displayName = (name != null && !name.isEmpty()) ? name : "Customer";

        // Split the 6 digits so each can be shown in its own styled box
        String d1 = String.valueOf(otp.charAt(0));
        String d2 = String.valueOf(otp.charAt(1));
        String d3 = String.valueOf(otp.charAt(2));
        String d4 = String.valueOf(otp.charAt(3));
        String d5 = String.valueOf(otp.charAt(4));
        String d6 = String.valueOf(otp.charAt(5));

        return "<!DOCTYPE html>"
            + "<html lang='en'><head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1.0'>"
            + "<title>NearBuy – Email Verification</title>"
            + "</head>"
            + "<body style='margin:0;padding:0;background-color:#E0F7FA;"
            +   "font-family:Arial,Helvetica,sans-serif;'>"

            // ── Outer wrapper ────────────────────────────────────────────
            + "<table width='100%' cellpadding='0' cellspacing='0'"
            +   " style='background-color:#E0F7FA;padding:40px 20px;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0'"
            +   " style='max-width:600px;width:100%;'>"

            // ── Header (teal gradient) ───────────────────────────────────
            + "<tr><td style='background:linear-gradient(135deg,#0097A7 0%,#006064 100%);"
            +   "border-radius:12px 12px 0 0;padding:36px 40px;text-align:center;'>"
            + "<div style='display:inline-block;background:#FF6D00;border-radius:50%;"
            +   "width:60px;height:60px;line-height:60px;text-align:center;"
            +   "font-size:28px;margin-bottom:16px;'>🛒</div>"
            + "<h1 style='color:#ffffff;margin:0;font-size:26px;"
            +   "font-weight:700;letter-spacing:1px;'>NearBuy</h1>"
            + "<p style='color:#B2EBF2;margin:6px 0 0;font-size:14px;'>Customer App</p>"
            + "</td></tr>"

            // ── Body (white card) ────────────────────────────────────────
            + "<tr><td style='background:#ffffff;padding:40px;'>"
            + "<p style='color:#1a1a2e;font-size:16px;margin:0 0 8px;'>"
            +   "Hello, <strong>" + displayName + "</strong> 👋</p>"
            + "<p style='color:#6b7280;font-size:15px;margin:0 0 28px;line-height:1.6;'>"
            +   "Thank you for joining <strong>NearBuy</strong>! Use the 6-digit verification "
            +   "code below to confirm your email address and complete your registration."
            + "</p>"

            // ── OTP code box (teal gradient) ─────────────────────────────
            + "<div style='background:linear-gradient(135deg,#0097A7 0%,#006064 100%);"
            +   "border-radius:12px;padding:32px 20px;text-align:center;margin-bottom:28px;'>"
            + "<p style='color:#B2EBF2;font-size:13px;text-transform:uppercase;"
            +   "letter-spacing:2px;margin:0 0 16px;'>Your Verification Code</p>"
            + "<div style='display:inline-block;'>"
            + otpDigitBox(d1) + otpDigitBox(d2) + otpDigitBox(d3)
            + "<span style='color:#B2EBF2;font-size:28px;font-weight:bold;margin:0 4px;'>–</span>"
            + otpDigitBox(d4) + otpDigitBox(d5) + otpDigitBox(d6)
            + "</div>"
            + "<p style='color:#B2EBF2;font-size:13px;margin:20px 0 0;'>"
            +   "⏱ This code expires in <strong style='color:#ffffff;'>10 minutes</strong>."
            + "</p>"
            + "</div>"

            // ── Security tip (orange-tinted) ─────────────────────────────
            + "<div style='background:#FFF3E0;border-left:4px solid #FF6D00;"
            +   "border-radius:4px;padding:16px;margin-bottom:28px;'>"
            + "<p style='color:#6b7280;font-size:13px;margin:0;line-height:1.6;'>"
            +   "🔒 <strong style='color:#1a1a2e;'>Security tip:</strong> "
            +   "Never share this code with anyone. NearBuy staff will never ask for your OTP."
            + "</p>"
            + "</div>"

            + "<p style='color:#6b7280;font-size:14px;margin:0;line-height:1.6;'>"
            +   "If you did not create a NearBuy account, please ignore this email."
            + "</p>"
            + "</td></tr>"

            // ── Footer (dark teal) ───────────────────────────────────────
            + "<tr><td style='background:#006064;border-radius:0 0 12px 12px;"
            +   "padding:24px 40px;text-align:center;'>"
            + "<p style='color:#B2EBF2;font-size:12px;margin:0;line-height:1.8;'>"
            +   "© 2025 NearBuy · Customer App<br>"
            +   "This is an automated message – please do not reply."
            + "</p>"
            + "</td></tr>"

            + "</table></td></tr></table>"
            + "</body></html>";
    }

    /** Renders a single OTP digit inside a styled inline box for the HTML email. */
    private static String otpDigitBox(String digit) {
        return "<span style='display:inline-block;"
                + "background:rgba(255,255,255,0.15);"
                + "color:#ffffff;font-size:28px;font-weight:700;"
                + "width:44px;height:52px;line-height:52px;"
                + "border-radius:8px;margin:0 3px;"
                + "border:1px solid rgba(255,255,255,0.25);"
                + "text-align:center;'>"
                + digit + "</span>";
    }

    /** @deprecated Use {@link #sendOtpEmail(String, String, String, EmailCallback)} with name. */
    @Deprecated
    public static void sendOtpEmail(String toEmail, String otpCode, EmailCallback callback) {
        sendOtpEmail(toEmail, null, otpCode, callback);
    }
}
