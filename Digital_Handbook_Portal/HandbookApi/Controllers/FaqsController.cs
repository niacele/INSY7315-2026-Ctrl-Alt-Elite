using Digital_Handbook_Portal.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HandbookApi.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class FaqsController : ControllerBase
    {
        private readonly Digital_Handbook_PortalContext _context;

        public FaqsController(Digital_Handbook_PortalContext context)
        {
            _context = context;
        }

        // GET: api/Faqs
        [HttpGet]
        public async Task<IActionResult> GetAllFaqs()
        {
            var faqs = await _context.Faq.ToListAsync();

            return Ok(new
            {
                success = true,
                message = "FAQs retrieved successfully.",
                data = faqs
            });
        }

        // GET: api/Faqs/query?topicKey=adverse_drug
        [HttpGet("query")]
        public async Task<IActionResult> QueryFaq([FromQuery] string topicKey)
        {
            if (string.IsNullOrWhiteSpace(topicKey))
            {
                return BadRequest(new
                {
                    success = false,
                    message = "Topic key is required."
                });
            }

            var faq = await _context.Faq
                .FirstOrDefaultAsync(f => f.TopicKey.ToLower() == topicKey.Trim().ToLower());

            if (faq == null)
            {
                return NotFound(new
                {
                    success = false,
                    message = $"No FAQ found for '{topicKey}'."
                });
            }

            return Ok(new
            {
                success = true,
                message = "FAQ retrieved successfully.",
                data = faq
            });
        }
    }
}