using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Digital_Handbook_Portal.Models
{
    [Table("Faq")]
    public class Faq
    {
        [Key]
        [Column("faqId")]
        public int FaqId { get; set; }

        [Required]
        [Column("topicKey")]
        public string TopicKey { get; set; } = string.Empty;

        [Required]
        [Column("question")]
        public string Question { get; set; } = string.Empty;

        [Required]
        [Column("answer")]
        public string Answer { get; set; } = string.Empty;

        [Column("category")]
        public string Category { get; set; } = "General";
    }
}