using Digital_Handbook_Portal.Models;
using Digital_Handbook_Portal.ViewModels;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HandbookApi.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class QuizzesController : ControllerBase
    {
        private readonly Digital_Handbook_PortalContext _context;

        public QuizzesController(Digital_Handbook_PortalContext context)
        {
            _context = context;
        }

        // GET: api/Quizzes
        [HttpGet]
        public async Task<ActionResult<ApiResponse<List<Quiz>>>> GetQuizzes()
        {
            var quizzes = await _context.Quiz
                .Include(q => q.questions)
                    .ThenInclude(q => q.options)
                .ToListAsync();

            return Ok(new ApiResponse<List<Quiz>> { Success = true, Data = quizzes });
        }

        // GET: api/Quizzes/results-summary
        [HttpGet("results-summary")]
        [Route("results-summary")]
        public async Task<ActionResult<ApiResponse<List<UserCpdSummaryViewModel>>>> GetResultsSummary()
        {
            try
            {
                // 1. Fetch raw attempts first to prevent EF translation exceptions
                var results = await _context.QuizResult
                    .Include(q => q.Quiz)
                    .Include(q => q.User)
                    .AsNoTracking()
                    .ToListAsync();

                if (results == null || !results.Any())
                {
                    return Ok(new ApiResponse<List<UserCpdSummaryViewModel>>
                    {
                        Success = true,
                        Data = new List<UserCpdSummaryViewModel>()
                    });
                }

                // 2. Perform aggregation in-memory
                var userSummaries = results
                    .GroupBy(q => q.UserId)
                    .Select(g =>
                    {
                        var firstUser = g.FirstOrDefault()?.User;
                        string userIdStr = g.Key.ToString();
                        string userEmail = !string.IsNullOrWhiteSpace(firstUser?.email)
                            ? firstUser.email
                            : $"User #{userIdStr}";

                        string derivedName = userEmail.Contains("@")
                            ? string.Join(" ", userEmail.Split('@')[0].Split('.', '_', '-').Select(s => s.Length > 0 ? char.ToUpper(s[0]) + s.Substring(1) : s))
                            : "Staff Member";

                        string displayName = !string.IsNullOrWhiteSpace(firstUser?.FullName)
                            ? firstUser.FullName
                            : derivedName;

                        return new UserCpdSummaryViewModel
                        {
                            UserId = userIdStr,
                            UserName = displayName,
                            Email = userEmail,
                            TotalCpdPoints = g.Sum(q => q.CpdPointsAwarded),
                            TotalQuizzesAttempted = g.Count(),
                            TotalQuizzesPassed = g.Count(q => q.IsPassed)
                        };
                    })
                    .OrderByDescending(u => u.TotalCpdPoints)
                    .ToList();

                return Ok(new ApiResponse<List<UserCpdSummaryViewModel>> { Success = true, Data = userSummaries });
            }
            catch (Exception ex)
            {
                return BadRequest(new ApiResponse<List<UserCpdSummaryViewModel>>
                {
                    Success = false,
                    Message = $"Error retrieving CPD summaries: {ex.Message}"
                });
            }
        }

        // GET: api/Quizzes/user-details/1
        [HttpGet("user-details/{userId:int}", Order = -1)]
        public async Task<ActionResult<ApiResponse<List<QuizResult>>>> GetUserQuizHistory(int userId)
        {
            var attempts = await _context.QuizResult
                .Include(q => q.Quiz)
                .Include(q => q.User)
                .Where(q => q.UserId == userId)
                .OrderByDescending(q => q.CompletedAt)
                .ToListAsync();

            return Ok(new ApiResponse<List<QuizResult>> { Success = true, Data = attempts });
        }

        // GET: api/Quizzes/5
        [HttpGet("{id:int}")]
        public async Task<ActionResult<ApiResponse<Quiz>>> GetQuiz(int id)
        {
            var quiz = await _context.Quiz
                .Include(q => q.questions)
                .ThenInclude(q => q.options)
                .FirstOrDefaultAsync(q => q.quizId == id);

            if (quiz == null)
            {
                return NotFound(new ApiResponse<Quiz> { Success = false, Message = "Quiz not found." });
            }

            return Ok(new ApiResponse<Quiz> { Success = true, Data = quiz });
        }

        // POST: api/Quizzes
        [HttpPost]
        public async Task<ActionResult<ApiResponse<Quiz>>> CreateQuiz([FromBody] Quiz quiz)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(new ApiResponse<Quiz> { Success = false, Message = "Invalid quiz data." });
            }

            quiz.createdAt = DateTime.UtcNow;
            if (quiz.questions != null)
            {
                quiz.totalQuestions = quiz.questions.Count;
            }

            _context.Quiz.Add(quiz);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<Quiz> { Success = true, Message = "Quiz created successfully.", Data = quiz });
        }

        // PUT: api/Quizzes/5
        [HttpPut("{id:int}")]
        public async Task<ActionResult<ApiResponse<Quiz>>> UpdateQuiz(int id, [FromBody] Quiz quiz)
        {
            if (id != quiz.quizId)
            {
                return BadRequest(new ApiResponse<Quiz> { Success = false, Message = "Quiz ID mismatch." });
            }

            var existingQuiz = await _context.Quiz
                .Include(q => q.questions)
                .ThenInclude(q => q.options)
                .FirstOrDefaultAsync(q => q.quizId == id);

            if (existingQuiz == null)
            {
                return NotFound(new ApiResponse<Quiz> { Success = false, Message = "Quiz not found." });
            }

            existingQuiz.title = quiz.title;
            existingQuiz.score = quiz.score;
            existingQuiz.passingScore = quiz.passingScore;
            existingQuiz.estimateTime = quiz.estimateTime;

            _context.Set<QuizQuestion>().RemoveRange(existingQuiz.questions);

            if (quiz.questions != null)
            {
                existingQuiz.questions = quiz.questions.Select(q => new QuizQuestion
                {
                    questionText = q.questionText,
                    options = q.options != null
                        ? q.options.Select(o => new QuizOption
                        {
                            optionText = o.optionText,
                            isCorrect = o.isCorrect
                        }).ToList()
                        : new List<QuizOption>()
                }).ToList();

                existingQuiz.totalQuestions = existingQuiz.questions.Count;
            }

            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<Quiz> { Success = true, Message = "Quiz updated successfully.", Data = existingQuiz });
        }

        // DELETE: api/Quizzes/5
        [HttpDelete("{id:int}")]
        public async Task<ActionResult<ApiResponse<bool>>> DeleteQuiz(int id)
        {
            var quiz = await _context.Quiz
                .Include(q => q.questions)
                .ThenInclude(q => q.options)
                .FirstOrDefaultAsync(q => q.quizId == id);

            if (quiz == null)
            {
                return NotFound(new ApiResponse<bool> { Success = false, Message = "Quiz not found.", Data = false });
            }

            _context.Quiz.Remove(quiz);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<bool> { Success = true, Message = "Quiz deleted successfully.", Data = true });
        }

        public class QuizSubmissionRequest
        {
            public int QuizId { get; set; }
            public string? ScoreFraction { get; set; }
            public int Percentage { get; set; }
            public bool Passed { get; set; }
            public double CpdPointsEarned { get; set; }
            public int? UserId { get; set; }
        }

        // POST: api/Quizzes/submit
        [HttpPost("submit")]
        public async Task<ActionResult<ApiResponse<object>>> SubmitQuizResult([FromBody] QuizSubmissionRequest submission)
        {
            if (submission == null)
            {
                return BadRequest(new ApiResponse<object> { Success = false, Message = "Invalid submission payload." });
            }

            var quiz = await _context.Quiz.FindAsync(submission.QuizId);
            if (quiz == null)
            {
                return NotFound(new ApiResponse<object> { Success = false, Message = "Quiz not found." });
            }

            int resolvedUserId = submission.UserId ?? 1;

            var quizResult = new QuizResult
            {
                UserId = resolvedUserId,
                QuizId = submission.QuizId,
                ScorePercentage = submission.Percentage,
                CpdPointsAwarded = (int)Math.Round(submission.CpdPointsEarned),
                IsPassed = submission.Passed,
                CompletedAt = DateTime.UtcNow
            };

            _context.QuizResult.Add(quizResult);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<object>
            {
                Success = true,
                Message = $"Quiz result recorded. Earned {submission.CpdPointsEarned} CPD points.",
                Data = new
                {
                    id = quizResult.Id,
                    quizId = submission.QuizId,
                    passed = submission.Passed,
                    cpdPointsEarned = submission.CpdPointsEarned,
                    userId = resolvedUserId,
                    submittedAt = quizResult.CompletedAt
                }
            });
        }
    }
}