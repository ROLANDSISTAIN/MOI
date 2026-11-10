using System;
using System.Drawing;
using System.Windows.Forms;

namespace MoiBouclier
{
    /// <summary>Premier lancement : explications, phrase d'engagement, puis installation.</summary>
    public class SetupForm : Form
    {
        const int W = 560;

        public SetupForm()
        {
            Text = "MOI — Installer le Bouclier PC";
            Icon = Theme.AppIcon;
            BackColor = Theme.Black;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(W + 80, 700);
            var col = Theme.Column();
            Controls.Add(col);
            void add(Control x) => col.Controls.Add(x);
            var c = Store.C;

            add(Theme.Label("Bouclier PC", Theme.Serif(28), Theme.Gold, W));
            add(Theme.Label("Le même Bouclier que sur ton téléphone, pour ton ordinateur.", Theme.Serif(13, FontStyle.Italic), Theme.Text, W));
            add(Theme.Spacer(8));
            foreach (var line in new[]
            {
                "•  Surveille Chrome, Edge, Firefox, Opera, Brave… : les sites et les recherches à caractère sexuel affichent l'écran STOP, et l'onglet est fermé.",
                "•  Bloque ces sites pour tout le PC (filtre DNS CleanBrowsing) et force SafeSearch sur Google et Bing.",
                "•  Désactive la navigation privée dans les navigateurs.",
                "•  Démarre avec Windows et se relance tout seul s'il est fermé.",
                "•  Pour l'arrêter : attendre 24 h après la demande, ou recopier ta phrase d'engagement.",
            }) add(Theme.Label(line, Theme.Body(11), Theme.Text, W));
            add(Theme.Spacer(12));

            add(Theme.Label("Ta phrase d'engagement (à recopier pour désactiver le Bouclier)", Theme.Body(10), Theme.Dim, W));
            var phrase = Theme.Box(c.Phrase, W, 2); add(phrase);
            add(Theme.Label("Message de l'écran STOP", Theme.Body(10), Theme.Dim, W));
            var msg = Theme.Box(c.Message, W, 3); add(msg);
            add(Theme.Label("Verset", Theme.Body(10), Theme.Dim, W));
            var verse = Theme.Box(c.Verse, W, 2); add(verse);

            var go = Theme.GoldButton("Installer et activer", 260);
            var status = Theme.Label("", Theme.Body(10.5f), Theme.Dim, W);
            go.Click += (s, e) =>
            {
                if (phrase.Text.Trim().Length < 10) { MessageBox.Show(this, "Ta phrase doit faire au moins 10 caractères."); return; }
                go.Enabled = false;
                status.Text = "Installation en cours…";
                Refresh();
                try
                {
                    c.Phrase = phrase.Text.Trim();
                    c.Message = msg.Text.Trim();
                    c.Verse = verse.Text.Trim();
                    Store.Save();
                    Guard.Install();
                    MessageBox.Show(this,
                        "MOI Bouclier est actif.\n\nTu le retrouves avec l'icône « M » dorée près de l'horloge (flèche ^ en bas à droite).\n\n" +
                        "Ferme et rouvre tes navigateurs pour que les nouvelles règles s'appliquent.",
                        "MOI — Bouclier PC", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    Close();
                }
                catch (Exception ex)
                {
                    Log.Write("Installation : " + ex);
                    status.Text = "Erreur : " + ex.Message;
                    status.ForeColor = Theme.Danger;
                    go.Enabled = true;
                }
            };
            add(go);
            add(status);
        }
    }
}
