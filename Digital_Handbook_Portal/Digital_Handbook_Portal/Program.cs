using Microsoft.EntityFrameworkCore;

namespace Digital_Handbook_Portal
{
    public class Program
    {
        public static void Main(string[] args)
        {

            var builder = WebApplication.CreateBuilder(args);

            //temp for scaffolding purposes
            builder.Services.AddDbContext<Digital_Handbook_PortalContext>(options =>
            options.UseInMemoryDatabase("ScaffoldingDb"));


            builder.Services.AddControllersWithViews();

            //sessions
            builder.Services.AddDistributedMemoryCache();

            builder.Services.AddSession(options =>
            {
                options.IdleTimeout = TimeSpan.FromHours(2);
                options.Cookie.HttpOnly = true;
                options.Cookie.IsEssential = true;
            });

            builder.Services.AddHttpClient("HandbookApi", client =>
            {
                //link to API
                client.BaseAddress = new Uri(builder.Configuration["ApiSettings:BaseUrl"]
        ?? "https://localhost:7000/");
            });

            //allows us to add session token to outbound requests to the API
            builder.Services.AddHttpContextAccessor();

            var app = builder.Build();

            if (!app.Environment.IsDevelopment())
            {
                app.UseExceptionHandler("/Home/Error");
                app.UseHsts();
            }

            app.UseHttpsRedirection();
            app.UseRouting();

            //sessions
            app.UseSession();
            app.UseAuthorization();
            app.UseAuthentication();

            app.MapStaticAssets();
            app.MapControllerRoute(
                name: "default",
                pattern: "{controller=Account}/{action=Login}/{id?}")
                .WithStaticAssets();

            app.Run();
        }
    }
}