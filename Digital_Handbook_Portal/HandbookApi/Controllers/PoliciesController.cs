using Digital_Handbook_Portal.Models;
using Google.Cloud.Storage.V1;
using HandbookApi.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HandbookApi.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PoliciesController : ControllerBase
    {
        private readonly Digital_Handbook_PortalContext _context;
        private readonly ICloudStorageService _storageService;

        public PoliciesController(Digital_Handbook_PortalContext context, ICloudStorageService storageService)
        {
            _context = context;
            _storageService = storageService;
        }

        // GET: api/Policies/5/file
        [HttpGet("{id:int}/file")]
        public async Task<IActionResult> DownloadPolicyFile(int id)
        {
            var policy = await _context.Policy.FindAsync(id);
            if (policy == null || string.IsNullOrWhiteSpace(policy.fileUrl))
            {
                return NotFound("Policy or document not found.");
            }

            try
            {
                //initialize storage client
                var storage = StorageClient.Create();
                var memoryStream = new MemoryStream();

                //extract relative object path - policies stored in digihandbook-bucket which is in the filepath so we need to remove it from the object name
                var uri = new Uri(policy.fileUrl);
                string objectName = uri.AbsolutePath.TrimStart('/')
                                       .Replace("digihandbook-bucket/", "");

                await storage.DownloadObjectAsync("digihandbook-bucket", objectName, memoryStream);
                memoryStream.Position = 0;

                return File(memoryStream, "application/pdf", Path.GetFileName(objectName));
            }
            catch (Google.GoogleApiException ex) when (ex.HttpStatusCode == System.Net.HttpStatusCode.NotFound)
            {
                return NotFound($"Document object was not found in storage bucket: {ex.Message}");
            }
            catch (Exception ex)
            {
                return StatusCode(500, $"Internal error downloading file: {ex.Message}");
            }
        }
        
        // GET: api/Policies
        [HttpGet]
        public async Task<ActionResult<ApiResponse<List<Policy>>>> GetPolicies()
        {
            try
            {
                var policies = await _context.Policy
                    .AsNoTracking()
                    .Include(p => p.Category)
                    .OrderBy(p => p.Title)
                    .Select(p => new Policy
                    {
                        policyId = p.policyId,
                        Title = p.Title,
                        contentSummary = p.contentSummary,
                        specificCategory = p.specificCategory,
                        fileUrl = p.fileUrl,
                        categoryId = p.categoryId,
                        Category = p.Category == null ? null : new PolicyCategory
                        {
                            categoryId = p.Category.categoryId,
                            categoryName = p.Category.categoryName,
                            subCategory = p.Category.subCategory
                        }
                    })
                    .ToListAsync();

                return Ok(new ApiResponse<List<Policy>> { Success = true, Data = policies });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new ApiResponse<List<Policy>>
                {
                    Success = false,
                    Message = $"API Internal Error: {ex.Message} | Inner: {ex.InnerException?.Message}"
                });
            }
        }

        // GET: api/Policies/5
        [HttpGet("{id}")]
        public async Task<ActionResult<ApiResponse<Policy>>> GetPolicy(int id)
        {
            var policy = await _context.Policy
                .AsNoTracking()
                .Include(p => p.Category)
                .FirstOrDefaultAsync(p => p.policyId == id);

            if (policy == null)
            {
                return NotFound(new ApiResponse<Policy> { Success = false, Message = "Policy not found." });
            }

            return Ok(new ApiResponse<Policy> { Success = true, Data = policy });
        }

        // GET: api/Policies/category/5
        [HttpGet("category/{categoryId}")]
        public async Task<ActionResult<ApiResponse<List<Policy>>>> GetPoliciesByCategory(int categoryId)
        {
            var policies = await _context.Policy
                .Where(p => p.categoryId == categoryId)
                .Include(p => p.Category)
                .ToListAsync();

            return Ok(new ApiResponse<List<Policy>> { Success = true, Data = policies });
        }

        // POST: api/Policies
        [HttpPost]
        [Consumes("multipart/form-data")]
        public async Task<ActionResult<ApiResponse<Policy>>> CreatePolicy([FromForm] Policy policy, [FromForm] IFormFile? pdfFile)
        {
            ModelState.Remove(nameof(Policy.policyId));
            ModelState.Remove(nameof(Policy.Category));

            if (!ModelState.IsValid)
            {
                var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
                return BadRequest(new ApiResponse<Policy> { Success = false, Message = $"Validation Error: {errors}" });
            }

            try
            {
                if (pdfFile != null && pdfFile.Length > 0)
                {
                    string uploadedUrl = await _storageService.UploadFileAsync(pdfFile, "policies");
                    policy.fileUrl = uploadedUrl;
                }

                policy.policyId = 0;

                _context.Policy.Add(policy);
                await _context.SaveChangesAsync();

                return Ok(new ApiResponse<Policy> { Success = true, Message = "Policy created successfully.", Data = policy });
            }
            catch (Exception ex)
            {
                string errorMessage = ex.InnerException != null ? ex.InnerException.Message : ex.Message;
                return StatusCode(500, new ApiResponse<Policy>
                {
                    Success = false,
                    Message = $"Database/API Exception: {errorMessage}"
                });
            }
        }

        // PUT: api/Policies/5
        [HttpPut("{id}")]
        [Consumes("multipart/form-data")]
        public async Task<ActionResult<ApiResponse<Policy>>> UpdatePolicy(int id, [FromForm] Policy policy, [FromForm] IFormFile? pdfFile)
        {
            var existingPolicy = await _context.Policy.FindAsync(id);
            if (existingPolicy == null)
            {
                return NotFound(new ApiResponse<Policy> { Success = false, Message = "Policy not found." });
            }

            existingPolicy.Title = policy.Title;
            existingPolicy.contentSummary = policy.contentSummary;
            existingPolicy.specificCategory = policy.specificCategory;
            existingPolicy.categoryId = policy.categoryId;

            if (pdfFile != null && pdfFile.Length > 0)
            {
                if (!string.IsNullOrWhiteSpace(existingPolicy.fileUrl))
                {
                    await _storageService.DeleteFileAsync(existingPolicy.fileUrl);
                }

                string uploadedUrl = await _storageService.UploadFileAsync(pdfFile, "policies");
                existingPolicy.fileUrl = uploadedUrl;
            }

            await _context.SaveChangesAsync();
            return Ok(new ApiResponse<Policy> { Success = true, Message = "Policy document updated successfully.", Data = existingPolicy });
        }

        // DELETE: api/Policies/5
        [HttpDelete("{id}")]
        public async Task<ActionResult<ApiResponse<bool>>> DeletePolicy(int id)
        {
            var policy = await _context.Policy.FindAsync(id);
            if (policy == null)
            {
                return NotFound(new ApiResponse<bool> { Success = false, Message = "Policy not found.", Data = false });
            }

            if (!string.IsNullOrWhiteSpace(policy.fileUrl))
            {
                try
                {
                    await _storageService.DeleteFileAsync(policy.fileUrl);
                }
                catch
                {
                    // Continue deletion if file was already missing in cloud storage
                }
            }

            _context.Policy.Remove(policy);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<bool> { Success = true, Message = "Policy and associated document deleted successfully.", Data = true });
        }
    }
}