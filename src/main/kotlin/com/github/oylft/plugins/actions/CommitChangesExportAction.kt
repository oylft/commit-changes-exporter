package com.github.oylft.plugins.actions

import com.github.oylft.plugins.Bundle
import com.intellij.openapi.ListSelection
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VfsUtilCore.copyFile
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Desktop
import java.io.File
import javax.swing.SwingUtilities


class CommitChangesExportAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.project ?: return

        val data: ListSelection<Change> = event.getData(VcsDataKeys.CHANGES_SELECTION) ?: return

        val fileList: ArrayList<VirtualFile> = ArrayList()

        for (change in data.list) {
            change.virtualFile?.let { fileList.add(it) }
        }

        if (fileList.isEmpty()) {
            Messages.showWarningDialog(
                Bundle.message("noChangesMessage"),
                Bundle.message("noChangesTitle")
            )
            return
        }

        val selectedFiles = FileChooser.chooseFiles(
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            project,
            null
        )
        if (selectedFiles.isEmpty()) {
            return
        }

        val targetDirectory = selectedFiles[0]


        val projectName = project.name

        val separator = File.separator

        val targetProjectDir = targetDirectory.path + separator + projectName + separator


        val basePath = project.basePath ?: return

        val projectBaseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return



        ApplicationManager.getApplication().runWriteAction {
            for (virtualFile in fileList) {
                try {
                    val relativeParentDir = VfsUtilCore.getRelativePath(virtualFile.parent, projectBaseDir)
                    val parentDir =
                        VfsUtil.createDirectories(targetProjectDir + relativeParentDir)
                    parentDir.findChild(virtualFile.name)?.delete(this)
                    copyFile(this, virtualFile, parentDir)
                } catch (e: Exception) {
                    thisLogger().error(e)
                }
            }

            SwingUtilities.invokeLater {
                val result: Int = Messages.showYesNoDialog(
                    Bundle.message("confirmMessage", targetProjectDir ),
                    Bundle.message("confirmTitle"),
                    Messages.getQuestionIcon()
                )
                if (result == Messages.YES) {
                    Desktop.getDesktop().open(File(targetProjectDir))
                }
            }

        }


    }


}