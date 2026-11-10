using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Automation;

namespace MoiBouclier
{
    /// <summary>
    /// Surveille la fenêtre au premier plan : si c'est un navigateur, on lit son titre (titre de la page,
    /// ou « … - Recherche Google ») et sa barre d'adresse (y compris ce qui est en train d'être tapé).
    /// </summary>
    public class Watcher
    {
        /// <summary>(fenêtre, mot trouvé, volontaire ?) — volontaire = c'est toi qui l'as tapé.</summary>
        public event Action<IntPtr, string, bool> Detected;
        public volatile bool Paused;

        /// <summary>Heure de la dernière touche du clavier (mise à jour par le crochet clavier du TrayApp).</summary>
        public static long LastKeyTicks;
        static double SinceKey => LastKeyTicks == 0 ? double.MaxValue : (DateTime.UtcNow.Ticks - LastKeyTicks) / (double)TimeSpan.TicksPerSecond;

        // Ce que tu as tapé toi-même dans la barre d'adresse (lu pendant que tu tapes).
        string _typed = "";
        DateTime _typedAt = DateTime.MinValue;

        readonly Dictionary<int, string> _procNames = new Dictionary<int, string>();
        readonly Dictionary<IntPtr, AutomationElement> _bars = new Dictionary<IntPtr, AutomationElement>();
        readonly Dictionary<IntPtr, DateTime> _retryAt = new Dictionary<IntPtr, DateTime>();
        Task<BarRead> _pendingRead;
        Thread _thread;

        [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
        [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr hWnd, out int pid);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern int GetWindowText(IntPtr hWnd, StringBuilder s, int max);

        public void Start()
        {
            _thread = new Thread(Loop) { IsBackground = true, Name = "MOI Watcher" };
            _thread.SetApartmentState(ApartmentState.MTA);
            _thread.Start();
        }

        void Loop()
        {
            while (true)
            {
                try { Tick(); }
                catch (Exception e) { Log.Write("Surveillance : " + e.Message); }
                Thread.Sleep(700);
            }
        }

        string ProcName(int pid)
        {
            if (_procNames.TryGetValue(pid, out var n)) return n;
            try { n = Process.GetProcessById(pid).ProcessName; } catch { n = ""; }
            if (_procNames.Count > 500) _procNames.Clear();
            _procNames[pid] = n;
            return n;
        }

        void Tick()
        {
            if (Paused || Store.IsUnlocked) return;
            var h = GetForegroundWindow();
            if (h == IntPtr.Zero) return;
            GetWindowThreadProcessId(h, out var pid);
            if (pid == Environment.ProcessId) return;
            var name = ProcName(pid);
            if (!Keywords.Browsers.Contains(name)) return;

            var extra = Store.C.ExtraWords;
            var sb = new StringBuilder(1024);
            GetWindowText(h, sb, sb.Capacity);
            var bar = ReadAddressBar(h, name);
            var barText = bar?.Text;

            // La barre a le curseur et tu viens de taper : c'est ton texte à toi.
            bool typingNow = bar != null && bar.Focused && SinceKey < 2;
            if (typingNow) { _typed = barText ?? ""; _typedAt = DateTime.UtcNow; }

            var hit = Keywords.Match(sb.ToString(), extra) ?? Keywords.Match(barText, extra);
            if (hit == null) return;
            Detected?.Invoke(h, hit, IsVoluntary(typingNow, extra));
        }

        /// <summary>
        /// Volontaire si : tu es en train de le taper, OU tu l'as tapé il y a moins d'1 min puis validé,
        /// OU tu viens d'appuyer sur une touche sans qu'on ait pu lire la barre (copier-coller + Entrée).
        /// Sinon (rien tapé : pub, pop-up, redirection, lien cliqué) → involontaire.
        /// </summary>
        bool IsVoluntary(bool typingNow, System.Collections.Generic.List<string> extra)
        {
            if (typingNow) return true;
            if ((DateTime.UtcNow - _typedAt).TotalSeconds < 60 && Keywords.Match(_typed, extra) != null) return true;
            bool sawTyping = (DateTime.UtcNow - _typedAt).TotalSeconds < SinceKey + 3;
            if (SinceKey < 1.5 && !sawTyping) return true;
            return false;
        }

        class BarRead { public string Text; public bool Focused; }

        /// <summary>Lit la barre d'adresse avec UI Automation, sans jamais bloquer la surveillance plus d'1 s.</summary>
        BarRead ReadAddressBar(IntPtr h, string proc)
        {
            if (_pendingRead != null && !_pendingRead.IsCompleted) return null; // une lecture précédente traîne encore
            _pendingRead = Task.Run(() => ReadBarNow(h, proc));
            return _pendingRead.Wait(1000) ? _pendingRead.Result : null;
        }

        BarRead ReadBarNow(IntPtr h, string proc)
        {
            try
            {
                AutomationElement bar;
                lock (_bars)
                {
                    if (!_bars.TryGetValue(h, out bar))
                    {
                        if (_retryAt.TryGetValue(h, out var at) && DateTime.UtcNow < at) return null;
                        var root = AutomationElement.FromHandle(h);
                        Condition cond = new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit);
                        if (proc.Equals("firefox", StringComparison.OrdinalIgnoreCase) || proc.Equals("librewolf", StringComparison.OrdinalIgnoreCase)
                            || proc.Equals("waterfox", StringComparison.OrdinalIgnoreCase) || proc.Equals("floorp", StringComparison.OrdinalIgnoreCase)
                            || proc.Equals("zen", StringComparison.OrdinalIgnoreCase))
                            cond = new AndCondition(cond, new PropertyCondition(AutomationElement.AutomationIdProperty, "urlbar-input"));
                        bar = root.FindFirst(TreeScope.Descendants, cond);
                        if (_bars.Count > 50) { _bars.Clear(); _retryAt.Clear(); }
                        if (bar != null) _bars[h] = bar;
                        else _retryAt[h] = DateTime.UtcNow.AddSeconds(5); // pas trouvée : on réessaie dans 5 s
                    }
                }
                if (bar == null) return null;
                var r = new BarRead { Focused = bar.Current.HasKeyboardFocus };
                r.Text = bar.TryGetCurrentPattern(ValuePattern.Pattern, out var p) ? ((ValuePattern)p).Current.Value : bar.Current.Name;
                return r;
            }
            catch
            {
                lock (_bars) { _bars.Remove(h); }
                return null;
            }
        }
    }
}
