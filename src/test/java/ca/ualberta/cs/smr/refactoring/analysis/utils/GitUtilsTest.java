package ca.ualberta.cs.smr.refactoring.analysis.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitUtilsTest {

    private GitUtils GIT_UTILS_TEST = GitUtils.getMockInstance();

    @Test
    void isConflictingFromMergeOutput() throws Exception {
        String mergeOutput = "Auto-merging roboguice/src/test/java/roboguice/event/ObservesTypeListenerTest.java\n" +
                "CONFLICT (content): Merge conflict in roboguice/src/test/java/roboguice/event/ObservesTypeListenerTest.java\n" +
                "Auto-merging roboguice/src/main/java/roboguice/service/RoboService.java\n" +
                "Auto-merging roboguice/src/main/java/roboguice/service/RoboIntentService.java\n" +
                "Auto-merging roboguice/src/main/java/roboguice/event/EventManager.xml\n" +
                "CONFLICT (content): Merge conflict in roboguice/src/main/java/roboguice/event/EventManager.xml\n" +
                "Auto-merging roboguice/src/main/java/roboguice/config/RoboModule.java\n" +
                "CONFLICT (content): Merge conflict in roboguice/src/main/java/roboguice/config/RoboModule.java\n" +
                "CONFLICT (modify/delete): roboguice/src/main/java/roboguice/application/RoboApplication.java deleted in HEAD and modified in bee336bae0845a0e49037d44efb0e1f5d90105e9. Version bee336bae0845a0e49037d44efb0e1f5d90105e9 of roboguice/src/main/java/roboguice/application/RoboApplication.java left in tree.\n" +
                "Auto-merging roboguice/src/main/java/roboguice/activity/RoboTabActivity.java\n" +
                "Auto-merging api/src/main/java/org/apache/cxf/databinding/WrapperCapableDatabinding.java\n" +
                "CONFLICT (add/add): Merge conflict in api/src/main/java/org/apache/cxf/databinding/WrapperCapableDatabinding.java\n" +
                "Auto-merging astroboy/src/roboguice/astroboy/activity/AstroPrefActivity.java\n" +
                "CONFLICT (rename/delete): astroboy/src/roboguice/astroboy/AstroboyApplication.java deleted in HEAD and renamed in bee336bae0845a0e49037d44efb0e1f5d90105e9. Version bee336bae0845a0e49037d44efb0e1f5d90105e9 of astroboy/src/roboguice/astroboy/AstroboyApplication.java left in tree.\n" +
                "CONFLICT (rename/rename): Rename \"core/src/com/google/inject/AnnotatedGuiceHierarchyTraversalFilter.java\"->\"core/src/com/google/inject/AnnotatedHierarchyTraversalFilter.java\" in branch \"HEAD\" rename \"core/src/com/google/inject/AnnotatedGuiceHierarchyTraversalFilter.java\"->\"guice/core/src/com/google/inject/AnnotatedGuiceHierarchyTraversalFilter.java\" in \"9417ae6b9cc80dda19232a530c5c7586b70b5e1e\"\n" +
                "CONFLICT (content): Merge conflict in TFC API/TFC/API/Constant/TFCBlockID.java\n" +
                "CONFLICT (rename/add): Rename src/org/numenta/nupic/data/SparseBinaryMatrix.java->src/main/java/org/numenta/nupic/data/SparseBinaryMatrix.java in 03eb1c1b15b058048efd17dc145d85a337f2b258. src/main/java/org/numenta/nupic/data/SparseBinaryMatrix.java added in HEAD\n" +
                "CONFLICT (rename/rename): Rename plugin_ide.ui/src-lang/melnorme/lang/ide/ui/preferences/ValidatedConfigBlock.java->plugin_ide.ui/src-lang/melnorme/lang/ide/ui/preferences/common/AbstractValidatedBlockExt.java in 614ac2a4c546271a5eb0c42f3bbd868f40cb1431. Rename plugin_ide.ui/src/LANG_PROJECT_ID/ide/ui/launch/LANGUAGE_LaunchShortcut.java->plugin_ide.ui/src-lang/melnorme/lang/ide/ui/preferences/common/AbstractValidatedBlockExt.java in HEAD\n" +
                "CONFLICT (not covered): Something else happened to a .java file.\n" +
                "CONFLICT (not covered): Something else happened to this file: A.cpp\n" +
                "CONFLICT (rename/delete): res/values/phrase_editor.java deleted in HEAD and renamed to app/src/main/res/values/phrase_editor.xml in 9cd3c3549ac0e705746ecc781f3b6f4c90133e9a. Version 9cd3c3549ac0e705746ecc781f3b6f4c90133e9a of app/src/main/res/values/phrase_editor.xml left in tree.\n" +
                "Auto-merging astroboy/pom.xml\n" +
                "Automatic merge failed; fix conflicts and then commit the result.\n";
        Map<String, String> expectedJavaConflicts = new HashMap<>();
        expectedJavaConflicts.put("roboguice/src/test/java/roboguice/event/ObservesTypeListenerTest.java", "content");
        expectedJavaConflicts.put("roboguice/src/main/java/roboguice/config/RoboModule.java", "content");
        expectedJavaConflicts.put("roboguice/src/main/java/roboguice/application/RoboApplication.java", "modify/delete");
        expectedJavaConflicts.put("api/src/main/java/org/apache/cxf/databinding/WrapperCapableDatabinding.java", "add/add");
        expectedJavaConflicts.put("astroboy/src/roboguice/astroboy/AstroboyApplication.java", "rename/delete");
        expectedJavaConflicts.put("core/src/com/google/inject/AnnotatedHierarchyTraversalFilter.java", "rename/rename");
        expectedJavaConflicts.put("TFC API/TFC/API/Constant/TFCBlockID.java", "content");
        expectedJavaConflicts.put("src/main/java/org/numenta/nupic/data/SparseBinaryMatrix.java", "rename/add");
        expectedJavaConflicts.put("plugin_ide.ui/src-lang/melnorme/lang/ide/ui/preferences/common/AbstractValidatedBlockExt.java", "rename/rename");
        expectedJavaConflicts.put("CONFLICT (not covered): Something else happened to a .java file.", "Undetected");
        expectedJavaConflicts.put("res/values/phrase_editor.java", "rename/delete");

        Map<String, String> actualJavaConflicts = new HashMap<>();
        assertTrue(GIT_UTILS_TEST.isConflictingFromMergeOutput(mergeOutput, actualJavaConflicts));

        assertEquals(expectedJavaConflicts, actualJavaConflicts);
    }

    @Test
    void getConflictingRegionsFromDiffOutput() {
        String diffOutput = "diff --cc api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "index d2ba74a,ae4655c..0000000\n" +
                "--- a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "+++ b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBindingNew.java\n" +
                "@@@ -59,0 -62,18 +62,21 @@@ public abstract class AbstractDataBindi\n" +
                "++<<<<<<< HEAD\n" +
                "++=======\n" +
                "+ \n" +
                "+     protected Bus getBus() {\n" +
                "+         if (bus == null) {\n" +
                "+             return BusFactory.getDefaultBus();\n" +
                "+         }\n" +
                "+         return bus;\n" +
                "+     }\n" +
                "+ \n" +
                "+     /**\n" +
                "+      * This call is used to set the bus. It should only be called once.\n" +
                "+      * \n" +
                "+      * @param bus\n" +
                "+      */\n" +
                "+     @Resource(name = \"cxf\")\n" +
                "+     public void setBus(Bus bus) {\n" +
                "+         assert this.bus == null || this.bus == bus;\n" +
                "+         this.bus = bus;\n" +
                "+     }\n" +
                "++>>>>>>> df6cb4842dca7d9aca02a654cb269b00fb6c95c0\n" +
                "@@@ -72,0 -93,1 +96,4 @@@\n" +
                "++<<<<<<< HEAD\n" +
                "++=======\n" +
                "+ \n" +
                "++>>>>>>> df6cb4842dca7d9aca02a654cb269b00fb6c95c0\n" +
                "@@@ -110,0 -132,1 +138,4 @@@\n" +
                "++<<<<<<< HEAD\n" +
                "++=======\n" +
                "+         \n" +
                "++>>>>>>> df6cb4842dca7d9aca02a654cb269b00fb6c95c0\n";
        String[] expectedConflictingRegionPaths = new String[]{
                "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                "api/src/main/java/org/apache/cxf/databinding/AbstractDataBindingNew.java"};
        List<int[][]> expectedConflictingRegions = Arrays.asList(
                new int[][]{{59, 0},{62, 18}},
                new int[][]{{72, 0},{93, 1}},
                new int[][]{{110, 0},{132, 1}});

        String[] actualConflictingRegionPaths = new String[2];
        List<int[][]> actualConflictingRegions = new ArrayList<>();
        GIT_UTILS_TEST.getConflictingRegionsFromDiffOutput(diffOutput, actualConflictingRegionPaths,
                actualConflictingRegions);

        assertTrue(Arrays.equals(expectedConflictingRegionPaths, actualConflictingRegionPaths));
        assertEquals(expectedConflictingRegions.size(), actualConflictingRegions.size());
        for (int i = 0; i < expectedConflictingRegions.size(); i++) {
            assertTrue(Arrays.deepEquals(expectedConflictingRegions.get(i), actualConflictingRegions.get(i)));
        }
    }

    @Test
    void getConflictingRegionHistoryFromGitOutput() {
        String logOutput = "commit baef2f510c06096975e435609509e644a531874c\n" +
                "Author: J. Daniel Kulp <dkulp@apache.org>\n" +
                "Alright everybody, this next one's coming straight from the heart\n" +
                "Making the lyrics up right off the top of my head\n" +
                "diff --git a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "--- a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBindingNew.java\n" +
                "+++ b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "@@ -18,46 +18,45 @@\n" +
                "Let me out, what you see is not the same person as me\n" +
                "My life's a lie, I'm not who you're looking at\n" +
                "commit 01251c9936ba0e7357b0fcbbf2d391f99714e4a3\n" +
                "Author: J. Daniel Kulp <dkulp@apache.org>\n" +
                "Date:   Tue Sep 2 16:34:56 2008 +0000\n" +
                "diff --git a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "--- a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "+++ b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "@@ -18,44 +18,46 @@\n" +
                "Let me out, set me free, I'm really old\n" +
                "This isn't me\n" +
                "commit bb65155165ff01ec0b0c8875aa242d476bf2bc2b\n" +
                "Author: J. Daniel Kulp <dkulp@apache.org>\n" +
                "Date:   Thu May 1 15:14:31 2008 +0000\n" +
                "diff --git a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "--- a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "+++ b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "@@ -18,45 +18,44 @@\n" +
                "My real body's slowing dying in a vat\n" +
                "Is anybody listening, can anyone understand\n" +
                "commit 88a9451b92180609b7bb93d0314bc8384f0c24a4\n" +
                "Author: J. Daniel Kulp <dkulp@apache.org>\n" +
                "Date:   Wed Apr 23 17:50:00 2008 +0000\n" +
                "Stop looking at me like that and actually help me\n" +
                "Help me, help me I'm gonna die\n" +
                "diff --git a/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "--- /dev/null\n" +
                "+++ b/api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java\n" +
                "@@ -0,0 +18,45 @@\n" +
                "Tiny Rick\n";
        List<GitUtils.CodeRegionChange> expectedCodeRegionChanges = Arrays.asList(
                new GitUtils.CodeRegionChange("baef2f510c06096975e435609509e644a531874c",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBindingNew.java",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                        18,46, 18,45),
                new GitUtils.CodeRegionChange("01251c9936ba0e7357b0fcbbf2d391f99714e4a3",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                        18,44 ,18,46),
                new GitUtils.CodeRegionChange("bb65155165ff01ec0b0c8875aa242d476bf2bc2b",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                        18,45, 18,44),
                new GitUtils.CodeRegionChange("88a9451b92180609b7bb93d0314bc8384f0c24a4",
                        "/dev/null",
                        "api/src/main/java/org/apache/cxf/databinding/AbstractDataBinding.java",
                        0,0, 18,45)
        );
        List<GitUtils.CodeRegionChange> actualCodeRegionChanges = new ArrayList<>();
        GIT_UTILS_TEST.getConflictingRegionHistoryFromGitOutput(logOutput, actualCodeRegionChanges);
        assertEquals(expectedCodeRegionChanges, actualCodeRegionChanges);
    }
}