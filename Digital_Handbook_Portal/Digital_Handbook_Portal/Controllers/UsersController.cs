using Digital_Handbook_Portal.Models;
using Digital_Handbook_Portal.ViewModels;
using Microsoft.AspNetCore.Mvc;
using System.Net.Http.Headers;

namespace Digital_Handbook_Portal.Controllers
{
    public class UsersController : Controller
    {
        private readonly HttpClient _httpClient;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public UsersController(IHttpClientFactory httpClientFactory, IHttpContextAccessor httpContextAccessor)
        {
            _httpClient = httpClientFactory.CreateClient("HandbookApi");
            _httpContextAccessor = httpContextAccessor;

            var token = _httpContextAccessor.HttpContext?.Session.GetString("AdminToken");
            if (!string.IsNullOrWhiteSpace(token))
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
            }
        }

        // GET: Users
        // GET: Users - will display list of users and sessions side by side

        // GET: Users (Staff Account List)
        public async Task<IActionResult> Index()
        {
            //call api/Users to get list of users
            var usersResponse = await _httpClient.GetFromJsonAsync<ApiResponse<List<User>>>("api/Users");

            //call api/Users/all-sessions to get list of sessions
            var sessionsResponse = await _httpClient.GetFromJsonAsync<ApiResponse<List<UserSession>>>("api/Users/all-sessions");

            var viewModel = new UserManagementViewModel
            {
                Users = usersResponse?.Data ?? new List<User>(),
                Sessions = sessionsResponse?.Data ?? new List<UserSession>()
            };

            return View(viewModel);
        }

        // GET: Users/AllSessions (Hit when clicking the "User Sessions" tile on the Home Dashboard)
        public async Task<IActionResult> AllSessions()
        {
            var response = await _httpClient.GetFromJsonAsync<ApiResponse<List<UserSession>>>("api/Users/all-sessions");
            return View(response?.Data ?? new List<UserSession>());
        }

        // GET: Users/Sessions/5 (Hit when clicking "Details" on a specific user's sessions)
        public async Task<IActionResult> Sessions(int id)
        {
            if (id <= 0) return RedirectToAction(nameof(Index));

            var userResponse = await _httpClient.GetFromJsonAsync<ApiResponse<User>>($"api/Users/{id}");
            if (userResponse == null || !userResponse.Success || userResponse.Data == null)
            {
                return NotFound();
            }

            var sessionsResponse = await _httpClient.GetFromJsonAsync<ApiResponse<List<UserSession>>>($"api/Users/{id}/sessions");

            ViewBag.UserEmail = userResponse.Data.email;
            ViewBag.UserId = userResponse.Data.userId;

            return View(sessionsResponse?.Data ?? new List<UserSession>());
        }

        // GET: Users/Details/5
        public async Task<IActionResult> Details(int id)
        {
            //calls api/Users/{id} to get user details and api/Users/{id}/sessions to get user sessions
            var userResponse = await _httpClient.GetFromJsonAsync<ApiResponse<User>>($"api/Users/{id}");
            if (userResponse == null || !userResponse.Success) return NotFound();

            var sessionsResponse = await _httpClient.GetFromJsonAsync<ApiResponse<List<UserSession>>>($"api/Users/{id}/sessions");

            var user = userResponse.Data;
            user.Sessions = sessionsResponse?.Data ?? new List<UserSession>();

            return View(user);
        }

        // GET: Users/Create
        public IActionResult Create()
        {
            return View();
        }

        // POST: Users/Create (Admin creates application user credentials)
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create(User user)
        {
            if (ModelState.IsValid)
            {
                var response = await _httpClient.PostAsJsonAsync("api/Users/admin-create", user);
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction(nameof(Index));
                }

                var errorResult = await response.Content.ReadFromJsonAsync<ApiResponse<User>>();
                ModelState.AddModelError(string.Empty, errorResult?.Message ?? "Failed to create user account.");
            }
            return View(user);
        }

        // GET: Users/Edit/5
        public async Task<IActionResult> Edit(int id)
        {
            var response = await _httpClient.GetFromJsonAsync<ApiResponse<User>>($"api/Users/{id}");
            if (response == null || !response.Success) return NotFound();

            return View(response.Data);
        }

        // POST: Users/Edit/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, User user)
        {
            if (id != user.userId) return NotFound();

            if (ModelState.IsValid)
            {
                var response = await _httpClient.PutAsJsonAsync($"api/Users/{id}", user);
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction(nameof(Index));
                }
                ModelState.AddModelError(string.Empty, "Failed to update user account.");
            }
            return View(user);
        }

        // GET: Users/Delete/5
        public async Task<IActionResult> Delete(int id)
        {
            var response = await _httpClient.GetFromJsonAsync<ApiResponse<User>>($"api/Users/{id}");
            if (response == null || !response.Success) return NotFound();

            return View(response.Data);
        }

        // POST: Users/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            await _httpClient.DeleteAsync($"api/Users/{id}");
            return RedirectToAction(nameof(Index));
        }
    }
}