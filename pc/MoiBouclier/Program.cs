using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;

namespace MoiBouclier
{
    static class Program
    {
        const string MutexName = @"Global\MOI_Bouclier_PC";
        const string ShowEventName = @"Global\MOI_Bouclier_PC_Show";

        [STAThread]
        static void Main(string[] args)
        {
            Application.SetHighDpiMode(HighDpiMode.PerMonitorV2);
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.ThreadException += (s, e) => Log.Write("Erreur : " + e.Exception);
            AppDomain.CurrentDomain.UnhandledException += (s, e) => Log.Write("Erreur fatale : " + e.ExceptionObject);

            // ---- Pas encore installé (ou nouvelle version téléchargée) : on installe ----
            if (!Guard.IsInstalledCopy)
            {
                if (!Guard.IsAdmin)
                {
                    try
                    {
                        Process.Start(new ProcessStartInfo(Environment.ProcessPath) { UseShellExecute = true, Verb = "runas" });
                    }
                    catch
                    {
                        MessageBox.Show("L'installation a besoin des droits administrateur. Clique sur « Oui » quand Windows le demande.",
                            "MOI — Bouclier PC", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }
                    return;
                }
                if (Store.C.Installed && File.Exists(Guard.ExePath))
                {
                    // Mise à jour : on remplace le programme sans rien demander, les réglages sont gardés.
                    try
                    {
                        Guard.Install();
                        MessageBox.Show("MOI Bouclier a été mis à jour. Tes réglages sont gardés.", "MOI — Bouclier PC",
                            MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }
                    catch (Exception e) { MessageBox.Show("Mise à jour impossible : " + e.Message, "MOI — Bouclier PC"); }
                    return;
                }
                Application.Run(new SetupForm());
                return;
            }

            // ---- Copie installée : une seule à la fois ----
            using var mutex = new Mutex(true, MutexName, out var first);
            if (!first)
            {
                if (!Array.Exists(args, a => a == "--bg"))
                {
                    try { using var ev = EventWaitHandle.OpenExisting(ShowEventName); ev.Set(); } catch { }
                }
                return;
            }
            Application.Run(new TrayApp(!Array.Exists(args, a => a == "--bg")));
            GC.KeepAlive(mutex);
        }

        public static EventWaitHandle CreateShowEvent()
        {
            try { return new EventWaitHandle(false, EventResetMode.AutoReset, ShowEventName); }
            catch { return null; }
        }
    }

    /// <summary>Le programme de fond : icône près de l'horloge, surveillance, maintenance des protections.</summary>
    class TrayApp : ApplicationContext
    {
        readonly NotifyIcon _icon;
        readonly Watcher _watcher = new Watcher();
        readonly Control _ui = new Control();
        readonly System.Windows.Forms.Timer _maintain = new System.Windows.Forms.Timer { Interval = 2 * 60_000 };
        SettingsForm _settings;
        DateTime _lastStop = DateTime.MinValue;

        [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr h);

        public TrayApp(bool openSettings)
        {
            _ui.CreateControl();
            _ = _ui.Handle;

            var menu = new ContextMenuStrip();
            menu.Items.Add("Ouvrir MOI Bouclier", null, (s, e) => ShowSettings());
            _icon = new NotifyIcon { Icon = Theme.AppIcon, Text = "MOI — Bouclier actif", Visible = true, ContextMenuStrip = menu };
            _icon.DoubleClick += (s, e) => ShowSettings();

            _watcher.Detected += (h, word) => _ui.BeginInvoke(new Action(() => OnDetected(h, word)));
            _watcher.Start();

            ThreadPool.QueueUserWorkItem(_ => Guard.Maintain());
            _maintain.Tick += (s, e) => ThreadPool.QueueUserWorkItem(_ => Guard.Maintain());
            _maintain.Start();

            var ev = Program.CreateShowEvent();
            if (ev != null)
            {
                var t = new Thread(() =>
                {
                    while (true) { ev.WaitOne(); _ui.BeginInvoke(new Action(ShowSettings)); }
                }) { IsBackground = true };
                t.Start();
            }
            if (openSettings) ShowSettings();
        }

        void ShowSettings()
        {
            if (_settings == null || _settings.IsDisposed)
            {
                _settings = new SettingsForm();
                _settings.FormClosed += (s, e) => _settings = null;
                _settings.Show();
            }
            if (_settings.WindowState == FormWindowState.Minimized) _settings.WindowState = FormWindowState.Normal;
            _settings.Activate();
        }

        void OnDetected(IntPtr browser, string word)
        {
            if (StopForm.IsShowing || Store.IsUnlocked) return;
            if ((DateTime.UtcNow - _lastStop).TotalMilliseconds < 1500) return;
            _lastStop = DateTime.UtcNow;
            Log.Write("Détecté : " + word);
            // Ferme l'onglet fautif (Ctrl+W), puis affiche l'écran STOP.
            try
            {
                SetForegroundWindow(browser);
                Thread.Sleep(80);
                SendKeys.SendWait("^w");
            }
            catch { }
            StopForm.ShowAll(word);
        }
    }
}
