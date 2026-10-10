using Digital_Handbook_Portal.Models;
using Digital_Handbook_Portal.ViewModels;
using Microsoft.AspNetCore.Mvc;
using System.Net.Http.Headers;

namespace Digital_Handbook_Portal.Controllers
{
    public class QuizsController : Controller
    {
        private readonly HttpClient _httpClient;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public QuizsController(IHttpClientFactory httpClientFactory, IHttpContextAccessor httpContextAccessor)
        {
            _httpClient = httpClientFactory.CreateClient("HandbookApi");
            _httpContextAccessor = httpContextAccessor;

            var token = _httpContextAccessor.HttpContext?.Session.GetString("AdminToken");
            if (!string.IsNullOrWhiteSpace(token))
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
            }
        }

        // GET: Quizzes (Displays Active Quizzes + Staff CPD Leaderboard)
        public async Task<IActionResult> Index()
        {
            var quizzesResponse = await _httpClient.GetFromJsonAsync<ApiResponse<List<Quiz>>>("api/Quizzes");
            var quizzes = quizzesResponse?.Data ?? new List<Quiz>();

            var summaries = new List<UserCpdSummaryViewModel>();
            var response = await _httpClient.GetAsync("api/Quizzes/results-summary");

            if (response.IsSuccessStatusCode)
            {
                var summariesResponse = await response.Content.ReadFromJsonAsync<ApiResponse<List<UserCpdSummaryViewModel>>>();
                summaries = summariesResponse?.Data ?? new List<UserCpdSummaryViewModel>();
            }

            var viewModel = new QuizIndexViewModel
            {
                Quizzes = quizzes,
                QuizResults = summaries
            };

            return View(viewModel);
        }

        // GET: Quizzes/UserDetails/1 (Fetches specific staff attempt history from API)
        public async Task<IActionResult> UserDetails(string id)
        {
            if (string.IsNullOrEmpty(id) || !int.TryParse(id, out int parsedUserId))
            {
                return NotFound();
            }

            var response = await _httpClient.GetFromJsonAsync<ApiResponse<List<QuizResult>>>($"api/Quizzes/user-details/{parsedUserId}");
            var userAttempts = response?.Data ?? new List<QuizResult>();

            var selectedUser = userAttempts.FirstOrDefault()?.User;
            string userEmail = selectedUser?.email ?? id;

            string derivedName = userEmail.Contains("@")
                ? string.Join(" ", userEmail.Split('@')[0].Split('.', '_', '-').Select(s => s.Length > 0 ? char.ToUpper(s[0]) + s.Substring(1) : s))
                : "Staff Member";

            ViewData["UserName"] = !string.IsNullOrWhiteSpace(selectedUser?.FullName) ? selectedUser.FullName : derivedName;
            ViewData["UserEmail"] = userEmail;

            return View(userAttempts);
        }

        // GET: Quizzes/Details/5
        public async Task<IActionResult> Details(int id)
        {
            var response = await _httpClient.GetFromJsonAsync<ApiResponse<Quiz>>($"api/Quizzes/{id}");
            if (response == null || !response.Success) return NotFound();

            return View(response.Data);
        }

        // GET: Quizzes/Create
        public IActionResult Create()
        {
            var quiz = new Quiz
            {
                questions = new List<QuizQuestion>
                {
                    new QuizQuestion
                    {
                        options = new List<QuizOption>
                        {
                            new QuizOption(),
                            new QuizOption()
                        }
                    }
                }
            };

            return View(quiz);
        }

        // POST: Quizzes/Create
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create(Quiz quiz)
        {
            CleanAndPrepareQuiz(quiz);

            if (ModelState.IsValid)
            {
                var response = await _httpClient.PostAsJsonAsync("api/Quizzes", quiz);
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction(nameof(Index));
                }
                ModelState.AddModelError(string.Empty, "Failed to create quiz via API.");
            }

            return View(quiz);
        }

        // GET: Quizzes/Edit/5
        public async Task<IActionResult> Edit(int id)
        {
            var response = await _httpClient.GetFromJsonAsync<ApiResponse<Quiz>>($"api/Quizzes/{id}");
            if (response == null || !response.Success) return NotFound();

            return View(response.Data);
        }

        // POST: Quizzes/Edit/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, Quiz quiz)
        {
            if (id != quiz.quizId) return NotFound();

            CleanAndPrepareQuiz(quiz);

            if (ModelState.IsValid)
            {
                var response = await _httpClient.PutAsJsonAsync($"api/Quizzes/{id}", quiz);
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction(nameof(Index));
                }
                ModelState.AddModelError(string.Empty, "Failed to update quiz via API.");
            }

            return View(quiz);
        }

        // GET: Quizzes/Delete/5
        public async Task<IActionResult> Delete(int id)
        {
            var response = await _httpClient.GetFromJsonAsync<ApiResponse<Quiz>>($"api/Quizzes/{id}");
            if (response == null || !response.Success) return NotFound();

            return View(response.Data);
        }

        // POST: Quizzes/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            await _httpClient.DeleteAsync($"api/Quizzes/{id}");
            return RedirectToAction(nameof(Index));
        }

        private void CleanAndPrepareQuiz(Quiz quiz)
        {
            if (quiz.questions != null)
            {
                quiz.questions = quiz.questions
                    .Where(q => !string.IsNullOrWhiteSpace(q.questionText))
                    .ToList();

                foreach (var question in quiz.questions)
                {
                    if (question.options != null)
                    {
                        question.options = question.options
                            .Where(o => !string.IsNullOrWhiteSpace(o.optionText))
                            .ToList();
                    }
                }

                quiz.totalQuestions = quiz.questions.Count;
            }
        }
    }
}