using HandbookApi.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi;

namespace HandbookApi
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);

            var connectionString = builder.Configuration.GetConnectionString("Digital_Handbook_PortalContext")
                ?? throw new InvalidOperationException("Connection string 'Digital_Handbook_PortalContext' not found.");

            builder.Services.AddDbContext<Digital_Handbook_PortalContext>(options =>
                options.UseNpgsql(connectionString));

            builder.Services.AddControllers()
                .AddJsonOptions(options =>
                {
                    options.JsonSerializerOptions.ReferenceHandler = System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
                    options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
                    options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
                });

            builder.Services.AddEndpointsApiExplorer();

            builder.Services.AddHttpContextAccessor();

            builder.Services.AddScoped<ICloudStorageService, CloudStorageService>();
            builder.Services.AddScoped<IEmailService, EmailService>();

            builder.Services.AddSwaggerGen(c =>
            {
                c.SwaggerDoc("v1", new OpenApiInfo { Title = "Handbook API", Version = "v1" });

                c.MapType<IFormFile>(() => new OpenApiSchema
                {
                    Type = JsonSchemaType.String,
                    Format = "binary"
                });

                c.CustomSchemaIds(type => type.FullName);
            });

            builder.Services.AddCors(options =>
            {
                options.AddPolicy("AllowAll", policy =>
                {
                    policy.AllowAnyOrigin()
                          .AllowAnyMethod()
                          .AllowAnyHeader();
                });
            });

            var app = builder.Build();

            app.UseSwagger();
            app.UseSwaggerUI(c =>
            {
                c.SwaggerEndpoint("/swagger/v1/swagger.json", "Handbook API v1");
            });

            app.UseHttpsRedirection();
            app.UseCors("AllowAll");
            app.UseAuthorization();

            app.MapControllers();

            AppContext.SetSwitch("Npgsql.EnableLegacyTimestampBehavior", true);

            app.Run();
        }
    }
}