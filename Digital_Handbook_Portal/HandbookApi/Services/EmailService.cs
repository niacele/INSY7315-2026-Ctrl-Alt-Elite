using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;
using Microsoft.Extensions.Configuration;

namespace HandbookApi.Services
{
    public class EmailService : IEmailService
    {
        private readonly IConfiguration _config;

        public EmailService(IConfiguration config)
        {
            _config = config;
        }

        public async Task SendWelcomeEmailAsync(string recipientEmail, string fullName, string temporaryPassword, int changePasswordHours = 1)
        {
            var emailSettings = _config.GetSection("EmailSettings");

            string senderEmail = emailSettings["SenderEmail"] ?? emailSettings["Username"] ?? string.Empty;
            string senderName = emailSettings["SenderName"] ?? "Digital Handbook Admin";

            if (string.IsNullOrWhiteSpace(senderEmail))
            {
                throw new ArgumentException("Sender email address is not configured in EmailSettings.");
            }

            if (string.IsNullOrWhiteSpace(recipientEmail))
            {
                throw new ArgumentException("Recipient email address cannot be empty.", nameof(recipientEmail));
            }

            var message = new MimeMessage();
            message.From.Add(new MailboxAddress(senderName, senderEmail));
            message.To.Add(new MailboxAddress(string.IsNullOrWhiteSpace(fullName) ? "Staff Member" : fullName, recipientEmail));
            message.Subject = "Welcome to Employee Digital Handbook - Account Credentials";

            string expirationTime = DateTime.UtcNow.AddHours(changePasswordHours).ToString("dd MMMM yyyy HH:mm") + " UTC";

            var bodyBuilder = new BodyBuilder
            {
                HtmlBody = $@"
                    <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>
                        <div style='background-color: #0056b3; color: white; padding: 20px; text-align: center;'>
                            <h2 style='margin: 0;'>Welcome to Employee Digital Handbook</h2>
                        </div>
                        <div style='padding: 24px; color: #333333;'>
                            <p>Dear <strong>{(string.IsNullOrWhiteSpace(fullName) ? "Staff Member" : fullName)}</strong>,</p>
                            <p>An account has been created for you on the Employee Digital Handbook system.</p>
                            
                            <div style='background-color: #f8f9fa; border-left: 4px solid #0056b3; padding: 16px; margin: 20px 0;'>
                                <p style='margin: 0 0 8px 0;'><strong>Work Email:</strong> {recipientEmail}</p>
                                <p style='margin: 0;'><strong>Temporary Password:</strong> <code style='font-size: 16px; background-color: #e9ecef; padding: 2px 6px; border-radius: 4px;'>{temporaryPassword}</code></p>
                            </div>

                            <p style='color: #d9534f; font-weight: bold;'>
                                ⚠️ For security purposes, you are required to log in to the Android App and change this password within {changePasswordHours} hour(s) (by {expirationTime}).
                            </p>

                            <p>Please log in using the mobile application and update your password immediately upon first launch.</p>
                            <br/>
                            <p>Kind regards,<br/><strong>Digital Handbook Administration Team</strong></p>
                        </div>
                    </div>"
            };

            message.Body = bodyBuilder.ToMessageBody();

            // Safe Port Resolution
            string rawPort = emailSettings["Port"] ?? "587";
            if (!int.TryParse(rawPort, out int portNumber))
            {
                portNumber = 587;
            }

            using var client = new SmtpClient();
            client.ServerCertificateValidationCallback = (s, c, h, e) => true;

            await client.ConnectAsync(
                emailSettings["SmtpServer"] ?? "smtp.gmail.com",
                portNumber,
                SecureSocketOptions.StartTls
            );

            await client.AuthenticateAsync(
                emailSettings["Username"] ?? senderEmail,
                emailSettings["Password"]
            );

            await client.SendAsync(message);
            await client.DisconnectAsync(true);
        }
    }
}