package it.polimi.elet.necst.heldroid.apk;

import java.io.File;

public interface DecodedPackage {
    static final String CLASSES_DEX_FILE_NAME = "classes.dex";
    static final String ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml";
    static final String SMALI_DIRECTORY_NAME = "smali";

    File getClassesDex();
    File getAndroidManifest();
    File getDecodedDirectory();
    File getSmaliDirectory();
    File getOriginalApk();

    void dispose();
}
