using Microsoft.EntityFrameworkCore;
using Digital_Handbook_Portal.Models;

public class Digital_Handbook_PortalContext(DbContextOptions<Digital_Handbook_PortalContext> options) : DbContext(options)
{
    public DbSet<BragBook> BragBook { get; set; } = default!;
    public DbSet<Community> Community { get; set; } = default!;
    public DbSet<Policy> Policy { get; set; } = default!;
    public DbSet<PolicyCategory> PolicyCategory { get; set; } = default!;
    public DbSet<User> User { get; set; } = default!;
    public DbSet<Resource> Resource { get; set; } = default!;
    public DbSet<Quiz> Quiz { get; set; } = default!;
    public DbSet<Doctor> Doctor { get; set; } = default!;
    public DbSet<UserSession> UserSession { get; set; } = default!;
    public DbSet<FragmentVisit> FragmentVisit { get; set; } = default!;
    public DbSet<QuizResult> QuizResult { get; set; } = default!;
    public DbSet<Faq> Faq { get; set; } = default!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Faq>(entity =>
        {
            entity.ToTable("Faq");
            entity.HasKey(e => e.FaqId);
        });

        modelBuilder.Entity<BragBook>(entity =>
        {
            entity.ToTable("BragBook");
            entity.HasKey(e => e.bragId);

            entity.Property(e => e.bragId).HasColumnName("bragId");
            entity.Property(e => e.content).HasColumnName("content");
            entity.Property(e => e.senderType).HasColumnName("senderType");
            entity.Property(e => e.recipientName).HasColumnName("recipientName");
            entity.Property(e => e.datePosted).HasColumnName("datePosted");

            entity.Ignore("imageUrl");
        });

        modelBuilder.Entity<Doctor>(entity =>
        {
            entity.ToTable("Doctor");
            entity.HasKey(e => e.doctorId);

            entity.Property(e => e.doctorId).HasColumnName("doctorId");
            entity.Property(e => e.doctorImg).HasColumnName("doctorImg");
            entity.Property(e => e.fName).HasColumnName("fName");
            entity.Property(e => e.lName).HasColumnName("lName");
            entity.Property(e => e.email).HasColumnName("email");
            entity.Property(e => e.phone).HasColumnName("phone");
            entity.Property(e => e.suiteNumber).HasColumnName("suiteNumber");

            entity.Ignore(e => e.FullName);
        });

        modelBuilder.Entity<UserSession>(entity =>
        {
            entity.ToTable("UserSession");
            entity.HasKey(e => e.SessionId);

            entity.HasMany(e => e.Visits)
                  .WithOne()
                  .HasForeignKey(e => e.SessionId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<FragmentVisit>(entity =>
        {
            entity.ToTable("FragmentVisit");
            entity.HasKey(e => e.VisitId);
        });

        modelBuilder.Entity<Policy>().ToTable("Policy");
        modelBuilder.Entity<PolicyCategory>().ToTable("PolicyCategory");
        modelBuilder.Entity<Community>().ToTable("Community");
        modelBuilder.Entity<User>().ToTable("User");
        modelBuilder.Entity<Resource>().ToTable("Resource");
        modelBuilder.Entity<Quiz>().ToTable("Quiz");
    }
}