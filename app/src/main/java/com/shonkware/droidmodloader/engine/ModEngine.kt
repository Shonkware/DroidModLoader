package com.shonkware.droidmodloader.engine

import com.shonkware.droidmodloader.engine.build.StagingManager
import com.shonkware.droidmodloader.engine.build.DiffEngine
import com.shonkware.droidmodloader.engine.build.FileChange
import com.shonkware.droidmodloader.engine.conflict.ConflictResolver
import com.shonkware.droidmodloader.engine.data.ModStateRepository
import com.shonkware.droidmodloader.engine.install.ModInstaller
import com.shonkware.droidmodloader.engine.io.FileScanner
import com.shonkware.droidmodloader.engine.data.InstalledModRecordRepository
import com.shonkware.droidmodloader.engine.model.InstalledModRecord
import com.shonkware.droidmodloader.engine.model.FileRecord
import com.shonkware.droidmodloader.engine.model.Mod
import com.shonkware.droidmodloader.engine.model.ModFile
import com.shonkware.droidmodloader.engine.model.ModType
import com.shonkware.droidmodloader.engine.model.DeployScope
import com.shonkware.droidmodloader.engine.rules.DeployFileClassifier
import com.shonkware.droidmodloader.engine.data.DeploymentManifestRepository
import com.shonkware.droidmodloader.engine.deploy.DeploymentManager
import com.shonkware.droidmodloader.engine.deploy.DeploymentResult
import com.shonkware.droidmodloader.engine.data.GameDeploymentConfigRepository
import com.shonkware.droidmodloader.engine.model.GameDeploymentConfig
import java.io.File
import android.content.Context
import android.net.Uri
import com.shonkware.droidmodloader.engine.deploy.TreeUriDeploymentManager
import com.shonkware.droidmodloader.engine.data.PluginListRepository
import com.shonkware.droidmodloader.engine.model.PluginEntry
import com.shonkware.droidmodloader.engine.plugins.PluginDiscovery
import com.shonkware.droidmodloader.engine.data.PluginOutputRepository
import com.shonkware.droidmodloader.engine.index.ModContentIndex
import com.shonkware.droidmodloader.engine.index.ModContentIndexer
import com.shonkware.droidmodloader.engine.install.PreparedArchiveInstaller
import com.shonkware.droidmodloader.engine.install.PreparedArchiveInstall
import com.shonkware.droidmodloader.engine.install.InstallerSelection
import com.shonkware.droidmodloader.engine.index.ModContentCategory
import com.shonkware.droidmodloader.engine.index.ModFilePreview
import com.shonkware.droidmodloader.engine.index.ModFilePreviewEntry
import com.shonkware.droidmodloader.engine.index.ModFilePreviewStatus
import com.shonkware.droidmodloader.engine.plugins.DataFolderPluginScanner
import com.shonkware.droidmodloader.engine.plugins.GamePluginRules
import com.shonkware.droidmodloader.engine.index.ModFileFolderSummary
import com.shonkware.droidmodloader.engine.overwrite.OverwriteEntry
import com.shonkware.droidmodloader.engine.overwrite.OverwriteScanner
import com.shonkware.droidmodloader.engine.baseline.DataBaselineFileRecord
import com.shonkware.droidmodloader.engine.baseline.DataBaselineRepository
import com.shonkware.droidmodloader.engine.baseline.DataBaselineSnapshot
import com.shonkware.droidmodloader.engine.overwrite.OverwriteScanResult
import com.shonkware.droidmodloader.engine.deploy.DeploymentTargetIdentity
import java.security.MessageDigest

data class UninstallResult(
    val removed: Boolean,
    val removedModId: String,
    val addCount: Int,
    val removeCount: Int,
    val updateCount: Int
)

class ModEngine(
    private val appContext: Context,
    private val tempDir: File,
    private val modsDir: File,
    private val stagingDir: File,
    private val stateFile: File,
    private val deploymentManifestFile: File,
    private val deployRootDir: File,
    private val gameConfigFile: File,
    private val pluginListFile: File,
    private val pluginsTxtFile: File,
    private val loadorderTxtFile: File

) {

    private val modInstaller = ModInstaller(tempDir, modsDir)
    private val resolver = ConflictResolver()
    private val stagingManager = StagingManager(stagingDir)
    private val stateRepository = ModStateRepository(stateFile)
    private val installedModRecordRepository = InstalledModRecordRepository()
    private val deployFileClassifier = DeployFileClassifier()
    private val deploymentManifestRepository = DeploymentManifestRepository(deploymentManifestFile)
    private val deploymentManager = DeploymentManager(deployRootDir)
    private val gameDeploymentConfigRepository = GameDeploymentConfigRepository(gameConfigFile)
    private val pluginListRepository = PluginListRepository(pluginListFile)
    private val pluginDiscovery = PluginDiscovery()
    private val modContentIndexer = ModContentIndexer()

    private val overwriteScanner = OverwriteScanner(appContext)

    private val preparedArchiveInstaller = PreparedArchiveInstaller(
        tempDir = tempDir,
        modsDir = modsDir
    )
    private val pluginOutputRepository = PluginOutputRepository(
        pluginsTxtFile = pluginsTxtFile,
        loadorderTxtFile = loadorderTxtFile
    )
    private val dataFolderPluginScanner = DataFolderPluginScanner(appContext)
    private val gamePluginRules = GamePluginRules()



    fun installArchive(archive: File, priority: Int, enabled: Boolean = true): Mod {
        val extractedDir = modInstaller.installArchive(archive)
        return buildModFromInstalledFolder(extractedDir, priority, enabled)
    }

    fun buildModFromInstalledFolder(
        modDir: File,
        priority: Int,
        enabled: Boolean = true
    ): Mod {
        return Mod(
            id = modDir.name,
            name = modDir.name,
            installPath = modDir.absolutePath,
            enabled = enabled,
            priority = priority,
            modType = detectModType(modDir)
        )
    }

    fun scanMod(mod: Mod): List<ModFile> {
        val modDir = File(mod.installPath)
        val scanner = FileScanner()
        scanner.scanDirectory(modDir, modDir, mod.name)

        return convertScannerResultsToModFiles(
            modId = mod.id,
            sourceModName = mod.name,
            fileMap = scanner.getFileMap()
        )
    }

    fun scanMods(mods: List<Mod>): List<ModFile> {
        val allModFiles = mutableListOf<ModFile>()

        for (mod in mods) {
            allModFiles.addAll(scanMod(mod))
        }

        return allModFiles
    }

    fun resolve(mods: List<Mod>): List<FileRecord> {
        val modFiles = scanMods(mods)
        val deployableModFiles = filterDeployableModFiles(modFiles)
        return resolver.resolve(mods, deployableModFiles)
    }

    fun rebuildStaging(mods: List<Mod>): List<FileRecord> {
        val winningRecords = resolve(mods)
        stagingManager.rebuildStaging(winningRecords)
        return winningRecords
    }

    fun saveMods(mods: List<Mod>) {
        stateRepository.saveMods(mods)
    }

    fun loadMods(): List<Mod> {
        return stateRepository.loadMods()
    }

    fun getInstalledModsFromFolders(): List<Mod> {
        val modDirs = modsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

        return modDirs.mapIndexed { index, modDir ->
            Mod(
                id = modDir.name,
                name = modDir.name,
                installPath = modDir.absolutePath,
                enabled = true,
                priority = index + 1,
                modType = detectModType(modDir)
            )
        }
    }

    fun rebuildStagingFromInstalledMods(): List<FileRecord> {
        val mods = getInstalledModsFromFolders()
        return rebuildStaging(mods)
    }

    fun saveInstalledModsFromFolders(): List<Mod> {
        val mods = getInstalledModsFromFolders()
        saveMods(mods)
        return mods
    }

    fun getCurrentMods(): List<Mod> {
        val savedMods = loadMods().sortedBy { it.priority }
        return if (savedMods.isNotEmpty()) {
            savedMods
        } else {
            getInstalledModsFromFolders()
        }
    }

    fun saveCurrentMods(mods: List<Mod>) {
        saveMods(normalizeModPriorities(mods))
    }

    fun rebuildStagingFromCurrentState(): List<FileRecord> {
        val mods = getCurrentMods()
        return rebuildStaging(mods)
    }

    fun uninstallModAndApplyDiff(modId: String): UninstallResult {
        val currentMods = getCurrentMods().sortedBy { it.priority }
        val modToRemove = currentMods.firstOrNull { it.id == modId }
            ?: return UninstallResult(
                removed = false,
                removedModId = modId,
                addCount = 0,
                removeCount = 0,
                updateCount = 0
            )

        val oldWinningRecords = resolve(currentMods)

        val remainingMods = currentMods
            .filterNot { it.id == modId }
            .mapIndexed { index, mod ->
                mod.copy(priority = index + 1)
            }

        saveCurrentMods(remainingMods)

        val newWinningRecords = resolve(remainingMods)

        val diffEngine = DiffEngine()
        val changes = diffEngine.diff(oldWinningRecords, newWinningRecords)

        stagingManager.applyChanges(changes)

        val modDir = File(modToRemove.installPath)
        if (modDir.exists()) {
            modDir.deleteRecursively()
        }

        return UninstallResult(
            removed = true,
            removedModId = modId,
            addCount = changes.count { it is FileChange.Add },
            removeCount = changes.count { it is FileChange.Remove },
            updateCount = changes.count { it is FileChange.Update }
        )
    }

    fun resetAllAppData(importsDir: File): Boolean {
        return try {
            if (tempDir.exists()) tempDir.deleteRecursively()
            if (modsDir.exists()) modsDir.deleteRecursively()
            if (stagingDir.exists()) stagingDir.deleteRecursively()
            if (importsDir.exists()) importsDir.deleteRecursively()
            if (stateFile.exists()) stateFile.delete()
            if (gameConfigFile.exists()) {
                gameConfigFile.delete()
            }

            if (pluginListFile.exists()) {
                pluginListFile.delete()
            }

            if (pluginsTxtFile.exists()) {
                pluginsTxtFile.delete()
            }

            if (loadorderTxtFile.exists()) {
                loadorderTxtFile.delete()
            }
            if (deployRootDir.exists()) {
                deployRootDir.deleteRecursively()
            }

            if (deploymentManifestFile.exists()) {
                deploymentManifestFile.delete()
            }

            if (deploymentManifestFile.parentFile?.exists() == true) {
                deploymentManifestFile.parentFile?.listFiles()
                    ?.filter {
                        (it.name.startsWith("deployment_manifest") && it.extension == "json") ||
                                (it.name.startsWith("data_baseline") && it.extension == "json")
                    }
                    ?.forEach { it.delete() }
            }

            tempDir.mkdirs()
            modsDir.mkdirs()
            stagingDir.mkdirs()
            stateFile.parentFile?.mkdirs()

            true
        } catch (e: Exception) {
            false
        }
    }

    fun hasSavedState(): Boolean {
        return stateFile.exists() && stateFile.readText().isNotBlank()
    }

    fun getCurrentModSummary(): Triple<Int, Int, Boolean> {
        val mods = getCurrentMods()
        val installedCount = mods.size
        val enabledCount = mods.count { it.enabled }
        val hasSaved = hasSavedState()

        return Triple(installedCount, enabledCount, hasSaved)
    }

    private fun detectModType(modDir: File): ModType {
        val allPaths = modDir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(modDir).path.lowercase().replace("\\", "/") }
            .toList()

        val hasLooseGameFiles = allPaths.any {
            it.startsWith("data/") ||
                    it.startsWith("meshes/") ||
                    it.startsWith("textures/") ||
                    it.startsWith("scripts/") ||
                    it.startsWith("interface/")
        }

        val hasArchiveFiles = allPaths.any {
            it.endsWith(".esp") || it.endsWith(".esm") || it.endsWith(".bsa")
        }

        return when {
            hasLooseGameFiles && hasArchiveFiles -> ModType.MIXED
            hasLooseGameFiles -> ModType.LOOSE
            else -> ModType.ARCHIVE
        }
    }

    private fun convertScannerResultsToModFiles(
        modId: String,
        sourceModName: String,
        fileMap: Map<String, List<com.shonkware.droidmodloader.engine.io.FileInfo>>): List<ModFile> {
        val results = mutableListOf<ModFile>()

        for ((_, infos) in fileMap) {
            for (info in infos) {
                results.add(
                    ModFile(
                        modId = modId,
                        sourceModName = sourceModName,
                        originalPath = info.originalPath,
                        normalizedPath = info.normalizedPath,
                        hash = info.hash
                    )
                )
            }
        }

        return results
    }

    private fun writeInstalledModRecord(
        modDir: File,
        sourceType: String,
        sourceArchiveName: String?) {
        val record = InstalledModRecord(
            modId = modDir.name,
            displayName = modDir.name,
            installPath = modDir.absolutePath,
            sourceType = sourceType,
            sourceArchiveName = sourceArchiveName,
            installedAtEpochMillis = System.currentTimeMillis()
        )

        installedModRecordRepository.saveRecord(modDir, record)
    }
    fun installArchiveWithRecord(
        archive: File,
        priority: Int,
        enabled: Boolean = true,
        sourceType: String = "imported_zip"): Mod {
        val extractedDir = modInstaller.installArchive(archive)
        writeInstalledModRecord(
            modDir = extractedDir,
            sourceType = sourceType,
            sourceArchiveName = archive.name
        )
        return buildModFromInstalledFolder(extractedDir, priority, enabled)
    }

    fun registerExistingInstalledFolderWithRecord(
        modDir: File,
        priority: Int,
        enabled: Boolean = true,
        sourceType: String): Mod {
        writeInstalledModRecord(
            modDir = modDir,
            sourceType = sourceType,
            sourceArchiveName = null
        )
        return buildModFromInstalledFolder(modDir, priority, enabled)
    }

    fun loadInstalledModRecord(mod: Mod): InstalledModRecord? {
        val modDir = File(mod.installPath)
        return installedModRecordRepository.loadRecord(modDir)
    }

    fun loadInstalledModRecords(mods: List<Mod>): Map<String, InstalledModRecord> {
        val results = mutableMapOf<String, InstalledModRecord>()

        for (mod in mods) {
            val record = loadInstalledModRecord(mod)
            if (record != null) {
                results[mod.id] = record
            }
        }

        return results
    }

    fun getDeployScopeForPath(normalizedPath: String): DeployScope {
        return deployFileClassifier.classify(normalizedPath)
    }

    fun classifyModFiles(modFiles: List<ModFile>): Map<DeployScope, List<ModFile>> {
        return modFiles.groupBy { deployFileClassifier.classify(it.normalizedPath) }
    }

    fun filterDeployableModFiles(modFiles: List<ModFile>): List<ModFile> {
        return modFiles.filter {
            val scope = deployFileClassifier.classify(it.normalizedPath)
            deployFileClassifier.isDeployableToCurrentStaging(scope)
        }
    }

    fun getCurrentWinningRecords(): List<FileRecord> {
        return resolve(getCurrentMods())
    }

    fun deployCurrentState(): DeploymentResult {
        val oldManifest = deploymentManifestRepository.load()
        val newWinningRecords = getCurrentWinningRecords()

        val (newManifest, result) = deploymentManager.deploy(oldManifest, newWinningRecords)
        deploymentManifestRepository.save(newManifest)

        return result
    }

    fun clearDeploymentManifest() {
        deploymentManifestRepository.clear()
    }

    fun saveGameDeploymentConfigs(configs: List<GameDeploymentConfig>) {
        gameDeploymentConfigRepository.save(configs)
    }

    fun loadGameDeploymentConfigs(): List<GameDeploymentConfig> {
        return gameDeploymentConfigRepository.load()
    }

    fun getGameDeploymentConfig(gameId: String): GameDeploymentConfig? {
        return loadGameDeploymentConfigs().firstOrNull { it.gameId == gameId }
    }

    fun validateTargetDataPath(path: String): Boolean {
        if (path.isBlank()) return false

        val target = File(path)
        val normalized = path.lowercase()

        if (normalized.contains("android/data/com.shonkware.droidmodloader")) return false
        if (normalized.endsWith("/")) return true

        return target.exists() || target.parentFile?.exists() == true
    }
    fun deployForGame(gameId: String): DeploymentResult {
        val config = getGameDeploymentConfig(gameId)

        val effectiveManifestFile = getEffectiveDeploymentManifestFile(gameId)
        val effectiveManifestRepository = DeploymentManifestRepository(effectiveManifestFile)
        val oldManifest = effectiveManifestRepository.load()
        val newWinningRecords = getCurrentWinningRecords()

        val usingTreeUri =
            config != null &&
                    config.realDeployEnabled &&
                    !config.targetTreeUri.isNullOrBlank()

        val (newManifest, result) = if (usingTreeUri) {
            val treeUri = Uri.parse(config!!.targetTreeUri)
            val treeDeploymentManager = TreeUriDeploymentManager(
                context = appContext,
                contentResolver = appContext.contentResolver,
                treeUri = treeUri
            )
            treeDeploymentManager.deploy(oldManifest, newWinningRecords)
        } else {
            val effectiveDeployRoot = if (
                config != null &&
                config.realDeployEnabled &&
                validateTargetDataPath(config.targetDataPath)
            ) {
                File(config.targetDataPath)
            } else {
                deployRootDir
            }

            val effectiveDeploymentManager = DeploymentManager(effectiveDeployRoot)
            effectiveDeploymentManager.deploy(oldManifest, newWinningRecords)
        }

        effectiveManifestRepository.save(newManifest)
        return result
    }

    private fun getEffectiveDeploymentManifestFile(gameId: String): File {
        return File(
            deploymentManifestFile.parentFile,
            buildTargetScopedFileName("deployment_manifest", gameId)
        )
    }

    fun discoverPluginsFromCurrentMods(): List<PluginEntry> {
        val winningRecords = getCurrentWinningRecords()
        return pluginDiscovery.discoverPluginsFromWinningRecords(winningRecords)
    }

    fun saveDiscoveredPlugins(): List<PluginEntry> {
        val plugins = discoverPluginsFromCurrentMods()
        pluginListRepository.save(plugins)
        return plugins
    }

    fun loadPlugins(): List<PluginEntry> {
        return pluginListRepository.load()
    }

    fun getCurrentPlugins(): List<PluginEntry> {
        val saved = loadPlugins().sortedBy { it.priority }
        return if (saved.isNotEmpty()) {
            saved
        } else {
            discoverPluginsFromCurrentMods()
        }
    }

    fun clearPluginList() {
        pluginListRepository.clear()
    }

    fun savePlugins(plugins: List<PluginEntry>) {
        pluginListRepository.save(plugins.sortedBy { it.priority })
    }

    fun saveCurrentPlugins(plugins: List<PluginEntry>) {
        savePlugins(plugins)
    }

    fun normalizePluginPriorities(plugins: List<PluginEntry>): List<PluginEntry> {
        return plugins.mapIndexed { index, plugin ->
            plugin.copy(priority = index + 1)
        }
    }

    fun buildPluginsTxt(plugins: List<PluginEntry>): String {
        return plugins
            .sortedBy { it.priority }
            .filter { it.enabled }
            .joinToString(separator = "\n") { it.pluginName }
    }

    fun buildLoadorderTxt(plugins: List<PluginEntry>): String {
        return plugins
            .sortedBy { it.priority }
            .joinToString(separator = "\n") { it.pluginName }
    }

    fun exportCurrentPluginOutputs(): Pair<String, String> {
        val plugins = getCurrentPlugins().sortedBy { it.priority }

        val pluginsTxt = buildPluginsTxt(plugins)
        val loadorderTxt = buildLoadorderTxt(plugins)

        pluginOutputRepository.savePluginsTxt(pluginsTxt)
        pluginOutputRepository.saveLoadorderTxt(loadorderTxt)

        return Pair(pluginsTxt, loadorderTxt)
    }

    fun readExportedPluginsTxt(): String {
        return pluginOutputRepository.readPluginsTxt()
    }

    fun readExportedLoadorderTxt(): String {
        return pluginOutputRepository.readLoadorderTxt()
    }

    fun normalizeModPriorities(mods: List<Mod>): List<Mod> {
        return mods.mapIndexed { index, mod ->
            mod.copy(priority = index + 1)
        }
    }
    fun indexModContent(mod: Mod): ModContentIndex {
        return modContentIndexer.indexMod(mod)
    }

    fun indexCurrentModContent(): Map<String, ModContentIndex> {
        return getCurrentMods().associate { mod ->
            mod.id to modContentIndexer.indexMod(mod)
        }
    }

    fun prepareArchiveInstall(archive: File): PreparedArchiveInstall {
        return preparedArchiveInstaller.prepare(archive)
    }

    fun finalizePreparedArchiveInstall(
        prepared: PreparedArchiveInstall,
        selectedOptionIds: Set<String>,
        priority: Int,
        enabled: Boolean = true,
        sourceType: String = "imported_archive"
    ): Mod {
        val finalDir = preparedArchiveInstaller.finalizeInstall(
            prepared = prepared,
            selection = InstallerSelection(selectedOptionIds)
        )

        writeInstalledModRecord(
            modDir = finalDir,
            sourceType = sourceType,
            sourceArchiveName = prepared.archiveName
        )

        return buildModFromInstalledFolder(
            modDir = finalDir,
            priority = priority,
            enabled = enabled
        )
    }

    fun cancelPreparedArchiveInstall(prepared: PreparedArchiveInstall) {
        preparedArchiveInstaller.cancel(prepared)
    }

    fun buildModFilePreview(mod: Mod): ModFilePreview {
        val index = indexModContent(mod)
        val winningRecords = getCurrentWinningRecords()
        val winningByPath = winningRecords.associateBy { it.normalizedPath }

        val entries = index.entries.map { entry ->
            val winner = winningByPath[entry.normalizedPath]

            val status = when {
                entry.category == ModContentCategory.PLUGIN -> ModFilePreviewStatus.PLUGIN
                entry.category == ModContentCategory.ARCHIVE -> ModFilePreviewStatus.ARCHIVE
                entry.category == ModContentCategory.CONFIG -> ModFilePreviewStatus.CONFIG
                entry.category == ModContentCategory.SETUP_ONLY -> ModFilePreviewStatus.SETUP_ONLY
                entry.category == ModContentCategory.DOCUMENTATION -> ModFilePreviewStatus.DOCUMENTATION
                entry.category == ModContentCategory.OPTIONAL_MODULE -> ModFilePreviewStatus.OPTIONAL
                entry.category == ModContentCategory.IGNORED -> ModFilePreviewStatus.IGNORED
                entry.category == ModContentCategory.UNKNOWN -> ModFilePreviewStatus.UNKNOWN

                entry.isDeployable && winner == null ->
                    ModFilePreviewStatus.NOT_DEPLOYED

                entry.isDeployable && winner != null && winner.winningModId == mod.id ->
                    ModFilePreviewStatus.WINNING

                entry.isDeployable && winner != null && winner.winningModId != mod.id ->
                    ModFilePreviewStatus.OVERWRITTEN

                else -> ModFilePreviewStatus.UNKNOWN
            }

            ModFilePreviewEntry(
                normalizedPath = entry.normalizedPath,
                originalPath = entry.originalPath,
                status = status,
                reason = entry.reason,
                winningModName = winner?.winningModName
            )
        }
        val sortedEntries = entries.sortedBy { it.normalizedPath }

        return ModFilePreview(
            modId = mod.id,
            modName = mod.name,
            entries = sortedEntries,
            folderSummaries = buildFolderSummaries(sortedEntries)
        )
    }

    fun scanDataFolderPlugins(gameId: String): List<PluginEntry> {
        val config = getGameDeploymentConfig(gameId) ?: return emptyList()

        val foundNames = when {
            config.realDeployEnabled && !config.targetTreeUri.isNullOrBlank() -> {
                dataFolderPluginScanner.scanTreeUriDataFolder(config.targetTreeUri)
            }

            config.realDeployEnabled && validateTargetDataPath(config.targetDataPath) -> {
                dataFolderPluginScanner.scanLocalDataFolder(File(config.targetDataPath))
            }

            else -> {
                dataFolderPluginScanner.scanLocalDataFolder(deployRootDir)
            }
        }

        return foundNames.mapIndexed { index, pluginName ->
            val rule = gamePluginRules.findRule(gameId, pluginName)
            val pluginType = detectPluginType(pluginName)

            PluginEntry(
                normalizedPath = pluginName.lowercase(),
                pluginName = pluginName,
                sourceModId = rule?.sourceType ?: "unmanaged_data",
                sourceModName = when (rule?.sourceType) {
                    "base_game" -> "Base Game"
                    "official_dlc" -> "Official DLC"
                    else -> "Unmanaged Data Folder"
                },
                enabled = rule?.defaultEnabled ?: false,
                priority = rule?.orderRank ?: Int.MAX_VALUE - index,
                pluginType = pluginType,
                sourceType = rule?.sourceType ?: "unmanaged_data",
                locked = rule?.locked ?: false,
                filePresentInDataFolder = true
            )
        }
    }

    fun applyModPriorityOrder(orderedModIds: List<String>) {
        val current = getCurrentMods().sortedBy { it.priority }
        val byId = current.associateBy { it.id }

        val reordered = orderedModIds.mapNotNull { byId[it] }

        if (reordered.size != current.size) {
            throw IllegalArgumentException(
                "Could not apply mod order: expected ${current.size} mods but got ${reordered.size}."
            )
        }

        val currentIds = current.map { it.id }.toSet()
        val orderedIds = orderedModIds.toSet()

        if (currentIds != orderedIds) {
            throw IllegalArgumentException("Could not apply mod order: ordered mod IDs do not match current mods.")
        }

        saveCurrentMods(
            reordered.mapIndexed { index, mod ->
                mod.copy(priority = index + 1)
            }
        )
    }

    fun applyPluginPriorityOrder(orderedPluginPaths: List<String>) {
        val current = getCurrentPlugins().sortedBy { it.priority }
        val byPath = current.associateBy { it.normalizedPath }

        val lockedPlugins = current
            .filter { it.locked }
            .sortedBy { it.priority }

        val lockedPaths = lockedPlugins.map { it.normalizedPath }.toSet()

        val orderedUnlockedPaths = orderedPluginPaths.filterNot { it in lockedPaths }

        val currentUnlocked = current.filterNot { it.locked }
        val currentUnlockedPaths = currentUnlocked.map { it.normalizedPath }.toSet()

        if (orderedUnlockedPaths.toSet() != currentUnlockedPaths) {
            throw IllegalArgumentException("Could not apply plugin order: ordered plugin paths do not match current unlocked plugins.")
        }

        val reorderedUnlocked = orderedUnlockedPaths.mapNotNull { byPath[it] }

        if (reorderedUnlocked.size != currentUnlocked.size) {
            throw IllegalArgumentException(
                "Could not apply plugin order: expected ${currentUnlocked.size} unlocked plugins but got ${reorderedUnlocked.size}."
            )
        }

        val reordered = lockedPlugins + reorderedUnlocked

        saveCurrentPlugins(
            reordered.mapIndexed { index, plugin ->
                plugin.copy(priority = index + 1)
            }
        )
    }

    fun scanOverwriteFiles(gameId: String): OverwriteScanResult {
        val baselineRepository = getDataBaselineRepository(gameId)
        val baseline = baselineRepository.load()

        if (baseline == null) {
            return OverwriteScanResult(
                baselineExists = false,
                entries = emptyList(),
                message = "No Data baseline exists yet. Index the current target Data folder first."
            )
        }

        val currentTargetFiles = scanTargetDataFiles(gameId)
        val baselineByPath = baseline.files.associateBy { it.normalizedPath }

        val manifestRepository = DeploymentManifestRepository(
            getEffectiveDeploymentManifestFile(gameId)
        )

        val deployedPaths = manifestRepository.load()
            .map { it.normalizedPath }
            .toSet()

        val entries = currentTargetFiles
            .filterNot { it.normalizedPath in deployedPaths }
            .filterNot { shouldIgnoreOverwritePath(it.normalizedPath) }
            .mapNotNull { currentFile ->
                val baselineFile = baselineByPath[currentFile.normalizedPath]

                when {
                    baselineFile == null -> {
                        OverwriteEntry(
                            normalizedPath = currentFile.normalizedPath,
                            reason = getOverwriteReason(currentFile.normalizedPath),
                            sizeBytes = currentFile.sizeBytes,
                            modifiedEpochMillis = currentFile.modifiedEpochMillis,
                            status = "NEW"
                        )
                    }

                    hasBaselineFileChanged(baselineFile, currentFile) -> {
                        OverwriteEntry(
                            normalizedPath = currentFile.normalizedPath,
                            reason = "File changed after baseline was created",
                            sizeBytes = currentFile.sizeBytes,
                            modifiedEpochMillis = currentFile.modifiedEpochMillis,
                            status = "CHANGED"
                        )
                    }

                    else -> null
                }
            }
            .sortedBy { it.normalizedPath }

        return OverwriteScanResult(
            baselineExists = true,
            entries = entries,
            message = if (entries.isEmpty()) {
                "Overwrite is clean. No new or changed untracked files detected."
            } else {
                "Detected ${entries.size} overwrite candidate files."
            }
        )
    }

    private fun buildFolderSummaries(
        entries: List<ModFilePreviewEntry>
    ): List<ModFileFolderSummary> {
        val grouped = entries.groupBy { entry ->
            val path = entry.normalizedPath
            if (path.contains("/")) {
                path.substringBefore("/") + "/"
            } else {
                path
            }
        }

        return grouped.map { (topLevelPath, groupEntries) ->
            val winningCount = groupEntries.count { it.status == ModFilePreviewStatus.WINNING }
            val overwrittenCount = groupEntries.count { it.status == ModFilePreviewStatus.OVERWRITTEN }
            val notDeployedCount = groupEntries.count { it.status == ModFilePreviewStatus.NOT_DEPLOYED }
            val pluginCount = groupEntries.count { it.status == ModFilePreviewStatus.PLUGIN }
            val archiveCount = groupEntries.count { it.status == ModFilePreviewStatus.ARCHIVE }
            val configCount = groupEntries.count { it.status == ModFilePreviewStatus.CONFIG }
            val setupCount = groupEntries.count { it.status == ModFilePreviewStatus.SETUP_ONLY }
            val documentationCount = groupEntries.count { it.status == ModFilePreviewStatus.DOCUMENTATION }
            val optionalCount = groupEntries.count { it.status == ModFilePreviewStatus.OPTIONAL }
            val ignoredCount = groupEntries.count { it.status == ModFilePreviewStatus.IGNORED }
            val unknownCount = groupEntries.count { it.status == ModFilePreviewStatus.UNKNOWN }

            val dominantStatus = when {
                overwrittenCount > 0 -> ModFilePreviewStatus.OVERWRITTEN
                winningCount > 0 -> ModFilePreviewStatus.WINNING
                pluginCount > 0 -> ModFilePreviewStatus.PLUGIN
                archiveCount > 0 -> ModFilePreviewStatus.ARCHIVE
                configCount > 0 -> ModFilePreviewStatus.CONFIG
                optionalCount > 0 -> ModFilePreviewStatus.OPTIONAL
                setupCount > 0 -> ModFilePreviewStatus.SETUP_ONLY
                documentationCount > 0 -> ModFilePreviewStatus.DOCUMENTATION
                ignoredCount > 0 -> ModFilePreviewStatus.IGNORED
                notDeployedCount > 0 -> ModFilePreviewStatus.NOT_DEPLOYED
                else -> ModFilePreviewStatus.UNKNOWN
            }

            ModFileFolderSummary(
                displayName = topLevelPath,
                normalizedPath = topLevelPath.removeSuffix("/"),
                isTopLevelFile = !topLevelPath.endsWith("/"),
                totalCount = groupEntries.size,
                winningCount = winningCount,
                overwrittenCount = overwrittenCount,
                notDeployedCount = notDeployedCount,
                pluginCount = pluginCount,
                archiveCount = archiveCount,
                configCount = configCount,
                setupCount = setupCount,
                documentationCount = documentationCount,
                optionalCount = optionalCount,
                ignoredCount = ignoredCount,
                unknownCount = unknownCount,
                dominantStatus = dominantStatus
            )
        }.sortedWith(
            compareBy<ModFileFolderSummary> { it.isTopLevelFile }
                .thenBy { it.displayName.lowercase() }
        )
    }

    private fun detectPluginType(pluginName: String): String {
        val lower = pluginName.lowercase()
        return when {
            lower.endsWith(".esm") -> "ESM"
            lower.endsWith(".esl") -> "ESL"
            else -> "ESP"
        }
    }

    private fun isOfficialGameDataFile(gameId: String, normalizedPath: String): Boolean {
        val lower = normalizedPath.lowercase()

        return when (gameId) {
            "skyrim_le" -> lower in setOf(
                "skyrim.esm",
                "update.esm",
                "dawnguard.esm",
                "hearthfires.esm",
                "dragonborn.esm",

                "skyrim - animations.bsa",
                "skyrim - interface.bsa",
                "skyrim - meshes.bsa",
                "skyrim - misc.bsa",
                "skyrim - shaders.bsa",
                "skyrim - sounds.bsa",
                "skyrim - textures.bsa",
                "skyrim - voices.bsa",
                "skyrim - voicesextra.bsa",

                "update.bsa",
                "dawnguard.bsa",
                "hearthfires.bsa",
                "dragonborn.bsa",

                "highrestexturepack01.esp",
                "highrestexturepack02.esp",
                "highrestexturepack03.esp",
                "highrestexturepack01.bsa",
                "highrestexturepack02.bsa",
                "highrestexturepack03.bsa"
            )

            "fallout_nv" -> lower in setOf(
                "falloutnv.esm",
                "deadmoney.esm",
                "honesthearts.esm",
                "oldworldblues.esm",
                "lonesomeroad.esm",
                "gunrunnersarsenal.esm",
                "classicpack.esm",
                "mercenarypack.esm",
                "tribalpack.esm",
                "caravanpack.esm"
            )

            "oblivion" -> lower in setOf(
                "oblivion.esm"
            )

            "fallout_4" -> lower in setOf(
                "fallout4.esm",
                "dlcrobot.esm",
                "dlcworkshop01.esm",
                "dlccoast.esm",
                "dlcworkshop02.esm",
                "dlcworkshop03.esm",
                "dlcnukaworld.esm"
            )

            else -> false
        }
    }

    private fun getDataBaselineFile(gameId: String): File {
        return File(
            deploymentManifestFile.parentFile,
            buildTargetScopedFileName("data_baseline", gameId)
        )
    }

    private fun getDataBaselineRepository(gameId: String): DataBaselineRepository {
        return DataBaselineRepository(getDataBaselineFile(gameId))
    }

    fun hasDataBaseline(gameId: String): Boolean {
        return getDataBaselineRepository(gameId).exists()
    }

    fun rebuildDataBaseline(gameId: String): DataBaselineSnapshot {
        val targetFiles = scanTargetDataFiles(gameId)
        val config = getGameDeploymentConfig(gameId)

        val targetDescription = when {
            config != null && config.realDeployEnabled && !config.targetTreeUri.isNullOrBlank() ->
                config.targetTreeUri ?: ""

            config != null && config.realDeployEnabled && validateTargetDataPath(config.targetDataPath) ->
                config.targetDataPath

            else ->
                deployRootDir.absolutePath
        }

        val manifestRepository = DeploymentManifestRepository(
            getEffectiveDeploymentManifestFile(gameId)
        )

        val deployedPaths = manifestRepository.load()
            .map { it.normalizedPath }
            .toSet()

        val baselineFiles = targetFiles
            .filterNot { it.normalizedPath in deployedPaths }
            .filterNot { shouldIgnoreOverwritePath(it.normalizedPath) }
            .map {
                DataBaselineFileRecord(
                    normalizedPath = it.normalizedPath,
                    sizeBytes = it.sizeBytes,
                    modifiedEpochMillis = it.modifiedEpochMillis
                )
            }
            .sortedBy { it.normalizedPath }

        val snapshot = DataBaselineSnapshot(
            gameId = gameId,
            createdAtEpochMillis = System.currentTimeMillis(),
            targetDescription = targetDescription,
            files = baselineFiles
        )

        getDataBaselineRepository(gameId).save(snapshot)
        return snapshot
    }

    private fun scanTargetDataFiles(gameId: String): List<com.shonkware.droidmodloader.engine.overwrite.TargetDataFileEntry> {
        val config = getGameDeploymentConfig(gameId)

        return when {
            config != null && config.realDeployEnabled && !config.targetTreeUri.isNullOrBlank() -> {
                overwriteScanner.scanTreeUriDataFolder(config.targetTreeUri)
            }

            config != null && config.realDeployEnabled && validateTargetDataPath(config.targetDataPath) -> {
                overwriteScanner.scanLocalDataFolder(File(config.targetDataPath))
            }

            else -> {
                overwriteScanner.scanLocalDataFolder(deployRootDir)
            }
        }
    }

    private fun hasBaselineFileChanged(
        baseline: DataBaselineFileRecord,
        current: com.shonkware.droidmodloader.engine.overwrite.TargetDataFileEntry
    ): Boolean {
        if (baseline.sizeBytes != null && current.sizeBytes != null && baseline.sizeBytes != current.sizeBytes) {
            return true
        }

        // SAF lastModified values are not always reliable, so only treat modified time as a signal
        // when both values are present and the size also changed elsewhere.
        return false
    }

    private fun shouldIgnoreOverwritePath(normalizedPath: String): Boolean {
        val lower = normalizedPath.lowercase()

        return lower == "plugins.txt" ||
                lower == "loadorder.txt" ||
                lower.endsWith(".bak") ||
                lower.endsWith(".tmp") ||
                lower.endsWith(".old")
    }

    private fun getOverwriteReason(normalizedPath: String): String {
        val lower = normalizedPath.lowercase()

        return when {
            lower.endsWith(".log") ->
                "Generated log file"

            lower.startsWith("skse/plugins/") ||
                    lower.startsWith("skse/plugins/") ||
                    lower.startsWith("nvse/plugins/") ||
                    lower.startsWith("obse/plugins/") ||
                    lower.startsWith("fose/plugins/") ||
                    lower.startsWith("f4se/plugins/") ->
                "Script extender generated file"

            lower.contains("cache") ->
                "Possible generated cache file"

            lower.endsWith(".ini") ->
                "Generated or externally modified config file"

            lower.endsWith(".esp") ||
                    lower.endsWith(".esm") ||
                    lower.endsWith(".esl") ->
                "New plugin file found outside Droid Mod Loader deployment"

            else ->
                "File was created or changed after the Data baseline was indexed"
        }
    }
    private fun getDeploymentTargetIdentity(gameId: String): DeploymentTargetIdentity {
        val config = getGameDeploymentConfig(gameId)

        return when {
            config != null &&
                    config.realDeployEnabled &&
                    !config.targetTreeUri.isNullOrBlank() -> {
                DeploymentTargetIdentity(
                    gameId = gameId,
                    mode = "tree_uri",
                    target = config.targetTreeUri ?: ""
                )
            }

            config != null &&
                    config.realDeployEnabled &&
                    validateTargetDataPath(config.targetDataPath) -> {
                DeploymentTargetIdentity(
                    gameId = gameId,
                    mode = "real_path",
                    target = config.targetDataPath
                )
            }

            else -> {
                DeploymentTargetIdentity(
                    gameId = gameId,
                    mode = "simulated",
                    target = deployRootDir.absolutePath
                )
            }
        }
    }

    private fun hashManifestKey(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))

        return digest
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    private fun buildTargetScopedFileName(
        prefix: String,
        gameId: String,
        extension: String = "json"
    ): String {
        val identity = getDeploymentTargetIdentity(gameId)
        val hash = hashManifestKey(identity.stableKey())

        return "${prefix}_${identity.gameId}_${identity.mode}_$hash.$extension"
    }

    fun getDeploymentTargetDebugSummary(gameId: String): String {
        val identity = getDeploymentTargetIdentity(gameId)
        val manifestName = buildTargetScopedFileName("deployment_manifest", gameId)
        val baselineName = buildTargetScopedFileName("data_baseline", gameId)

        return buildString {
            appendLine("Deployment target identity:")
            appendLine(identity.displaySummary())
            appendLine("Manifest file: $manifestName")
            appendLine("Baseline file: $baselineName")
        }
    }


}