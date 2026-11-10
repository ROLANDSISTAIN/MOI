using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace MoiBouclier
{
    /// <summary>Noir élégant et doré, comme MOI sur le téléphone.</summary>
    public static class Theme
    {
        public static readonly Color Black = Color.FromArgb(0x0B, 0x0B, 0x0C);
        public static readonly Color Surface = Color.FromArgb(0x15, 0x15, 0x17);
        public static readonly Color Surface2 = Color.FromArgb(0x1E, 0x1E, 0x21);
        public static readonly Color Line = Color.FromArgb(0x2A, 0x2A, 0x2E);
        public static readonly Color Gold = Color.FromArgb(0xD4, 0xAF, 0x37);
        public static readonly Color Text = Color.FromArgb(0xF2, 0xEF, 0xE6);
        public static readonly Color Dim = Color.FromArgb(0x9A, 0x97, 0x8F);
        public static readonly Color Danger = Color.FromArgb(0xC0, 0x56, 0x3F);

        public static Font Serif(float size, FontStyle st = FontStyle.Regular) => new Font("Georgia", size, st);
        public static Font Body(float size, FontStyle st = FontStyle.Regular) => new Font("Segoe UI", size, st);

        static Icon _icon;

        /// <summary>Un « M » doré sur un rond noir.</summary>
        public static Icon AppIcon
        {
            get
            {
                if (_icon != null) return _icon;
                using var bmp = new Bitmap(64, 64);
                using (var g = Graphics.FromImage(bmp))
                {
                    g.SmoothingMode = SmoothingMode.AntiAlias;
                    g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
                    g.Clear(Color.Transparent);
                    using var bg = new SolidBrush(Black);
                    g.FillEllipse(bg, 1, 1, 62, 62);
                    using var pen = new Pen(Gold, 4);
                    g.DrawEllipse(pen, 3, 3, 58, 58);
                    using var f = new Font("Georgia", 30, FontStyle.Bold, GraphicsUnit.Pixel);
                    using var gold = new SolidBrush(Gold);
                    var sf = new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center };
                    g.DrawString("M", f, gold, new RectangleF(0, 2, 64, 62), sf);
                }
                _icon = Icon.FromHandle(bmp.GetHicon());
                return _icon;
            }
        }

        public static Label Label(string text, Font font, Color color, int width)
        {
            return new Label
            {
                Text = text, Font = font, ForeColor = color, BackColor = Color.Transparent,
                AutoSize = true, MaximumSize = new Size(width, 0), Margin = new Padding(0, 0, 0, 6),
            };
        }

        public static Button GoldButton(string text, int width)
        {
            var b = new Button
            {
                Text = text, Width = width, Height = 42, FlatStyle = FlatStyle.Flat,
                BackColor = Gold, ForeColor = Black, Font = Body(10.5f, FontStyle.Bold),
                Cursor = Cursors.Hand, Margin = new Padding(0, 6, 0, 6),
            };
            b.FlatAppearance.BorderSize = 0;
            return b;
        }

        public static Button GhostButton(string text, int width, Color? color = null)
        {
            var b = new Button
            {
                Text = text, Width = width, Height = 38, FlatStyle = FlatStyle.Flat,
                BackColor = Surface2, ForeColor = color ?? Text, Font = Body(10),
                Cursor = Cursors.Hand, Margin = new Padding(0, 4, 0, 4),
            };
            b.FlatAppearance.BorderColor = Line;
            return b;
        }

        public static TextBox Box(string text, int width, int lines = 1)
        {
            return new TextBox
            {
                Text = text, Width = width, Multiline = lines > 1, Height = lines > 1 ? 22 * lines + 8 : 28,
                ScrollBars = lines > 1 ? ScrollBars.Vertical : ScrollBars.None,
                BackColor = Surface2, ForeColor = Text, BorderStyle = BorderStyle.FixedSingle,
                Font = Body(10.5f), Margin = new Padding(0, 0, 0, 10),
            };
        }

        /// <summary>Une colonne qui défile, avec une largeur fixe pour le contenu.</summary>
        public static FlowLayoutPanel Column()
        {
            return new FlowLayoutPanel
            {
                Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false,
                AutoScroll = true, BackColor = Black, Padding = new Padding(28, 22, 28, 22),
            };
        }

        public static Panel Spacer(int h) => new Panel { Height = h, Width = 10, Margin = Padding.Empty, BackColor = Color.Transparent };
    }
}
