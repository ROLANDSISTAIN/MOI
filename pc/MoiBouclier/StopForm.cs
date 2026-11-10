using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;

namespace MoiBouclier
{
    /// <summary>L'écran plein « STOP », sur tous les écrans, impossible à fermer avant la fin du délai.</summary>
    public class StopForm : Form
    {
        static readonly List<StopForm> Open = new List<StopForm>();
        public static bool IsShowing => Open.Count > 0;

        bool _allowClose;
        int _left;
        readonly Label _count;
        readonly Button _btn;
        readonly Timer _timer = new Timer { Interval = 1000 };
        readonly Timer _front = new Timer { Interval = 400 };

        public static void ShowAll(string word)
        {
            if (IsShowing) return;
            var main = new StopForm(Screen.PrimaryScreen, true, word);
            Open.Add(main);
            foreach (var s in Screen.AllScreens)
                if (!s.Primary) Open.Add(new StopForm(s, false, word));
            foreach (var f in Open) f.Show();
            main.Activate();
        }

        static void CloseAll()
        {
            foreach (var f in Open.ToArray()) { f._allowClose = true; f.Close(); }
            Open.Clear();
        }

        StopForm(Screen screen, bool main, string word)
        {
            FormBorderStyle = FormBorderStyle.None;
            StartPosition = FormStartPosition.Manual;
            Bounds = screen.Bounds;
            TopMost = true;
            ShowInTaskbar = false;
            BackColor = Theme.Black;
            KeyPreview = true;
            Icon = Theme.AppIcon;
            Text = "MOI — STOP";
            _front.Tick += (s, e) => { TopMost = true; if (main && Form.ActiveForm == null) Activate(); };
            _front.Start();
            if (!main) return;

            var c = Store.C;
            _left = Math.Max(5, c.DelaySec);
            var w = Math.Min(760, screen.Bounds.Width - 80);

            var col = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.TopDown, WrapContents = false, AutoSize = true,
                BackColor = Theme.Black, Padding = new Padding(0),
            };
            col.Controls.Add(Theme.Label("STOP", Theme.Serif(64, FontStyle.Bold), Theme.Gold, w));
            col.Controls.Add(Theme.Label("Tu valais mieux que ça hier. Tu vaux mieux que ça aujourd'hui.", Theme.Serif(16, FontStyle.Italic), Theme.Text, w));
            col.Controls.Add(Theme.Spacer(14));
            col.Controls.Add(Theme.Label(c.Message, Theme.Body(15), Theme.Text, w));
            col.Controls.Add(Theme.Spacer(8));
            col.Controls.Add(Theme.Label(c.Verse, Theme.Serif(14, FontStyle.Italic), Theme.Gold, w));
            if (!string.IsNullOrWhiteSpace(c.Letter))
            {
                col.Controls.Add(Theme.Spacer(12));
                col.Controls.Add(Theme.Label("Lettre à moi-même", Theme.Body(10, FontStyle.Bold), Theme.Dim, w));
                col.Controls.Add(Theme.Label(c.Letter.Length > 600 ? c.Letter.Substring(0, 600) + "…" : c.Letter, Theme.Body(12.5f), Theme.Text, w));
            }
            col.Controls.Add(Theme.Spacer(18));
            col.Controls.Add(Theme.Label("Respire : inspire 4 s · bloque 4 s · expire 6 s. Lève-toi, bois de l'eau, fais 20 pompes.", Theme.Body(11.5f), Theme.Dim, w));
            col.Controls.Add(Theme.Spacer(10));
            _count = Theme.Label("", Theme.Body(12), Theme.Dim, w);
            col.Controls.Add(_count);
            _btn = Theme.GoldButton("Je choisis la liberté 🙏", 320);
            _btn.Enabled = false;
            _btn.BackColor = Theme.Surface2;
            _btn.ForeColor = Theme.Dim;
            _btn.Click += (s, e) =>
            {
                Store.C.Stopped++;
                Store.C.LastStop = Store.Now;
                Store.Save();
                CloseAll();
            };
            col.Controls.Add(_btn);
            col.Controls.Add(Theme.Spacer(8));
            col.Controls.Add(Theme.Label($"Tentations stoppées sur ce PC : {c.Stopped}   ·   Détecté : « {word} »", Theme.Body(10), Theme.Dim, w));

            Controls.Add(col);
            Load += (s, e) =>
            {
                col.PerformLayout();
                col.Location = new Point(Math.Max(20, (ClientSize.Width - col.PreferredSize.Width) / 2),
                                         Math.Max(20, (ClientSize.Height - col.PreferredSize.Height) / 2));
            };
            UpdateCount();
            _timer.Tick += (s, e) => { _left--; UpdateCount(); };
            _timer.Start();
        }

        void UpdateCount()
        {
            if (_count == null) return;
            if (_left > 0)
            {
                _count.Text = $"Tu pourras continuer dans {_left} s.";
            }
            else
            {
                _timer.Stop();
                _count.Text = "Tu peux continuer. Choisis bien.";
                _btn.Enabled = true;
                _btn.BackColor = Theme.Gold;
                _btn.ForeColor = Theme.Black;
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            if (!_allowClose && e.CloseReason != CloseReason.WindowsShutDown) { e.Cancel = true; return; }
            _timer.Stop();
            _front.Stop();
            base.OnFormClosing(e);
        }

        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == (Keys.Alt | Keys.F4) || keyData == Keys.Escape) return true;
            return base.ProcessCmdKey(ref msg, keyData);
        }
    }
}
