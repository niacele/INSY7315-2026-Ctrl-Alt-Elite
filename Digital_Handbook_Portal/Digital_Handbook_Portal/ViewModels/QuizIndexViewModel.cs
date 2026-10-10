using Digital_Handbook_Portal.Models;

namespace Digital_Handbook_Portal.ViewModels
{
    public class QuizIndexViewModel
    {
        public List<Quiz> Quizzes { get; set; } = new List<Quiz>();
        public List<UserCpdSummaryViewModel> QuizResults { get; set; } = new List<UserCpdSummaryViewModel>();
    }
}