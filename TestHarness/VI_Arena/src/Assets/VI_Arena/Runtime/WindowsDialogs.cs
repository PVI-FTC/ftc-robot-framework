using System;
using System.Runtime.InteropServices;

namespace VI_Arena
{
    /// <summary>
    /// The Windows "Open" and "Browse for folder" boxes for the built app. Unity's own file picker only
    /// exists in the Editor, so the built app asks Windows directly. Each call waits until the person
    /// closes the box (it only runs when they press a button) and returns null if they cancel or the
    /// box can't open.
    /// </summary>
    public static class WindowsDialogs
    {
        /// <summary>
        /// Asks for one file. <paramref name="filter"/> is Windows' "name\0pattern\0" list, e.g.
        /// "Run recordings (*.json)\0*.json\0".
        /// </summary>
        public static string OpenFile(string title, string startFolder, string filter)
        {
            try
            {
                var box = new OpenFileName();
                box.structSize = Marshal.SizeOf(box);
                box.owner = GetActiveWindow();
                box.filter = filter + "\0";
                box.file = new string('\0', 2048);
                box.maxFile = box.file.Length;
                box.initialDir = startFolder;
                box.title = title;
                box.flags = OfnFileMustExist | OfnPathMustExist | OfnNoChangeDir | OfnExplorer;
                return GetOpenFileNameW(box) ? box.file.TrimEnd('\0') : null;
            }
            catch (Exception e) when (e is DllNotFoundException || e is EntryPointNotFoundException || e is MarshalDirectiveException)
            {
                return null;
            }
        }

        /// <summary>Asks for a folder, with <paramref name="title"/> as the question above the folder tree.</summary>
        public static string PickFolder(string title)
        {
            try
            {
                // The folder box needs COM; the newer, resizable look also needs a single-threaded one.
                int com = CoInitializeEx(IntPtr.Zero, CoinitApartmentThreaded);
                var info = new BrowseInfo
                {
                    owner = GetActiveWindow(),
                    title = title,
                    flags = BifReturnOnlyFsDirs | (com >= 0 ? BifNewDialogStyle : 0),
                };
                IntPtr picked = SHBrowseForFolderW(ref info);
                if (picked == IntPtr.Zero)
                {
                    return null;
                }
                var path = new System.Text.StringBuilder(1024);
                bool ok = SHGetPathFromIDListW(picked, path);
                CoTaskMemFree(picked);
                return ok ? path.ToString() : null;
            }
            catch (Exception e) when (e is DllNotFoundException || e is EntryPointNotFoundException || e is MarshalDirectiveException)
            {
                return null;
            }
        }

        private const int OfnFileMustExist = 0x1000;
        private const int OfnPathMustExist = 0x800;
        private const int OfnNoChangeDir = 0x8;
        private const int OfnExplorer = 0x80000;
        private const uint BifReturnOnlyFsDirs = 0x1;
        private const uint BifNewDialogStyle = 0x40;
        private const int CoinitApartmentThreaded = 0x2;

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private class OpenFileName
        {
            public int structSize;
            public IntPtr owner;
            public IntPtr instance;
            public string filter;
            public string customFilter;
            public int maxCustomFilter;
            public int filterIndex;
            public string file;
            public int maxFile;
            public string fileTitle;
            public int maxFileTitle;
            public string initialDir;
            public string title;
            public int flags;
            public short fileOffset;
            public short fileExtension;
            public string defExt;
            public IntPtr customData;
            public IntPtr hook;
            public string templateName;
            public IntPtr reserved;
            public int reserved2;
            public int flagsEx;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct BrowseInfo
        {
            public IntPtr owner;
            public IntPtr root;
            public IntPtr displayName;
            public string title;
            public uint flags;
            public IntPtr callback;
            public IntPtr parameter;
            public int image;
        }

        [DllImport("comdlg32.dll", CharSet = CharSet.Unicode)]
        private static extern bool GetOpenFileNameW([In, Out] OpenFileName box);

        [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
        private static extern IntPtr SHBrowseForFolderW(ref BrowseInfo info);

        [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
        private static extern bool SHGetPathFromIDListW(IntPtr idList, System.Text.StringBuilder path);

        [DllImport("ole32.dll")]
        private static extern int CoInitializeEx(IntPtr reserved, int mode);

        [DllImport("ole32.dll")]
        private static extern void CoTaskMemFree(IntPtr memory);

        [DllImport("user32.dll")]
        private static extern IntPtr GetActiveWindow();
    }
}
