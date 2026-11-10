using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace MoiBouclier
{
    /// <summary>Détection des recherches et sites à caractère sexuel (même liste que le Bouclier Android).</summary>
    public static class Keywords
    {
        /// <summary>Mots détectés seulement comme mot entier.</summary>
        static readonly string[] Words =
        {
            "porn", "porno", "pornographie", "pornographique", "porne", "xxx", "sexe", "sex", "sexy",
            "nude", "nudes", "nue", "nues", "naked", "nsfw", "hentai", "rule34", "r34",
            "milf", "boobs", "tits", "seins nus", "fesses nues", "pussy", "blowjob",
            "sodomie", "masturbation", "masturber", "branlette", "orgasme",
            "erotique", "erotic", "camgirl", "cam girl", "webcam sexe", "onlyfans", "only fans",
            "fansly", "escorte girl", "strip tease", "striptease", "lingerie sexy",
            "baise", "plan cul", "salope", "chaudasse", "stepmom", "stepsister",
            "sextape", "sex tape", "leak nude", "deepnude", "xvideo", "xvideos", "xnxx", "xhamster",
            "pornhub", "youporn", "redtube", "brazzers", "spankbang", "eporner", "tnaflix", "porntrex",
            "chaturbate", "stripchat", "bongacams", "livejasmin", "cam4", "camsoda",
            "jacquieetmichel", "jacquie et michel", "nhentai", "hanime", "motherless", "beeg",
            "tube8", "porn300", "hqporner", "daftsex", "sxyprn", "thothub", "fapello", "erome",
        };

        /// <summary>Morceaux d'adresses web, détectés n'importe où.</summary>
        static readonly string[] DomainParts =
        {
            "pornhub", "xvideos", "xnxx", "xhamster", "youporn", "redtube", "brazzers", "spankbang",
            "eporner", "tnaflix", "porntrex", "chaturbate", "stripchat", "bongacams", "livejasmin",
            "camsoda", "onlyfans", "fansly", "nhentai", "hanime", "motherless", "hqporner",
            "daftsex", "sxyprn", "thothub", "fapello", "erome.com", "rule34", "jacquieetmichel",
            "porn", "xxx", "sex.com", "hentai", "nsfw",
        };

        /// <summary>Navigateurs surveillés (nom du processus, sans .exe).</summary>
        public static readonly HashSet<string> Browsers = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
        {
            "chrome", "msedge", "firefox", "opera", "opera_gx", "brave", "vivaldi", "yandex", "browser",
            "iexplore", "waterfox", "librewolf", "floorp", "thorium", "chromium", "epic", "torch", "maxthon",
            "ucbrowser", "palemoon", "seamonkey", "arc", "zen", "duckduckgo", "comet", "avastbrowser",
            "avgbrowser", "ccleanerbrowser", "coccoc", "360chrome", "qqbrowser", "sogouexplorer", "tor",
        };

        static Regex _regex;
        static string _regexKey;

        public static string Normalize(string s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            var d = s.ToLowerInvariant().Normalize(NormalizationForm.FormD);
            var sb = new StringBuilder(d.Length);
            foreach (var c in d)
                if (CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark) sb.Append(c);
            return sb.ToString();
        }

        static Regex WordRegex(IEnumerable<string> extra)
        {
            var all = Words.Concat(extra.Select(Normalize).Where(w => w.Length >= 3)).Distinct().ToList();
            var key = string.Join("|", all);
            if (_regex != null && _regexKey == key) return _regex;
            var alt = string.Join("|", all.OrderByDescending(w => w.Length).Select(Regex.Escape));
            _regex = new Regex("(^|[^a-z0-9])(" + alt + ")([^a-z0-9]|$)", RegexOptions.Compiled);
            _regexKey = key;
            return _regex;
        }

        /// <summary>Renvoie le mot détecté, ou null si le texte est propre.</summary>
        public static string Match(string raw, IEnumerable<string> extra)
        {
            if (string.IsNullOrWhiteSpace(raw) || raw.Length < 2) return null;
            var t = Normalize(raw);
            foreach (var p in DomainParts) if (t.Contains(p)) return p;
            var spaced = t.Replace('+', ' ').Replace('_', ' ').Replace('-', ' ').Replace("%20", " ");
            var m = WordRegex(extra ?? Enumerable.Empty<string>()).Match(spaced);
            return m.Success ? m.Groups[2].Value : null;
        }
    }
}
