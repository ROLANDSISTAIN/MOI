using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Security;
using System.Security.Principal;
using System.Text;
using Microsoft.Win32;

namespace MoiBouclier
{
    /// <summary>
    /// Les protections de fond : DNS filtrant (CleanBrowsing « Adult »), règles des navigateurs
    /// (SafeSearch, pas de navigation privée, pas de DNS chiffré qui contournerait le filtre),
    /// et la tâche planifiée qui relance MOI Bouclier à chaque connexion et chaque minute.
    /// </summary>
    public static class Guard
    {
        public const string TaskName = "MOI Bouclier";
        public static readonly string InstallDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "MOI Bouclier");
        public static readonly string ExePath = Path.Combine(InstallDir, "MOI-Bouclier.exe");

        // CleanBrowsing « Adult Filter » : bloque les sites adultes, force SafeSearch, laisse YouTube normal.
        static readonly string[] Dns4 = { "185.228.168.10", "185.228.169.11" };
        static readonly string[] Dns6 = { "2a0d:2a00:1::1", "2a0d:2a00:2::1" };

        public static bool IsAdmin
        {
            get
            {
                try { return new WindowsPrincipal(WindowsIdentity.GetCurrent()).IsInRole(WindowsBuiltInRole.Administrator); }
                catch { return false; }
            }
        }

        public static bool IsInstalledCopy =>
            string.Equals(Path.GetFullPath(Environment.ProcessPath ?? ""), Path.GetFullPath(ExePath), StringComparison.OrdinalIgnoreCase);

        // ------------------------------------------------------------------ outils

        public static int Run(string file, string args, int timeoutMs = 20000)
        {
            try
            {
                var psi = new ProcessStartInfo(file, args)
                {
                    CreateNoWindow = true, UseShellExecute = false,
                    RedirectStandardOutput = true, RedirectStandardError = true,
                };
                using var p = Process.Start(psi);
                p.StandardOutput.ReadToEndAsync();
                p.StandardError.ReadToEndAsync();
                if (!p.WaitForExit(timeoutMs)) { try { p.Kill(); } catch { } return -1; }
                return p.ExitCode;
            }
            catch (Exception e) { Log.Write($"{file} {args} : {e.Message}"); return -1; }
        }

        static string Sys(string exe) => Path.Combine(Environment.SystemDirectory, exe);

        // ------------------------------------------------------------------ installation

        public static void Install()
        {
            Directory.CreateDirectory(InstallDir);
            var me = Environment.ProcessPath;
            if (!string.Equals(Path.GetFullPath(me), Path.GetFullPath(ExePath), StringComparison.OrdinalIgnoreCase))
            {
                // Si une ancienne version tourne, on l'arrête pour pouvoir la remplacer.
                foreach (var p in Process.GetProcessesByName("MOI-Bouclier"))
                    if (p.Id != Environment.ProcessId) { try { p.Kill(); p.WaitForExit(3000); } catch { } }
                File.Copy(me, ExePath, true);
            }
            Store.C.Installed = true;
            Store.Save();
            Maintain(force: true);
            Run(Sys("schtasks.exe"), $"/Run /TN \"{TaskName}\"");
        }

        /// <summary>Remet en place toutes les protections si elles ont été modifiées.</summary>
        public static void Maintain(bool force = false)
        {
            if (!IsAdmin) return;
            if (Store.IsUnlocked && !force) return;
            try { ApplyDns(); } catch (Exception e) { Log.Write("DNS : " + e.Message); }
            try { ApplyPolicies(); } catch (Exception e) { Log.Write("Règles : " + e.Message); }
            try { if (force || !TaskExists()) CreateTask(); } catch (Exception e) { Log.Write("Tâche : " + e.Message); }
        }

        public static void Uninstall()
        {
            Run(Sys("schtasks.exe"), $"/Delete /TN \"{TaskName}\" /F");
            try { RestoreDns(); } catch (Exception e) { Log.Write("DNS : " + e.Message); }
            try { RemovePolicies(); } catch (Exception e) { Log.Write("Règles : " + e.Message); }
            // Le programme ne peut pas s'effacer lui-même pendant qu'il tourne : un petit script le fait 3 s après.
            var cmd = $"/c timeout /t 3 /nobreak >nul & rmdir /s /q \"{InstallDir}\" & rmdir /s /q \"{Store.Dir}\"";
            Process.Start(new ProcessStartInfo(Sys("cmd.exe"), cmd) { CreateNoWindow = true, UseShellExecute = false });
        }

        // ------------------------------------------------------------------ DNS

        static IEnumerable<NetworkInterface> Adapters() =>
            NetworkInterface.GetAllNetworkInterfaces().Where(n =>
                n.OperationalStatus == OperationalStatus.Up &&
                n.NetworkInterfaceType != NetworkInterfaceType.Loopback &&
                n.NetworkInterfaceType != NetworkInterfaceType.Tunnel);

        public static bool DnsOk()
        {
            try
            {
                var list = Adapters().ToList();
                return list.Count > 0 && list.All(n =>
                {
                    var v4 = n.GetIPProperties().DnsAddresses.Where(a => a.AddressFamily == AddressFamily.InterNetwork).Select(a => a.ToString()).ToList();
                    return v4.Count > 0 && v4.All(a => Dns4.Contains(a));
                });
            }
            catch { return false; }
        }

        static void ApplyDns()
        {
            bool changed = false;
            foreach (var n in Adapters())
            {
                var dns = n.GetIPProperties().DnsAddresses;
                var v4 = dns.Where(a => a.AddressFamily == AddressFamily.InterNetwork).Select(a => a.ToString()).ToList();
                var v6 = dns.Where(a => a.AddressFamily == AddressFamily.InterNetworkV6).Select(a => a.ToString().Split('%')[0]).ToList();
                var name = n.Name.Replace("\"", "");
                if (!(v4.Count > 0 && v4.All(a => Dns4.Contains(a)) && v4.Contains(Dns4[0])))
                {
                    Run(Sys("netsh.exe"), $"interface ipv4 set dnsservers name=\"{name}\" source=static address={Dns4[0]} register=primary validate=no");
                    Run(Sys("netsh.exe"), $"interface ipv4 add dnsservers name=\"{name}\" address={Dns4[1]} index=2 validate=no");
                    changed = true;
                }
                if (n.Supports(NetworkInterfaceComponent.IPv6) && !v6.Contains(Dns6[0]))
                {
                    Run(Sys("netsh.exe"), $"interface ipv6 set dnsservers name=\"{name}\" source=static address={Dns6[0]} register=primary validate=no");
                    Run(Sys("netsh.exe"), $"interface ipv6 add dnsservers name=\"{name}\" address={Dns6[1]} index=2 validate=no");
                    changed = true;
                }
            }
            if (changed)
            {
                Run(Sys("ipconfig.exe"), "/flushdns");
                Log.Write("Filtre DNS remis en place.");
            }
        }

        static void RestoreDns()
        {
            foreach (var n in NetworkInterface.GetAllNetworkInterfaces().Where(n => n.NetworkInterfaceType != NetworkInterfaceType.Loopback))
            {
                var name = n.Name.Replace("\"", "");
                Run(Sys("netsh.exe"), $"interface ipv4 set dnsservers name=\"{name}\" source=dhcp");
                Run(Sys("netsh.exe"), $"interface ipv6 set dnsservers name=\"{name}\" source=dhcp");
            }
            Run(Sys("ipconfig.exe"), "/flushdns");
        }

        // ------------------------------------------------------------------ règles des navigateurs

        class Pol { public string Key, Name; public object Value; public RegistryValueKind Kind; }

        static readonly Pol[] Policies =
        {
            // Google Chrome
            new Pol { Key = @"SOFTWARE\Policies\Google\Chrome", Name = "DnsOverHttpsMode", Value = "off", Kind = RegistryValueKind.String },
            new Pol { Key = @"SOFTWARE\Policies\Google\Chrome", Name = "IncognitoModeAvailability", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Google\Chrome", Name = "ForceGoogleSafeSearch", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Google\Chrome", Name = "BrowserGuestModeEnabled", Value = 0, Kind = RegistryValueKind.DWord },
            // Microsoft Edge
            new Pol { Key = @"SOFTWARE\Policies\Microsoft\Edge", Name = "DnsOverHttpsMode", Value = "off", Kind = RegistryValueKind.String },
            new Pol { Key = @"SOFTWARE\Policies\Microsoft\Edge", Name = "InPrivateModeAvailability", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Microsoft\Edge", Name = "ForceBingSafeSearch", Value = 2, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Microsoft\Edge", Name = "ForceGoogleSafeSearch", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Microsoft\Edge", Name = "BrowserGuestModeEnabled", Value = 0, Kind = RegistryValueKind.DWord },
            // Brave
            new Pol { Key = @"SOFTWARE\Policies\BraveSoftware\Brave", Name = "DnsOverHttpsMode", Value = "off", Kind = RegistryValueKind.String },
            new Pol { Key = @"SOFTWARE\Policies\BraveSoftware\Brave", Name = "IncognitoModeAvailability", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\BraveSoftware\Brave", Name = "ForceGoogleSafeSearch", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\BraveSoftware\Brave", Name = "BrowserGuestModeEnabled", Value = 0, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\BraveSoftware\Brave", Name = "TorDisabled", Value = 1, Kind = RegistryValueKind.DWord },
            // Firefox
            new Pol { Key = @"SOFTWARE\Policies\Mozilla\Firefox", Name = "DisablePrivateBrowsing", Value = 1, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Mozilla\Firefox\DNSOverHTTPS", Name = "Enabled", Value = 0, Kind = RegistryValueKind.DWord },
            new Pol { Key = @"SOFTWARE\Policies\Mozilla\Firefox\DNSOverHTTPS", Name = "Locked", Value = 1, Kind = RegistryValueKind.DWord },

            // Bloqueur de pubs installé d'office et impossible à retirer (uBlock Origin) :
            // moins de pubs = moins de redirections vers des sites adultes.
            new Pol { Key = @"SOFTWARE\Policies\Google\Chrome\ExtensionInstallForcelist", Name = "4701", Value = UboLiteChrome, Kind = RegistryValueKind.String },
            new Pol { Key = @"SOFTWARE\Policies\BraveSoftware\Brave\ExtensionInstallForcelist", Name = "4701", Value = UboLiteChrome, Kind = RegistryValueKind.String },
            new Pol { Key = @"SOFTWARE\Policies\Microsoft\Edge\ExtensionInstallForcelist", Name = "4701", Value = UboLiteEdge, Kind = RegistryValueKind.String },
            new Pol { Key = @"SOFTWARE\Policies\Mozilla\Firefox", Name = "ExtensionSettings", Value = UboFirefox, Kind = RegistryValueKind.String },
        };

        const string UboLiteChrome = "ddkjiahejlhfcafbddmgiahcphecmpfh;https://clients2.google.com/service/update2/crx";
        const string UboLiteEdge = "cimighlppcgcoapaliogpjjdehbnofhn;https://edge.microsoft.com/extensionwebstorecdn/v1/crx";
        const string UboFirefox = "{\"uBlock0@raymondhill.net\":{\"installation_mode\":\"force_installed\",\"install_url\":\"https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi\"}}";

        static RegistryKey Hklm => RegistryKey.OpenBaseKey(RegistryHive.LocalMachine, RegistryView.Registry64);

        public static bool PoliciesOk()
        {
            try
            {
                using var root = Hklm;
                return Policies.All(p =>
                {
                    using var k = root.OpenSubKey(p.Key);
                    return k != null && Equals(k.GetValue(p.Name)?.ToString(), p.Value.ToString());
                });
            }
            catch { return false; }
        }

        static void ApplyPolicies()
        {
            using var root = Hklm;
            foreach (var p in Policies)
            {
                using var k = root.CreateSubKey(p.Key, true);
                if (!Equals(k.GetValue(p.Name)?.ToString(), p.Value.ToString())) k.SetValue(p.Name, p.Value, p.Kind);
            }
        }

        static void RemovePolicies()
        {
            using var root = Hklm;
            foreach (var p in Policies)
            {
                using var k = root.OpenSubKey(p.Key, true);
                k?.DeleteValue(p.Name, false);
            }
            try { root.DeleteSubKey(@"SOFTWARE\Policies\Mozilla\Firefox\DNSOverHTTPS", false); } catch { }
        }

        // ------------------------------------------------------------------ tâche planifiée

        public static bool TaskExists() => Run(Sys("schtasks.exe"), $"/Query /TN \"{TaskName}\"") == 0;

        static void CreateTask()
        {
            var user = SecurityElement.Escape(WindowsIdentity.GetCurrent().Name);
            var exe = SecurityElement.Escape(ExePath);
            var xml = $@"<?xml version=""1.0"" encoding=""UTF-16""?>
<Task version=""1.2"" xmlns=""http://schemas.microsoft.com/windows/2004/02/mit/task"">
  <RegistrationInfo><Description>MOI — Bouclier PC</Description></RegistrationInfo>
  <Triggers>
    <LogonTrigger><Enabled>true</Enabled><UserId>{user}</UserId></LogonTrigger>
    <TimeTrigger>
      <Repetition><Interval>PT1M</Interval><StopAtDurationEnd>false</StopAtDurationEnd></Repetition>
      <StartBoundary>2026-01-01T00:00:00</StartBoundary>
      <Enabled>true</Enabled>
    </TimeTrigger>
  </Triggers>
  <Principals>
    <Principal id=""Author""><UserId>{user}</UserId><LogonType>InteractiveToken</LogonType><RunLevel>HighestAvailable</RunLevel></Principal>
  </Principals>
  <Settings>
    <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>
    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
    <AllowHardTerminate>false</AllowHardTerminate>
    <StartWhenAvailable>true</StartWhenAvailable>
    <RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>
    <IdleSettings><StopOnIdleEnd>false</StopOnIdleEnd><RestartOnIdle>false</RestartOnIdle></IdleSettings>
    <AllowStartOnDemand>true</AllowStartOnDemand>
    <Enabled>true</Enabled>
    <Hidden>false</Hidden>
    <RunOnlyIfIdle>false</RunOnlyIfIdle>
    <ExecutionTimeLimit>PT0S</ExecutionTimeLimit>
    <Priority>7</Priority>
  </Settings>
  <Actions Context=""Author"">
    <Exec><Command>{exe}</Command><Arguments>--bg</Arguments></Exec>
  </Actions>
</Task>";
            var tmp = Path.Combine(Path.GetTempPath(), "moi_bouclier_task.xml");
            File.WriteAllText(tmp, xml, Encoding.Unicode);
            var code = Run(Sys("schtasks.exe"), $"/Create /TN \"{TaskName}\" /XML \"{tmp}\" /F");
            try { File.Delete(tmp); } catch { }
            if (code != 0) Log.Write("Création de la tâche : code " + code);
        }
    }
}
