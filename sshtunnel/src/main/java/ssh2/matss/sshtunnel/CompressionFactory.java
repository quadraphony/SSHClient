package ssh2.matss.sshtunnel;

import ssh2.matss.sshtunnel.aidl.ICompressor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class CompressionFactory {
    private CompressionFactory() { }

    private static class CompressorEntry
    {
        String type;
        String compressorClass;

        private CompressorEntry(String type, String compressorClass)
        {
            this.type = type;
            this.compressorClass = compressorClass;
        }
    }

    private static List<CompressorEntry> compressors = new ArrayList<>();

    static
    {
        /* Higher Priority First */
        compressors.add(new CompressorEntry("zlib", "com.trilead.ssh2.compression.Zlib"));
        compressors.add(new CompressorEntry("zlib@openssh.com", "com.trilead.ssh2.compression.ZlibOpenSSH"));
        compressors.add(new CompressorEntry("none", ""));
    }

    static void addCompressor(String protocolName, String className) {
        compressors.add(new CompressorEntry(protocolName, className));
    }

    public static String[] getDefaultCompressorList()
    {
        String[] list = new String[compressors.size()];
        for (int i = 0; i < compressors.size(); i++)
        {
            CompressorEntry ce = compressors.get(i);
            list[i] = ce.type;
        }
        return list;
    }

    public static void checkCompressorList(String[] compressorCandidates)
    {
        for (String compressorCandidate : compressorCandidates) {
            getEntry(compressorCandidate);
        }
    }

    public static ICompressor createCompressor(String type)
    {
        try
        {
            CompressorEntry ce = getEntry(type);
            if ("".equals(ce.compressorClass))
                return null;

            Class<?> cc = Class.forName(ce.compressorClass);
            Constructor<?> constructor = cc.getConstructor();
            return (ICompressor) constructor.newInstance();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Cannot instantiate " + type);
        }
    }

    private static CompressorEntry getEntry(String type)
    {
        for (CompressorEntry ce : compressors) {
            if (ce.type.equals(type))
                return ce;
        }
        throw new IllegalArgumentException("Unknown algorithm " + type);
    }
}

