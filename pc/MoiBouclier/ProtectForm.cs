using System;
using System.Drawing;
using System.Windows.Forms;

namespace MoiBouclier
{
    /// <summary>
    /// Petit encadré doré en bas à droite quand une pub, une pop-up ou une redirection a voulu t'emmener
    /// sur un site adulte sans que tu l'aies cherché. Pas de délai, pas de reproche, ne compte pas comme une tentation.
    /// </summary>
    public class ProtectForm : Form
    {
        static ProtectForm _open;
        readonly Timer _close = new Timer { Interval = 7000 };

        public static void Display(int avoided)
        {
            try { _open?.Close(); } catch { }
            _open = new ProtectForm(avoided);
            _open.Show();
        }

        protected override bool ShowWithoutActivation => true; // ne vole pas le clavier

        ProtectForm(int avoided)
        {
            FormBorderStyle = FormBorderStyle.None;
            StartPosition = FormStartPosition.Manual;
            TopMost = true;
            ShowInTaskbar = false;
            BackColor = Theme.Gold;            // fin liseré doré
            Padding = new Padding(2);
            Icon = Theme.AppIcon;
            Text = "MOI — Protégé";

            const int w = 380;
            var col = new FlowLayoutPanel
            {
                Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false,
                BackColor = Theme.Black, Padding = new Padding(18, 14, 18, 12),
            };
            col.Controls.Add(Theme.Label("🛡  Le Bouclier t'a protégé", Theme.Serif(14, FontStyle.Bold), Theme.Gold, w));
            col.Controls.Add(Theme.Label("Une pub ou une redirection a voulu t'emmener sur un site adulte. L'onglet a été fermé.",
                Theme.Body(10.5f), Theme.Text, w));
            col.Controls.Add(Theme.Label("Ce n'est pas ta faute. Tu peux continuer tranquillement.", Theme.Body(10.5f, FontStyle.Italic), Theme.Text, w));
            col.Controls.Add(Theme.Label($"Pièges évités sur ce PC : {avoided}", Theme.Body(9), Theme.Dim, w));
            Controls.Add(col);

            var area = Screen.PrimaryScreen.WorkingArea;
            Size = new Size(w + 40, 150);
            Load += (s, e) =>
            {
                col.PerformLayout();
                Height = Math.Max(120, col.PreferredSize.Height + 4);
                Location = new Point(area.Right - Width - 16, area.Bottom - Height - 16);
            };
            EventHandler close = (s, e) => Close();
            Click += close;
            foreach (Control c in col.Controls) c.Click += close;
            col.Click += close;
            _close.Tick += close;
            _close.Start();
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            _close.Stop();
            if (_open == this) _open = null;
            base.OnFormClosed(e);
        }
    }
}
