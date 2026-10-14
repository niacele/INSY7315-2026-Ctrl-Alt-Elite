using Digital_Handbook_Portal.Models;
using HandbookApi.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HandbookApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class DoctorsController : ControllerBase
    {
        private readonly Digital_Handbook_PortalContext _context;
        private readonly ICloudStorageService _cloudStorageService;

        public DoctorsController(Digital_Handbook_PortalContext context, ICloudStorageService cloudStorageService)
        {
            _context = context;
            _cloudStorageService = cloudStorageService;
        }

        // GET: api/Doctors
        [HttpGet]
        public async Task<ActionResult<ApiResponse<List<Doctor>>>> GetDoctors()
        {
            try
            {
                var doctors = await _context.Doctor.ToListAsync();
                return Ok(new ApiResponse<List<Doctor>>
                {
                    Success = true,
                    Message = "Doctors retrieved successfully",
                    Data = doctors
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new ApiResponse<List<Doctor>>
                {
                    Success = false,
                    Message = $"Server error: {ex.Message}",
                    Data = null
                });
            }
        }

        // GET: api/Doctors/5
        [HttpGet("{id}")]
        public async Task<ActionResult<ApiResponse<Doctor>>> GetDoctorById(int id)
        {
            var doctor = await _context.Doctor.FindAsync(id);
            if (doctor == null)
            {
                return NotFound(new ApiResponse<Doctor>
                {
                    Success = false,
                    Message = "Doctor not found",
                    Data = null
                });
            }

            return Ok(new ApiResponse<Doctor>
            {
                Success = true,
                Message = "Doctor retrieved successfully",
                Data = doctor
            });
        }

        // POST: api/Doctors
        [HttpPost]
        [Consumes("multipart/form-data")]
        public async Task<ActionResult<ApiResponse<Doctor>>> CreateDoctor([FromForm] Doctor doctor, IFormFile? imageFile)
        {
            try
            {
                if (imageFile != null && imageFile.Length > 0)
                {
                    string fileNameForStorage = $"doctors/{Guid.NewGuid()}_{imageFile.FileName}";

                    string uploadedUrl = await _cloudStorageService.UploadFileAsync(imageFile, fileNameForStorage);

                    doctor.doctorImg = uploadedUrl;
                }

                // 3. Save to database
                _context.Doctor.Add(doctor);
                await _context.SaveChangesAsync();

                return CreatedAtAction(nameof(GetDoctorById), new { id = doctor.doctorId }, new ApiResponse<Doctor>
                {
                    Success = true,
                    Message = "Doctor created successfully",
                    Data = doctor
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new ApiResponse<Doctor>
                {
                    Success = false,
                    Message = $"Failed to create doctor: {ex.Message}",
                    Data = null
                });
            }
        }

        // PUT: api/Doctors/5
        [HttpPut("{id}")]
        [Consumes("multipart/form-data")]
        public async Task<ActionResult<ApiResponse<Doctor>>> EditDoctor(int id, [FromForm] Doctor doctor, IFormFile? imageFile)
        {
            if (id != doctor.doctorId)
            {
                return BadRequest(new ApiResponse<Doctor> { Success = false, Message = "ID mismatch" });
            }

            try
            {
                var existingDoctor = await _context.Doctor.FindAsync(id);
                if (existingDoctor == null)
                {
                    return NotFound(new ApiResponse<Doctor> { Success = false, Message = "Doctor not found" });
                }

                if (imageFile != null && imageFile.Length > 0)
                {
                    string fileNameForStorage = $"doctors/{Guid.NewGuid()}_{imageFile.FileName}";
                    string uploadedUrl = await _cloudStorageService.UploadFileAsync(imageFile, fileNameForStorage);
                    existingDoctor.doctorImg = uploadedUrl;
                }
                else if (!string.IsNullOrWhiteSpace(doctor.doctorImg))
                {
                    existingDoctor.doctorImg = doctor.doctorImg;
                }

                existingDoctor.fName = doctor.fName;
                existingDoctor.lName = doctor.lName;
                existingDoctor.email = doctor.email;
                existingDoctor.phone = doctor.phone;
                existingDoctor.suiteNumber = doctor.suiteNumber;

                await _context.SaveChangesAsync();

                return Ok(new ApiResponse<Doctor>
                {
                    Success = true,
                    Message = "Doctor updated successfully",
                    Data = existingDoctor
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new ApiResponse<Doctor>
                {
                    Success = false,
                    Message = $"Failed to update doctor: {ex.Message}",
                    Data = null
                });
            }
        }

        // DELETE: api/Doctors/5
        [HttpDelete("{id}")]
        public async Task<ActionResult<ApiResponse<bool>>> DeleteDoctor(int id)
        {
            var doctor = await _context.Doctor.FindAsync(id);
            if (doctor == null)
            {
                return NotFound(new ApiResponse<bool> { Success = false, Message = "Doctor not found", Data = false });
            }

            _context.Doctor.Remove(doctor);
            await _context.SaveChangesAsync();

            return Ok(new ApiResponse<bool> { Success = true, Message = "Doctor deleted successfully", Data = true });
        }
    }
}