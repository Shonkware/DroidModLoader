package com.shonkware.droidmodloader.engine.rules

import com.shonkware.droidmodloader.engine.model.DeployScope

class DeployFileClassifier {

    fun classify(normalizedPath: String): DeployScope {
        val path = normalizedPath
            .replace("\\", "/")
            .lowercase()
            .trim('/')

        val fileName = path.substringAfterLast('/')

        return when {
            path.isBlank() ->
                DeployScope.IGNORE

            isUnsafePath(path) ->
                DeployScope.IGNORE

            path == "plugins.txt" || path == "loadorder.txt" ->
                DeployScope.PROFILE_ONLY

            isIgnored(path) ->
                DeployScope.IGNORE

            isSetupOnly(path) ->
                DeployScope.MANAGER_ONLY

            isDocumentation(path, fileName) ->
                DeployScope.MANAGER_ONLY

            isSourceOrDevFile(path, fileName) ->
                DeployScope.MANAGER_ONLY

            isProbableGameRootFile(path) ->
                DeployScope.GAME_ROOT

            else ->
                DeployScope.DATA
        }
    }

    fun isDataDeployable(scope: DeployScope): Boolean {
        return scope == DeployScope.DATA
    }

    fun isGameRootDeployable(scope: DeployScope): Boolean {
        return scope == DeployScope.GAME_ROOT
    }

    fun isDeployable(scope: DeployScope): Boolean {
        return scope == DeployScope.DATA || scope == DeployScope.GAME_ROOT
    }

    fun isDeployableToCurrentStaging(scope: DeployScope): Boolean {
        return isDataDeployable(scope)
    }

    private fun isUnsafePath(path: String): Boolean {
        return path.startsWith("/") ||
                path.startsWith("\\") ||
                path.contains("\\") ||
                path.contains(":") ||
                path.split("/").any { it == ".." }
    }

    private fun isIgnored(path: String): Boolean {
        val fileName = path.substringAfterLast("/")

        return path.startsWith("__macosx/") ||
                path.startsWith(".git/") ||
                path.startsWith(".github/") ||
                path.startsWith(".svn/") ||
                fileName == ".ds_store" ||
                fileName == "thumbs.db" ||
                fileName.endsWith(".url") ||
                fileName.endsWith(".lnk") ||
                fileName.endsWith(".bak") ||
                fileName.endsWith(".tmp")
    }

    private fun isSetupOnly(path: String): Boolean {
        return path.startsWith("fomod/") ||
                path.startsWith("omod conversion data/") ||
                path.startsWith("omod/") ||
                path.startsWith("wizard images/") ||
                path.endsWith("moduleconfig.xml") ||
                path.endsWith("info.xml") ||
                path.endsWith("script.cs") ||
                path.endsWith("script.txt")
    }

    private fun isDocumentation(
        path: String,
        fileName: String
    ): Boolean {
        if (
            path.startsWith("docs/") ||
            path.startsWith("doc/") ||
            path.startsWith("documentation/") ||
            path.startsWith("readme/")
        ) {
            return true
        }

        if (fileName.contains("readme")) return true
        if (fileName.contains("changelog")) return true
        if (fileName.contains("changes")) return true
        if (fileName.contains("license")) return true
        if (fileName.contains("credits")) return true

        return fileName.endsWith(".txt") ||
                fileName.endsWith(".md") ||
                fileName.endsWith(".rtf") ||
                fileName.endsWith(".pdf")
    }

    private fun isSourceOrDevFile(
        path: String,
        fileName: String
    ): Boolean {
        if (
            path.startsWith("src/") ||
            path.startsWith("source/") ||
            path.startsWith("sources/")
        ) {
            return true
        }

        return fileName.endsWith(".cpp") ||
                fileName.endsWith(".c") ||
                fileName.endsWith(".h") ||
                fileName.endsWith(".hpp") ||
                fileName.endsWith(".sln") ||
                fileName.endsWith(".vcxproj") ||
                fileName.endsWith(".vcproj") ||
                fileName.endsWith(".filters") ||
                fileName.endsWith(".pdb") ||
                fileName.endsWith(".lib") ||
                fileName.endsWith(".exp")
    }

    private fun isProbableGameRootFile(path: String): Boolean {
        if (path.contains("/")) return false

        return path.endsWith(".dll") ||
                path.endsWith(".exe") ||
                path.endsWith(".asi") ||
                path == "d3d9.dll" ||
                path == "d3d11.dll" ||
                path == "dxgi.dll" ||
                path == "dinput8.dll" ||
                path == "xinput1_3.dll" ||
                path == "enblocal.ini" ||
                path == "enbseries.ini" ||
                path.startsWith("skse_loader") ||
                path.startsWith("nvse_loader") ||
                path.startsWith("obse_loader") ||
                path.startsWith("fose_loader") ||
                path.startsWith("f4se_loader")
    }
}