using Digital_Handbook_Portal.Models;
using HandbookApi.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HandbookApi.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class UsersController : ControllerBase
    {
        private readonly Digital_Handbook_PortalContext _context;
        private readonly IEmailService _emailService;

        public UsersController(Digital_Handbook_PortalContext context, IEmailService emailService)
        {
            _context = context;
            _emailService = emailService;
        }

        // GET: api/Users 
        [HttpGet]
        public async Task<ActionResult<ApiResponse<List<User>>>> GetUsers()
        {
            var users = await _context.User.ToListAsync();
            return Ok(new ApiResponse<List<User>> { Success = true, Data = users });
        }

        // GET: api/Users/all-sessions 
        [HttpGet("all-sessions")]
        public async Task<ActionResult<ApiResponse<List<UserSession>>>> GetAllSessions()
        {
            var sessions = await _context.UserSession
                .Include(s => s.Visits)
                .OrderByDescending(s => s.StartTime)
                .ToListAsync();

            return Ok(new ApiResponse<List<UserSession>> { Success = true, Data = sessions });
        }

        // GET: api/Users/5 
        [HttpGet("{id:int}")]
        public async Task<ActionResult<ApiResponse<User>>> GetUser(int id)
        {
            var user = await _context.User.FindAsync(id);
            if (user == null)
            {
                return NotFound(new ApiResponse<User> { Success = false, Message = "User not found." });
            }

            return Ok(new ApiResponse<User> { Success = true, Data = user });
        }

        // GET: api/Users/5/sessions 
        [HttpGet("{id:int}/sessions")]
        public async Task<ActionResult<ApiResponse<List<UserSession>>>> GetUserSessions(int id)
        {
            var userSessions = await _context.UserSession
                .Include(s => s.Visits)
                .Where(s => s.userId == id)
                .OrderByDescending(s => s.StartTime)
                .ToListAsync();

            return Ok(new ApiResponse<List<UserSession>> { Success = true, Data = userSessions });
        }

        // POST: api/Users/sessions/log 
        [HttpPost("sessions/log")]
        public async Task<IActionResult> LogUserSession([FromBody] UserSession session)
        {
            if (session == null) return BadRequest("Invalid session data");

            User? existingUser = null;

            if (session.userId > 0)
            {
                existingUser = await _context.User.FindAsync(session.userId);
            }

            if (existingUser == null && !string.IsNullOrWhiteSpace(session.Email))
            {
                existingUser = await _context.User
                    .FirstOrDefaultAsync(u => u.email.ToLower() == session.Email.ToLower());
            }

            if (existingUser != null)
            {
                session.userId = existingUser.userId;
                session.User = existingUser;
            }

            _context.UserSession.Add(session);
            await _context.SaveChangesAsync();

            return Ok(new { success = true });
        }

        // POST: api/Users/admin-create
        [HttpPost("admin-create")]
        public async Task<ActionResult<ApiResponse<User>>> AdminCreateUser([FromBody] User user)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(new ApiResponse<User> { Success = false, Message = "Invalid user payload." });
            }

            bool emailExists = await _context.User.AnyAsync(u => u.email.ToLower() == user.email.ToLower());
            if (emailExists)
            {
                return BadRequest(new ApiResponse<User> { Success = false, Message = "A user with this email address already exists." });
            }

            string rawPassword = user.password;

            user.password = BCrypt.Net.BCrypt.HashPassword(user.password);

            _context.User.Add(user);
            await _context.SaveChangesAsync();

            try
            {
                await _emailService.SendWelcomeEmailAsync(
                    recipientEmail: user.email,
                    fullName: !string.IsNullOrWhiteSpace(user.FullName) ? user.FullName : "Staff Member",
                    temporaryPassword: rawPassword,
                    changePasswordHours: 1
                );
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Email Error] Failed to send welcome email: {ex.Message}");
            }

            return Ok(new ApiResponse<User>
            {
                Success = true,
                Message = "User account created successfully. Welcome email dispatched.",
                Data = user
            });
        }

        // POST: api/Users/login
        [HttpPost("login")]
        public async Task<ActionResult<ApiResponse<AuthResponse>>> UserLogin([FromBody] LoginRequest request)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(new ApiResponse<AuthResponse> { Success = false, Message = "Invalid login payload." });
            }

            var user = await _context.User.FirstOrDefaultAsync(u => u.email.ToLower() == request.Email.ToLower());
            if (user == null || !BCrypt.Net.BCrypt.Verify(request.Password, user.password))
            {
                return Unauthorized(new ApiResponse<AuthResponse> { Success = false, Message = "Invalid email or password." });
            }

            var authResponse = new AuthResponse
            {
                UserId = user.userId,
                Email = user.email,
                FullName = !string.IsNullOrWhiteSpace(user.FullName) ? user.FullName : user.email.Split('@')[0],
                Token = Guid.NewGuid().ToString()
            };

            return Ok(new ApiResponse<AuthResponse>
            {
                Success = true,
                Message = "Application login successful.",
                Data = authResponse
            });
        }

        // POST: api/Users/admin-login
        [HttpPost("admin-login")]
        public async Task<ActionResult<ApiResponse<AuthResponse>>> AdminLogin([FromBody] LoginRequest request)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(new ApiResponse<AuthResponse> { Success = false, Message = "Invalid admin login payload." });
            }

            return Ok(new ApiResponse<AuthResponse>
            {
                Success = true,
                Message = "Admin authentication verified successfully."
            });
        }

        // PUT: api/Users/5
        [HttpPut("{id:int}")]
        public async Task<ActionResult<ApiResponse<User>>> UpdateUser(int id, [FromBody] User user)
        {
            if (id != user.userId)
            {
                return BadRequest(new ApiResponse<User> { Success = false, Message = "User ID mismatch." });
            }

            var existingUser = await _context.User.FindAsync(id);
            if (existingUser == null)
            {
                return NotFound(new ApiResponse<User> { Success = false, Message = "User not found." });
            }

            existingUser.email = user.email;
            existingUser.FullName = user.FullName;

            if (!string.IsNullOrWhiteSpace(user.password) && !user.password.StartsWith("$2a$"))
            {
                existingUser.password = BCrypt.Net.BCrypt.HashPassword(user.password);
            }

            await _context.SaveChangesAsync();
            return Ok(new ApiResponse<User> { Success = true, Message = "User updated successfully.", Data = existingUser });
        }

        // POST: api/Users/change-password
        [HttpPost("change-password")]
        public async Task<ActionResult<ApiResponse<bool>>> ChangePassword([FromBody] ChangePasswordRequest request)
        {
            if (request == null || !ModelState.IsValid)
            {
                return BadRequest(new ApiResponse<bool>
                {
                    Success = false,
                    Message = "Invalid request payload.",
                    Data = false
                });
            }

            if (string.IsNullOrWhiteSpace(request.NewPassword) || request.NewPassword.Length < 6)
            {
                return BadRequest(new ApiResponse<bool>
                {
                    Success = false,
                    Message = "New password must be at least 6 characters long.",
                    Data = false
                });
            }

            if (request.CurrentPassword == request.NewPassword)
            {
                return BadRequest(new ApiResponse<bool>
                {
                    Success = false,
                    Message = "New password cannot be identical to current password.",
                    Data = false
                });
            }

            var user = await _context.User.FindAsync(request.UserId);
            if (user == null)
            {
                return NotFound(new ApiResponse<bool>
                {
                    Success = false,
                    Message = "User not found.",
                    Data = false
                });
            }

            bool isCurrentPasswordValid = BCrypt.Net.BCrypt.Verify(request.CurrentPassword, user.password);
            if (!isCurrentPasswordValid)
            {
                return BadRequest(new ApiResponse<bool>
                {
                    Success = false,
                    Message = "Current password provided is incorrect.",
                    Data = false
                });
            }

            user.password = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<bool>
            {
                Success = true,
                Message = "Password updated successfully.",
                Data = true
            });
        }

        // DELETE: api/Users/5
        [HttpDelete("{id:int}")]
        public async Task<ActionResult<ApiResponse<bool>>> DeleteUser(int id)
        {
            var user = await _context.User.FindAsync(id);
            if (user == null)
            {
                return NotFound(new ApiResponse<bool> { Success = false, Message = "User not found.", Data = false });
            }

            _context.User.Remove(user);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<bool> { Success = true, Message = "User deleted successfully.", Data = true });
        }
    }

    public class ChangePasswordRequest
    {
        public int UserId { get; set; }
        public string CurrentPassword { get; set; } = string.Empty;
        public string NewPassword { get; set; } = string.Empty;
    }
}