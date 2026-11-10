using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;

namespace MoiBouclier
{
    public class Config
    {
        public string Message { get; set; } = "Tu vaux plus que quelques minutes de plaisir. Rappelle-toi l'homme que tu veux devenir.";
        public string Verse { get; set; } = "« Fuyez la débauche. » — 1 Corinthiens 6:18";
        public string Letter { get; set; } = "";
        public string Phrase { get; set; } = "Je choisis la liberté. Je deviens l'homme que Dieu veut que je sois.";
        public int DelaySec { get; set; } = 30;
        public List<string> ExtraWords { get; set; } = new List<string>();
        public int Stopped { get; set; }
        public long LastStop { get; set; }
        /// <summary>Pubs / redirections / pop-ups fermées sans que tu les aies cherchées (pas des tentations).</summary>
        public int Avoided { get; set; }
        public long UnlockRequestedAt { get; set; }
        public long UnlockUntil { get; set; }
        public bool Installed { get; set; }
    }

    /// <summary>Réglages dans C:\ProgramData\MOI Bouclier\config.json (modifiable seulement en administrateur).</summary>
    public static class Store
    {
        public static readonly string Dir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "MOI Bouclier");
        static readonly string FilePath = Path.Combine(Dir, "config.json");
        static readonly object Lock = new object();
        static Config _c;

        public static Config C
        {
            get { lock (Lock) { return _c ??= Load(); } }
        }

        static Config Load()
        {
            try
            {
                if (File.Exists(FilePath))
                    return JsonSerializer.Deserialize<Config>(File.ReadAllText(FilePath)) ?? new Config();
            }
            catch (Exception e) { Log.Write("Lecture config : " + e.Message); }
            return new Config();
        }

        public static void Save()
        {
            lock (Lock)
            {
                try
                {
                    Directory.CreateDirectory(Dir);
                    var tmp = FilePath + ".tmp";
                    File.WriteAllText(tmp, JsonSerializer.Serialize(_c ?? new Config(), new JsonSerializerOptions { WriteIndented = true }));
                    File.Copy(tmp, FilePath, true);
                    File.Delete(tmp);
                }
                catch (Exception e) { Log.Write("Écriture config : " + e.Message); }
            }
        }

        public static long Now => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        public const long HourMs = 3600_000L;

        public static bool IsUnlocked => Now < C.UnlockUntil;

        /// <summary>Le déverrouillage par attente est possible entre 24 h et 48 h après la demande.</summary>
        public static bool WaitDone => C.UnlockRequestedAt > 0 && Now >= C.UnlockRequestedAt + 24 * HourMs && Now < C.UnlockRequestedAt + 48 * HourMs;

        public static bool WaitPending => C.UnlockRequestedAt > 0 && Now < C.UnlockRequestedAt + 24 * HourMs;

        public static void Unlock15Min()
        {
            C.UnlockUntil = Now + 15 * 60_000L;
            C.UnlockRequestedAt = 0;
            Save();
        }

        public static bool PhraseMatches(string typed)
        {
            string n(string s) => string.Join(" ", Keywords.Normalize(s ?? "").Split(new[] { ' ', '\t', '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries))
                .TrimEnd('.', ' ');
            return n(typed).Length > 0 && n(typed) == n(C.Phrase);
        }
    }

    public static class Log
    {
        public static void Write(string msg)
        {
            try
            {
                Directory.CreateDirectory(Store.Dir);
                var f = Path.Combine(Store.Dir, "log.txt");
                if (File.Exists(f) && new FileInfo(f).Length > 200_000) File.Delete(f);
                File.AppendAllText(f, DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + "  " + msg + Environment.NewLine);
            }
            catch { }
        }
    }
}
