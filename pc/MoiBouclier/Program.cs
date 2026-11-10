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

            // ---- Copie installée lancée à la main sans droits admin : on se relance en admin ----
            bool bg = Array.Exists(args, a => a == "--bg");
            if (!bg && !Guard.IsAdmin)
            {
                try { Process.Start(new ProcessStartInfo(Environment.ProcessPath) { UseShellExecute = true, Verb = "runas" }); } catch { }
                return;
            }

            // ---- Copie installée : une seule à la fois ----
            Mutex mutex;
            bool first;
            try { mutex = new Mutex(true, MutexName, out first); }
            catch (UnauthorizedAccessException) { return; } // déjà lancée avec plus de droits
            using var _m = mutex;
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
        readonly System.Collections.Generic.Queue<DateTime> _recentAvoided = new System.Collections.Generic.Queue<DateTime>();

        [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr h);

        // Crochet clavier global : on note seulement QUAND une touche est appuyée (jamais laquelle).
        delegate IntPtr KeyboardProc(int nCode, IntPtr wParam, IntPtr lParam);
        [DllImport("user32.dll", SetLastError = true)] static extern IntPtr SetWindowsHookEx(int idHook, KeyboardProc fn, IntPtr hMod, uint threadId);
        [DllImport("user32.dll")] static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);
        [DllImport("kernel32.dll", CharSet = CharSet.Unicode)] static extern IntPtr GetModuleHandle(string name);
        static KeyboardProc _keyProc; // gardé en mémoire pour que le ramasse-miettes ne le supprime pas
        static IntPtr _keyHook;

        static void InstallKeyHook()
        {
            _keyProc = (n, w, l) =>
            {
                if (n >= 0 && (w == (IntPtr)0x0100 || w == (IntPtr)0x0104)) Watcher.LastKeyTicks = DateTime.UtcNow.Ticks; // WM_KEYDOWN / WM_SYSKEYDOWN
                return CallNextHookEx(_keyHook, n, w, l);
            };
            _keyHook = SetWindowsHookEx(13 /* WH_KEYBOARD_LL */, _keyProc, GetModuleHandle(null), 0);
            if (_keyHook == IntPtr.Zero) Log.Write("Crochet clavier impossible : code " + Marshal.GetLastWin32Error());
        }

        public TrayApp(bool openSettings)
        {
            _ui.CreateControl();
            _ = _ui.Handle;

            var menu = new ContextMenuStrip();
            menu.Items.Add("Ouvrir MOI Bouclier", null, (s, e) => ShowSettings());
            _icon = new NotifyIcon { Icon = Theme.AppIcon, Text = "MOI — Bouclier actif", Visible = true, ContextMenuStrip = menu };
            _icon.DoubleClick += (s, e) => ShowSettings();

            InstallKeyHook();
            _watcher.Detected += (h, word, voluntary) => _ui.BeginInvoke(new Action(() => OnDetected(h, word, voluntary)));
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

        void OnDetected(IntPtr browser, string word, bool voluntary)
        {
            if (StopForm.IsShowing || Store.IsUnlocked) return;
            if ((DateTime.UtcNow - _lastStop).TotalMilliseconds < 1500) return;
            _lastStop = DateTime.UtcNow;

            // Garde-fou : 3 « involontaires » en 10 min, ce n'est plus un accident → écran STOP normal.
            if (!voluntary)
            {
                while (_recentAvoided.Count > 0 && (DateTime.UtcNow - _recentAvoided.Peek()).TotalMinutes > 10) _recentAvoided.Dequeue();
                if (_recentAvoided.Count >= 2) voluntary = true;
                else _recentAvoided.Enqueue(DateTime.UtcNow);
            }
            Log.Write((voluntary ? "Détecté : " : "Redirection / pub fermée : ") + word);
            // Ferme l'onglet fautif (Ctrl+W), puis affiche l'écran STOP.
            try
            {
                SetForegroundWindow(browser);
                Thread.Sleep(80);
                SendKeys.SendWait("^w");
            }
            catch { }
            if (voluntary) { StopForm.ShowAll(word); return; }
            Store.C.Avoided++;
            Store.Save();
            ProtectForm.Display(Store.C.Avoided);
        }
    }
}
