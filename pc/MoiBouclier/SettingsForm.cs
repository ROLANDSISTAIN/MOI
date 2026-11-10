using System;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;

namespace MoiBouclier
{
    /// <summary>Réglages : message, verset, lettre, délai, mots ajoutés, et l'arrêt protégé (24 h ou phrase).</summary>
    public class SettingsForm : Form
    {
        const int W = 560;
        readonly FlowLayoutPanel _col = Theme.Column();
        readonly Timer _refresh = new Timer { Interval = 5000 };

        public SettingsForm()
        {
            Text = "MOI — Bouclier PC";
            Icon = Theme.AppIcon;
            BackColor = Theme.Black;
            ForeColor = Theme.Text;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(W + 80, 720);
            MinimumSize = new Size(W + 96, 500);
            Controls.Add(_col);
            Build();
            _refresh.Tick += (s, e) => { if (StateKey() != _state) Build(); };
            _refresh.Start();
            FormClosed += (s, e) => _refresh.Stop();
        }

        string _state;

        /// <summary>On ne reconstruit la fenêtre que si l'état change (pour ne pas effacer ce que tu tapes).</summary>
        static string StateKey() => $"{Store.IsUnlocked}|{Store.WaitDone}|{Store.WaitPending}|{Store.C.Stopped}";

        void Build()
        {
            _state = StateKey();
            var scroll = _col.VerticalScroll.Value;
            _col.SuspendLayout();
            _col.Controls.Clear();
            var c = Store.C;
            void add(Control x) => _col.Controls.Add(x);

            add(Theme.Label("Bouclier PC", Theme.Serif(26), Theme.Gold, W));

            // ---- État ----
            bool dns = Guard.DnsOk(), pol = Guard.PoliciesOk(), task = Guard.TaskExists();
            string ok(bool b) => b ? "✅" : "⚠️";
            add(Theme.Label(Store.IsUnlocked ? "⏸️  Déverrouillé : la surveillance est en pause." : "✅  Surveillance active (Chrome, Edge, Firefox, Opera, Brave…)",
                Theme.Body(11), Store.IsUnlocked ? Theme.Gold : Theme.Text, W));
            add(Theme.Label($"{ok(dns)}  Filtre des sites (DNS CleanBrowsing){(dns ? "" : " — sera remis en place")}", Theme.Body(11), Theme.Text, W));
            add(Theme.Label($"{ok(pol)}  Règles des navigateurs : SafeSearch, pas de navigation privée", Theme.Body(11), Theme.Text, W));
            add(Theme.Label($"{ok(task)}  Démarrage automatique et relance chaque minute", Theme.Body(11), Theme.Text, W));
            if (!Guard.IsAdmin)
                add(Theme.Label("Lancé sans les droits administrateur : le filtre et les règles ne peuvent pas être vérifiés.", Theme.Body(10), Theme.Danger, W));
            add(Theme.Spacer(6));
            add(Theme.Label($"Tentations stoppées sur ce PC : {c.Stopped}" +
                (c.LastStop > 0 ? $"  ·  dernière le {DateTimeOffset.FromUnixTimeMilliseconds(c.LastStop).LocalDateTime:dd/MM/yyyy à HH:mm}" : ""),
                Theme.Body(11, FontStyle.Bold), Theme.Gold, W));
            add(Theme.Spacer(14));

            // ---- Écran STOP ----
            add(Theme.Label("ÉCRAN STOP", Theme.Body(9.5f, FontStyle.Bold), Theme.Dim, W));
            add(Theme.Label("Message", Theme.Body(10), Theme.Dim, W));
            var msg = Theme.Box(c.Message, W, 3); add(msg);
            add(Theme.Label("Verset", Theme.Body(10), Theme.Dim, W));
            var verse = Theme.Box(c.Verse, W, 2); add(verse);
            add(Theme.Label("Lettre à moi-même (facultatif)", Theme.Body(10), Theme.Dim, W));
            var letter = Theme.Box(c.Letter, W, 5); add(letter);
            add(Theme.Label("Délai avant de pouvoir continuer (10 à 120 s)", Theme.Body(10), Theme.Dim, W));
            var delay = new NumericUpDown
            {
                Minimum = 10, Maximum = 120, Value = Math.Clamp(c.DelaySec, 10, 120), Width = 90,
                BackColor = Theme.Surface2, ForeColor = Theme.Text, Font = Theme.Body(10.5f), Margin = new Padding(0, 0, 0, 10),
            };
            add(delay);
            add(Theme.Label("Mots ou sites à bloquer en plus (un par ligne)", Theme.Body(10), Theme.Dim, W));
            var extra = Theme.Box(string.Join(Environment.NewLine, c.ExtraWords), W, 4); add(extra);

            var save = Theme.GoldButton("Enregistrer", 200);
            save.Click += (s, e) =>
            {
                var words = extra.Lines.Select(l => l.Trim()).Where(l => l.Length >= 3).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
                var removed = c.ExtraWords.Any(o => !words.Contains(o, StringComparer.OrdinalIgnoreCase));
                if (!Store.IsUnlocked && ((int)delay.Value < c.DelaySec || removed))
                {
                    MessageBox.Show(this, "Pour baisser le délai ou retirer un mot, il faut d'abord déverrouiller (en bas de cette fenêtre).",
                        "MOI — Bouclier PC", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }
                c.Message = msg.Text.Trim();
                c.Verse = verse.Text.Trim();
                c.Letter = letter.Text.Trim();
                c.DelaySec = (int)delay.Value;
                c.ExtraWords = words;
                Store.Save();
                Build();
            };
            add(save);
            add(Theme.Spacer(20));

            // ---- Arrêt protégé ----
            add(Theme.Label("ARRÊTER OU DÉSINSTALLER", Theme.Body(9.5f, FontStyle.Bold), Theme.Dim, W));
            if (Store.IsUnlocked)
            {
                var until = DateTimeOffset.FromUnixTimeMilliseconds(c.UnlockUntil).LocalDateTime;
                add(Theme.Label($"Déverrouillé jusqu'à {until:HH:mm}. Ensuite, tout se remet en place tout seul.", Theme.Body(11), Theme.Gold, W));
                add(Theme.Label("Nouvelle phrase d'engagement (facultatif)", Theme.Body(10), Theme.Dim, W));
                var phrase = Theme.Box(c.Phrase, W, 2); add(phrase);
                var savePhrase = Theme.GhostButton("Changer la phrase", 200);
                savePhrase.Click += (s, e) =>
                {
                    if (phrase.Text.Trim().Length < 10) { MessageBox.Show(this, "La phrase doit faire au moins 10 caractères."); return; }
                    c.Phrase = phrase.Text.Trim(); Store.Save(); Build();
                };
                add(savePhrase);
                var relock = Theme.GhostButton("Reverrouiller maintenant", 260);
                relock.Click += (s, e) => { c.UnlockUntil = 0; Store.Save(); Guard.Maintain(true); Build(); };
                add(relock);
                var uninstall = Theme.GhostButton("Désinstaller complètement", 260, Theme.Danger);
                uninstall.Click += (s, e) =>
                {
                    if (MessageBox.Show(this, "Retirer MOI Bouclier, le filtre DNS et les règles des navigateurs ?",
                            "Désinstaller", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes) return;
                    Guard.Uninstall();
                    MessageBox.Show(this, "MOI Bouclier est désinstallé.", "MOI — Bouclier PC");
                    Environment.Exit(0);
                };
                add(uninstall);
            }
            else
            {
                add(Theme.Label("Deux façons de déverrouiller pendant 15 minutes : attendre 24 h après ta demande, ou recopier ta phrase d'engagement.",
                    Theme.Body(10.5f), Theme.Text, W));
                if (Store.WaitDone)
                {
                    var now = Theme.GoldButton("Les 24 h sont passées : déverrouiller", 340);
                    now.Click += (s, e) => { Store.Unlock15Min(); Build(); };
                    add(now);
                }
                else if (Store.WaitPending)
                {
                    var at = DateTimeOffset.FromUnixTimeMilliseconds(c.UnlockRequestedAt + 24 * Store.HourMs).LocalDateTime;
                    add(Theme.Label($"Demande en cours : possible à partir du {at:dd/MM à HH:mm}.", Theme.Body(10.5f), Theme.Gold, W));
                    var cancel = Theme.GhostButton("Annuler la demande", 220);
                    cancel.Click += (s, e) => { c.UnlockRequestedAt = 0; Store.Save(); Build(); };
                    add(cancel);
                }
                else
                {
                    var ask = Theme.GhostButton("Demander l'arrêt (attendre 24 h)", 300);
                    ask.Click += (s, e) => { c.UnlockRequestedAt = Store.Now; Store.Save(); Build(); };
                    add(ask);
                }
                add(Theme.Spacer(6));
                add(Theme.Label("Recopie ta phrase d'engagement :", Theme.Body(10), Theme.Dim, W));
                add(Theme.Label("« " + c.Phrase + " »", Theme.Serif(11.5f, FontStyle.Italic), Theme.Gold, W));
                var typed = Theme.Box("", W, 2); add(typed);
                var unlock = Theme.GhostButton("Déverrouiller", 200);
                unlock.Click += (s, e) =>
                {
                    if (Store.PhraseMatches(typed.Text)) { Store.Unlock15Min(); Build(); }
                    else MessageBox.Show(this, "Ce n'est pas exactement ta phrase.", "MOI — Bouclier PC");
                };
                add(unlock);
            }
            add(Theme.Spacer(20));
            _col.ResumeLayout();
            try { _col.VerticalScroll.Value = Math.Min(scroll, _col.VerticalScroll.Maximum); } catch { }
        }
    }
}
